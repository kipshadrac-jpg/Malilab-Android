package online.malilab.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit
import online.malilab.app.MainActivity
import online.malilab.app.R

/** Looks at every plan a few times a day and tells the person when a payment is due. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            Reminders.check(applicationContext)
        } catch (e: Exception) {
            // A reminder must never crash the app; the next run tries again.
        }
        return Result.success()
    }
}

/** The "I paid" button on a reminder: logs the payment on the phone, then closes the notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Reminders.ACTION_PAID) return
        val uid = intent.getStringExtra("uid") ?: return
        val plan = PortfolioStore.all().firstOrNull { it.uid == uid } ?: return
        PortfolioStore.save(
            plan.copy(payments = plan.payments + Payment(java.util.UUID.randomUUID().toString(), Day.today().iso(), plan.contribution)),
        )
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra("nid", 0))
    }
}

object Reminders {
    const val CHANNEL = "payments"
    const val ACTION_PAID = "online.malilab.app.MARK_PAID"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Payment reminders", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Tells you when a payment into one of your plans is due."
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /** Starts the regular check (every 6 hours) and runs one check now. Safe to call again and again. */
    fun schedule(context: Context) {
        ensureChannel(context)
        val work = WorkManager.getInstance(context)
        work.enqueueUniquePeriodicWork("payment-reminders", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build())
        runNow(context)
    }

    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("payment-reminders-now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ReminderWorker>().build())
    }

    fun canNotify(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    fun check(context: Context) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val prefs = context.getSharedPreferences("malilab_reminders", Context.MODE_PRIVATE)
        val today = Day.today()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        for (p in PortfolioStore.all()) {
            if (!p.reminderOn || p.contribution <= 0.0) continue
            val st = PortfolioEngine.standing(p, today)
            val due = st.nextDue ?: continue
            if (st.matured) continue
            val daysLeft = (due.epoch() - today.epoch()).toInt()
            if (daysLeft > p.reminderDaysBefore) continue
            if (daysLeft == p.reminderDaysBefore && hour < p.reminderHour) continue
            if (p.payments.any { it.date == due.iso() || it.date == today.iso() }) continue
            val key = p.uid + "|" + due.iso()
            if (prefs.getBoolean(key, false)) continue
            post(context, p, due, daysLeft)
            prefs.edit().putBoolean(key, true).apply()
        }
    }

    private fun post(context: Context, p: Plan, due: Day, daysLeft: Int) {
        val id = p.uid.hashCode()
        val whenText = when {
            daysLeft <= 0 -> "today"
            daysLeft == 1 -> "tomorrow"
            else -> "on " + due.pretty()
        }
        val open = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val paid = PendingIntent.getBroadcast(
            context, id + 1,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_PAID).putExtra("uid", p.uid).putExtra("nid", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val note = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("Payment due " + whenText)
            .setContentText(kes(p.contribution) + " into " + p.name)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "I paid", paid)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, note)
        } catch (e: SecurityException) {
            // The person turned notifications off in the meantime.
        }
    }
}
