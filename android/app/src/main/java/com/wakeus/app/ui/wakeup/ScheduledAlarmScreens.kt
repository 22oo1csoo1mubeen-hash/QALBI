package com.wakeus.app.ui.wakeup

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.ScheduledAlarm
import com.wakeus.app.data.model.ScheduledAlarmStatus
import com.wakeus.app.data.repository.QalbiRepository
import com.wakeus.app.ui.components.QalbiRomanticButton
import com.wakeus.app.viewmodel.CreateScheduleViewModel
import com.wakeus.app.viewmodel.ScheduledAlarmsViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat as JavaDateFormat
import java.util.Calendar
import java.util.TimeZone

private val ScheduleBg = Color(0xFFFBF6F3)
private val ScheduleWine = Color(0xFF8B253E)
private val ScheduleInk = Color(0xFF301019)
private val ScheduleMuted = Color(0xFF7C5560)
private val ScheduleCard = Color(0xFFFFF9FA)
private val ScheduleBorder = Color(0xFFF3D4DC)

@Composable
fun ScheduleAlarmScreen(
    friendId: String,
    onBack: () -> Unit,
    viewModel: CreateScheduleViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val friends by QalbiRepository.friends.collectAsState(initial = emptyList())
    val friend = friends.firstOrNull { it.id == friendId } ?: QalbiRepository.getFriendById(friendId)
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }) }
    var selectedTime by remember {
        mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) })
    }

    if (!view.isInEditMode) SideEffect {
        WindowInsetsControllerCompat((view.context as? android.app.Activity)?.window ?: return@SideEffect, view)
            .isAppearanceLightStatusBars = true
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.created) { if (state.created) onBack() }

    Box(Modifier.fillMaxSize().background(ScheduleBg)) {
        Image(
            painter = painterResource(R.drawable.overall_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ScheduleWine)
                    }
                    Text(
                        "Schedule Alarm",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        color = ScheduleInk,
                    )
                }
            },
        ) { insets ->
            Column(
                Modifier.fillMaxSize().padding(insets).padding(horizontal = 22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(14.dp))
                if (friend == null) {
                    CircularProgressIndicator(color = ScheduleWine)
                    Text("Loading connected friend…", color = ScheduleMuted, modifier = Modifier.padding(16.dp))
                    return@Column
                }
                if (friend.isDemo || friend.connectionId.isBlank()) {
                    Text("Scheduling is available for connected QALBI friends.", color = ScheduleMuted)
                    return@Column
                }

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ScheduleCard),
                    border = BorderStroke(1.dp, ScheduleBorder),
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Alarm, contentDescription = null, tint = ScheduleWine, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Alarm for", fontSize = 12.sp, color = ScheduleMuted)
                            Text(friend.displayName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = ScheduleInk)
                            Text("@${friend.username}", fontSize = 13.sp, color = ScheduleMuted)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ScheduleCard),
                    border = BorderStroke(1.dp, ScheduleBorder),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("Choose when to wake them", fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = ScheduleInk)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val c = selectedDate.clone() as Calendar
                                DatePickerDialog(context, { _, year, month, day ->
                                    selectedDate = (selectedDate.clone() as Calendar).apply {
                                        set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
                                    }
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
                                    datePicker.minDate = System.currentTimeMillis()
                                    show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = ScheduleWine)
                            Spacer(Modifier.width(8.dp))
                            Text(JavaDateFormat.getDateInstance(JavaDateFormat.FULL).format(selectedDate.time), color = ScheduleWine)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val c = selectedTime
                                TimePickerDialog(context, { _, hour, minute ->
                                    selectedTime = (selectedTime.clone() as Calendar).apply {
                                        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
                                    }
                                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), DateFormat.is24HourFormat(context)).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = ScheduleWine)
                            Spacer(Modifier.width(8.dp))
                            Text(JavaDateFormat.getTimeInstance(JavaDateFormat.SHORT).format(selectedTime.time), color = ScheduleWine)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Time zone: ${TimeZone.getDefault().id}", color = ScheduleMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = message,
                            onValueChange = { if (it.length <= 160) message = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Personal message (optional)") },
                            placeholder = { Text("Wake up, friend! ❤️") },
                            supportingText = { Text("${message.length}/160") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                val scheduledAtMillis = remember(selectedDate, selectedTime) {
                    (selectedDate.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                QalbiRomanticButton(
                    text = if (state.isCreating) "Scheduling…" else "Schedule Wake-Up ❤️",
                    onClick = {
                        if (scheduledAtMillis <= System.currentTimeMillis()) {
                            viewModel.clearError()
                            snackbar.currentSnackbarData?.dismiss()
                            scope.launch { snackbar.showSnackbar("Choose a future date and time.") }
                        } else {
                            viewModel.create(friend, scheduledAtMillis, TimeZone.getDefault().id, message)
                        }
                    },
                    enabled = !state.isCreating,
                    isLoading = state.isCreating,
                    leadingIcon = Icons.Filled.Alarm,
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun ScheduledAlarmsScreen(
    onBack: () -> Unit,
    viewModel: ScheduledAlarmsViewModel = viewModel(),
) {
    val view = LocalView.current
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmCancel by remember { mutableStateOf<ScheduledAlarm?>(null) }

    if (!view.isInEditMode) SideEffect {
        WindowInsetsControllerCompat((view.context as? android.app.Activity)?.window ?: return@SideEffect, view)
            .isAppearanceLightStatusBars = true
    }
    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it) } }

    confirmCancel?.let { schedule ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            containerColor = ScheduleCard,
            title = { Text("Cancel this alarm?", color = ScheduleInk, fontFamily = FontFamily.Serif) },
            text = { Text("${schedule.receiverDisplayName} will not receive this scheduled wake-up.", color = ScheduleMuted) },
            confirmButton = {
                TextButton(onClick = { viewModel.cancel(schedule); confirmCancel = null }) {
                    Text("Cancel schedule", color = ScheduleWine)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Keep it", color = ScheduleMuted) } },
        )
    }

    Box(Modifier.fillMaxSize().background(ScheduleBg)) {
        Image(painterResource(R.drawable.overall_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ScheduleWine) }
                    Text("Scheduled Alarms", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = ScheduleInk)
                }
            },
        ) { insets ->
            Column(
                Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(10.dp))
                Text("Your upcoming wake-up calls", color = ScheduleMuted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                Text("To change a time, cancel that schedule and create a new one.", color = ScheduleMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                if (state.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(35.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ScheduleWine)
                    }
                } else if (state.schedules.isEmpty()) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = ScheduleCard), border = BorderStroke(1.dp, ScheduleBorder)) {
                        Text("No scheduled alarms yet. Open a connected friend's profile and choose Schedule Alarm.", color = ScheduleMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(22.dp))
                    }
                } else {
                    state.schedules.forEach { schedule ->
                        ScheduledAlarmCard(
                            schedule = schedule,
                            isCancelling = state.cancellingId == schedule.scheduleId,
                            onCancel = { confirmCancel = schedule },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ScheduledAlarmCard(schedule: ScheduledAlarm, isCancelling: Boolean, onCancel: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ScheduleCard),
        border = BorderStroke(1.dp, ScheduleBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = ScheduleWine)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("For ${schedule.receiverDisplayName}", fontWeight = FontWeight.SemiBold, color = ScheduleInk, fontSize = 16.sp)
                    Text(schedule.formattedSchedule(), color = ScheduleMuted, fontSize = 13.sp)
                }
                Text(statusLabel(schedule.status), color = statusColor(schedule.status), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            if (schedule.message.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("“${schedule.message}”", color = ScheduleMuted, fontFamily = FontFamily.Serif, fontSize = 14.sp)
            }
            if (!schedule.failureCode.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Details: ${schedule.failureCode.replace('_', ' ')}", color = Color(0xFF9D253F), fontSize = 12.sp)
            }
            if (schedule.canBeCancelled) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancel, enabled = !isCancelling) {
                    Text(if (isCancelling) "Cancelling…" else "Cancel schedule", color = ScheduleWine)
                }
            }
        }
    }
}

private fun statusLabel(status: ScheduledAlarmStatus): String = when (status) {
    ScheduledAlarmStatus.SCHEDULED -> "Scheduled"
    ScheduledAlarmStatus.PROCESSING -> "Sending"
    ScheduledAlarmStatus.SENT -> "Sent"
    ScheduledAlarmStatus.FAILED -> "Failed"
    ScheduledAlarmStatus.CANCELLED -> "Cancelled"
    ScheduledAlarmStatus.EXPIRED -> "Expired"
}

private fun statusColor(status: ScheduledAlarmStatus): Color = when (status) {
    ScheduledAlarmStatus.SCHEDULED -> Color(0xFF986F19)
    ScheduledAlarmStatus.PROCESSING -> Color(0xFF7A5862)
    ScheduledAlarmStatus.SENT -> Color(0xFF23764A)
    ScheduledAlarmStatus.FAILED, ScheduledAlarmStatus.EXPIRED -> Color(0xFF9D253F)
    ScheduledAlarmStatus.CANCELLED -> ScheduleMuted
}

