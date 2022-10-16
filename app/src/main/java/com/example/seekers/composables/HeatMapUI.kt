package com.example.seekers.composables

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.seekers.R
import com.example.seekers.general.AvatarIcon
import com.example.seekers.general.NavRoutes
import com.example.seekers.general.avatarList
import com.example.seekers.general.secondsToText
import com.example.seekers.ui.theme.Emerald
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.ui.theme.SizzlingRed
import com.example.seekers.utils.InGameStatus
import com.example.seekers.utils.Player
import com.example.seekers.viewModels.HeatMapViewModel
import com.google.maps.android.compose.MapUiSettings
import java.util.*

val uiSettings = MapUiSettings(
    compassEnabled = true,
    indoorLevelPickerEnabled = true,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = true,
    scrollGesturesEnabled = true,
    scrollGesturesEnabledDuringRotateOrZoom = true,
    tiltGesturesEnabled = true,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = true
)

@Composable
fun GameTopBar(modifier: Modifier = Modifier, showNews: (Boolean) -> Unit, vm: HeatMapViewModel) {
    val players by vm.players.observeAsState(listOf())
    val hasNewNews by vm.hasNewNews.observeAsState(false)
    val hidingAmount by derivedStateOf {
        players.count { player ->
            player.inGameStatus == InGameStatus.HIDING.ordinal
                    || player.inGameStatus == InGameStatus.MOVING.ordinal
                    || player.inGameStatus == InGameStatus.INVISIBLE.ordinal
                    || player.inGameStatus == InGameStatus.DECOYED.ordinal
        }
    }
    Card(
        modifier = modifier,
        backgroundColor = Emerald,
        border = BorderStroke(1.dp, Raisin),
        shape = RoundedCornerShape(5.dp)
    ) {
        Box(
            Modifier.padding(8.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PeopleAlt,
                    contentDescription = "",
                    tint = Raisin
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "$hidingAmount left",
                    color = Raisin,
                    fontSize = 16.sp
                )
            }
            Box(modifier = Modifier.align(Alignment.Center)) {
                GameTimer(vm = vm)
            }

            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                NewsButton(onClick = {
                    showNews(true)
                    vm.hasNewNews.value = false
                }, hasNew = hasNewNews)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerListDialog(onDismiss: () -> Unit, players: List<Player>) {
    val height = LocalConfiguration.current.screenHeightDp * 0.7
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .padding(32.dp),
            backgroundColor = Powder,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                Text(text = "PLAYERS", fontSize = 22.sp)
                Spacer(modifier = Modifier.height(32.dp))
                PlayerList(players = players)
            }
        }
    }
}

@Composable
fun PlayerList(players: List<Player>) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(players) {
            PlayerTile(player = it)
        }
    }
}

@Composable
fun PlayerTile(player: Player) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    Column(Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp), elevation = 4.dp
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarIcon(
                    resourceId = avatarList[player.avatarId], imgModifier = Modifier
                        .size(35.dp)
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = player.nickname,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width((screenWidth * 0.3).dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusPill(inGameStatus = player.inGameStatus)
            }
        }

    }
}

@Composable
fun StatusPill(inGameStatus: Int) {
    val status = InGameStatus.values()[inGameStatus].name.lowercase(Locale.getDefault())
    val color = if (inGameStatus == InGameStatus.SEEKER.ordinal) SizzlingRed else Color.LightGray

    Card(shape = RoundedCornerShape(16.dp), backgroundColor = color) {
        Text(
            text = status.uppercase(Locale.ROOT),
            fontSize = 16.sp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(vertical = 4.dp),
            color = Color.White
        )
    }
}

fun makeToast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PowersDialog(onDismiss: () -> Unit, vm: HeatMapViewModel, gameId: String) {
    val context = LocalContext.current
    val list = listOf<Power>(Power.INVISIBILITY, Power.JAMMER, Power.DECOY, Power.REVEAL)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            backgroundColor = Powder,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp)
            ) {
                items(list) { power ->
                    PowerButton(power = power, vm, gameId)
                }
            }
        }
    }
}

@Composable
fun PowerButton(power: Power, vm: HeatMapViewModel, gameId: String) {
    val icon = when (power.icon) {
        1 -> Icons.Filled.VisibilityOff
        3 -> Icons.Filled.DirectionsRun
        4 -> Icons.Filled.Radar
        else -> Icons.Filled.HelpOutline
    }
    val icon2 = when (power.icon) {
        2 -> painterResource(id = R.drawable.block_radar)
        else -> null
    }

    fun actionToDo(powerActionInt: Int) {
        val action = when (powerActionInt) {
            1 -> vm.activateInvisibility(gameId)
            2 -> vm.activateJammer(gameId)
            3 -> vm.deployDecoy(gameId)
            4 -> vm.revealSeekers()
            else -> {}
        }
        return action
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 10.dp)
            .clickable(
                enabled = true,
                onClick = { actionToDo(power.action) }
            ),
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            border = BorderStroke(2.dp, Color.Black),
            modifier = Modifier.size(100.dp)
        ) {
            if (power.icon != 2) {
                Icon(
                    icon,
                    contentDescription = "power",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Icon(
                    icon2!!,
                    contentDescription = "power",
                    modifier = Modifier.padding(16.dp),
                    tint = Color.Unspecified
                )
            }
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Text(text = power.text)
    }
}

enum class Power(val icon: Int, val text: String, val duration: Int, val action: Int) {
    INVISIBILITY(1, "Invisibility", 30, 1),
    JAMMER(2, "Jammer", 5, 2),
    DECOY(3, "Decoy", 30, 3),
    REVEAL(4, "Reveal seekers", 5, 4)
}

@Composable
fun PowerActiveIndicator(modifier: Modifier = Modifier, power: Power, countdown: Int) {
    val icon = when (power.icon) {
        1 -> Icons.Filled.VisibilityOff
        3 -> Icons.Filled.DirectionsRun
        4 -> Icons.Filled.Radar
        else -> Icons.Filled.HelpOutline
    }
    val icon2 = when (power.icon) {
        2 -> painterResource(id = R.drawable.block_radar)
        else -> null
    }

    val progress = countdown.toFloat() / power.duration
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Card(modifier = Modifier.size(64.dp), shape = CircleShape) {
            if (power.icon != 2) {
                Icon(
                    icon,
                    contentDescription = "power",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(16.dp)
                )
            } else {
                Icon(
                    icon2!!,
                    contentDescription = "power",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(16.dp),
                    tint = Color.Unspecified
                )
            }
        }

        CircularProgressIndicator(
            progress = animatedProgress,
            strokeWidth = 6.dp,
            color = SizzlingRed,
            modifier = Modifier
                .size(64.dp)
        )
    }
}

@Composable
fun Jammer() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = "You have been jammed", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun NewsButton(modifier: Modifier = Modifier, onClick: () -> Unit, hasNew: Boolean) {
    Box {
        IconButton(
            onClick = { onClick() },
            content = {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "news",
                        tint = Raisin
                    )
                }
            })
        if (hasNew) {
            Surface(
                color = Color.Red, shape = CircleShape, modifier = Modifier
                    .size(8.dp)
                    .align(
                        Alignment.TopEnd
                    )
            ) {}
        }
    }
}

@Composable
fun GameTimer(vm: HeatMapViewModel) {
    val countdown by vm.countdown.observeAsState()
    countdown?.let {
        Row(
            modifier = Modifier
                .border(BorderStroke(1.dp, Raisin))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.Alarm, contentDescription = "", tint = Raisin)
            Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
                val timeText = secondsToText(it)
                if (timeText == "Time's up!")
                    Text(text = timeText, color = Raisin, fontSize = 16.sp)
                else
                    Text(text = timeText, color = Raisin, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun EndTimerSkip(lobbyEndCountdown: Int, onClick: () -> Unit) {
    Card(
        backgroundColor = Color.White,
        modifier = Modifier
            .padding(10.dp)
            .clickable {
                onClick()
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "The game will soon end")
            Text(text = "Press to skip")
            Text(text = secondsToText(lobbyEndCountdown))
        }
    }
}

