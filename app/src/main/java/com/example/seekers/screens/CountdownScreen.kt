package com.example.seekers.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.NavRoutes
import com.example.seekers.viewModels.CountdownViewModel

@Composable
fun CountdownScreen(
    gameId: String,
    navController: NavHostController,
    vm: CountdownViewModel = viewModel(),
) {
    val initialValue by vm.initialValue.observeAsState()
    val countdown by vm.countdown.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.startService(context, gameId)
        vm.getInitialValue(gameId)
    }

    LaunchedEffect(countdown) {
        countdown?.let {
            if (it == 0) {
                vm.stopService(context)
                navController.navigate(NavRoutes.Heatmap.route + "/$gameId")
            }
        }
    }

    initialValue?.let { initial ->
        countdown?.let { cd ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp), contentAlignment = Alignment.Center
            ) {
                countdown?.let {
                    CountdownTimerUI(countdown = cd, initialTime = initial)
                }
            }
        }
    }

}

@Composable
fun CountdownTimerUI(countdown: Int, initialTime: Int) {
    val floatLeft = if (countdown == 0) 0f else (countdown.toFloat() / initialTime)
    val animatedProgress = animateFloatAsState(
        targetValue = floatLeft,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = animatedProgress,
            strokeWidth = 10.dp,
            color = Color.LightGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
                .height(0.dp)
                .onGloballyPositioned {
                    size = it.size
                }
                .offset(0.dp, -(with(LocalDensity.current) { size.width.toDp() }) / 2)
        )
        Text(text = convertToClock(countdown), style = MaterialTheme.typography.h1)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(text = "Hiding phase")
        }
    }
}

fun convertToClock(seconds: Int): String {
    val minutes = (seconds / 60)
    val minutesString = if (minutes < 10) "0$minutes" else minutes.toString()
    val secs = seconds % 60
    val secsString = if (secs < 10) "0$secs" else secs.toString()
    return "$minutesString:$secsString"
}