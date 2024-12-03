package com.example.agenda_pf

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("taskTitle") ?: return

        // Obtener el NotificationManager para gestionar las notificaciones
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //cada notificación tenga un ID único
        val notificationId = Random.nextInt()  // ID único para cada notificación

        // Crea la notificación
        val notification = NotificationCompat.Builder(context, "task_reminder_channel")
            .setContentTitle("Recordatorio: $taskTitle")
            .setContentText("¡Es hora de tu tarea!")
            .setSmallIcon(R.drawable.ic_notification)  // Asegúrate de tener este ícono en recursos
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Mostrar la notificación
        notificationManager.notify(notificationId, notification)
    }
}
