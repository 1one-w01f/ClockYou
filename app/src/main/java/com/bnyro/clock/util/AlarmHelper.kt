package com.bnyro.clock.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.bnyro.clock.R
import com.bnyro.clock.obj.Alarm
import com.bnyro.clock.receivers.AlarmReceiver
import com.bnyro.clock.ui.MainActivity
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

object AlarmHelper {
    const val EXTRA_ID = "alarm_id"

//    fun enqueueWithTimezoneAdjustment(context: Context, alarm: Alarm) {
//        cancel(context, alarm)
//        if (!alarm.enabled) {
//            return
//        }
//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val alarmInfo = AlarmManager.AlarmClockInfo(
//            getAlarmScheduleTimeWithTimezoneAdjustment(alarm),
//            getOpenAppIntent(context, alarm)
//        )
//        alarmManager.setAlarmClock(alarmInfo, getPendingIntent(context, alarm))
//    }

    fun enqueue(context: Context, alarm: Alarm) {
        cancel(context, alarm)
        if (!alarm.enabled) {
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmInfo = AlarmManager.AlarmClockInfo(
//            getAlarmScheduleTime(alarm),
            getAlarmScheduleTimeWithTimezoneAdjustment(alarm),
            getOpenAppIntent(context, alarm)
        )
        alarmManager.setAlarmClock(alarmInfo, getPendingIntent(context, alarm))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun hasPermission(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun cancel(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(getPendingIntent(context, alarm))
    }

    private fun getPendingIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context.applicationContext, AlarmReceiver::class.java)
            .putExtra(EXTRA_ID, alarm.id)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOpenAppIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context.applicationContext, MainActivity::class.java)
            .putExtra(EXTRA_ID, alarm.id)
        return PendingIntent.getActivity(
            context.applicationContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Calculate the epoch time for scheduling an alarm with timezone adjustment
     */
    private fun getAlarmScheduleTimeWithTimezoneAdjustment(alarm: Alarm): Long {
        val calendar = GregorianCalendar()
        calendar.time = TimeHelper.currentTime

        // reset the calendar time to the start of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var alarmTz = TimeZone.getDefault()
        if (alarm.tzId != null) {
            alarmTz = TimeZone.getTimeZone(alarm.tzId)
        }
        val alarmTzOffset = alarmTz.getOffset(Calendar.getInstance().timeInMillis)

        val localTz = TimeZone.getDefault()
        val localTzOffset = localTz.getOffset(Calendar.getInstance().timeInMillis)

        // add the milliseconds from the new alarm
        calendar.add(Calendar.MILLISECOND, alarm.time.toInt() - alarmTzOffset + localTzOffset)
        Log.d("myTag", "original alarm time = ${calendar.time.time}")

//        // adjust for alarm timezone and local offsets
//        calendar.add(Calendar.MILLISECOND, -1 * localTzOffset)
//        Log.d("myTag", "after minus localtz alarm time = ${calendar.time.time}")
//
//        calendar.add(Calendar.MILLISECOND, alarmTzOffset)
//        Log.d("myTag", "after adding alarm timezine = ${calendar.time.time}")
//        // if this alarm is local (has no timezone specified), and
//        // the event has already passed for the day, then schedule for the next day
//        if (alarm.tzId == null && calendar.time.time < TimeHelper.currentTime.time) {
//            calendar.add(Calendar.HOUR_OF_DAY, 24)
//            Log.d("myTag", "passed for the day adjustment = ${calendar.time.time}")
//        }

        // if the event has already passed for the day, schedule for the next day
        if (calendar.time.time < TimeHelper.currentTime.time) {
            calendar.add(Calendar.HOUR_OF_DAY, 24)
            Log.d("myTag", "passed for the day adjustment = ${calendar.time.time}")
        }
        return calendar.timeInMillis
    }

    /**
     * Calculate the epoch time for scheduling an alarm
     */
    private fun getAlarmScheduleTime(alarm: Alarm): Long {
        val calendar = GregorianCalendar()
        calendar.time = TimeHelper.currentTime

        // reset the calendar time to the start of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // add the milliseconds from the new alarm
        calendar.add(Calendar.MILLISECOND, alarm.time.toInt())

        // if the event has already passed for the day, schedule for the next day
        if (calendar.time.time < TimeHelper.currentTime.time) {
            calendar.add(Calendar.HOUR_OF_DAY, 24)
        }
        return calendar.timeInMillis
    }

    fun getAlarmTime(alarm: Alarm): Long {
        val calendar = GregorianCalendar()
        calendar.time = TimeHelper.currentTime

        // reset the calendar time to the start of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // add the milliseconds from the new alarm
        calendar.add(Calendar.MILLISECOND, alarm.time.toInt())

        calendar.add(Calendar.DATE, getPostponeDays(alarm, calendar))
        return calendar.timeInMillis
    }

    private fun getPostponeDays(alarm: Alarm, calendar: GregorianCalendar): Int {
        if (alarm.days.isEmpty() && alarm.repeat) return 0

        val hasEventPassed = calendar.time.time < TimeHelper.currentTime.time

        if (alarm.repeat) {
            val today = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val eventDay = when {
                alarm.days.last() >= today -> {
                    // Get the next alarm
                    val day = alarm.days.first { it >= today }
                    when {
                        // If the alarm is not set up for today or is setup for today and it hasn't ringed yet, do nothing
                        day > today || (day == today && !hasEventPassed) -> day
                        // If there was an alarm today but it already ringed and there is more in the weekend, skip to the next one.
                        day == today && alarm.days.last() > today -> alarm.days.first { it > today }
                        else -> alarm.days.first()
                    }
                }
                else -> alarm.days.first()
            }
            var dayDiff = eventDay - today
            // If an alarm is set on repeat but only set up for one day, check if has already played and reset the days accordingly
            if (dayDiff < 0 || (hasEventPassed && dayDiff == 0)) dayDiff += 7
            return dayDiff
        }

        // the alarm is a one time alarm and hence the day only needs to be incremented when it's not today
        return if (hasEventPassed) 1 else 0
    }

    fun snooze(context: Context, oldAlarm: Alarm) {
        val snoozeMinutes = oldAlarm.snoozeMinutes
        val calendar = GregorianCalendar()
        val nowEpoch = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayEpoch = calendar.timeInMillis
        val snoozeTime = nowEpoch - todayEpoch + 1000 * 60 * snoozeMinutes
        enqueue(context, oldAlarm.copy(time = snoozeTime))
    }

    /**
     * @return the days of the week mapped to an index 0-Sunday, 1-Monday, ..., 6-Saturday.
     * The list order will match the user preferred days of the week order.
     */
    fun getDaysOfWeekByLocale(context: Context): List<Pair<String, Int>> {
        val availableDays = context.resources.getStringArray(R.array.available_days).toList()
        val firstDayIndex = GregorianCalendar().firstDayOfWeek - 1
        val daysWithIndex = availableDays.mapIndexed { index, s -> s to index }
        return daysWithIndex.subList(firstDayIndex, 7) + daysWithIndex.subList(0, firstDayIndex)
    }
}
