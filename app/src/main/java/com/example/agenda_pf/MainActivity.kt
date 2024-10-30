package com.example.agenda_pf

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.agenda_pf.ui.theme.Agenda_PFTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Agenda_PFTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun MyApp() {
    var shouldShowOnboarding by rememberSaveable { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var selectedTask by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf(
        listOf(
            Note(
                title = "Nota 1",
                description = "Descripción de prueba",
                files = listOf(),
                audios = listOf()
            ),
            Note(
                title = "Nota 2",
                description = "Otra descripción",
                files = listOf(),
                audios = listOf()
            )
        )
    )}
    var tasks by remember { mutableStateOf(listOf("Tarea 1: Descripción de prueba", "Tarea 2: Revisar documentación")) }

    if (shouldShowOnboarding) {
        OnboardingScreen(onContinueClicked = { shouldShowOnboarding = false })
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                SectionHeader("Notas")
                NoteSection(
                    notes = notes,
                    onEditNote = { index, newNote ->
                        notes = notes.toMutableList().apply { set(index, newNote) }
                    },
                    onDeleteNote = { index ->
                        notes = notes.toMutableList().apply { removeAt(index) }
                    },
                    onNoteClick = { note ->
                        selectedNote = note
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionHeader("Tareas")
                TaskList(
                    tasks = tasks,
                    onEditTask = { index, newTask ->
                        tasks = tasks.toMutableList().apply { set(index, newTask) }
                    },
                    onDeleteTask = { index ->
                        tasks = tasks.toMutableList().apply { removeAt(index) }
                    },
                    onTaskClick = { task ->
                        selectedTask = task
                    }
                )

                if (showAddDialog) {
                    AddNoteOrTaskDialog(
                        onAddNote = { title, description, files, audios ->
                            notes = notes + Note(title, description, files, audios)
                            showAddDialog = false
                        },
                        onAddTask = { _, _, _, _ -> },
                        onDismiss = { showAddDialog = false }
                    )
                }
            }

            // Mostrar la vista detallada de la nota seleccionada
            selectedNote?.let { note ->
                NoteDetailScreen(note = note, onDismiss = { selectedNote = null })
            }

            // Mostrar la vista detallada de la tarea seleccionada
            selectedTask?.let { task ->
                TaskDetailScreen(task = task, onDismiss = { selectedTask = null })
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun NoteSection(
    notes: List<Note>,
    onEditNote: (Int, Note) -> Unit,
    onDeleteNote: (Int) -> Unit,
    onNoteClick: (Note) -> Unit
) {
    LazyColumn {
        items(notes.size) { index ->
            NoteItem(note = notes[index], onEdit = {
                onEditNote(index, it)
            }, onDelete = {
                onDeleteNote(index)
            }, onClick = {
                onNoteClick(notes[index])
            })
        }
    }
}

@Composable
fun NoteItem(note: Note, onEdit: (Note) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(note.description) }

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditing) {
                TextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Editar Nota") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        onEdit(note.copy(description = editText))
                        isEditing = false
                    }) {
                        Text("Guardar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { isEditing = false }) {
                        Text("Cancelar")
                    }
                }
            } else {
                Text(text = note.title)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = note.description)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                isEditing = true
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                onDelete()
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskList(
    tasks: List<String>,
    onEditTask: (Int, String) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onTaskClick: (String) -> Unit
) {
    LazyColumn {
        items(tasks.size) { index ->
            TaskItem(task = tasks[index], onEdit = {
                onEditTask(index, it)
            }, onDelete = {
                onDeleteTask(index)
            }, onClick = {
                onTaskClick(tasks[index])
            })
        }
    }
}

@Composable
fun TaskItem(task: String, onEdit: (String) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(task) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            TextField(
                value = editText,
                onValueChange = { editText = it },
                label = { Text("Editar Tarea") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                onEdit(editText)
                isEditing = false
            }) {
                Text("Guardar")
            }
            Button(onClick = { isEditing = false }) {
                Text("Cancelar")
            }
        } else {
            Text(
                text = task,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    onClick = {
                        isEditing = true
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    onClick = {
                        onDelete()
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NoteDetailScreen(note: Note, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title) },
        text = {
            Column {
                Text(text = "Descripción: ${note.description}")
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Archivos adjuntos:")
                LazyColumn {
                    items(note.files) { (uri, description) ->
                        Text(text = "Archivo: ${uri.path} - Descripción: $description")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Audios adjuntos:")
                LazyColumn {
                    items(note.audios) { uri ->
                        Text(text = "Audio: ${uri.path}")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun TaskDetailScreen(task: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tarea Detallada") },
        text = { Text(task) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun AddNoteOrTaskDialog(
    onAddNote: (String, String, List<Pair<Uri, String>>, List<Uri>) -> Unit,
    onAddTask: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputTitle by remember { mutableStateOf("") }
    var inputDescription by remember { mutableStateOf("") }
    var isTask by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    var files by remember { mutableStateOf<List<Pair<Uri, String>>>(emptyList()) }
    var audios by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val context = LocalContext.current

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->  // Especificamos que es una lista de Uri
        if (uris != null) {
            files = uris.map { it to "" }
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            audios = audios + uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isTask) "Agregar Tarea" else "Agregar Nota") },
        text = {
            Column {
                TextField(
                    value = inputTitle,
                    onValueChange = { inputTitle = it },
                    label = { Text("Título") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = inputDescription,
                    onValueChange = { inputDescription = it },
                    label = { Text("Descripción") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!isTask) {
                    Button(onClick = { fileLauncher.launch("image/* video/*") }) {
                        Text("Seleccionar archivos multimedia")
                    }
                    LazyColumn {
                        items(files) { (uri, description) ->
                            Row {
                                Text("Archivo: ${uri.path}")
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = description,
                                    onValueChange = { newDesc ->
                                        files = files.map { if (it.first == uri) uri to newDesc else it }
                                    },
                                    label = { Text("Descripción") }
                                )
                            }
                        }
                    }

                    Button(onClick = { audioLauncher.launch("audio/*") }) {
                        Text("Grabar audio")
                    }
                    LazyColumn {
                        items(audios) { uri ->
                            Text("Audio: ${uri.path}")
                        }
                    }
                }

                if (isTask) {
                    DatePickerComponent(onDateSelected = { selectedDate = it })
                    TimePickerComponent(onTimeSelected = { selectedTime = it })
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Es una tarea")
                    Switch(
                        checked = isTask,
                        onCheckedChange = { isTask = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isTask) {
                        onAddTask(inputTitle, inputDescription, selectedDate, selectedTime)
                    } else {
                        onAddNote(inputTitle, inputDescription, files, audios)
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DatePickerComponent(onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val date = remember { mutableStateOf("") }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            date.value = "$day/${month + 1}/$year"
            onDateSelected(date.value)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Button(onClick = { datePickerDialog.show() }) {
        Text(text = if (date.value.isEmpty()) "Seleccionar Fecha" else date.value)
    }
}

@Composable
fun TimePickerComponent(onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val time = remember { mutableStateOf("") }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            time.value = "$hour:$minute"
            onTimeSelected(time.value)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    Button(onClick = { timePickerDialog.show() }) {
        Text(text = if (time.value.isEmpty()) "Seleccionar Hora" else time.value)
    }
}

@Composable
fun OnboardingScreen(
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to the Agenda App!")
        Button(
            modifier = Modifier.padding(vertical = 24.dp),
            onClick = onContinueClicked
        ) {
            Text("Continue")
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320)
@Composable
fun OnboardingPreview() {
    Agenda_PFTheme {
        OnboardingScreen(onContinueClicked = {})
    }
}

@Preview(showBackground = true)
@Composable
fun MyAppPreview() {
    Agenda_PFTheme {
        MyApp()
    }
}

data class Note(
    val title: String,
    val description: String,
    val files: List<Pair<Uri, String>>,
    val audios: List<Uri>
)
