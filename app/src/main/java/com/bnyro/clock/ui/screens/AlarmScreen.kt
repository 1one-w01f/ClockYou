package com.bnyro.clock.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.bnyro.clock.BuildConfig
import com.bnyro.clock.R
import com.bnyro.clock.obj.Alarm
import com.bnyro.clock.ui.common.BlobIconBox
import com.bnyro.clock.ui.components.AlarmFilterSection
import com.bnyro.clock.ui.components.AlarmRow
import com.bnyro.clock.ui.components.AlarmSettingsSheet
import com.bnyro.clock.ui.components.ClickableIcon
import com.bnyro.clock.ui.components.DialogButton
import com.bnyro.clock.ui.dialog.TimePickerDialog
import com.bnyro.clock.ui.model.AlarmModel
import com.bnyro.clock.ui.model.ClockModel
import com.bnyro.clock.ui.nav.TopBarScaffold
import com.bnyro.clock.util.AlarmHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    onClickSettings: () -> Unit,
    alarmModel: AlarmModel,
    clockModel: ClockModel

) {
    val context = LocalContext.current
    var showCreationDialog by remember {
        mutableStateOf(false)
    }
    var showTimeZoneDialog by remember {
        mutableStateOf(false)
    }
    val alarms by alarmModel.alarms.collectAsState()
    val filters by alarmModel.filters.collectAsState()
    var alarmTime by remember {
        mutableStateOf(0.toLong())
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            if (!AlarmHelper.hasPermission(context)) {
                val intent = Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${BuildConfig.APPLICATION_ID}".toUri()
                }
                context.startActivity(intent)
            }
        }
    }

    TopBarScaffold(title = stringResource(R.string.alarm), onClickSettings, fab = {
        if (!alarmModel.showFilter) {
            FloatingActionButton(
                onClick = {
                    showCreationDialog = true
                }
            ) {
                Icon(Icons.Default.Create, null)
            }
        }
    }, actions = {
        ClickableIcon(
            imageVector = Icons.Default.FilterAlt
        ) {
            alarmModel.showFilter = !alarmModel.showFilter
            if (!alarmModel.showFilter) alarmModel.resetFilters()
        }
    }) { pv ->

        if (alarms.isEmpty()) {
            BlobIconBox(icon = R.drawable.ic_alarm)
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pv)
        ) {

            item {
                if (alarmModel.showFilter) {
                    AlarmFilterSection(
                        filters,
                        { alarmModel.updateLabelFilter(it) },
                        { alarmModel.updateWeekDayFilter(it) },
                        { alarmModel.updateStartTimeFilter(it) },
                        { alarmModel.updateEndTimeFilter(it) },
                    )
                }

            }

            items(alarms) {
                var showDeletionDialog by remember {
                    mutableStateOf(false)
                }

                val dismissState = rememberDismissState(
                    confirmValueChange = { dismissValue ->
                        when (dismissValue) {
                            DismissValue.DismissedToEnd -> {
                                showDeletionDialog = true
                            }

                            else -> {}
                        }
                        false
                    }
                )
                SwipeToDismiss(
                    state = dismissState,
                    directions = setOf(DismissDirection.StartToEnd),
                    dismissContent = {
                        AlarmRow(it, alarmModel)
                    },
                    background = {}
                )

                if (showDeletionDialog) {
                    AlertDialog(
                        onDismissRequest = { showCreationDialog = false },
                        title = {
                            Text(text = stringResource(R.string.delete_alarm))
                        },
                        text = {
                            Text(text = stringResource(R.string.irreversible))
                        },
                        confirmButton = {
                            DialogButton(label = android.R.string.ok) {
                                alarmModel.deleteAlarm(context, it)
                                showDeletionDialog = false
                            }
                        },
                        dismissButton = {
                            DialogButton(label = android.R.string.cancel) {
                                showDeletionDialog = false
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (showCreationDialog) {
            TimePickerDialog(
                label = stringResource(R.string.new_alarm),
                onDismissRequest = { showCreationDialog = false }
            ) {
                alarmTime = it.toLong()

                Log.d("myTag", "alarmTime $alarmTime");


                showCreationDialog = false
                showTimeZoneDialog = true


            }
        }
    }
    alarmModel.selectedAlarm?.let {
        AlarmSettingsSheet(
            onDismissRequest = { alarmModel.selectedAlarm = null },
            currentAlarm = it,
            onSave = { newAlarm ->
                alarmModel.updateAlarm(context, newAlarm)
            }
        )
    }

    if (showTimeZoneDialog) {
        val newTimeZones = remember {
            clockModel.selectedTimeZones.toMutableStateList()
        }

        AlertDialog(
            onDismissRequest = {
                 showTimeZoneDialog = false
            },
            confirmButton = {
                DialogButton(label = android.R.string.ok) {
//                    clockModel.setTimeZones(newTimeZones)

                    Log.d("myTag", "newTimeZones.size = " + newTimeZones.size)

                    newTimeZones.forEach{ timezone ->

                        Log.d("myTag", "alarmTime $alarmTime");


                        val alarm = Alarm(
                            time = alarmTime, tzId = timezone.name,
                            tzDisplayName = timezone.displayName)
                        alarmModel.createAlarm(alarm)

                        Log.d("myTag", "timezome " + timezone.countryName)
                        Log.d("myTag", "timezome " + timezone.displayName)
                        Log.d("myTag", "timezome " + timezone.name)
                        Log.d("myTag", "timezome " + timezone.offset)


                    }

                    showTimeZoneDialog = false
                }
            },
            dismissButton = {
                DialogButton(label = android.R.string.cancel) {
                    // create the original alarm on dismiss
                    val alarm = Alarm(time = alarmTime)
                    alarmModel.createAlarm(alarm)

                    Log.d("myTag", "alarmTime $alarmTime");


                    showTimeZoneDialog = false
                }
            },
            title = {
                Text(stringResource(R.string.timezones))
            },
            text = {
                var searchQuery by remember {
                    mutableStateOf("")
                }

                Column(
                    modifier = Modifier
                        .heightIn(300.dp, 450.dp)
                        .fillMaxSize()
                ) {
                    OutlinedTextField(
                        modifier = Modifier.padding(vertical = 10.dp),
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.search)) }
                    )

                    LazyColumn {
                        val lowerQuery = searchQuery.lowercase()
                        val filteredZones = clockModel.timeZones.filter {
                            it.countryName.lowercase()
                                .contains(lowerQuery) || it.displayName.lowercase()
                                .contains(lowerQuery)
                        }

                        items(filteredZones) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ListItem(headlineContent = {
                                    Text(text = it.displayName)
                                }, supportingContent = {
                                    Text(
                                        text = it.countryName
                                    )
                                }, leadingContent = {
                                    Checkbox(
                                        checked = newTimeZones.contains(it),
                                        onCheckedChange = { newCheckedState ->
                                            if (!newCheckedState) {
                                                newTimeZones.remove(it)
                                            } else {
                                                newTimeZones.add(it)
                                            }
                                        }
                                    )
                                }, trailingContent = {
                                    Text((it.offset.toFloat() / 1000 / 3600).toString())
                                })
                            }
                        }
                    }
                }
            }
        )
    }
}
