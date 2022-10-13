package com.example.seekers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.seekers.general.AvatarIcon
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.SizzlingRed
import java.util.*

@Composable
fun PlayerListButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(shape = CircleShape, elevation = 4.dp, modifier = modifier.clickable { onClick() }) {
        Icon(
            imageVector = Icons.Filled.List,
            contentDescription = "playerList",
            modifier = Modifier.padding(8.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerListDialog(onDismiss: () -> Unit, players: List<Player>) {
    val height = LocalConfiguration.current.screenHeightDp * 0.7
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .padding(32.dp),
            backgroundColor = Powder,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 32.dp)) {
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
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp), elevation = 4.dp) {
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
                Text(text = player.nickname, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width((screenWidth*0.3).dp))
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

@Composable
fun PowersDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(8.dp), backgroundColor = Powder) {

        }
    }
}

@Composable
fun PowerButton(power: Power, onPress: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(shape = MaterialTheme.shapes.medium) {
            Icon(painter = painterResource(id = power.icon), contentDescription = "power", modifier = Modifier.padding(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = power.text)
    }
}

enum class Power(val icon: Int, val text: String, val duration: Int) {
    INVISIBILITY(1, "Invisibility", 15),
    JAMMER(2, "Jammer", 5),
    DECOY(3, "Decoy", 15),
    REVEAL(4, "Reveal seekers", 5)
}

