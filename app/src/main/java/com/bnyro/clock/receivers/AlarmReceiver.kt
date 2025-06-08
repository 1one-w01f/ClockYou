package com.bnyro.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.bnyro.clock.db.DatabaseHolder
import com.bnyro.clock.services.AlarmService
import com.bnyro.clock.util.AlarmHelper
import com.bnyro.clock.util.TimeHelper
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.e("receiver", "received")
        val id = intent.getLongExtra(AlarmHelper.EXTRA_ID, -1).takeIf { it != -1L } ?: return
        val alarm = runBlocking {
            DatabaseHolder.instance.alarmsDao().findById(id)
        }

//        if (alarm == null) {
//            Log.e("myTag", "idk why but alarm is null")
//            Log.e("myTag", "the null alarm had an id $id")
//        }

        val currentDay = TimeHelper.getCurrentWeekDay()

        if (alarm.tzId.isNullOrBlank()) { // original handling logic for local alarms (w/o timezone)

            // if today is one of the days that the alarm should fire
            // or that this is an one-off alarm, then we play the alarm
            if (currentDay - 1 in alarm.days || !alarm.repeat) {
                val playAlarm = Intent(context, AlarmService::class.java)
                playAlarm.putExtra(AlarmHelper.EXTRA_ID, id)
                ContextCompat.startForegroundService(context, playAlarm)
            }

        } else { // new handling logic for alarms w/ timezone
            val curCal = GregorianCalendar()
            curCal.time = TimeHelper.currentTime

            val alarmTz = TimeZone.getTimeZone(alarm.tzId)
            val alarmTzOffset = alarmTz.getOffset(Calendar.getInstance().timeInMillis)

            val localTz = TimeZone.getDefault()
            val localTzOffset = localTz.getOffset(Calendar.getInstance().timeInMillis)

            // current time in the alarm time zone
            curCal.add(Calendar.MILLISECOND, alarmTzOffset - localTzOffset)

            // if today (in the alarm time zone) is one of the days that the alarm should fire
            // or that this is an one-off alarm, then we play the alarm
            if (curCal.get(Calendar.DAY_OF_WEEK) - 1 in alarm.days || !alarm.repeat) {
                val playAlarm = Intent(context, AlarmService::class.java)
                playAlarm.putExtra(AlarmHelper.EXTRA_ID, id)
                ContextCompat.startForegroundService(context, playAlarm)
            }
        }

        // re-enqueue the alarm for the next day
        if (alarm.repeat) {
            AlarmHelper.enqueue(context, alarm)
        } else {
            alarm.enabled = false
            runBlocking {
                DatabaseHolder.instance.alarmsDao().update(alarm)
            }
        }

    }
}
