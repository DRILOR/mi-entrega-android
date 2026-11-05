package com.example.mientrega

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mientrega.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// El ViewModel conecta las pantallas con Room; la interfaz nunca ejecuta SQL.
class PlannerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = PlannerDatabase.get(app).dao()
    val subjects = dao.subjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assignments = dao.assignments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var busy = MutableStateFlow(false)
        private set
    var message = MutableStateFlow<String?>(null)
        private set
    fun clearMessage() { message.value = null }
    private fun write(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try { action(); onSuccess() }
            catch (e: CancellationException) { throw e }
            catch (e: SQLiteConstraintException) {
                message.value = "No se pudo guardar el cambio. Evita materias repetidas y elimina o reasigna sus actividades antes de borrar una materia."
            }
            catch (e: Exception) { message.value = "No se pudo guardar. Tus datos anteriores se conservan. Intenta nuevamente." }
            finally { busy.value = false }
        }
    }
    fun saveSubject(item: Subject, done: () -> Unit) {
        val error = Rules.subjectError(item.name)
        if (error != null) { message.value = error; return }
        if (subjects.value.any { it.id != item.id && it.name.equals(item.name.trim(), true) }) {
            message.value = "Ya existe una materia con ese nombre."; return
        }
        write(done) { dao.save(item.copy(name = item.name.trim())) }
    }
    fun saveAssignment(item: Assignment, done: () -> Unit) {
        if (item.title.isBlank() || subjects.value.none { it.id == item.subjectId }) {
            message.value = "Completa el título y selecciona una materia existente."; return
        }
        write(done) { dao.save(item.copy(title = item.title.trim(), description = item.description.trim())) }
    }
    fun deleteSubject(item: Subject) {
        if (assignments.value.any { it.subjectId == item.id }) {
            message.value = "Esta materia tiene actividades. Elimínalas o asígnalas a otra materia antes de borrarla."; return
        }
        write { dao.delete(item) }
    }
    fun deleteAssignment(item: Assignment) = write { dao.delete(item) }
    fun complete(item: Assignment) = write { dao.changeStatus(item.id, "Completada") }
}
