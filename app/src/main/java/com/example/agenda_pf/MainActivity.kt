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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.window.Dialog as Dialog
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build


class MainActivity : ComponentActivity() {

    // Definición de permisos requeridos
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    // Declaración del lanzador de permisos
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Toast.makeText(this, "Todos los permisos concedidos", Toast.LENGTH_SHORT).show()
            } else {
                // Lógica para verificar permisos específicos
                val deniedPermissions = permissions.filter { !it.value }
                if (deniedPermissions.containsKey(android.Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
                if (deniedPermissions.containsKey(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Permiso de lectura denegado", Toast.LENGTH_SHORT).show()
                }
                if (deniedPermissions.containsKey(android.Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(this, "Permiso de audio denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Crear instancias de los repositorios
        val database = DatabaseProvider.getDatabase(applicationContext)
        val noteRepository = OfflineNoteRepository(database.noteDao())
        val taskRepository = OfflineTaskRepository(database.taskDao())

        // Crear instancias de los ViewModelFactory con los repositorios
        val noteViewModelFactory = NoteViewModelFactory(noteRepository)
        val taskViewModelFactory = TaskViewModelFactory(taskRepository)

        // Configurar la interfaz de usuario con Jetpack Compose
        setContent {
            Agenda_PFTheme {
                val navController = rememberNavController()

                // Instancia de los ViewModels usando los factories
                val noteViewModel: NoteViewModel = viewModel(factory = noteViewModelFactory)
                val taskViewModel: TaskViewModel = viewModel(factory = taskViewModelFactory)

                // Configuración de la navegación
                Navigation(navController, noteViewModel, taskViewModel)
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
                    title = {
                        Text(
                            stringResource(R.string.agenda),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color(0xFFE1BEE7)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    showDialog = true
                }) { // Muestra el diálogo al hacer clic
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_input_add),
                        contentDescription = "Agregar"
                    )
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
                    Text(
                        stringResource(R.string.notas),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Image(
                        painter = painterResource(id = R.drawable.ic_tareas),
                        contentDescription = "Tareas",
                        modifier = Modifier
                            .size(150.dp)
                            .clickable { navController.navigate("tasksList") }
                    )
                    Text(
                        stringResource(R.string.tareas),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        var imageUris by remember { mutableStateOf(listOf<Uri>()) }
        var videoUris by remember { mutableStateOf(listOf<Uri>()) }
        val audioUris = remember { mutableStateListOf<Uri>() }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
        var isRecording by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val mediaRecorder = remember { MediaRecorder() }
        val mediaPlayer = remember { MediaPlayer() }
        val tempAudioFile = remember { mutableStateOf<File?>(null) }
        val tempVideoUri = remember { mutableStateOf<Uri?>(null) }

        // Lanzadores para multimedia
        val launcherTakePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                val uri = saveImageToInternalStorage(context, it)
                imageUris = imageUris + uri
            }
        }

        val launcherSelectPictures = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris.forEach { uri ->
                val savedUri = saveUriToInternalStorage(context, uri, "image_${System.currentTimeMillis()}.jpg")
                imageUris = imageUris + savedUri
            }
        }

        val launcherCaptureVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            if (success) {
                tempVideoUri.value?.let { uri ->
                    val savedUri = saveUriToInternalStorage(context, uri, "video_${System.currentTimeMillis()}.mp4")
                    videoUris = videoUris + savedUri
                } ?: run {
                    Toast.makeText(context, "Error al capturar el video", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Captura de video cancelada", Toast.LENGTH_SHORT).show()
            }
        }

        val launcherSelectVideos = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris.forEach { uri ->
                val savedUri = saveUriToInternalStorage(context, uri, "video_${System.currentTimeMillis()}.mp4")
                videoUris = videoUris + savedUri
            }
        }

        // UI para la pantalla de agregar nota
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Título y descripción de la nota
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

            // Botones para multimedia
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón para tomar una foto
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.CAMERA), 101)) {
                        launcherTakePicture.launch()
                    } else {
                        Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_foto),
                        contentDescription = "Tomar Foto"
                    )
                }

                // Botón para seleccionar una foto de la galería
                IconButton(onClick = {
                    launcherSelectPictures.launch("image/*")
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gallery),
                        contentDescription = "Seleccionar Fotos"
                    )
                }

                // Botón para grabar un video
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.CAMERA), 103)) {
                        val videoFile = createVideoFile(context)
                        val videoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            videoFile
                        )
                        tempVideoUri.value = videoUri
                        launcherCaptureVideo.launch(videoUri)
                    } else {
                        Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video),
                        contentDescription = "Capturar Video"
                    )
                }

                // Botón para seleccionar un video de la galería
                IconButton(onClick = {
                    launcherSelectVideos.launch("video/*")
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video2),
                        contentDescription = "Seleccionar Videos"
                    )
                }

                // Botón para grabar audio
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.RECORD_AUDIO), 105)) {
                        if (isRecording) {
                            stopRecording(mediaRecorder, tempAudioFile, audioUris, context)
                            isRecording = false
                        } else {
                            startRecording(mediaRecorder, tempAudioFile, context)
                            isRecording = true
                        }
                    } else {
                        Toast.makeText(context, "Permiso de grabación de audio requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_audio),
                        contentDescription = if (isRecording) "Detener Grabación" else "Grabar Audio",
                        tint = if (isRecording) Color.Red else Color.Black
                    )
                }
            }

            // Visualización de multimedia (imágenes, videos, audios)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(imageUris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Imagen seleccionada",
                        modifier = Modifier.size(60.dp)
                            .clickable { selectedImageUri = uri },
                        contentScale = ContentScale.Crop
                    )
                }
                items(videoUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable { selectedVideoUri = uri },
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

            // Visualización ampliada de imagen seleccionada
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
                                setOnErrorListener { _, _, _ ->
                                    Toast.makeText(
                                        context,
                                        "Error al reproducir el video",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Botón para guardar la nota con multimedia
            Button(
                onClick = {
                    val note = Note(
                        title = title.text,
                        description = description.text,
                        multimedia = imageUris.joinToString(",") { it.toString() } + "|" +
                                videoUris.joinToString(",") { it.toString() } + "|" +
                                audioUris.joinToString(",") { it.toString() }
                    )
                    viewModel.addNote(note)
                    navController.navigate("notesList")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Guardar Nota")
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
        var isRecording by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val mediaRecorder = remember { MediaRecorder() }
        val mediaPlayer = remember { MediaPlayer() }
        val tempAudioFile = remember { mutableStateOf<File?>(null) }
        val tempVideoUri = remember { mutableStateOf<Uri?>(null) }

        // Lanzadores para multimedia
        val launcherTakePicture =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                bitmap?.let {
                    val uri = saveImageToInternalStorage(context, it)
                    imageUris = imageUris + uri
                }
            }


        val launcherSelectPictures =
            rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                uris.forEach { uri ->
                    val savedUri = saveUriToInternalStorage(
                        context,
                        uri,
                        "image_${System.currentTimeMillis()}.jpg"
                    )
                    imageUris = imageUris + savedUri
                }
            }


        val launcherCaptureVideo =
            rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
                if (success) {
                    tempVideoUri.value?.let { uri ->
                        val savedUri = saveUriToInternalStorage(
                            context,
                            uri,
                            "video_${System.currentTimeMillis()}.mp4"
                        )
                        videoUris = videoUris + savedUri
                    } ?: run {
                        Toast.makeText(context, "Error al capturar el video", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(context, "Captura de video cancelada", Toast.LENGTH_SHORT).show()
                }
            }


        val launcherSelectVideos =
            rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                uris.forEach { uri ->
                    val savedUri = saveUriToInternalStorage(
                        context,
                        uri,
                        "video_${System.currentTimeMillis()}.mp4"
                    )
                    videoUris = videoUris + savedUri
                }
            }


        // Control de fecha y hora
        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf("") }
        var showTimePicker by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Campos de título y descripción
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

            // Botones de multimedia
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón para tomar una foto
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.CAMERA), 101)) {
                        launcherTakePicture.launch()
                    } else {
                        Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_foto),
                        contentDescription = "Tomar Foto"
                    )
                }

                // Botón para seleccionar una foto de la galería
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 102)) {
                        launcherSelectPictures.launch("image/*")
                    } else {
                        Toast.makeText(context, "Permiso de galería requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gallery),
                        contentDescription = "Seleccionar Fotos"
                    )
                }

                // Botón para grabar un video
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.CAMERA), 103)) {
                        val videoFile = createVideoFile(context)
                        val videoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            videoFile
                        )
                        tempVideoUri.value = videoUri
                        launcherCaptureVideo.launch(videoUri)
                    } else {
                        Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video),
                        contentDescription = "Capturar Video"
                    )
                }

                // Botón para seleccionar un video de la galería
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 104)) {
                        launcherSelectVideos.launch("video/*")
                    } else {
                        Toast.makeText(context, "Permiso de galería requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video2),
                        contentDescription = "Seleccionar Videos"
                    )
                }

                // Botón para grabar audio
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.RECORD_AUDIO), 105)) {
                        if (isRecording) {
                            stopRecording(mediaRecorder, tempAudioFile, audioUris, context)
                            isRecording = false
                        } else {
                            startRecording(mediaRecorder, tempAudioFile, context)
                            isRecording = true
                        }
                    } else {
                        Toast.makeText(context, "Permiso de grabación de audio requerido", Toast.LENGTH_SHORT).show()
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
                        modifier = Modifier.size(60.dp)
                            .clickable { selectedImageUri = uri },
                        contentScale = ContentScale.Crop
                    )
                }
                items(videoUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable { selectedVideoUri = uri },
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

            // Visualización ampliada de imagen seleccionada
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
                                setOnErrorListener { _, _, _ ->
                                    Toast.makeText(
                                        context,
                                        "Error al reproducir el video",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Botón para guardar tarea
            Button(
                onClick = {
                    // Convertir la fecha y hora seleccionada a un Long
                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val date = formatter.parse(dueDate) // Convierte la fecha seleccionada a Date
                    val dueDateLong = date?.time ?: System.currentTimeMillis() // Convierte Date a Long


                    val task = Task(
                        title = title.text,
                        description = description.text,
                        dueDate = dueDateLong
                    )
                    viewModel.addTask(task)


                    // Programar la alarma
                    scheduleAlarm(
                        context = context,
                        title = task.title,
                        description = task.description,
                        timeInMillis = dueDateLong
                    )
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


    //Solicitar permisos
    fun checkAndRequestPermissions(context: Context, permissions: Array<String>, requestCode: Int): Boolean {
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        return if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
            false
        } else {
            true
        }
    }



    //Inicar la grabacion de audio
    fun startRecording(
        mediaRecorder: MediaRecorder,
        tempAudioFile: MutableState<File?>,
        context: Context
    ) {
        // Agrega los permisos requeridos y un requestCode (ejemplo: 103 para grabación de audio)
        if (!checkAndRequestPermissions(context, requiredPermissions, 103)) {
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
            Toast.makeText(context, "Error al iniciar grabación: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }



    // Detener grabación
    fun stopRecording(
        mediaRecorder: MediaRecorder,
        tempAudioFile: MutableState<File?>,
        audioUris: MutableList<Uri>,
        context: Context
    ) {
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


    fun createVideoFile(context: Context): File {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "video_${System.currentTimeMillis()}.mp4")
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
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color(
                            0xFFE1BEE7
                        )
                    ),
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
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color(
                            0xFFE1BEE7
                        )
                    ),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = note.title, fontWeight = FontWeight.Bold)
                Text(text = note.description)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar"
                )
            }
        }
    }


    @Composable
    fun TaskItem(
        task: Task,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onCompleteChange: (Boolean) -> Unit
    ) {
        val dateFormatter = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ) // Formato para mostrar la fecha
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
                Text(
                    text = "Fecha: $formattedDate",
                    fontSize = 12.sp,
                    color = Color.Gray
                ) // Muestra la fecha
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
        var imageUris by remember { mutableStateOf(listOf<Uri>()) }
        var videoUris by remember { mutableStateOf(listOf<Uri>()) }
        val audioUris = remember { mutableStateListOf<Uri>() }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
        var isRecording by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val mediaRecorder = remember { MediaRecorder() }
        val mediaPlayer = remember { MediaPlayer() }
        val tempAudioFile = remember { mutableStateOf<File?>(null) }
        val tempVideoUri = remember { mutableStateOf<Uri?>(null) }

        // Lanzadores para multimedia
        val launcherTakePicture =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                bitmap?.let {
                    val uri = saveImageToInternalStorage(context, it)
                    imageUris = imageUris + uri
                }
            }

        val launcherSelectPictures = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris.forEach { uri ->
                val savedUri = saveUriToInternalStorage(context, uri, "image_${System.currentTimeMillis()}.jpg")
                imageUris = imageUris + savedUri
            }
        }

        val launcherCaptureVideo =
            rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
                if (success) {
                    tempVideoUri.value?.let { uri ->
                        val savedUri = saveUriToInternalStorage(
                            context,
                            uri,
                            "video_${System.currentTimeMillis()}.mp4"
                        )
                        videoUris = videoUris + savedUri
                    } ?: run {
                        Toast.makeText(context, "Error al capturar el video", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        val launcherSelectVideos = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris.forEach { uri ->
                val savedUri = saveUriToInternalStorage(context, uri, "video_${System.currentTimeMillis()}.mp4")
                videoUris = videoUris + savedUri
            }
        }


        // Cargar la nota al iniciar la pantalla
        LaunchedEffect(note) {
            note?.let {
                title = TextFieldValue(it.title)
                description = TextFieldValue(it.description)

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
            // Campos de texto
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Título") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            // Botones de multimedia
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón para tomar una foto
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.CAMERA), 101)) {
                        launcherTakePicture.launch()
                    } else {
                        Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_foto),
                        contentDescription = "Tomar Foto"
                    )
                }
                //Boton para seleccionar imagen de galeria
                IconButton(onClick = { launcherSelectPictures.launch("image/*") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gallery),
                        contentDescription = "Seleccionar Fotos"
                    )
                }

                // Botón para grabar un video
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.CAMERA), 103)) {
                        val videoFile = createVideoFile(context)
                        val videoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            videoFile
                        )
                        tempVideoUri.value = videoUri
                        launcherCaptureVideo.launch(videoUri)
                    } else {
                        Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video),
                        contentDescription = "Capturar Video"
                    )
                }
                //boton para seleccionar video de galeria
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
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                items(imageUris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Imagen seleccionada",
                        modifier = Modifier.size(60.dp).clickable { selectedImageUri = uri },
                        contentScale = ContentScale.Crop
                    )
                }

                items(videoUris) { uri ->
                    Box(
                        modifier = Modifier.size(60.dp).clickable { selectedVideoUri = uri },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video),
                            contentDescription = "Video seleccionado"
                        )
                    }
                }

                items(audioUris) { uri ->
                    Box(
                        modifier = Modifier.size(60.dp).clickable { playAudio(mediaPlayer, uri, context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_audio),
                            contentDescription = "Audio seleccionado"
                        )
                    }
                }
            }

            // Visualización ampliada de imagen
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

            // Botones de guardar y cancelar
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    note?.let {
                        viewModel.updateNote(
                            Note(
                                id = it.id,
                                title = title.text,
                                description = description.text,
                                multimedia = imageUris.joinToString(",") { it.toString() } +
                                        "|" +
                                        videoUris.joinToString(",") { it.toString() } +
                                        "|" +
                                        audioUris.joinToString(",") { it.toString() }
                            )
                        )
                    }
                    navController.navigate("notesList")
                }) {
                    Text("Guardar Cambios")
                }

                OutlinedButton(onClick = { navController.navigate("notesList") }) {
                    Text("Cancelar")
                }
            }
        }
    }



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
        var isRecording by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val mediaRecorder = remember { MediaRecorder() }
        val mediaPlayer = remember { MediaPlayer() }
        val tempAudioFile = remember { mutableStateOf<File?>(null) }

        val tempImageUri = remember { mutableStateOf<Uri?>(null) }
        val tempVideoUri = remember { mutableStateOf<Uri?>(null) }

        // Lanzadores para imágenes, videos y audios
        val launcherTakePicture =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                bitmap?.let {
                    val uri = saveImageToInternalStorage(context, it)
                    imageUris = imageUris + uri
                }
            }


        val launcherSelectPictures =
            rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                uris.forEach { uri ->
                    val savedUri = saveUriToInternalStorage(
                        context,
                        uri,
                        "image_${System.currentTimeMillis()}.jpg"
                    )
                    imageUris = imageUris + savedUri
                }
            }


        val launcherCaptureVideo =
            rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
                if (success) {
                    tempVideoUri.value?.let { uri ->
                        val savedUri = saveUriToInternalStorage(
                            context,
                            uri,
                            "video_${System.currentTimeMillis()}.mp4"
                        )
                        videoUris = videoUris + savedUri
                    } ?: run {
                        Toast.makeText(context, "Error al capturar el video", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }


        val launcherSelectVideos =
            rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                uris.forEach { uri ->
                    val savedUri = saveUriToInternalStorage(
                        context,
                        uri,
                        "video_${System.currentTimeMillis()}.mp4"
                    )
                    videoUris = videoUris + savedUri
                }
            }


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
            // Campos de texto para título y descripción
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Título") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            // Botón para seleccionar fecha
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
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

            // Botones para multimedia
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón para tomar una foto
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.CAMERA), 101)) {
                        launcherTakePicture.launch()
                    } else {
                        Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_foto),
                        contentDescription = "Tomar Foto"
                    )
                }

                // Botón para seleccionar una foto de la galería
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 102)) {
                        launcherSelectPictures.launch("image/*")
                    } else {
                        Toast.makeText(context, "Permiso de galería requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gallery),
                        contentDescription = "Seleccionar Fotos"
                    )
                }

                // Botón para grabar un video
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.CAMERA), 103)) {
                        val videoFile = createVideoFile(context)
                        val videoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            videoFile
                        )
                        tempVideoUri.value = videoUri
                        launcherCaptureVideo.launch(videoUri)
                    } else {
                        Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video),
                        contentDescription = "Capturar Video"
                    )
                }

                // Botón para seleccionar un video de la galería
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 104)) {
                        launcherSelectVideos.launch("video/*")
                    } else {
                        Toast.makeText(context, "Permiso de galería requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video2),
                        contentDescription = "Seleccionar Videos"
                    )
                }

                // Botón para grabar audio
                IconButton(onClick = {
                    if (checkAndRequestPermissions(context, arrayOf(Manifest.permission.RECORD_AUDIO), 105)) {
                        if (isRecording) {
                            stopRecording(mediaRecorder, tempAudioFile, audioUris, context)
                            isRecording = false
                        } else {
                            startRecording(mediaRecorder, tempAudioFile, context)
                            isRecording = true
                        }
                    } else {
                        Toast.makeText(context, "Permiso de grabación de audio requerido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_audio),
                        contentDescription = if (isRecording) "Detener Grabación" else "Grabar Audio",
                        tint = if (isRecording) Color.Red else Color.Black
                    )
                }
            }



            // Mostrar multimedia existente
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                items(imageUris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Imagen seleccionada",
                        modifier = Modifier.size(60.dp).clickable { selectedImageUri = uri },
                        contentScale = ContentScale.Crop
                    )
                }

                items(videoUris) { uri ->
                    Box(
                        modifier = Modifier.size(60.dp).clickable { selectedVideoUri = uri },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video),
                            contentDescription = "Video seleccionado"
                        )
                    }
                }

                items(audioUris) { uri ->
                    Box(
                        modifier = Modifier.size(60.dp)
                            .clickable { playAudio(mediaPlayer, uri, context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_audio),
                            contentDescription = "Audio seleccionado"
                        )
                    }
                }
            }

            // Ver imagen en grande
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
                                setOnErrorListener { _, _, _ ->
                                    Toast.makeText(
                                        context,
                                        "Error al reproducir el video",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Espaciado adicional antes de los botones
            Spacer(modifier = Modifier.height(32.dp))

            // Botones para guardar y cancelar
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        task?.let {
                            val formatter =
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
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
                                            videoUris.joinToString(",") { it.toString() } +
                                            "|" +
                                            audioUris.joinToString(",") { it.toString() }
                                )
                            )
                        }
                        navController.navigate("tasksList")
                    },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text("Guardar Cambios")
                }

                OutlinedButton(
                    onClick = { navController.navigate("tasksList") },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text("Cancelar")
                }
            }
        }
    }


    fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): Uri {
        val filename = "image_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
        return Uri.fromFile(file)
    }


    fun saveUriToInternalStorage(context: Context, uri: Uri, filename: String): Uri {
        val file = File(context.filesDir, filename)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return Uri.fromFile(file)
    }


    @Preview(showBackground = true)
    @Composable
    fun MainScreenPreview() {
        val navController = rememberNavController()
        Agenda_PFTheme {
            MainScreen(navController)
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Agenda Notifications"
            val descriptionText = "Canal para recordatorios de la agenda"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("agenda_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun scheduleAlarm(context: Context, title: String, description: String, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("description", description)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    }

}