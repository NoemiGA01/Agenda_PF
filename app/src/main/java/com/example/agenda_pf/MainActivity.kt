package com.example.agenda_pf


import android.app.Activity
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.agenda_pf.R
import com.example.agenda_pf.data.database.DatabaseProvider
import com.example.agenda_pf.viewmodel.NoteViewModelFactory
import com.example.agenda_pf.viewmodel.TaskViewModelFactory
import com.example.agenda_pf.data.repository.OfflineNoteRepository
import com.example.agenda_pf.data.repository.OfflineTaskRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

@Composable
fun AddNoteScreen(viewModel: NoteViewModel, navController: NavHostController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }

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

        // Nueva sección para íconos de multimedia
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
                val note = Note(title = title.text, description = description.text)
                viewModel.addNote(note)
                navController.navigate("notesList")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(stringResource(R.string.guardar_nota))
        }
    }
}

@Composable
fun AddTaskScreen(viewModel: TaskViewModel, navController: NavHostController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var dueDate by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf(listOf<Uri>()) }
    var videoUris by remember { mutableStateOf(listOf<Uri>()) }
    val audioUris = remember { mutableStateListOf<Uri>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mediaRecorder = remember { MediaRecorder() }
    val mediaPlayer = remember { MediaPlayer() }
    val tempAudioFile = remember { mutableStateOf<File?>(null) }

    val tempImageUri = remember { mutableStateOf<Uri?>(null) }
    val tempVideoUri = remember { mutableStateOf<Uri?>(null) }

    val launcherTakePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri.value != null) {
            tempImageUri.value?.let {
                imageUris = imageUris + it
            }
        }
    }

    val launcherSelectPictures = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty()) {
            imageUris = imageUris + uris
        }
    }

    val launcherCaptureVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            tempVideoUri.value?.let { uri ->
                if (!videoUris.contains(uri)) {
                    videoUris = videoUris + uri
                }
            }
            tempVideoUri.value = null // Restablece la URI temporal después de capturar el video
        }
    }

    val launcherSelectVideos = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty()) {
            videoUris = videoUris + uris
        }
    }

    val launcherSelectAudios = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty()) {
            audioUris.addAll(uris)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Campo de título
        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Título") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Campo de descripción
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Descripción") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Botón para seleccionar fecha y hora
        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(if (dueDate.isEmpty()) "Seleccionar Fecha y Hora" else "Fecha: $dueDate")
        }

        if (showDatePicker) {
            val calendar = Calendar.getInstance()
            android.app.DatePickerDialog(
                context,
                { _, year, month, day ->
                    selectedDate = "$day/${month + 1}/$year"
                    showDatePicker = false
                    showTimePicker = true
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        if (showTimePicker) {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    dueDate = "$selectedDate $hour:$minute"
                    showTimePicker = false
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Sección de íconos multimedia
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = {
                val uri = createTempImageUri(context)
                tempImageUri.value = uri
                launcherTakePicture.launch(uri)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_foto),
                    contentDescription = "Tomar Foto"
                )
            }

            IconButton(onClick = { launcherSelectPictures.launch("image/*") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gallery),
                    contentDescription = "Seleccionar Fotos"
                )
            }

            IconButton(onClick = {
                val uri = createTempVideoUri(context)
                tempVideoUri.value = uri
                launcherCaptureVideo.launch(uri)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video),
                    contentDescription = "Capturar Video"
                )
            }

            IconButton(onClick = { launcherSelectVideos.launch("video/*") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video2),
                    contentDescription = "Seleccionar Videos"
                )
            }

            IconButton(onClick = {
                if (isRecording) {
                    stopRecording(mediaRecorder, tempAudioFile, audioUris, context)
                    isRecording = false
                } else {
                    startRecording(mediaRecorder, tempAudioFile, context)
                    isRecording = true
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_audio),
                    contentDescription = if (isRecording) "Detener Grabación" else "Grabar Audio",
                    tint = if (isRecording) Color.Red else Color.Black
                )
            }
        }

        // Visualización de multimedia
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(imageUris) { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Imagen seleccionada",
                    modifier = Modifier
                        .size(60.dp)
                        .clickable { selectedImageUri = uri },
                    contentScale = ContentScale.Crop
                )
            }

            items(videoUris) { uri ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clickable { selectedVideoUri = uri }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video),
                        contentDescription = "Video seleccionado",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            items(audioUris) { uri ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clickable {
                            playAudio(mediaPlayer, uri, context) // Llama a la función para reproducir el audio
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_audio),
                        contentDescription = "Audio seleccionado",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

        }

        // Mostrar imagen seleccionada en grande
        if (selectedImageUri != null) {
            Dialog(onDismissRequest = { selectedImageUri = null }) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Imagen ampliada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Reproducción de video seleccionado
        if (selectedVideoUri != null) {
            Dialog(onDismissRequest = { selectedVideoUri = null }) {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(selectedVideoUri)
                            setOnPreparedListener { it.start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Botón para guardar tarea
        Button(
            onClick = {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = formatter.parse(dueDate)
                val dueDateLong = date?.time ?: System.currentTimeMillis()

                val task = Task(
                    title = title.text,
                    description = description.text,
                    dueDate = dueDateLong,
                    multimedia = imageUris.joinToString(",") { it.toString() } + "|" +
                            videoUris.joinToString(",") { it.toString() } + "|" +
                            audioUris.joinToString(",") { it.toString() }
                )
                viewModel.addTask(task)
                navController.navigate("tasksList")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Guardar Tarea")
        }
    }
}


//Solicitar permisos
fun checkAndRequestPermissions(context: Context): Boolean {
    val permissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val missingPermissions = permissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
    if (missingPermissions.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            context as Activity,
            missingPermissions.toTypedArray(),
            100 // Código de solicitud
        )
        return false
    }
    return true
}


//Inicar la grabacion de audio
fun startRecording(mediaRecorder: MediaRecorder, tempAudioFile: MutableState<File?>, context: Context) {
    if (!checkAndRequestPermissions(context)) {
        Toast.makeText(context, "Permisos necesarios no concedidos", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val audioFile = createTempAudioFile(context) // Crea un archivo temporal
        tempAudioFile.value = audioFile
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC) // Fuente de audio
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) // Formato de salida
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // Codificador
            setOutputFile(audioFile.absolutePath) // Archivo de salida
            prepare() // Prepara el MediaRecorder
            start() // Comienza la grabación
        }
        Toast.makeText(context, "Grabación iniciada", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al iniciar grabación: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}


// Detener grabación
fun stopRecording
            (mediaRecorder: MediaRecorder,
             tempAudioFile: MutableState<File?>,
             audioUris: MutableList<Uri>,
             context: Context) {
    try {
        mediaRecorder.apply {
            stop() // Detiene la grabación
            reset() // Resetea el MediaRecorder para su próximo uso
        }
        tempAudioFile.value?.let { file ->
            val uri = Uri.fromFile(file) // Convierte el archivo en un URI
            if (!audioUris.contains(uri)) { // Verifica que no se dupliquen URIs
                audioUris.add(uri) // Agrega el URI a la lista de audios
            }
        }
        Toast.makeText(context, "Grabación guardada", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al detener grabación", Toast.LENGTH_SHORT).show()
    }
}

// Reproducir audio
fun playAudio(mediaPlayer: MediaPlayer, uri: Uri, context: Context) {
    try {
        mediaPlayer.apply {
            reset() // Resetea el MediaPlayer para su próximo uso
            setDataSource(context, uri) // Configura la fuente de audio con el URI
            prepare() // Prepara el MediaPlayer
            start() // Inicia la reproducción
        }
        Toast.makeText(context, "Reproduciendo audio", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
    }
}

fun createTempAudioFile(context: Context): File {
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
    if (storageDir != null && !storageDir.exists()) {
        storageDir.mkdirs() // Crea el directorio si no existe
    }
    return File(storageDir, "temp_audio_${System.currentTimeMillis()}.3gp")
}


// Función para crear un archivo temporal donde guardar la imagen tomada por la cámara
fun createTempImageUri(context: Context): Uri {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "temp_image_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: throw IllegalStateException("No se pudo crear el URI de imagen")
}


// Función para crear un archivo temporal para video
fun createTempVideoUri(context: Context): Uri {
    val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "temp_video_${System.currentTimeMillis()}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    }
    return context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: throw IllegalStateException("No se pudo crear el URI de video")
}





fun saveImageToGallery(context: Context, bitmap: Bitmap): Uri {
    val filename = "${System.currentTimeMillis()}.jpg"
    val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File(imagesDir, filename)
    val fos = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
    fos.close()
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
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

        // Nueva sección para íconos de multimedia
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
                    note?.let {
                        viewModel.updateNote(Note(id = it.id, title = title.text, description = description.text))
                    }
                    navController.navigate("notesList")
                },
                modifier = Modifier.padding(4.dp)
            ) {
                Text(stringResource(R.string.guardar_cambios))
            }

            OutlinedButton(
                onClick = { navController.navigate("notesList") },
                modifier = Modifier.padding(4.dp)
            ) {
                Text(stringResource(R.string.cancelar))
            }
        }
    }
}

//Pantalla de editar tareas
@Composable
fun EditTaskScreen(viewModel: TaskViewModel, navController: NavHostController, taskId: Int) {
    val task by viewModel.getTaskById(taskId).collectAsState(initial = null)
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var dueDate by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf(listOf<Uri>()) }
    var videoUris by remember { mutableStateOf(listOf<Uri>()) }
    val audioUris = remember { mutableStateListOf<Uri>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }


    var isRecording by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mediaRecorder = remember { MediaRecorder() }
    val mediaPlayer = remember { MediaPlayer() }

    // URI temporal para capturar imágenes, videos y audios
    val tempImageUri = remember { mutableStateOf<Uri?>(null) }
    val tempVideoUri = remember { mutableStateOf<Uri?>(null) }
    val tempAudioFile = remember { mutableStateOf<File?>(null) }

    // Lanzadores para tomar fotos, grabar videos y seleccionar multimedia desde la galería
    val launcherTakePicture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempImageUri.value != null) {
                tempImageUri.value?.let {
                    imageUris = imageUris + it
                }
            }
        }

    val launcherSelectPictures =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                imageUris = imageUris + uris
            }
        }

    val launcherCaptureVideo =
        rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            if (success && tempVideoUri.value != null) {
                tempVideoUri.value?.let {
                    videoUris = videoUris + it
                }
            }
        }

    val launcherSelectVideos =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                videoUris = videoUris + uris
            }
        }


    val launcherSelectAudios =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                audioUris.addAll(uris)
            }
        }

    // Cargar datos de la tarea seleccionada
    LaunchedEffect(task) {
        task?.let {
            title = TextFieldValue(it.title)
            description = TextFieldValue(it.description)
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dueDate = formatter.format(it.dueDate)

            val multimediaParts = it.multimedia.split("|")
            imageUris = multimediaParts.getOrNull(0)?.split(",")?.mapNotNull { uriString ->
                uriString.trim().takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
            } ?: emptyList()
            videoUris = multimediaParts.getOrNull(1)?.split(",")?.mapNotNull { uriString ->
                uriString.trim().takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
            } ?: emptyList()

            audioUris.clear()
            audioUris.addAll(
                multimediaParts.getOrNull(2)?.split(",")?.mapNotNull { uri ->
                    uri.trim().takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
                } ?: emptyList()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Campo de título
        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Título") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Campo de descripción
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Descripción") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Botón para modificar la fecha y hora
        Button(
            onClick = { /* Mostrar DatePicker y TimePicker */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(if (dueDate.isEmpty()) "Modificar Fecha y Hora" else "Fecha: $dueDate")
        }
        // Sección de multimedia
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Icono para tomar foto
            IconButton(onClick = {
                val uri = createTempImageUri(context)
                tempImageUri.value = uri
                launcherTakePicture.launch(uri)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_foto),
                    contentDescription = "Tomar Foto"
                )
            }

            // Icono para seleccionar fotos desde la galería
            IconButton(onClick = { launcherSelectPictures.launch("image/*") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gallery),
                    contentDescription = "Seleccionar Imágenes"
                )
            }

            // Icono para capturar video
            IconButton(onClick = {
                val uri = createTempVideoUri(context)
                tempVideoUri.value = uri
                launcherCaptureVideo.launch(uri)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video),
                    contentDescription = "Grabar Video"
                )
            }

            // Icono para seleccionar videos desde la galería
            IconButton(onClick = { launcherSelectVideos.launch("video/*") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video2),
                    contentDescription = "Seleccionar Videos"
                )
            }

            // Icono para grabar audio
            IconButton(onClick = {
                if (isRecording) {
                    stopRecording(mediaRecorder, tempAudioFile, audioUris, context)
                    isRecording = false
                } else {
                    startRecording(mediaRecorder, tempAudioFile, context)
                    isRecording = true
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_audio),
                    contentDescription = if (isRecording) "Detener Grabación" else "Grabar Audio",
                    tint = if (isRecording) Color.Red else Color.Black
                )
            }
        }

        // Mostrar audios seleccionados
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(audioUris) { uri ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clickable { playAudio(mediaPlayer, uri, context) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_audio),
                        contentDescription = "Audio seleccionado",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }


        // Mostrar imagen seleccionada en grande
        if (selectedImageUri != null) {
            Dialog(onDismissRequest = { selectedImageUri = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable { selectedImageUri = null }
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Imagen en grande",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Reproducir video seleccionado
        if (selectedVideoUri != null) {
            Dialog(onDismissRequest = { selectedVideoUri = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable { selectedVideoUri = null }
                ) {
                    AndroidView(
                        factory = {
                            VideoView(it).apply {
                                setVideoURI(selectedVideoUri)
                                setOnPreparedListener { it.start() }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Botones de guardar y cancelar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    task?.let {
                        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val date = formatter.parse(dueDate)
                        val dueDateLong = date?.time ?: it.dueDate

                        viewModel.updateTask(
                            Task(
                                id = it.id,
                                title = title.text,
                                description = description.text,
                                dueDate = dueDateLong,
                                multimedia = imageUris.joinToString(",") { it.toString() } +
                                        "|" +
                                        videoUris.joinToString(",") { it.toString() },
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


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    Agenda_PFTheme {
        MainScreen(navController)
    }
}