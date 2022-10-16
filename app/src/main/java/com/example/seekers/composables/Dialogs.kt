package com.example.seekers.composables

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.seekers.utils.FirebaseHelper
import com.example.seekers.utils.News
import com.example.seekers.utils.Player
import com.example.seekers.R
import com.example.seekers.general.*
import com.example.seekers.screens.DefineAreaButton
import com.example.seekers.screens.RadarScreen
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.SizzlingRed
import com.example.seekers.viewModels.LobbyCreationScreenViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.maps.model.CameraPosition
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// MAP
//region CONTAINS: RadarDialog
@Composable
fun RadarDialog(
    gameId: String,
    onDismiss: () -> Unit
) {
    val height = LocalConfiguration.current.screenHeightDp * 0.8
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Powder,
            modifier = Modifier.height(height.dp)
        ) {
            RadarScreen(gameId = gameId)
        }
    }
}
//endregion
//region CONTAINS: ShowMyQRDialog and QRScannerDialog

@Composable
fun ShowMyQRDialog(onDismiss: () -> Unit) {
    val playerId = FirebaseHelper.uid!!
    val qrBitmap = generateQRCode(playerId)
    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QRCodeComponent(bitmap = qrBitmap)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QRScannerDialog(onDismiss: () -> Unit, onScanned: (String) -> Unit) {
    val context = LocalContext.current
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Box(Modifier.fillMaxSize()) {
            QRScanner(context = context, onScanned = onScanned)
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text(text = "Cancel")
            }
        }
    }
}

//endregion
//region CONTAINS: PlayerFoundDialog and SendSelfieDialog
@Composable
fun PlayerFoundDialog(playerFound: Player?, onCancel: () -> Unit, onTakePic: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "You found ${playerFound?.nickname}", fontSize = 22.sp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CustomButton(text = "Take a selfie") {
                        onTakePic()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    CustomButton(text = "Close") {
                        onCancel()
                    }
                }
            }
        }
    }
}

@Composable
fun SendSelfieDialog(
    selfie: Bitmap,
    onDismiss: () -> Unit,
    sendPic: () -> Unit,
    takeNew: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(backgroundColor = Color.White, shape = RoundedCornerShape(8.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),

                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        bitmap = selfie.asImageBitmap(),
                        contentDescription = "selfie",
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .aspectRatio(selfie.width.toFloat() / selfie.height)
                            .fillMaxWidth()
                    )
                    CustomButton(text = "Cancel") {
                        onDismiss()
                    }
                    CustomButton(text = "Take another") {
                        takeNew()
                    }
                    CustomButton(text = "Send") {
                        sendPic()
                    }
                }
            }
        }
    }
}

//endregion
//region CONTAINS: NewsDialog / NewsItem / timeStampToString

@Composable
fun NewsDialog(newsList: List<News>, gameId: String, onDismiss: () -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .height((screenHeight * 0.8).dp)
                .fillMaxWidth(), backgroundColor = Powder, shape = RoundedCornerShape(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "EVENTS",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(newsList) {
                        NewsItem(news = it, gameId = gameId)
                    }
                }
            }

        }
    }
}

@Composable
fun NewsItem(news: News, gameId: String) {
    var bitmap: Bitmap? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        if (news.picId.isNotBlank()) {
            val ONE_MEGABYTE: Long = 1024 * 1024
            FirebaseHelper.getSelfieImage(gameId = gameId, news.picId)
                .getBytes(ONE_MEGABYTE)
                .addOnSuccessListener {
                    val retrievedBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    bitmap = retrievedBitmap
                }
        }
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White,
        elevation = 5.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "selfie",
                    modifier = Modifier
                        .aspectRatio(it.width.toFloat() / it.height)
                        .fillMaxWidth()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = news.text)
                Text(
                    text = "${timeStampToTimeString(news.timestamp)}",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}

fun timeStampToTimeString(timestamp: Timestamp): String? {
    val localDateTime =
        timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return localDateTime.format(formatter)
}

//endregion d

// LOBBY
//region CONTAINS: EditRulesDialog / ShowRules / EditRulesForm
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditRulesDialog(
    vm: LobbyCreationScreenViewModel,
    gameId: String,
    isCreator: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()
    val countdown by vm.countdown.observeAsState()
    val center by vm.center.observeAsState()

    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${if (isCreator) "EDIT" else "CHECK"} RULES")
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "close dialog",
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .clickable { onDismissRequest() }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (isCreator) {
                        EditRulesForm(vm = vm)
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
                            CustomButton(text = "Save") {
                                if (maxPlayers!! >= 2 && timeLimit!! >= 10 && radius != null && countdown!! >= 30 && center != null) {
                                    val centerGeoPoint =
                                        GeoPoint(center!!.latitude, center!!.longitude)
                                    val changeMap = mapOf(
                                        Pair("center", centerGeoPoint),
                                        Pair("maxPlayers", maxPlayers!!),
                                        Pair("timeLimit", timeLimit!!),
                                        Pair("radius", radius!!),
                                        Pair("countdown", countdown!!)
                                    )
                                    vm.updateLobby(changeMap, gameId = gameId)
                                    Toast.makeText(context, "Game rules updated", Toast.LENGTH_LONG)
                                        .show()
                                    onDismissRequest()
                                } else {
                                    if (maxPlayers!! < 2) {
                                        vm.showMaxPlayersError.value = true
                                        vm.maxPlayersError.value = true
                                    }
                                    if (timeLimit!! < 10) {
                                        vm.showTimeLimitError.value = true
                                        vm.timeLimitError.value = true
                                    }
                                    if (countdown!! < 30) {
                                        vm.showCountDownError.value = true
                                        vm.countDownError.value = true
                                    }
                                }
                            }
                        }
                    } else {
                        ShowRules(vm = vm)
                    }
                }
            }
        }
    }
}

@Composable
fun ShowRules(vm: LobbyCreationScreenViewModel) {
    val lobby by vm.lobby.observeAsState()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Maximum amount of players: ${lobby?.maxPlayers}")
        Text(text = "Time limit: ${lobby?.timeLimit} minutes")
        Text(text = "Play area radius: ${lobby?.radius} meters")
        Text(text = "Time to hide: ${lobby?.countdown} seconds")
    }
}

@Composable
fun EditRulesForm(vm: LobbyCreationScreenViewModel) {
    val maxPlayers by vm.maxPlayers.observeAsState()
    val maxPlayersError by vm.maxPlayersError.observeAsState(false)
    val showMaxPlayersError by vm.maxPlayersError.observeAsState(false)

    val timeLimit by vm.timeLimit.observeAsState()
    val timeLimitError by vm.timeLimitError.observeAsState(false)
    val showTimeLimitError by vm.timeLimitError.observeAsState(false)

    val countdown by vm.countdown.observeAsState()
    val countDownError by vm.countDownError.observeAsState(false)
    val showCountDownError by vm.countDownError.observeAsState(false)

    val showMap by vm.showMap.observeAsState(false)
    val center by vm.center.observeAsState()
    val cameraState = rememberCameraPositionState()

    LaunchedEffect(center) {
        center?.let {
            cameraState.position = CameraPosition.fromLatLngZoom(it, 14f)
        }
    }

    if (!showMap) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column() {
                Input(
                    title = stringResource(id = R.string.max_players),
                    value = maxPlayers?.toString() ?: "",
                    isError = maxPlayersError,
                    keyboardType = KeyboardType.Number,
                    onChangeValue = { vm.updateMaxPlayers(it.toIntOrNull()) })
                if (showMaxPlayersError) ValidationErrorRow(
                    text = "Minimum 2 players",
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Column() {
                Input(
                    title = stringResource(id = R.string.time_limit),
                    value = timeLimit?.toString() ?: "",
                    isError = timeLimitError,
                    keyboardType = KeyboardType.Number,
                    onChangeValue = { vm.updateTimeLimit(it.toIntOrNull()) })
                if (showTimeLimitError) ValidationErrorRow(
                    text = "Minimum 10 minutes",
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Column() {
                Input(
                    title = stringResource(id = R.string.countdown),
                    value = countdown?.toString() ?: "",
                    isError = countDownError,
                    keyboardType = KeyboardType.Number,
                    onChangeValue = { vm.updateCountdown(it.toIntOrNull()) })
                if (showCountDownError) ValidationErrorRow(
                    text = "Minimum 30 seconds",
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            DefineAreaButton(vm = vm, text = "EDIT PLAY AREA")
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = Icons.Filled.Cancel, contentDescription = "cancel",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clickable {
                        vm.updateShowMap(false)
                    }
            )
            AreaSelectionMap(
                vm = vm,
                properties = MapProperties(
                    mapType = MapType.SATELLITE,
                    isMyLocationEnabled = true,
                ),
                settings = MapUiSettings(
                    zoomControlsEnabled = true,
                    zoomGesturesEnabled = true,
                    rotationGesturesEnabled = false,
                    scrollGesturesEnabled = true
                ),
                state = cameraState
            )
        }
    }
}
//endregion
//region CONTAINS: LeaveGameDialog and DismissLobbyDialog
@Composable
fun LeaveGameDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        title = { Text(text = "Quit?") },
        text = { Text(text = "Are you sure you want to leave?") },
        onDismissRequest = onDismissRequest,
        dismissButton = {
            Button(onClick = { onDismissRequest() }) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm() },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SizzlingRed,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Leave")
            }
        }
    )
}

@Composable
fun DismissLobbyDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        title = { Text(text = "Quit?") },
        text = { Text(text = "Are you sure you want to dismiss this lobby?") },
        onDismissRequest = onDismissRequest,
        dismissButton = {
            Button(onClick = { onDismissRequest() }) {
                Text(text = "Keep")
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm() },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SizzlingRed,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Dismiss")
            }
        }
    )
}
//endregion
//region CONTAINS: QRDialog
@Composable
fun QRDialog(
    bitmap: Bitmap,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            backgroundColor = Color.White,
            shape = MaterialTheme.shapes.medium
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(10.dp)
            ) {
                QRCodeComponent(bitmap = bitmap)
            }
        }
    }
}
//endregion

// StartGameScreen
//region CONTAINS: LogOutDialog
@Composable
fun LogOutDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        title = { Text(text = "Logging out?") },
        text = { Text(text = "Are you sure you want to log out?") },
        onDismissRequest = onDismissRequest,
        dismissButton = {
            Button(onClick = { onDismissRequest() }) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm() },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SizzlingRed,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Log Out")
            }
        }
    )
}
//endregion
//region CONTAINS: TutorialDialog / PagerSampleItemText / PagerSampleItem

@OptIn(ExperimentalPagerApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Powder)
        ) {
            Column(
                Modifier
                    .background(Powder)
                    .fillMaxSize()
            ) {
                val pagerState = rememberPagerState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "close dialog",
                        modifier = Modifier
                            .width(30.dp)
                            .height(30.dp)
                            .clickable { onDismiss() }
                    )
                }
                HorizontalPager(
                    count = 8,
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { page ->
                    PagerSampleItem(
                        page = page,
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
internal fun PagerSampleItemText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(5.dp),
        fontSize = 18.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
internal fun PagerSampleItem(
    page: Int,
    modifier: Modifier = Modifier,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val screenWidth = LocalConfiguration.current.screenWidthDp

    when (page) {
        0 -> {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerSampleItemText(text = "Create a lobby to host your own game as the Seeker, and share the QR code for all your friends!")
                Image(
                    painter = painterResource(id = R.drawable.tutorial1),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center,
                    modifier = Modifier.height((screenHeight * 0.5).dp),
                )
                PagerSampleItemText(text = "Or join a lobby of a friend with a QR code and get ready to hide!")

            }
        }
        1 -> {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerSampleItemText(text = "Remember to pick a funny nickname so your friends can recognize you!")
                Image(
                    painter = painterResource(id = R.drawable.tutorial2_1),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center
                )
                PagerSampleItemText(text = "And most importantly, a cute avatar")
                Image(
                    painter = painterResource(id = R.drawable.tutorial2_2),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center
                )
            }
        }
        2 -> {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerSampleItemText(text = "Set the rules for your game")
                Image(
                    painter = painterResource(id = R.drawable.tutorial3),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center,
                    modifier = Modifier.height((screenHeight * 0.5).dp),
                )
                PagerSampleItemText(text = "And don't forget to define the play area before creating the lobby!")

            }
        }
        3 -> {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerSampleItemText(text = "Setting the play area is simple")
                Image(
                    painter = painterResource(id = R.drawable.tutorial4),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center,
                    modifier = Modifier.height((screenHeight * 0.5).dp),
                )
                PagerSampleItemText(text = "Just move the camera to a favorable position, set the radius with the vertical slider and you're good to go!")
            }
        }
        4 -> {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerSampleItemText(text = "While waiting for your friends, open up the QR code for them to scan from your phone")
                Image(
                    painter = painterResource(id = R.drawable.tutorial5_1),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center
                )
                PagerSampleItemText(text = "In case you want to change the rules, here's the option to do that!")
                Image(
                    painter = painterResource(id = R.drawable.tutorial5_3),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center
                )
                PagerSampleItemText(text = "Now start the game and off you go, hide quickly!")
            }
        }
        5 -> {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerSampleItemText(text = "Your best friend while playing the game will be the bottom drawer - providing you with helpful tools and abilities")
                Image(
                    painter = painterResource(id = R.drawable.tutorial6_1),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center
                )
                Spacer(Modifier.height(30.dp))
                PagerSampleItemText(text = "To keep up with the game's progression, you will be able to see the players left hiding, duration left and notifications of events at the top of your screen")
                Image(
                    painter = painterResource(id = R.drawable.tutorial6_2),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center,
                    modifier = Modifier.width((screenWidth * 0.8).dp)
                )

            }
        }
        6 -> {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerSampleItemText(text = "If you're hiding, you will have the option to use 4 different abilities to help you in the game - press the magic wand to see them!")
                Image(
                    painter = painterResource(id = R.drawable.tutorial6_3),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center,
                    modifier = Modifier.width((screenWidth * 0.8).dp)
                )
                PagerSampleItemText(text = "Or in the case of a seeker, you will have the option to use a radar, which scans for nearby players that are hiding!")
                Image(
                    painter = painterResource(id = R.drawable.tutorial6_4),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center,
                    modifier = Modifier.width((screenWidth * 0.8).dp)
                )
                PagerSampleItemText(text = "Other options in the bottom drawer include a QR code / scanner for when you either find someone or you get found yourself, a list of players in the game and an option to leave the game")
            }
        }
        7 -> {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerSampleItemText(
                    text = "REVEAL SEEKERS shows seekers' locations on your map for 10 seconds\n" +
                            "\n" +
                            "INVISIBILITY hides your position on the seekers' maps for 15 seconds\n"
                )
                Image(
                    painter = painterResource(id = R.drawable.tutorial6_5),
                    contentDescription = "tutorial",
                    alignment = Alignment.Center,
                    modifier = Modifier.width((screenWidth * 0.8).dp)
                )
                PagerSampleItemText(
                    text = "JAMMER blocks seekers' screens for 10 seconds\n" +
                            "\n" +
                            "DECOY freezes your location for 15 seconds"
                )
            }
        }
    }
}
//endregion
