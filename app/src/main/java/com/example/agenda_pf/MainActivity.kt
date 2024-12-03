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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.random.Random
import java.util.Date




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
        // Crear el canal de notificación al inicio de la app
        createNotificationChannel(this)
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun AddNoteScreen(viewModel: NoteViewModel, navController: NavHostController) {
        var title by remember { mutableStateOf(TextFieldValue("")) }
        var description by remember { mutableStateOf(TextFieldValue("")) }
        var imageUris by remember { mutableStateOf(listOf<Uri>()) }
        var videoUris by remember { mutableStateOf(listOf<Uri>()) }
        var audioUris = remember { mutableStateListOf<Uri>() }
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

            // Visualización de multimedia con eliminación al mantener presionado
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Elementos de imagen
                items(imageUris) { uri ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(60.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .size(60.dp)
                                .combinedClickable(
                                    onClick = { selectedImageUri = uri },
                                    onLongClick = {
                                        val updatedList = imageUris.toMutableList()
                                        updatedList.remove(uri)
                                        imageUris = updatedList
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Elementos de video
                items(videoUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .combinedClickable(
                                onClick = { selectedVideoUri = uri },
                                onLongClick = {
                                    val updatedList = videoUris.toMutableList()
                                    updatedList.remove(uri)
                                    videoUris = updatedList
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video),
                            contentDescription = "Video seleccionado",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Elementos de audio
                items(audioUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .combinedClickable(
                                onClick = { playAudio(mediaPlayer, uri, context) },
                                onLongClick = {
                                    val updatedList = audioUris.toMutableList()
                                    updatedList.remove(uri)
                                    audioUris = updatedList as SnapshotStateList<Uri>
                                }
                            )
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



    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun AddTaskScreen(viewModel: TaskViewModel, navController: NavHostController) {
        var title by remember { mutableStateOf(TextFieldValue("")) }
        var description by remember { mutableStateOf(TextFieldValue("")) }
        var dueDate by remember { mutableStateOf<Long?>(null) }
        var imageUris by remember { mutableStateOf(listOf<Uri>()) }
        var videoUris by remember { mutableStateOf(listOf<Uri>()) }
        var audioUris = remember { mutableStateListOf<Uri>() }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
        var isRecording by remember { mutableStateOf(false) }



        val context = LocalContext.current
        val mediaRecorder = remember { MediaRecorder() }
        val mediaPlayer = remember { MediaPlayer() }
        val tempAudioFile = remember { mutableStateOf<File?>(null) }
        val tempVideoUri = remember { mutableStateOf<Uri?>(null) }

        // Mantener las fechas separadas para que no se sobreescriban
        var taskDueDate by remember { mutableStateOf<Long?>(null) }
        val reminders = remember { mutableStateListOf<Pair<Long, String>>() } // Recordatorios: Pair<TimeMillis, DisplayText>




        fun scheduleNotification(context: Context, timeMillis: Long, message: String, taskTitle: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("notification_message", message)
                putExtra("taskTitle", taskTitle) // Pasar el título de la tarea
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                timeMillis.hashCode(), // ID único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                pendingIntent
            )
        }

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
        // Variables separadas para controlar los diálogos
        var showDatePickerForDueDate by remember { mutableStateOf(false) }
        var showDatePickerForReminder by remember { mutableStateOf(false) }

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
            // Botón de Fecha Final
            Button(onClick = { showDatePickerForDueDate = true }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    text = if (taskDueDate != null) {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(taskDueDate!!))
                    } else {
                        "Seleccionar Fecha Final"
                    }
                )
            }

// Mostrar DatePicker para la Fecha Final
            if (showDatePickerForDueDate) {
                val calendar = Calendar.getInstance()
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val dueDateCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                        }

                        // Mostrar TimePicker después de seleccionar la fecha
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                dueDateCalendar.set(Calendar.HOUR_OF_DAY, hour)
                                dueDateCalendar.set(Calendar.MINUTE, minute)
                                dueDateCalendar.set(Calendar.SECOND, 0)
                                dueDateCalendar.set(Calendar.MILLISECOND, 0)

                                taskDueDate = dueDateCalendar.timeInMillis // Asigna la fecha final
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                        showDatePickerForDueDate = false
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }


            // Botón para Agregar Recordatorio
            Button(onClick = { showDatePickerForReminder = true }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Agregar Recordatorio")
            }

// Mostrar DatePicker para agregar recordatorio
            if (showDatePickerForReminder) {
                val calendar = Calendar.getInstance()
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val reminderCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                        }

                        // Mostrar TimePicker para el recordatorio
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                reminderCalendar.set(Calendar.HOUR_OF_DAY, hour)
                                reminderCalendar.set(Calendar.MINUTE, minute)
                                reminderCalendar.set(Calendar.SECOND, 0)
                                reminderCalendar.set(Calendar.MILLISECOND, 0)

                                // Agregar recordatorio a la lista
                                reminders.add(
                                    Pair(
                                        reminderCalendar.timeInMillis,
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(reminderCalendar.time)
                                    )
                                )
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                        showDatePickerForReminder = false
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            // Mostrar Lista de Recordatorios
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recordatorios", style = MaterialTheme.typography.titleMedium)
            reminders.forEachIndexed { index, reminder ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(reminder.second)
                    IconButton(onClick = { reminders.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
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
    // Visualización de multimedia con eliminación al mantener presionado
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Elementos de imagen
                items(imageUris) { uri ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(60.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .size(60.dp)
                                .combinedClickable(
                                    onClick = { selectedImageUri = uri },
                                    onLongClick = {
                                        val updatedList = imageUris.toMutableList()
                                        updatedList.remove(uri)
                                        imageUris = updatedList
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Elementos de video
                items(videoUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .combinedClickable(
                                onClick = { selectedVideoUri = uri },
                                onLongClick = {
                                    val updatedList = videoUris.toMutableList()
                                    updatedList.remove(uri)
                                    videoUris = updatedList
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video),
                            contentDescription = "Video seleccionado",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Elementos de audio
                items(audioUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .combinedClickable(
                                onClick = { playAudio(mediaPlayer, uri, context) },
                                onLongClick = {
                                    val updatedList = audioUris.toMutableList()
                                    updatedList.remove(uri)
                                    audioUris = updatedList as SnapshotStateList<Uri>
                                }
                            )
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

            // Botón para guardar la tarea
            Button(
                onClick = {
                    val task = Task(
                        title = title.text,
                        description = description.text,
                        dueDate = taskDueDate ?: System.currentTimeMillis(),
                        reminderDate = reminders.firstOrNull()?.first,
                        multimedia = imageUris.joinToString(",") { it.toString() } + "|" +
                                videoUris.joinToString(",") { it.toString() } + "|" +
                                audioUris.joinToString(",") { it.toString() }
                    )
                    viewModel.addTask(task)
                    // Programar las notificaciones de los recordatorios
                    reminders.forEach {
                        scheduleNotification(
                            context = context,
                            timeMillis = it.first, // Tiempo del recordatorio
                            message = "Recordatorio: ${it.second}", // Mensaje del recordatorio
                            taskTitle = title.text // Título de la tarea
                        )
                    }

// Programar la notificación para la fecha final
                    taskDueDate?.let {
                        scheduleNotification(
                            context = context,
                            timeMillis = it, // Tiempo de vencimiento
                            message = "¡Tu tarea ya venció!", // Mensaje de vencimiento
                            taskTitle = title.text // Título de la tarea
                        )
                    }

                    navController.navigate("tasksList")
                },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text("Guardar Tarea")
            }

        }
    }





    //Solicitar permisos
    fun checkAndRequestPermissions(
        context: Context,
        permissions: Array<String>,
        requestCode: Int
    ): Boolean {
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


    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun EditNoteScreen(viewModel: NoteViewModel, navController: NavHostController, noteId: Int) {
        val note by viewModel.getNoteById(noteId).collectAsState(initial = null)
        var title by remember { mutableStateOf(TextFieldValue("")) }
        var description by remember { mutableStateOf(TextFieldValue("")) }
        var imageUris by remember { mutableStateOf(listOf<Uri>()) }
        var videoUris by remember { mutableStateOf(listOf<Uri>()) }
        var audioUris = remember { mutableStateListOf<Uri>() }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
        var isRecording by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val mediaRecorder = remember { MediaRecorder() }
        val mediaPlayer = remember { MediaPlayer() }
        val tempAudioFile = remember { mutableStateOf<File?>(null) }
        val tempVideoUri = remember { mutableStateOf<Uri?>(null) }

        val reminders = remember { mutableStateListOf<Pair<String, Long>>() }

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

            // Visualización de multimedia con eliminación al mantener presionado
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Elementos de imagen
                items(imageUris) { uri ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(60.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .size(60.dp)
                                .combinedClickable(
                                    onClick = { selectedImageUri = uri },
                                    onLongClick = {
                                        val updatedList = imageUris.toMutableList()
                                        updatedList.remove(uri)
                                        imageUris = updatedList
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Elementos de video
                items(videoUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .combinedClickable(
                                onClick = { selectedVideoUri = uri },
                                onLongClick = {
                                    val updatedList = videoUris.toMutableList()
                                    updatedList.remove(uri)
                                    videoUris = updatedList
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video),
                            contentDescription = "Video seleccionado",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Elementos de audio
                items(audioUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .combinedClickable(
                                onClick = { playAudio(mediaPlayer, uri, context) },
                                onLongClick = {
                                    val updatedList = audioUris.toMutableList()
                                    updatedList.remove(uri)
                                    audioUris = updatedList as SnapshotStateList<Uri>
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_audio),
                            contentDescription = "Audio seleccionado",
                            modifier = Modifier.align(Alignment.Center)
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


    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun EditTaskScreen(viewModel: TaskViewModel, navController: NavHostController, taskId: Int) {
        val task by viewModel.getTaskById(taskId).collectAsState(initial = null)
        var title by remember { mutableStateOf(TextFieldValue("")) }
        var description by remember { mutableStateOf(TextFieldValue("")) }
        var dueDate by remember { mutableStateOf<Long?>(null) }
        var imageUris by remember { mutableStateOf(listOf<Uri>()) }
        var videoUris by remember { mutableStateOf(listOf<Uri>()) }
        var audioUris = remember { mutableStateListOf<Uri>() }
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

        val reminders = remember { mutableStateListOf<Pair<Long, String>>() }

        var showDatePickerForDueDate by remember { mutableStateOf(false) }
        var showDatePickerForReminder by remember { mutableStateOf(false) }

        fun scheduleNotification(context: Context, timeMillis: Long, message: String, taskTitle: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("notification_message", message)
                putExtra("task_title", taskTitle)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                timeMillis.hashCode(), // Un identificador único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                pendingIntent
            )
        }


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
                dueDate = it.dueDate

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

            // Mostrar y editar fecha final
            Button(onClick = { showDatePickerForDueDate = true }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    text = dueDate?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                    } ?: "Seleccionar Fecha Final"
                )
            }

            // Mostrar DatePicker para la Fecha Final
            if (showDatePickerForDueDate) {
                val calendar = Calendar.getInstance()
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val dueDateCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                        }

                        // Mostrar TimePicker después de seleccionar la fecha
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                dueDateCalendar.set(Calendar.HOUR_OF_DAY, hour)
                                dueDateCalendar.set(Calendar.MINUTE, minute)
                                dueDateCalendar.set(Calendar.SECOND, 0)
                                dueDateCalendar.set(Calendar.MILLISECOND, 0)

                                dueDate = dueDateCalendar.timeInMillis // Asigna la fecha final
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                        showDatePickerForDueDate = false
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            Button(onClick = { dueDate = null }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Eliminar Fecha Final")
            }




            // Botón para Agregar Recordatorio
            Button(onClick = { showDatePickerForReminder = true }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Agregar Recordatorio")
            }

// Mostrar DatePicker para agregar recordatorio
            if (showDatePickerForReminder) {
                val calendar = Calendar.getInstance()
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val reminderCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                        }

                        // Mostrar TimePicker para el recordatorio
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                reminderCalendar.set(Calendar.HOUR_OF_DAY, hour)
                                reminderCalendar.set(Calendar.MINUTE, minute)
                                reminderCalendar.set(Calendar.SECOND, 0)
                                reminderCalendar.set(Calendar.MILLISECOND, 0)

                                // Agregar recordatorio a la lista
                                reminders.add(
                                    Pair(
                                        reminderCalendar.timeInMillis,
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(reminderCalendar.time)
                                    )
                                )
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                        showDatePickerForReminder = false
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            // Mostrar Lista de Recordatorios
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recordatorios", style = MaterialTheme.typography.titleMedium)
            reminders.forEachIndexed { index, reminder ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(reminder.second)
                    IconButton(onClick = { reminders.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
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


// Visualización de multimedia con eliminación al mantener presionado
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Elementos de imagen
                items(imageUris) { uri ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(60.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .size(60.dp)
                                .combinedClickable(
                                    onClick = { selectedImageUri = uri },
                                    onLongClick = {
                                        val updatedList = imageUris.toMutableList()
                                        updatedList.remove(uri)
                                        imageUris = updatedList
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Elementos de video
                items(videoUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .combinedClickable(
                                onClick = { selectedVideoUri = uri },
                                onLongClick = {
                                    val updatedList = videoUris.toMutableList()
                                    updatedList.remove(uri)
                                    videoUris = updatedList
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video),
                            contentDescription = "Video seleccionado",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Elementos de audio
                items(audioUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .combinedClickable(
                                onClick = { playAudio(mediaPlayer, uri, context) },
                                onLongClick = {
                                    val updatedList = audioUris.toMutableList()
                                    updatedList.remove(uri)
                                    audioUris = updatedList as SnapshotStateList<Uri>
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_audio),
                            contentDescription = "Audio seleccionado",
                            modifier = Modifier.align(Alignment.Center)
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


                            viewModel.updateTask(
                                Task(
                                    id = it.id,
                                    title = title.text,
                                    description = description.text,
                                    dueDate = dueDate ?: System.currentTimeMillis(),
                                    multimedia = imageUris.joinToString(",") { it.toString() } +
                                            "|" +
                                            videoUris.joinToString(",") { it.toString() } +
                                            "|" +
                                            audioUris.joinToString(",") { it.toString() }
                                )
                            )
                            // Programar notificaciones para recordatorios
                            reminders.forEach { reminder ->
                                scheduleNotification(
                                    context,
                                    timeMillis = reminder.first,
                                    message = "Recordatorio: ${reminder.second}",
                                    taskTitle = title.text
                                )
                            }

                            // También programa una notificación para la fecha final (opcional)
                            dueDate?.let {
                                scheduleNotification(
                                    context,
                                    timeMillis = it,
                                    message = "¡La tarea '${title.text}' vence ahora!",
                                    taskTitle = title.text
                                )
                            }

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

    //funcion para recordatorios
    fun setReminder(context: Context, dueDate: Long, taskTitle: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("taskTitle", taskTitle)
        }

        // Generamos un PendingIntent único utilizando un ID aleatorio.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Random.nextInt(), // ID único para cada recordatorio
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programamos la alarma para la hora seleccionada.
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            dueDate,
            pendingIntent
        )
    }


    //Canal de notificacion
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_reminder_channel",  // Este es el mismo nombre que usas en ReminderReceiver
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de recordatorios de tareas."
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}