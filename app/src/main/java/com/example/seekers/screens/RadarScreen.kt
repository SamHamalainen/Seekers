package com.example.seekers.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.seekers.Player
import com.example.seekers.R
import com.example.seekers.general.AvatarIcon
import com.example.seekers.general.CustomButton
import com.example.seekers.general.avatarList
import com.example.seekers.ui.theme.Emerald
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.ui.theme.SizzlingRed
import com.example.seekers.viewModels.RadarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RadarScreen(
    vm: RadarViewModel = viewModel(),
    gameId: String
) {
    val scope = rememberCoroutineScope()
    val playersInGame by vm.players.observeAsState(listOf())
    val scanning by vm.scanningStatus.observeAsState(0)

    Column(
        modifier = Modifier
            .fillMaxSize().background(Powder),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val text = when (scanning) {
            ScanningStatus.BEFORE_SCAN.value -> "SCAN TO FIND NEARBY PLAYERS"
            ScanningStatus.SCANNING.value -> "SCANNING..."
            ScanningStatus.SCANNING_STOPPED.value -> "FOUND ${playersInGame.size} PLAYERS NEARBY"
            else -> ""
        }
        Text(
            text = text,
            fontSize = 24.sp,
            modifier = Modifier.padding(22.dp),
            textAlign = TextAlign.Center
        )
        if (scanning == ScanningStatus.SCANNING.value) {
            ScanningLottie()
        } else {
            FoundPlayerList(playersAndDistance = playersInGame, vm = vm, gameId = gameId)
        }
        CustomButton(text = "Scan", modifier = Modifier.padding(22.dp)) {
            vm.updateScanStatus(ScanningStatus.SCANNING.value)
            scope.launch {
                val gotPlayers = withContext(Dispatchers.IO) {
                    vm.getPlayers(gameId)
                }
                delay(5000)
                if (gotPlayers) {
                    vm.updateScanStatus(ScanningStatus.SCANNING_STOPPED.value)
                }
            }
        }
    }
}

enum class ScanningStatus(val value: Int) {
    BEFORE_SCAN(0),
    SCANNING(1),
    SCANNING_STOPPED(2)
}

@Composable
fun ScanningLottie() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scanning_nearby))
    LottieAnimation(composition)
}

@Composable
fun FoundPlayerList(
    playersAndDistance: List<Pair<Player, Float>>,
    vm: RadarViewModel,
    gameId: String
) {
    val height = LocalConfiguration.current.screenHeightDp * 0.5

    LazyColumn(
        modifier = Modifier
            .padding(15.dp)
            .height(height.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(playersAndDistance) { player ->
            FoundPlayerCard(player = player.first, distance = player.second)
        }
    }
}

@Composable
fun FoundPlayerCard(
    player: Player,
    distance: Float
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val avatarID = when (player.avatarId) {
        0 -> R.drawable.bee
        1 -> R.drawable.chameleon
        2 -> R.drawable.chick
        3 -> R.drawable.cow
        4 -> R.drawable.crab
        5 -> R.drawable.dog
        6 -> R.drawable.elephant
        7 -> R.drawable.fox
        8 -> R.drawable.koala
        9 -> R.drawable.lion
        10 -> R.drawable.penguin
        else -> R.drawable.whale
    }

    val backgroundColor = when (player.distanceStatus) {
        1 -> SizzlingRed
        2 -> Color.Yellow
        3 -> Emerald
        else -> Color.White
    }
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
                Card(shape = RoundedCornerShape(16.dp), backgroundColor = backgroundColor) {
                    Text(
                        text = "${distance.toInt()} m",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(vertical = 4.dp),
                        color = Raisin
                    )
                }
            }
        }
    }
    /*
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = 10.dp,
        backgroundColor = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                backgroundColor = avatarBackground,
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterStart)
            ) {
                Image(
                    painter = painterResource(id = avatarID),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp)
                )
            }
            Text(
                text = player.nickname,
                modifier = Modifier
                    .align(Alignment.Center)
            )
            Text(
                text = "${distance.toInt()} m",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            )
        }
    } */
}
