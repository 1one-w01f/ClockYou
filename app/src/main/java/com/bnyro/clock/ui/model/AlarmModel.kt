package com.bnyro.clock.ui.model

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bnyro.clock.db.DatabaseHolder
import com.bnyro.clock.obj.Alarm
import com.bnyro.clock.obj.AlarmFilters
import com.bnyro.clock.util.AlarmHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Collections

class AlarmModel : ViewModel() {
    var selectedAlarm: Alarm? by mutableStateOf(null)
    var showFilter by mutableStateOf(false)
    val filters = MutableStateFlow(AlarmFilters())
    val alarms: StateFlow<List<Alarm>> =
        combine(DatabaseHolder.instance.alarmsDao().getAllStream(), filters) { items, filter ->
            items.filter { alarm ->
                (filter.startTime <= alarm.time && alarm.time <= filter.endTime)
                        && !Collections.disjoint(filter.weekDays, alarm.days)
                        && (alarm.label?.lowercase()?.contains(filter.label.lowercase()) ?: true)

            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = listOf()
        )

    fun createAlarm(alarm: Alarm): Long {

        // we modified this to return the ID after inserting into the DB
        // we need the ID so that we can update (enqueue) a new alarm after creation

        val retId: Long = runBlocking {
            DatabaseHolder.instance.alarmsDao().insert(alarm)
        }
//        var retId: Long = 0
//        viewModelScope.launch(Dispatchers.IO) {
//            retId = DatabaseHolder.instance.alarmsDao().insert(alarm)
//
//            Log.e("myTag", "inside viewModelScope, retId from createAlarm is $retId")
//        }
//        Log.e("myTag", "outside viewModelScope, retId from createAlarm is $retId")
        Log.e("myTag", "retId from createAlarm is $retId")
        return retId
    }

    fun updateAlarm(context: Context, alarm: Alarm) {
        AlarmHelper.enqueue(context, alarm)
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseHolder.instance.alarmsDao().update(alarm)
        }
    }

    fun deleteAlarm(context: Context, alarm: Alarm) {
        AlarmHelper.cancel(context, alarm)
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseHolder.instance.alarmsDao().delete(alarm)
        }
    }

    fun updateLabelFilter(label: String) {
        filters.update { it.copy(label = label) }
    }

    fun updateWeekDayFilter(weekDays: List<Int>) {
        filters.update { it.copy(weekDays = weekDays) }
    }

    fun updateStartTimeFilter(startTime: Long) {
        filters.update { it.copy(startTime = startTime) }
    }

    fun updateEndTimeFilter(endTime: Long) {
        filters.update { it.copy(endTime = endTime) }
    }

    fun resetFilters() {
        filters.update { AlarmFilters() }
    }
}
