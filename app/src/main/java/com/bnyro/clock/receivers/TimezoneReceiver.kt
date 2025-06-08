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

//            context.stopService(intent)

            val alarms = runBlocking(Dispatchers.IO) {
                DatabaseHolder.instance.alarmsDao().getAll()
            }
            Log.d("myTag", "TimezoneReceiver.onReceive: alarms size = ${alarms.size}")
            alarms.forEach {
                AlarmHelper.enqueue(context, it)
                Log.d("myTag", "TimezoneReceiver.onReceive: alarm enqueued")
            }
        }


    }
}
