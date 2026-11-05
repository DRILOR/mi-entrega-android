package com.example.mientrega

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.mientrega.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// El ViewModel conecta las pantallas y la sesión local con Room.
@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(app: Application) : AndroidViewModel(app) {
    private val database = PlannerDatabase.get(app)
    private val dao = database.dao()
    private val session = app.getSharedPreferences("mi-entrega-session", Context.MODE_PRIVATE)

    private val _activeUserId = MutableStateFlow<Long?>(null)
    val activeUserId = _activeUserId.asStateFlow()
    private val _authReady = MutableStateFlow(false)
    val authReady = _authReady.asStateFlow()

    val subjects = activeUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.subjects(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val assignments = activeUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.assignments(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var busy = MutableStateFlow(false)
        private set
    var message = MutableStateFlow<String?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                val savedId = session.getLong("active_user_id", -1L)
                if (savedId > 0L && dao.userById(savedId) != null) {
                    _activeUserId.value = savedId
                } else {
                    session.edit().remove("active_user_id").apply()
                }
            } catch (_: Exception) {
                message.value = "No se pudo abrir la información local. Comprueba el almacenamiento del dispositivo."
            } finally {
                _authReady.value = true
            }
        }
    }

    fun clearMessage() { message.value = null }

    private fun startSession(userId: Long) {
        session.edit().putLong("active_user_id", userId).apply()
        _activeUserId.value = userId
    }

    fun logout() {
        if (busy.value) return
        session.edit().remove("active_user_id").apply()
        _activeUserId.value = null
        message.value = null
    }

    private fun write(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try {
                action()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (_: SQLiteConstraintException) {
                message.value = "Ese correo o nombre ya está registrado, o el registro tiene actividades asociadas."
            } catch (e: IllegalArgumentException) {
                message.value = e.message ?: "Los datos no son válidos."
            } catch (_: Exception) {
                message.value = "No se pudo guardar. Tus datos anteriores se conservan. Intenta nuevamente."
            } finally {
                busy.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmation: String) {
        val error = AuthRules.registerError(name, email, password, confirmation)
        if (error != null) { message.value = error; return }
        val normalized = AuthRules.normalizeEmail(email)
        write {
            if (dao.userByEmail(normalized) != null) {
                throw IllegalArgumentException("Ya existe una cuenta con ese correo.")
            }
            val digest = withContext(Dispatchers.Default) { PasswordHasher.create(password) }
            val newId = database.withTransaction {
                val firstAccount = dao.userCount() == 0
                val id = dao.insertUser(StudentUser(
                    displayName = name.trim(), email = normalized,
                    passwordHash = digest.hash, passwordSalt = digest.salt
                ))
                // Los datos v1 se asignan una sola vez al primer estudiante registrado.
                if (firstAccount) {
                    dao.claimLegacySubjects(id)
                    dao.claimLegacyAssignments(id)
                }
                id
            }
            startSession(newId)
        }
    }

    fun login(email: String, password: String) {
        val error = AuthRules.loginError(email, password)
        if (error != null) { message.value = error; return }
        write {
            val account = dao.userByEmail(AuthRules.normalizeEmail(email))
            val correct = account != null && withContext(Dispatchers.Default) {
                PasswordHasher.verify(password, PasswordDigest(account.passwordHash, account.passwordSalt))
            }
            if (!correct || account == null) {
                throw IllegalArgumentException("Correo o contraseña incorrectos.")
            }
            startSession(account.id)
        }
    }

    fun saveSubject(item: Subject, done: () -> Unit) {
        val id = activeUserId.value ?: return
        val error = Rules.subjectError(item.name)
        if (error != null) { message.value = error; return }
        if (subjects.value.any {
                it.ownerId == id && it.id != item.id && it.name.equals(item.name.trim(), true)
            }) {
            message.value = "Ya existe una materia con ese nombre."
            return
        }
        write(done) {
            if (item.id == 0L) {
                dao.insertSubject(item.copy(name = item.name.trim(), ownerId = id))
            } else {
                require(dao.updateSubject(item.id, id, item.name.trim(), item.color) == 1) {
                    "No se encontró la materia de tu cuenta."
                }
            }
        }
    }

    fun saveAssignment(item: Assignment, done: () -> Unit) {
        val id = activeUserId.value ?: return
        if (item.title.isBlank() || subjects.value.none { it.ownerId == id && it.id == item.subjectId }) {
            message.value = "Completa el título y selecciona una materia de tu cuenta."
            return
        }
        write(done) {
            require(dao.ownsSubject(item.subjectId, id) == 1) {
                "La materia seleccionada no pertenece a tu cuenta."
            }
            if (item.id == 0L) {
                dao.insertAssignment(item.copy(title = item.title.trim(),
                    description = item.description.trim(), ownerId = id))
            } else {
                require(dao.updateAssignment(item.id, id, item.title.trim(),
                    item.description.trim(), item.subjectId, item.type, item.dueDate,
                    item.priority, item.status) == 1) {
                    "No se encontró la actividad de tu cuenta."
                }
            }
        }
    }

    fun deleteSubject(item: Subject) {
        val id = activeUserId.value ?: return
        if (item.ownerId != id) return
        if (assignments.value.any { it.ownerId == id && it.subjectId == item.id }) {
            message.value = "Esta materia tiene actividades. Elimínalas o asígnalas a otra materia antes de borrarla."
            return
        }
        write { dao.deleteSubject(item.id, id) }
    }

    fun deleteAssignment(item: Assignment) {
        val id = activeUserId.value ?: return
        if (item.ownerId != id) return
        write { dao.deleteAssignment(item.id, id) }
    }

    fun complete(item: Assignment) {
        val id = activeUserId.value ?: return
        if (item.ownerId != id) return
        write { dao.changeStatus(item.id, id, "Completada") }
    }
}
