package com.example.stamped.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.stamped.R
import java.util.concurrent.TimeUnit

class DailyTipWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        // Користи го јазикот зачуван од корисникот, не системскиот
        val lang = LocaleHelper.getSavedLanguage(ctx)
        val tip = TravelTips.random(lang)
        showNotification(ctx, tip)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "daily_tips"
        const val NOTIF_ID = 1001
        const val WORK_NAME = "daily_tip_worker"
        private const val PREFS = "stamped_settings"
        private const val KEY_ENABLED = "daily_notifications_enabled"

        private fun showNotification(ctx: Context, message: String) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    ctx.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = ctx.getString(R.string.notif_channel_desc)
                }
                nm.createNotificationChannel(channel)
            }

            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            val pi = if (intent != null) {
                PendingIntent.getActivity(
                    ctx, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else null

            val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle(ctx.getString(R.string.notif_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            nm.notify(NOTIF_ID, notif)
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().build()
            val request = PeriodicWorkRequestBuilder<DailyTipWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.MINUTES) // прв повик за 30 мин
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Веднаш испрати известување — за тестирање.
         */
        fun sendTestNotification(context: Context) {
            val ctx = context.applicationContext
            val lang = LocaleHelper.getSavedLanguage(ctx)
            val tip = TravelTips.random(lang)
            showNotification(ctx, tip)
        }

        fun isEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true)
        }

        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply()
            if (enabled) schedule(context) else cancel(context)
        }
    }
}
