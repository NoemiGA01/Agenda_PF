package com.example.agenda_pf
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.agenda_pf.ui.theme.Agenda_PFTheme

class  MainActivity : ComponentActivity() {
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

@Composable
fun MyApp() {
    var notes by rememberSaveable { mutableStateOf(mutableListOf("Nota 1", "Nota 2")) }
    var tasks by rememberSaveable { mutableStateOf(mutableListOf("Investigar routers", "Descartar routers TLink y Cisco")) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Acción para añadir nueva nota/tarea */ }) {
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
            NoteSection(notes, onEditNote = { index, newNote -> notes[index] = newNote }, onDeleteNote = { index -> notes.removeAt(index) })

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Tareas")
            TaskList(
                tasks = tasks,
                onEditTask = { index, newTask -> tasks[index] = newTask },
                onDeleteTask = { index -> tasks.removeAt(index) }
            )
        }
    }
}



@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun NoteSection(
    notes: List<String>,
    onEditNote: (Int, String) -> Unit,
    onDeleteNote: (Int) -> Unit
) {
    LazyColumn {
        itemsIndexed(notes) { index, note ->
            NoteItem(
                note = note,
                onEdit = { newNote -> onEditNote(index, newNote) },
                onDelete = { onDeleteNote(index) }
            )
        }
    }
}

@Composable
fun NoteItem(
    note: String,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(note) }

    if (isEditing) {
        Column {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Editar Nota") }
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    onEdit(text)
                    isEditing = false
                }) {
                    Text("Guardar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { isEditing = false }) {
                    Text("Cancelar")
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(note, modifier = Modifier.weight(1f))
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}


@Composable
fun TaskList(
    tasks: List<String>,
    onEditTask: (Int, String) -> Unit,
    onDeleteTask: (Int) -> Unit
) {
    LazyColumn {
        itemsIndexed(tasks) { index, task ->
            TaskItem(
                task = task,
                onEdit = { newTask -> onEditTask(index, newTask) },
                onDelete = { onDeleteTask(index) }
            )
        }
    }
}


@Composable
fun TaskItem(
    task: String,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(task) }

    if (isEditing) {
        Column {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Editar Tarea") }
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    onEdit(text)
                    isEditing = false
                }) {
                    Text("Guardar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { isEditing = false }) {
                    Text("Cancelar")
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(task, modifier = Modifier.weight(1f))
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
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
