package com.example.agenda_pf


import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
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

// Import para los íconos en Material3
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.agenda_pf.R
import com.example.agenda_pf.data.database.DatabaseProvider
import com.example.agenda_pf.viewmodel.NoteViewModelFactory
import com.example.agenda_pf.viewmodel.TaskViewModelFactory
import com.example.agenda_pf.data.repository.OfflineNoteRepository
import com.example.agenda_pf.data.repository.OfflineTaskRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File
import coil.compose.rememberAsyncImagePainter
import android.Manifest
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider


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
                title = { Text(stringResource(R.string.agenda), fontWeight = FontWeight.Bold, fontSize = 24.sp) },
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
                Text(stringResource(R.string.notas), fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(40.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_tareas),
                    contentDescription = "Tareas",
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { navController.navigate("tasksList") }
                )
                Text(stringResource(R.string.tareas), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    )

    // Cuadro de diálogo de selección
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = stringResource(R.string.agregar_nota_o_tarea)) },
            text = { Text(text = stringResource(R.string.selecciona_una_opci_n_para_agregar)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        navController.navigate("addNote") // Navega a la pantalla de agregar nota
                    }
                ) {
                    Text(stringResource(R.string.agregar_nota))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        navController.navigate("addTask") // Navega a la pantalla de agregar tarea
                    }
                ) {
                    Text(stringResource(R.string.agregar_tarea))
                }
            }
        )
    }
}

fun uriToFile(context: Context, uri: Uri): File? {
    val contentResolver = context.contentResolver
    val fileName = System.currentTimeMillis().toString() + ".jpg" // Nombre único para el archivo
    val file = File(context.cacheDir, fileName)

    return try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveBitmapToFile(context: Context, bitmap: Bitmap): File? {
    val fileName = System.currentTimeMillis().toString() + ".jpg"
    val file = File(context.cacheDir, fileName)
    return try {
        file.outputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun AddNoteScreen(viewModel: NoteViewModel, navController: NavHostController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var mediaFiles by remember { mutableStateOf(listOf<File>()) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Lanzador para seleccionar imágenes desde la galería
    val imageGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            file?.let { mediaFiles = mediaFiles + it }
        }
    }

    // Lanzador para tomar fotos con la cámara
    val imageCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val file = saveBitmapToFile(context, it)
            file?.let { mediaFiles = mediaFiles + it }
        }
    }

    // Lanzador para seleccionar videos desde la galería
    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            file?.let { mediaFiles = mediaFiles + it }
        }
    }

    // Lanzador para grabar videos con la cámara
    val videoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { isSuccess: Boolean ->
        if (isSuccess) {
            val videoFile = File(context.filesDir, "video_${System.currentTimeMillis()}.mp4")
            mediaFiles = mediaFiles + videoFile
            Toast.makeText(context, "Video guardado en: ${videoFile.absolutePath}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No se grabó ningún video", Toast.LENGTH_SHORT).show()
        }
    }

    // Lanzador para manejar permisos de almacenamiento
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            videoGalleryLauncher.launch("video/*")
        } else {
            Toast.makeText(context, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Lanzador para manejar permisos de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempVideoFile = File(context.filesDir, "video_${System.currentTimeMillis()}.mp4")
            val videoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempVideoFile
            )
            videoCameraLauncher.launch(videoUri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { navController.navigate("main") }, modifier = Modifier.fillMaxWidth()) {
            Text("Regresar")
        }

        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Título") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Descripción") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Botones para agregar imágenes y videos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { showImageDialog = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_foto),
                    contentDescription = "Agregar Foto"
                )
            }
            IconButton(onClick = { showVideoDialog = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video),
                    contentDescription = "Agregar Video"
                )
            }
        }

        // Mostrar lista de archivos multimedia
        LazyColumn {
            items(mediaFiles) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (file.extension == "mp4") {
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    setVideoPath(file.absolutePath)
                                    start()
                                }
                            },
                            modifier = Modifier.size(150.dp)
                        )
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(file),
                            contentDescription = "Vista previa",
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    IconButton(onClick = { mediaFiles = mediaFiles - file }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar Archivo")
                    }
                }
            }
        }

        Button(
            onClick = {
                try {
                    val note = Note(
                        title = title.text,
                        description = description.text,
                        multimedia = mediaFiles.joinToString(separator = ",") { "\"${it.absolutePath}\"" }
                    )
                    viewModel.addNote(note)
                    navController.navigate("notesList")
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al guardar la nota: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Guardar Nota")
        }

        // Cuadro de diálogo para agregar imágenes
        if (showImageDialog) {
            AlertDialog(
                onDismissRequest = { showImageDialog = false },
                title = { Text("Agregar Imagen") },
                text = { Text("Elige una opción:") },
                confirmButton = {
                    TextButton(onClick = {
                        imageCameraLauncher.launch(null)
                        showImageDialog = false
                    }) {
                        Text("Tomar Foto")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        imageGalleryLauncher.launch("image/*")
                        showImageDialog = false
                    }) {
                        Text("Subir Imagen")
                    }
                }
            )
        }

        // Cuadro de diálogo para agregar videos
        if (showVideoDialog) {
            AlertDialog(
                onDismissRequest = { showVideoDialog = false },
                title = { Text("Agregar Video") },
                text = { Text("Elige una opción:") },
                confirmButton = {
                    TextButton(onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        showVideoDialog = false
                    }) {
                        Text("Grabar Video")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        showVideoDialog = false
                    }) {
                        Text("Subir Video")
                    }
                }
            )
        }
    }
}








@Composable
fun AddTaskScreen(viewModel: TaskViewModel, navController: NavHostController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var dueDate by remember { mutableStateOf("") } // Fecha seleccionada como texto
    var showDatePicker by remember { mutableStateOf(false) } // Estado para el DatePicker
    var showTimePicker by remember { mutableStateOf(false) } // Estado para el TimePicker
    var selectedDate by remember { mutableStateOf("") } // Almacena la fecha seleccionada
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { navController.navigate("main") }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.regresar))
        }

        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text(stringResource(R.string.t_tulo)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text(stringResource(R.string.descripci_n)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Botón para seleccionar fecha y hora
        Button(
            onClick = { showDatePicker = true }, // Cambia el estado para mostrar el DatePicker
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = if (dueDate.isEmpty()) "Seleccionar Fecha y Hora" else "Fecha: $dueDate")
        }

        // Mostrar el DatePickerDialog
        if (showDatePicker) {
            val calendar = Calendar.getInstance()
            android.app.DatePickerDialog(
                context,
                { _, year, month, day ->
                    selectedDate = "$day/${month + 1}/$year"
                    showDatePicker = false
                    showTimePicker = true // Muestra el TimePicker después de seleccionar la fecha
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Mostrar el TimePickerDialog
        if (showTimePicker) {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    dueDate = "$selectedDate $hour:$minute" // Combina la fecha y la hora
                    showTimePicker = false
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Sección de iconos multimedia
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { /* Acción para añadir foto */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_foto),
                    contentDescription = "Agregar Foto"
                )
            }
            IconButton(onClick = { /* Acción para añadir video */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video),
                    contentDescription = "Agregar Video"
                )
            }
            IconButton(onClick = { /* Acción para añadir audio */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_audio),
                    contentDescription = "Agregar Audio"
                )
            }
        }

        Button(
            onClick = {
                // Convertir la fecha y hora seleccionada a un Long
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = formatter.parse(dueDate) // Convierte la fecha seleccionada a Date
                val dueDateLong = date?.time ?: System.currentTimeMillis() // Convierte Date a Long

                // Crear la tarea con dueDate como Long
                val task = Task(
                    title = title.text,
                    description = description.text,
                    dueDate = dueDateLong
                )
                viewModel.addTask(task)
                navController.navigate("tasksList")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(stringResource(R.string.guardar_tarea))
        }
    }
}

@Composable
fun EditNoteScreen(viewModel: NoteViewModel, navController: NavHostController, noteId: Int) {
    val note by viewModel.getNoteById(noteId).collectAsState(initial = null)
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var mediaFiles by remember { mutableStateOf(listOf<File>()) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Lanzador para seleccionar imágenes desde la galería
    val imageGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            file?.let { mediaFiles = mediaFiles + it }
        }
    }

    // Lanzador para tomar fotos con la cámara
    val imageCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val file = saveBitmapToFile(context, it)
            file?.let { mediaFiles = mediaFiles + it }
        }
    }

    // Lanzador para seleccionar videos desde la galería
    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            file?.let { mediaFiles = mediaFiles + it }
        }
    }

    // Lanzador para grabar videos con la cámara
    val videoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { isSuccess: Boolean ->
        if (isSuccess) {
            val videoFile = File(context.filesDir, "video_${System.currentTimeMillis()}.mp4")
            mediaFiles = mediaFiles + videoFile
            Toast.makeText(context, "Video guardado en: ${videoFile.absolutePath}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No se grabó ningún video", Toast.LENGTH_SHORT).show()
        }
    }

    // Lanzador para manejar permisos de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempVideoFile = File(context.filesDir, "video_${System.currentTimeMillis()}.mp4")
            val videoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempVideoFile
            )
            videoCameraLauncher.launch(videoUri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(note) {
        note?.let {
            title = TextFieldValue(it.title)
            description = TextFieldValue(it.description)
            mediaFiles = it.multimedia
                .removeSurrounding("[", "]")
                .split(",")
                .map { path -> File(path.trim('"')) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Título") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Descripción") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Botones para agregar imágenes, videos y audios
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { showImageDialog = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_foto),
                    contentDescription = "Agregar Foto"
                )
            }
            IconButton(onClick = { showVideoDialog = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video),
                    contentDescription = "Agregar Video"
                )
            }
        }

        LazyColumn {
            items(mediaFiles) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (file.extension == "mp4") {
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    setVideoPath(file.absolutePath)
                                    start()
                                }
                            },
                            modifier = Modifier.size(150.dp)
                        )
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(file),
                            contentDescription = "Vista previa",
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    IconButton(onClick = { mediaFiles = mediaFiles - file }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar Archivo")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    note?.let {
                        viewModel.updateNote(
                            Note(
                                id = it.id,
                                title = title.text,
                                description = description.text,
                                multimedia = mediaFiles.joinToString(separator = ",") { "\"${it.absolutePath}\"" }
                            )
                        )
                    }
                    navController.navigate("notesList")
                },
                modifier = Modifier.padding(4.dp)
            ) {
                Text("Guardar Cambios")
            }

            OutlinedButton(
                onClick = { navController.navigate("notesList") },
                modifier = Modifier.padding(4.dp)
            ) {
                Text("Cancelar")
            }
        }

        // Cuadro de diálogo para agregar imágenes
        if (showImageDialog) {
            AlertDialog(
                onDismissRequest = { showImageDialog = false },
                title = { Text("Agregar Imagen") },
                text = { Text("Elige una opción:") },
                confirmButton = {
                    TextButton(onClick = {
                        imageCameraLauncher.launch(null)
                        showImageDialog = false
                    }) {
                        Text("Tomar Foto")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        imageGalleryLauncher.launch("image/*")
                        showImageDialog = false
                    }) {
                        Text("Subir Imagen")
                    }
                }
            )
        }

        // Cuadro de diálogo para agregar videos
        if (showVideoDialog) {
            AlertDialog(
                onDismissRequest = { showVideoDialog = false },
                title = { Text("Agregar Video") },
                text = { Text("Elige una opción:") },
                confirmButton = {
                    TextButton(onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        showVideoDialog = false
                    }) {
                        Text("Grabar Video")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        videoGalleryLauncher.launch("video/*")
                        showVideoDialog = false
                    }) {
                        Text("Subir Video")
                    }
                }
            )
        }
    }
}








// Función para mostrar el DatePicker
@Composable
fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            onDateSelected("$selectedDay/${selectedMonth + 1}/$selectedYear")
        },
        year, month, day
    ).show()
}

// Función para mostrar el TimePicker
fun showTimePicker(context: Context, onTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected("$selectedHour:$selectedMinute")
        },
        hour, minute, true
    ).show()
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
                    Text(stringResource(R.string.lista_de_notas))
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFE1BEE7)),
                navigationIcon = {
                    TextButton(onClick = { navController.navigate("main") }) {
                        Text(stringResource(R.string.regresar), color = Color.White)
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
                    placeholder = { Text(stringResource(R.string.buscar_notas)) },
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
                        Text(stringResource(R.string.no_hay_notas_disponibles))
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
                title = { Text(stringResource(R.string.lista_de_tareas)) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFE1BEE7)),
                navigationIcon = {
                    TextButton(onClick = { navController.navigate("main") }) {
                        Text(stringResource(R.string.regresar), color = Color.White)
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
                    placeholder = { Text(stringResource(R.string.buscar_tareas)) },
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
                        Text(stringResource(R.string.no_hay_tareas_disponibles))
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


@Composable
fun TaskItem(task: Task, onEdit: () -> Unit, onDelete: () -> Unit, onCompleteChange: (Boolean) -> Unit) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) // Formato para mostrar la fecha
    val formattedDate = dateFormatter.format(task.dueDate) // Convierte Long a String

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
            Text(text = "Fecha: $formattedDate", fontSize = 12.sp, color = Color.Gray) // Muestra la fecha
        }
        IconButton(onClick = onEdit, enabled = !task.isCompleted) {
            Icon(Icons.Filled.Edit, contentDescription = "Editar")
        }
        IconButton(onClick = onDelete, enabled = !task.isCompleted) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
        }
    }
}





//Pantalla de editar tareas
@Composable
fun EditTaskScreen(viewModel: TaskViewModel, navController: NavHostController, taskId: Int) {
    val task by viewModel.getTaskById(taskId).collectAsState(initial = null)
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var dueDate by remember { mutableStateOf("") } // Fecha seleccionada como texto
    var showDatePicker by remember { mutableStateOf(false) } // Estado para el DatePicker
    var showTimePicker by remember { mutableStateOf(false) } // Estado para el TimePicker
    var selectedDate by remember { mutableStateOf("") } // Almacena la fecha seleccionada
    val context = LocalContext.current

    LaunchedEffect(task) {
        task?.let {
            title = TextFieldValue(it.title)
            description = TextFieldValue(it.description)
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dueDate = formatter.format(it.dueDate) // Convierte Long a String
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text(stringResource(R.string.t_tulo)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text(stringResource(R.string.descripci_n)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Botón para modificar la fecha y hora
        Button(
            onClick = { showDatePicker = true }, // Muestra el DatePicker al hacer clic
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = if (dueDate.isEmpty()) "Modificar Fecha y Hora" else "Fecha: $dueDate")
        }

        // Mostrar el DatePickerDialog
        if (showDatePicker) {
            val calendar = Calendar.getInstance()
            android.app.DatePickerDialog(
                context,
                { _, year, month, day ->
                    selectedDate = "$day/${month + 1}/$year"
                    showDatePicker = false
                    showTimePicker = true // Mostrar el TimePicker después de seleccionar la fecha
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Mostrar el TimePickerDialog
        if (showTimePicker) {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    dueDate = "$selectedDate $hour:$minute" // Combina la fecha y la hora
                    showTimePicker = false
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Sección de iconos multimedia
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { /* Acción para añadir foto */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_foto),
                    contentDescription = "Agregar Foto"
                )
            }
            IconButton(onClick = { /* Acción para añadir video */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video),
                    contentDescription = "Agregar Video"
                )
            }
            IconButton(onClick = { /* Acción para añadir audio */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_audio),
                    contentDescription = "Agregar Audio"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    task?.let {
                        // Convertir la fecha seleccionada a Long
                        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val date = formatter.parse(dueDate)
                        val dueDateLong = date?.time ?: it.dueDate // Usa la nueva fecha o la anterior

                        viewModel.updateTask(
                            Task(
                                id = it.id,
                                title = title.text,
                                description = description.text,
                                dueDate = dueDateLong, // Actualiza la fecha
                                isCompleted = it.isCompleted
                            )
                        )
                    }
                    navController.navigate("tasksList")
                },
                modifier = Modifier.padding(4.dp)
            ) {
                Text(stringResource(R.string.guardar_cambios))
            }

            OutlinedButton(
                onClick = { navController.navigate("tasksList") },
                modifier = Modifier.padding(4.dp)
            ) {
                Text(stringResource(R.string.cancelar))
            }
        }
    }
}



