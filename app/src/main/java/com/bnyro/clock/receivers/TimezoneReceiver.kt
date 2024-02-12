package com.bnyro.clock.receivers


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bnyro.clock.db.DatabaseHolder
import com.bnyro.clock.services.AlarmService
import com.bnyro.clock.util.AlarmHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking


class TimezoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (android.content.Intent.ACTION_TIMEZONE_CHANGED == action) {

//            context.stopService(Intent(context, AlarmService.Companion::class.java))
//            context.startService(Intent(context, AlarmService.Companion::class.java))

            // todo: java.lang.RuntimeException: Unable to start service com.bnyro.clock.services.AlarmService@a365942 with Intent { cmp=com.bnyro.clock.debug/com.bnyro.clock.services.AlarmService (has extras) }: java.lang.IllegalStateException: Timer already cancelled.
            context.stopService(intent)
            context.stopService(intent)

            Log.d("myTag", "action $action in TimezoneReceiver onReceive")

            val alarms = runBlocking(Dispatchers.IO) {
                DatabaseHolder.instance.alarmsDao().getAll()
            }
            Log.d("myTag", "alarms size = ${alarms.size}")
            alarms.forEach {
                AlarmHelper.enqueue(context, it)
            }
        }


    }
}
