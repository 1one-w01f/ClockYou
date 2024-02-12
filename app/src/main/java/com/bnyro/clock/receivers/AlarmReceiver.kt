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

        val currentDay = TimeHelper.getCurrentWeekDay()

        if (!alarm.repeat) { // original handling logic for non-repeat alarms
            val playAlarm = Intent(context, AlarmService::class.java)
            playAlarm.putExtra(AlarmHelper.EXTRA_ID, id)
            ContextCompat.startForegroundService(context, playAlarm)
        }
        else {
            if (alarm.tzId == null) { // original handling logic for local alarms
                if (currentDay - 1 in alarm.days) {
                    val playAlarm = Intent(context, AlarmService::class.java)
                    playAlarm.putExtra(AlarmHelper.EXTRA_ID, id)
                    ContextCompat.startForegroundService(context, playAlarm)
                }
            }
            else { // new handling logic for repeat alarms w/ timezone

                val calendar = GregorianCalendar()
                calendar.time = TimeHelper.currentTime

                val alarmTz = TimeZone.getTimeZone(alarm.tzId)
                val alarmTzOffset = alarmTz.getOffset(Calendar.getInstance().timeInMillis)

                val localTz = TimeZone.getDefault()
                val localTzOffset = localTz.getOffset(Calendar.getInstance().timeInMillis)

                // current time in the alarm time zone
                calendar.add(Calendar.MILLISECOND, alarmTzOffset - localTzOffset)

//                val dow = calendar.get(Calendar.DAY_OF_WEEK)
//                Log.d("myTag", "new handling logic for repeat alarms w/ timezone")
//                Log.d("myTag", "alarm.days = ${alarm.days}")
//                Log.d("myTag", "alarm day of week = $dow")
//                Log.d("myTag", "dest alarm epoch time = ${calendar.time.time}")

                if (calendar.get(Calendar.DAY_OF_WEEK) - 1 in alarm.days) {
                    val playAlarm = Intent(context, AlarmService::class.java)
                    playAlarm.putExtra(AlarmHelper.EXTRA_ID, id)
                    ContextCompat.startForegroundService(context, playAlarm)
                }

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
