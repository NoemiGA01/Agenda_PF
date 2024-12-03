package com.example.agenda_pf

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("notification_message") ?: "Tienes un recordatorio"
        val notificationId = System.currentTimeMillis().toInt()
        val taskTitle = intent.getStringExtra("taskTitle") ?: return
        // Crear canal de notificación para Android 8.0+ (API 26+)
        val channelId = "task_reminder_channel"
        val channelName = "Recordatorios de Tareas"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCIA ALTA
            ).apply {
                description = "Notificaciones para recordatorios de tareas"
            }
            notificationManager.createNotificationChannel(channel) // Crear el canal
        }

        // Crear la notificación
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono de la notificación
            .setContentTitle("Recordatorio:$taskTitle") // Título de la notificación
            .setContentText(message) // Texto de la notificación
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad ALTA
            .setAutoCancel(true) // Descartar notificación al tocarla
            .build()

        // Mostrar la notificación
        notificationManager.notify(notificationId, notification)
    }
}
