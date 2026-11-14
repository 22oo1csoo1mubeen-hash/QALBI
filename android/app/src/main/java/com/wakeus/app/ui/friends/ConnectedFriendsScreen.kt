package com.wakeus.app.ui.friends

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.wakeus.app.R
import com.wakeus.app.data.model.AvatarPreset
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.ScheduledAlarm
import com.wakeus.app.data.model.ScheduledAlarmStatus
import com.wakeus.app.viewmodel.ScheduledAlarmsViewModel
import com.wakeus.app.viewmodel.FriendsViewModel
import java.text.DateFormat as JavaDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

private val ScreenBgFallback = Color(0xFFFBF6F3)
private val DeepWine = Color(0xFF80243C)
private val DarkWineText = Color(0xFF331119)
private val MutedWineText = Color(0xFF7A5862)
private val CardBg = Color(0xFFFFF9F9)
private val CardBorderColor = Color(0xFFF3D5DB)

@Composable
fun ConnectedFriendsScreen(
    onBack: () -> Unit,
    onFriendSelected: (String) -> Unit,
    onConnectFriend: (() -> Unit)? = null,
    viewModel: FriendsViewModel = viewModel(),
) {
    val friends by viewModel.friends.collectAsState()
    val schedulesViewModel: ScheduledAlarmsViewModel = viewModel()
    val schedulesState by schedulesViewModel.uiState.collectAsState()
    var scheduleClockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            scheduleClockMillis = System.currentTimeMillis()
        }
    }
    val upcomingSchedules = schedulesState.schedules.filter { it.isActiveAt(scheduleClockMillis) }
    val context = LocalContext.current
    var editingSchedule by remember { mutableStateOf<ScheduledAlarm?>(null) }
    var cancellingSchedule by remember { mutableStateOf<ScheduledAlarm?>(null) }
    var editDate by remember { mutableStateOf(Calendar.getInstance()) }
    var editTime by remember { mutableStateOf(Calendar.getInstance()) }
    var editMessage by remember { mutableStateOf("") }

    editingSchedule?.let { schedule ->
        AlertDialog(
            onDismissRequest = { if (schedulesState.editingId == null) editingSchedule = null },
            containerColor = CardBg,
            title = { Text("Edit alarm for ${schedule.receiverDisplayName}", color = DarkWineText) },
            text = {
                Column {
                    TextButton(onClick = {
                        val current = editDate.clone() as Calendar
                        DatePickerDialog(context, { _, year, month, day ->
                            editDate = (editDate.clone() as Calendar).apply {
                                set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
                            }
                        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).apply {
                            datePicker.minDate = System.currentTimeMillis()
                            show()
                        }
                    }) {
                        Text("Date: ${JavaDateFormat.getDateInstance(JavaDateFormat.FULL).format(editDate.time)}", color = DeepWine)
                    }
                    TextButton(onClick = {
                        val current = editTime
                        TimePickerDialog(context, { _, hour, minute ->
                            editTime = (editTime.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
                            }
                        }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), DateFormat.is24HourFormat(context)).show()
                    }) {
                        Text("Time: ${JavaDateFormat.getTimeInstance(JavaDateFormat.SHORT).format(editTime.time)}", color = DeepWine)
                    }
                    OutlinedTextField(
                        value = editMessage,
                        onValueChange = { if (it.length <= 160) editMessage = it },
                        label = { Text("Message (optional)") },
                        supportingText = { Text("${editMessage.length}/160") },
                        minLines = 2,
                        maxLines = 4,
                    )
                    if (schedulesState.editingId == schedule.scheduleId) {
                        Text("Saving changes…", color = MutedWineText, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = schedulesState.editingId == null,
                    onClick = {
                        val scheduledAt = (editDate.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, editTime.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, editTime.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        schedulesViewModel.edit(schedule, scheduledAt, schedule.timeZoneId, editMessage)
                        editingSchedule = null
                    },
                ) { Text("Save changes", color = DeepWine) }
            },
            dismissButton = {
                TextButton(enabled = schedulesState.editingId == null, onClick = { editingSchedule = null }) {
                    Text("Close", color = MutedWineText)
                }
            },
        )
    }
    cancellingSchedule?.let { schedule ->
        AlertDialog(
            onDismissRequest = { cancellingSchedule = null },
            containerColor = CardBg,
            title = { Text("Cancel scheduled alarm?", color = DarkWineText) },
            text = { Text("${schedule.receiverDisplayName} will no longer receive this alarm.", color = MutedWineText) },
            confirmButton = {
                TextButton(onClick = {
                    schedulesViewModel.cancel(schedule)
                    cancellingSchedule = null
                }) { Text("Cancel alarm", color = DeepWine) }
            },
            dismissButton = { TextButton(onClick = { cancellingSchedule = null }) { Text("Keep it", color = MutedWineText) } },
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBgFallback)
    ) {
        // Shared romantic background
        Image(
            painter = painterResource(id = R.drawable.overall_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Elegant top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DeepWine,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Connected Friends",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = DeepWine,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                if (onConnectFriend != null) {
                    IconButton(onClick = onConnectFriend) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Connect Friend",
                            tint = DeepWine,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))

                if (friends.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0x35FCEDF0))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_empty_friends_hearts),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "No connected friends yet",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = DarkWineText,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Exchange a single-use connection code with someone special to connect privately and start sending wake-up calls.",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MutedWineText,
                                textAlign = TextAlign.Center,
                            )

                            if (onConnectFriend != null) {
                                Spacer(Modifier.height(20.dp))
                                OutlinedButton(
                                    onClick = onConnectFriend,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                                ) {
                                    Text(
                                        text = "Connect to a Friend",
                                        color = DeepWine,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "${friends.size} friend${if (friends.size > 1) "s" else ""} connected",
                        fontSize = 13.sp,
                        color = MutedWineText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    )

                    friends.forEach { friend ->
                        RomanticFriendListItem(
                            friend = friend,
                            onClick = { onFriendSelected(friend.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Scheduled Alarms",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    color = DeepWine,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                )
                Text(
                    text = "Your upcoming wake-up calls for all connected friends",
                    fontSize = 13.sp,
                    color = MutedWineText,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                )
                if (schedulesState.isLoading) {
                    Text("Loading scheduled alarms…", color = MutedWineText, modifier = Modifier.padding(12.dp))
                } else if (upcomingSchedules.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorderColor),
                    ) {
                        Text(
                            "No scheduled alarms yet. Open a friend's profile to schedule one.",
                            color = MutedWineText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                        )
                    }
                } else {
                    upcomingSchedules.forEach { schedule ->
                        FriendScheduledAlarmCard(
                            schedule = schedule,
                            isEditing = schedulesState.editingId == schedule.scheduleId,
                            onEdit = {
                                val zone = runCatching { TimeZone.getTimeZone(schedule.timeZoneId) }
                                    .getOrDefault(TimeZone.getDefault())
                                editDate = Calendar.getInstance(zone).apply { timeInMillis = schedule.scheduledAtMillis }
                                editTime = Calendar.getInstance(zone).apply { timeInMillis = schedule.scheduledAtMillis }
                                editMessage = schedule.message
                                editingSchedule = schedule
                            },
                            onCancel = { cancellingSchedule = schedule },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                schedulesState.error?.let { error ->
                    Text(error, color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.padding(6.dp))
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FriendScheduledAlarmCard(
    schedule: ScheduledAlarm,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = DeepWine, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("For ${schedule.receiverDisplayName}", color = DarkWineText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("@${schedule.receiverUsername}", color = MutedWineText, fontSize = 12.sp)
                }
                Surface(shape = RoundedCornerShape(50), color = Color(0xFFFCEBEF)) {
                    Text(
                        schedule.status.name.lowercase().replaceFirstChar(Char::uppercase),
                        color = DeepWine,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(schedule.formattedSchedule(), color = DarkWineText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (schedule.message.isNotBlank()) {
                Text(schedule.message, color = MutedWineText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (schedule.status == ScheduledAlarmStatus.SCHEDULED && schedule.canBeCancelled) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEdit, enabled = !isEditing) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = DeepWine, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isEditing) "Saving…" else "Edit", color = DeepWine)
                    }
                    TextButton(onClick = onCancel, enabled = !isEditing) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MutedWineText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", color = MutedWineText)
                    }
                }
            }
        }
    }
}

@Composable
private fun RomanticFriendListItem(
    friend: Friend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepWine, Color(0xFFA03350))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = friend.initials.ifBlank { "?" },
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = DarkWineText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "@${friend.username}",
                    fontSize = 12.sp,
                    color = MutedWineText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFCEBEF),
                ) {
                    Text(
                        text = friend.status.ifBlank { "Connected" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepWine,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = Color(0xFFB88C95),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
