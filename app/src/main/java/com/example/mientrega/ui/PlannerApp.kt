package com.example.mientrega.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.mientrega.*
import com.example.mientrega.data.*
import com.example.mientrega.reminders.ReminderPolicy
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val subjectColors = listOf(Color(0xFF5650D8), Color(0xFF008B87), Color(0xFFAD5800), Color(0xFF8B48AE))
private val subjectColorNames = listOf("Índigo", "Verde", "Naranja", "Violeta")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun dateLabel(day: Long) = LocalDate.ofEpochDay(day).format(dateFormatter)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerApp(model: PlannerViewModel) {
    val currentUserId by model.activeUserId.collectAsStateWithLifecycle()
    // El StateFlow puede conservar un valor previo durante un instante tras cambiar de cuenta.
    // Nunca presentar datos de otro estudiante ni siquiera durante esa transición.
    val storedSubjects by model.subjects.collectAsStateWithLifecycle()
    val storedAssignments by model.assignments.collectAsStateWithLifecycle()
    val subjects = storedSubjects.filter { currentUserId != null && it.ownerId == currentUserId }
    val assignments = storedAssignments.filter { currentUserId != null && it.ownerId == currentUserId }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Los cambios de fecha/hora/zona se propagan desde el receptor de LocalDay.
    val today = rememberLocalDay { model.syncReminders() }
    // Solo en eventos reales de ciclo de vida, nunca durante una recomposición.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.syncReminders()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val busy by model.busy.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val mainScreen = route in listOf("home", "subjects", "assignments")
    val isSettings = route == "settings"
    val snackbar = remember { SnackbarHostState() }
    var deleteSubject by remember { mutableStateOf<Subject?>(null) }
    var deleteActivity by remember { mutableStateOf<Assignment?>(null) }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); model.clearMessage() } }
    val title = when (route) {
        "subjects" -> "Materias"
        "assignments" -> "Actividades"
        "subject/{id}" -> if ((entry?.arguments?.getLong("id") ?: 0L) == 0L) "Nueva materia" else "Editar materia"
        "assignment/{id}" -> if ((entry?.arguments?.getLong("id") ?: 0L) == 0L) "Nueva actividad" else "Editar actividad"
        "settings" -> "Ajustes"
        else -> "Mi Entrega"
    }
    Scaffold(
        topBar = {
            if (!isSettings) {
                TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }, navigationIcon = {
                    if (!mainScreen) IconButton(onClick = { nav.popBackStack() }, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver sin guardar")
                    }
                }, actions = {
                    if (route == "home") {
                        IconButton(onClick = { nav.navigate("settings") { launchSingleTop = true } }) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                        }
                    }
                    TextButton(onClick = model::logout, enabled = !busy) { Text("Salir") }
                })
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (mainScreen) NavigationBar {
                val routes = listOf("home", "subjects", "assignments")
                val labels = listOf("Inicio", "Materias", "Actividades")
                val icons = listOf(Icons.Default.Home, Icons.Default.Book, Icons.AutoMirrored.Filled.List)
                routes.forEachIndexed { index, target ->
                    NavigationBarItem(selected = route == target, onClick = {
                        nav.navigate(target) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
                    }, icon = { Icon(icons[index], contentDescription = null) }, label = { Text(labels[index]) })
                }
            }
        },
        // La pantalla de ajustes dibuja su propio Scaffold y gestiona sus insets.
        contentWindowInsets = if (isSettings) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(assignments, subjects, today, onAdd = { nav.navigate(if (subjects.isEmpty()) "subject/0" else "assignment/0") },
                    onEdit = { nav.navigate("assignment/${it.id}") })
            }
            composable("subjects") {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Button(onClick = { nav.navigate("subject/0") }, modifier = Modifier.fillMaxWidth()) { Text("+ Nueva materia") } }
                    if (subjects.isEmpty()) item { EmptyMessage("Todavía no tienes materias registradas", "Agrega una materia para organizar tus actividades.") }
                    items(subjects, key = { it.id }) { subject ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(18.dp).background(subjectColors.getOrElse(subject.color) { subjectColors[0] }, CircleShape))
                                Spacer(Modifier.width(12.dp))
                                Text(subject.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { nav.navigate("subject/${subject.id}") }) { Icon(Icons.Default.Edit, "Editar ${subject.name}") }
                                IconButton(onClick = { deleteSubject = subject }, enabled = !busy) { Icon(Icons.Default.Delete, "Eliminar ${subject.name}") }
                            }
                        }
                    }
                }
            }
            composable("assignments") {
                ActivitiesScreen(assignments, subjects, today, busy,
                    onAdd = { nav.navigate(if (subjects.isEmpty()) "subject/0" else "assignment/0") },
                    onEdit = { nav.navigate("assignment/${it.id}") },
                    onDelete = { deleteActivity = it }, onComplete = model::complete)
            }
            composable("subject/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                val id = backStack.arguments?.getLong("id") ?: 0L
                val current = subjects.firstOrNull { it.id == id }
                if (id != 0L && current == null) LoadingRecord() else SubjectForm(current, busy,
                    onCancel = { nav.popBackStack() }, onSave = { model.saveSubject(it) { nav.popBackStack() } })
            }
            composable("assignment/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                val id = backStack.arguments?.getLong("id") ?: 0L
                val current = assignments.firstOrNull { it.id == id }
                if (id != 0L && current == null) LoadingRecord() else ActivityForm(current, subjects, busy,
                    onCancel = { nav.popBackStack() }, onSave = { model.saveAssignment(it) { nav.popBackStack() } })
            }
            composable("settings") {
                SettingsScreen(model = model, onBack = { nav.popBackStack() })
            }
        }
    }
    deleteSubject?.let { subject ->
        ConfirmDelete(subject.name, onDismiss = { deleteSubject = null }, onConfirm = { model.deleteSubject(subject); deleteSubject = null })
    }
    deleteActivity?.let { activity ->
        ConfirmDelete(activity.title, onDismiss = { deleteActivity = null }, onConfirm = { model.deleteAssignment(activity); deleteActivity = null })
    }
}

@Composable
private fun LoadingRecord() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }

@Composable
private fun EmptyMessage(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SectionHeading(section: ActivitySection, count: Int) {
    Text("${section.label} ($count)", style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

@Composable
private fun ActivitiesScreen(
    assignments: List<Assignment>, subjects: List<Subject>, today: Long, busy: Boolean,
    onAdd: () -> Unit, onEdit: (Assignment) -> Unit,
    onDelete: (Assignment) -> Unit, onComplete: (Assignment) -> Unit
) {
    // El destino conserva estos valores al editar y al cambiar entre pestañas.
    // PlannerApp está asociado a key(userId), por lo que no se comparten entre cuentas.
    var subjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    val filters = ActivityFilters(subjectId, status)
    val groups = remember(assignments, today, filters) {
        ActivityOrganization.group(assignments, today, filters)
    }
    val count = groups.values.sumOf { it.size }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "add") {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text(if (subjects.isEmpty()) "+ Crear primera materia" else "+ Nueva actividad")
            }
        }
        item(key = "filters") {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Filtrar actividades", style = MaterialTheme.typography.titleMedium)
                    Choice("Materia", if (subjectId == null) "Todas las materias"
                        else subjects.firstOrNull { it.id == subjectId }?.name ?: "Materia no disponible",
                        listOf("Todas las materias") + subjects.map { it.name }) { index ->
                        subjectId = if (index == 0) null else subjects[index - 1].id
                    }
                    Choice("Estado", status ?: "Todos los estados", listOf("Todos los estados") + Rules.statuses) { index ->
                        status = if (index == 0) null else Rules.statuses[index - 1]
                    }
                    TextButton(onClick = { subjectId = null; status = null }, enabled = filters.isActive) {
                        Text("Restablecer filtros")
                    }
                    Text("$count ${if (count == 1) "actividad" else "actividades"} · Fecha ↑, prioridad alta primero",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (count == 0) {
            item(key = "empty") {
                when {
                    filters.isActive -> EmptyMessage("No se encontraron actividades con estos filtros",
                        "Prueba otra materia o estado, o restablece los filtros.")
                    subjects.isEmpty() -> EmptyMessage("Todavía no tienes materias registradas",
                        "Crea una materia y después registra una tarea, un proyecto o un examen.")
                    else -> EmptyMessage("Sin actividades todavía", "Registra una tarea, un proyecto o un examen.")
                }
            }
        } else {
            ActivitySection.entries.forEach { section ->
                val sectionItems = groups.getValue(section)
                item(key = section.name) { SectionHeading(section, sectionItems.size) }
                if (sectionItems.isEmpty()) item { Text(section.emptyMessage) }
                items(sectionItems, key = { it.id }) { item ->
                    ActivityCard(item, subjects.firstOrNull { it.id == item.subjectId }?.name ?: "Materia",
                        today, busy, onEdit = { onEdit(item) }, onDelete = { onDelete(item) },
                        onComplete = { onComplete(item) })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(assignments: List<Assignment>, subjects: List<Subject>, today: Long, onAdd: () -> Unit, onEdit: (Assignment) -> Unit) {
    val pending = assignments.filter { it.status != "Completada" }
    val groups = ActivityOrganization.group(assignments, today)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Tu próximo paso, más claro", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
                Text("${pending.size} ${if (pending.size == 1) "pendiente" else "pendientes"}", style = MaterialTheme.typography.headlineMedium)
                val overdueCount = pending.count { it.dueDate < today }
                val completedCount = assignments.count { it.status == "Completada" }
                Text("$overdueCount ${if (overdueCount == 1) "vencida" else "vencidas"} · $completedCount ${if (completedCount == 1) "completada" else "completadas"}")
                Text("${groups.getValue(ActivitySection.TODAY).size} para hoy · ${assignments.count { it.status == "En progreso" }} en progreso")
                Text("Pendientes incluye las actividades en progreso.", style = MaterialTheme.typography.bodySmall)
            } }
        }
        item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text(if (subjects.isEmpty()) "Agregar mi primera materia" else "Agregar actividad") } }
        ActivitySection.entries.forEach { section ->
            val sectionItems = groups.getValue(section)
            item(key = section.name) { SectionHeading(section, sectionItems.size) }
            if (sectionItems.isEmpty()) item { Text(section.emptyMessage) }
            items(sectionItems, key = { it.id }) { item ->
                Card(onClick = { onEdit(item) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text(subjects.firstOrNull { it.id == item.subjectId }?.name ?: "Materia")
                        Text("${item.type} · ${dateLabel(item.dueDate)}")
                        Text("Prioridad ${item.priority.lowercase()} · ${item.status}")
                        if (section == ActivitySection.OVERDUE) Text("Entrega vencida", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(item: Assignment, subject: String, today: Long, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onComplete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(subject)
            Text("${item.type} · Entrega: ${dateLabel(item.dueDate)}")
            Text("Prioridad ${item.priority.lowercase()}", fontWeight = FontWeight.SemiBold)
            Text(item.status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (item.description.isNotBlank()) Text(item.description)
            if (ActivityOrganization.section(item, today) == ActivitySection.OVERDUE) Text("Entrega vencida", color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onEdit, enabled = !busy) { Text("Editar") }
                TextButton(onClick = onDelete, enabled = !busy) { Text("Eliminar") }
            }
            if (item.status != "Completada") OutlinedButton(onClick = onComplete, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Marcar como completada") }
        }
    }
}

@Composable
private fun SubjectForm(current: Subject?, busy: Boolean, onCancel: () -> Unit, onSave: (Subject) -> Unit) {
    var name by rememberSaveable(current?.id) { mutableStateOf(current?.name ?: "") }
    var color by rememberSaveable(current?.id) { mutableIntStateOf(current?.color ?: 0) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val error = if (attempted) Rules.subjectError(name) else null
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("Nombre de la materia *") }, singleLine = true,
            isError = error != null, supportingText = { Text(error ?: "Máximo 60 caracteres") }, modifier = Modifier.fillMaxWidth())
        Text("Color", style = MaterialTheme.typography.titleMedium)
        subjectColors.forEachIndexed { index, value ->
            FilterChip(selected = color == index, onClick = { color = index }, label = { Text(subjectColorNames[index]) }, leadingIcon = {
                Box(Modifier.size(16.dp).background(value, CircleShape))
            })
        }
        Button(onClick = { attempted = true; if (Rules.subjectError(name) == null) onSave(Subject(current?.id ?: 0, name, color)) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Guardando…" else "Guardar materia") }
        OutlinedButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}

@Composable
private fun Choice(label: String, value: String, options: List<String>, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: $value" }) {
                Text(value, Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp).widthIn(max = 360.dp)) {
                options.forEachIndexed { index, option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(index); expanded = false }) }
            }
        }
    }
}

@Composable
private fun ActivityForm(current: Assignment?, subjects: List<Subject>, busy: Boolean, onCancel: () -> Unit, onSave: (Assignment) -> Unit) {
    var title by rememberSaveable(current?.id) { mutableStateOf(current?.title ?: "") }
    var description by rememberSaveable(current?.id) { mutableStateOf(current?.description ?: "") }
    var subjectId by rememberSaveable(current?.id) { mutableStateOf(current?.subjectId) }
    var type by rememberSaveable(current?.id) { mutableStateOf(current?.type ?: "Tarea") }
    var date by rememberSaveable(current?.id) { mutableStateOf(current?.let { LocalDate.ofEpochDay(it.dueDate).toString() } ?: "") }
    var priority by rememberSaveable(current?.id) { mutableStateOf(current?.priority ?: "Media") }
    var status by rememberSaveable(current?.id) { mutableStateOf(current?.status ?: "Pendiente") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val error = if (attempted) Rules.activityError(title, subjectId, date) else null
    val selectedDate = Rules.parseDate(date)
    val reminderMessage = selectedDate?.let { epochDay ->
        val zone = ZoneId.systemDefault()
        val reminder = ReminderPolicy.reminderInstant(
            epochDay,
            Clock.systemDefaultZone(),
            zone
        )
        if (reminder == null) {
            "⚠️ La fecha ya pasó o no se programará aviso"
        } else {
            val localReminder = reminder.atZone(zone)
            "🔔 Te recordaremos el ${localReminder.toLocalDate().format(dateFormatter)} " +
                "a las ${localReminder.toLocalTime().format(timeFormatter)}"
        }
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Los campos con * son obligatorios.")
        OutlinedTextField(value = title, onValueChange = { title = it.take(100) }, label = { Text("Título *") }, singleLine = true,
            isError = attempted && title.isBlank(), supportingText = { if (attempted && title.isBlank()) Text("Escribe el título.") }, modifier = Modifier.fillMaxWidth())
        Choice("Materia *", subjects.firstOrNull { it.id == subjectId }?.name ?: "Seleccionar materia", subjects.map { it.name }, subjects.isNotEmpty()) { subjectId = subjects[it].id }
        if (attempted && subjectId == null) Text("Selecciona una materia.", color = MaterialTheme.colorScheme.error)
        if (subjects.isEmpty()) Text("Regresa a Materias y crea una antes de guardar.")
        Choice("Tipo", type, Rules.types) { type = Rules.types[it] }
        Text("Fecha de entrega *", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = {
            val selected = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
            DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d).toString() }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
        }, modifier = Modifier.fillMaxWidth()) { Text(Rules.parseDate(date)?.let { dateLabel(it) } ?: "Seleccionar fecha") }
        if (reminderMessage != null) {
            Text(
                reminderMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (attempted && Rules.parseDate(date) == null) Text("Selecciona una fecha.", color = MaterialTheme.colorScheme.error)
        Choice("Prioridad", priority, Rules.priorities) { priority = Rules.priorities[it] }
        Choice("Estado", status, Rules.statuses) { status = Rules.statuses[it] }
        OutlinedTextField(value = description, onValueChange = { description = it.take(2000) }, label = { Text("Descripción (opcional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            attempted = true
            if (Rules.activityError(title, subjectId, date) == null) {
                onSave(Assignment(current?.id ?: 0, title, description, subjectId!!, type, Rules.parseDate(date)!!, priority, status))
            }
        }, enabled = !busy && subjects.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Guardando…" else "Guardar actividad") }
        OutlinedButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}

@Composable
private fun ConfirmDelete(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("¿Eliminar registro?") },
        text = { Text("Se eliminará «$name». Esta acción no se puede deshacer.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
