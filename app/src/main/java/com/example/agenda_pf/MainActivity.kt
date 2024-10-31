package com.example.agenda_pf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.agenda_pf.data.entities.Note
import com.example.agenda_pf.data.entities.Task
import com.example.agenda_pf.ui.theme.Agenda_PFTheme
import com.example.agenda_pf.viewmodel.NoteViewModel
import com.example.agenda_pf.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

// Import para los íconos en Material3
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.alpha
import com.example.agenda_pf.data.database.DatabaseProvider
import com.example.agenda_pf.viewmodel.NoteViewModelFactory
import com.example.agenda_pf.viewmodel.TaskViewModelFactory
import com.example.agenda_pf.data.repository.OfflineNoteRepository
import com.example.agenda_pf.data.repository.OfflineTaskRepository

// MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crear instancias de los repositorios
        val database = DatabaseProvider.getDatabase(applicationContext)
        val noteRepository = OfflineNoteRepository(database.noteDao())
        val taskRepository = OfflineTaskRepository(database.taskDao())

        // Crear instancias de los ViewModelFactory con los repositorios
        val noteViewModelFactory = NoteViewModelFactory(noteRepository)
        val taskViewModelFactory = TaskViewModelFactory(taskRepository)

        setContent {
            Agenda_PFTheme {
                val navController = rememberNavController()

                // Instancia los ViewModels usando los factories
                val noteViewModel: NoteViewModel = viewModel(factory = noteViewModelFactory)
                val taskViewModel: TaskViewModel = viewModel(factory = taskViewModelFactory)

                Navigation(navController, noteViewModel, taskViewModel)
            }
        }
    }
}


// Navigation
@Composable
fun Navigation(
    navController: NavHostController,
    noteViewModel: NoteViewModel,
    taskViewModel: TaskViewModel
) {
    NavHost(navController, startDestination = "main") {
        composable("main") { MainScreen(navController) }
        composable("notesList") { NotesListScreen(noteViewModel, navController) }
        composable("tasksList") { TasksListScreen(taskViewModel, navController) }
        composable("addNote") { AddNoteScreen(noteViewModel, navController) }
        composable("addTask") { AddTaskScreen(taskViewModel, navController) }
        composable("editNote/{index}") { backStackEntry ->
            val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
            index?.let { EditNoteScreen(noteViewModel, navController, it) }
        }
        composable("editTask/{index}") { backStackEntry ->
            val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
            index?.let { EditTaskScreen(taskViewModel, navController, it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    var showDialog by remember { mutableStateOf(false) } // Estado para mostrar el diálogo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFE1BEE7)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { // Muestra el diálogo al hacer clic
                Icon(painter = painterResource(id = android.R.drawable.ic_input_add), contentDescription = "Agregar")
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8BBD0)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_notas),
                    contentDescription = "Notas",
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { navController.navigate("notesList") }
                )
                Text("Notas", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(40.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_tareas),
                    contentDescription = "Tareas",
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { navController.navigate("tasksList") }
                )
                Text("Tareas", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    )

    // Cuadro de diálogo de selección
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Agregar Nota o Tarea") },
            text = { Text(text = "Selecciona una opción para agregar:") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        navController.navigate("addNote") // Navega a la pantalla de agregar nota
                    }
                ) {
                    Text("Agregar Nota")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        navController.navigate("addTask") // Navega a la pantalla de agregar tarea
                    }
                ) {
                    Text("Agregar Tarea")
                }
            }
        )
    }
}



// AddNoteScreen
@Composable
fun AddNoteScreen(viewModel: NoteViewModel, navController: NavHostController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.padding(16.dp)) {

        Button(onClick = { navController.navigate("main") }) {
            Text("Regresar")
        }

        TextField(value = title, onValueChange = { title = it }, placeholder = { Text("Título") })
        TextField(value = description, onValueChange = { description = it }, placeholder = { Text("Descripción") })

        Button(onClick = {
            val note = Note(title = title.text, description = description.text)
            viewModel.addNote(note)
            navController.navigate("notesList")
        }) {
            Text("Guardar Nota")
        }
    }
}

// AddTaskScreen
@Composable
fun AddTaskScreen(viewModel: TaskViewModel, navController: NavHostController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.padding(16.dp)) {

        Button(onClick = { navController.navigate("main") }) {
            Text("Regresar")
        }

        TextField(value = title, onValueChange = { title = it }, placeholder = { Text("Título") })
        TextField(value = description, onValueChange = { description = it }, placeholder = { Text("Descripción") })

        Button(onClick = {
            val task = Task(title = title.text, description = description.text, dueDate = System.currentTimeMillis())
            viewModel.addTask(task)
            navController.navigate("tasksList")
        }) {
            Text("Guardar Tarea")
        }
    }
}

// NotesListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(viewModel: NoteViewModel, navController: NavHostController) {
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    var searchText by remember { mutableStateOf("") }
    val filteredNotes = notes.filter {
        it.title.contains(searchText, ignoreCase = true) ||
                it.description.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Lista de Notas")
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFE1BEE7)),
                navigationIcon = {
                    TextButton(onClick = { navController.navigate("main") }) {
                        Text("Regresar", color = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addNote") }) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = "Agregar Nota"
                )
            }
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar Notas") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay notas disponibles")
                    }
                } else {
                    LazyColumn {
                        items(filteredNotes) { note ->
                            NoteItem(
                                note,
                                onEdit = { navController.navigate("editNote/${note.id}") },
                                onDelete = { viewModel.deleteNote(note) }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun TaskItem(task: Task, onEdit: () -> Unit, onDelete: () -> Unit, onCompleteChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Checkbox para marcar como completada
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { isChecked ->
                onCompleteChange(isChecked)
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (task.isCompleted) 0.5f else 1f) // Reducir opacidad si está completada
        ) {
            Text(text = task.title, fontWeight = FontWeight.Bold)
            Text(text = task.description)
        }
        IconButton(onClick = onEdit, enabled = !task.isCompleted) {
            Icon(Icons.Filled.Edit, contentDescription = "Editar")
        }
        IconButton(onClick = onDelete, enabled = !task.isCompleted) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
        }
    }
}



// TasksListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksListScreen(viewModel: TaskViewModel, navController: NavHostController) {
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    var searchText by remember { mutableStateOf("") }
    val filteredTasks = tasks.filter {
        it.title.contains(searchText, ignoreCase = true) ||
                it.description.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Tareas") },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFE1BEE7)),
                navigationIcon = {
                    TextButton(onClick = { navController.navigate("main") }) {
                        Text("Regresar", color = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addTask") }) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = "Agregar Tarea"
                )
            }
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar Tareas") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                if (filteredTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay tareas disponibles")
                    }
                } else {
                    LazyColumn {
                        items(filteredTasks) { task ->
                            TaskItem(
                                task,
                                onEdit = { navController.navigate("editTask/${task.id}") },
                                onDelete = { viewModel.deleteTask(task) },
                                onCompleteChange = { isChecked ->
                                    viewModel.updateTask(task.copy(isCompleted = isChecked))
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}






// NoteItem
@Composable
fun NoteItem(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = note.title, fontWeight = FontWeight.Bold)
            Text(text = note.description)
        }
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
    }
}

// TaskItem
@Composable
fun TaskItem(task: Task, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = task.title, fontWeight = FontWeight.Bold)
            Text(text = task.description)
        }
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
    }
}

// EditNoteScreen
@Composable
fun EditNoteScreen(viewModel: NoteViewModel, navController: NavHostController, noteId: Int) {
    val note by viewModel.getNoteById(noteId).collectAsState(initial = null)
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(note) {
        note?.let {
            title = TextFieldValue(it.title)
            description = TextFieldValue(it.description)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(value = title, onValueChange = { title = it }, placeholder = { Text("Título") })
        TextField(value = description, onValueChange = { description = it }, placeholder = { Text("Descripción") })

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                note?.let {
                    viewModel.updateNote(Note(id = it.id, title = title.text, description = description.text))
                }
                navController.navigate("notesList")
            }) {
                Text("Guardar Cambios")
            }

            // Botón de Cancelar
            OutlinedButton(onClick = { navController.navigate("notesList") }) {
                Text("Cancelar")
            }
        }
    }
}



// EditTaskScreen
@Composable
fun EditTaskScreen(viewModel: TaskViewModel, navController: NavHostController, taskId: Int) {
    val task by viewModel.getTaskById(taskId).collectAsState(initial = null)
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(task) {
        task?.let {
            title = TextFieldValue(it.title)
            description = TextFieldValue(it.description)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(value = title, onValueChange = { title = it }, placeholder = { Text("Título") })
        TextField(value = description, onValueChange = { description = it }, placeholder = { Text("Descripción") })

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                task?.let {
                    viewModel.updateTask(Task(id = it.id, title = title.text, description = description.text, dueDate = it.dueDate))
                }
                navController.navigate("tasksList")
            }) {
                Text("Guardar Cambios")
            }

            // Botón de Cancelar
            OutlinedButton(onClick = { navController.navigate("tasksList") }) {
                Text("Cancelar")
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    Agenda_PFTheme {
        MainScreen(navController)
    }
}
