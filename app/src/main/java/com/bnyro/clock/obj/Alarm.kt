package com.bnyro.clock.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * @param days The days of the week to ring the alarm. Sunday-0, Monday-1 ,... ,Saturday-6
 * @param snoozeMinutes How long the snooze should last in minutes (default 10).
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var time: Long,
    var label: String? = null,
    var enabled: Boolean = false,
    var days: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6),
    var vibrate: Boolean = false,
    var soundName: String? = null,
    var soundUri: String? = null,
    var tzOffset: Int? = 0,
    var tzName: String? = null,
    @ColumnInfo(defaultValue = "1") var repeat: Boolean = false,
    @ColumnInfo(defaultValue = "1") var snoozeEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "10") var snoozeMinutes: Int = 10,
    @ColumnInfo(defaultValue = "1") var soundEnabled: Boolean = true
) {
    @Ignore
    val isWeekends: Boolean = days == listOf(0, 6)

    @Ignore
    val isWeekdays: Boolean = days == listOf(1, 2, 3, 4, 5)

    @Ignore
    val isRepeatEveryday: Boolean = days.size == 7
}
