package com.example.agenda_pf

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Tarea Pendiente"
        val description = intent.getStringExtra("description") ?: "Recuerda completar tu tarea."
        val isDue = intent.getBooleanExtra("isDue", false) // Indica si es la fecha de vencimiento

        val notificationTitle = if (isDue) "Cumplimiento de Tarea" else "Recordatorio"
        val notificationMessage = if (isDue) {
            "La tarea '$title' debe cumplirse ahora."
        } else {
            "Recordatorio: $description"
        }

        // Crear el builder de la notificación
        val notificationBuilder = NotificationCompat.Builder(context, "agenda_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Verificar el permiso antes de enviar la notificación
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(context)) {
                notify((System.currentTimeMillis() % 10000).toInt(), notificationBuilder.build())
            }
        } else {
            // Si no tiene permiso, puedes implementar el manejo del permiso aquí (opcional)
        }
    }
}

