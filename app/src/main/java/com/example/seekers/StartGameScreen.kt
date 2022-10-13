package com.example.seekers

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.seekers.general.LogOutButton
import com.example.seekers.ui.theme.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.*

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun StartGameScreen(
    navController: NavController,
    permissionVM: PermissionsViewModel
) {
    val context = LocalContext.current
    var showLogOutDialog by remember { mutableStateOf(false) }
    val screenHeight = LocalConfiguration.current.screenHeightDp * 0.3
    var showTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
    }

    Surface {
        Box(
            Modifier
                .fillMaxSize()
                .background(Powder)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight.dp)
                        .padding(horizontal = 15.dp, vertical = 5.dp)
                        .clickable { navController.navigate(NavRoutes.AvatarPicker.route + "/true") },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.illustration1),
                            contentDescription = "illustration",
                            modifier = Modifier
                                .fillMaxSize(),
                            alignment = Alignment.CenterStart
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(32.dp)
                        ) {
                            Column {
                                Text(text = "CREATE\nLOBBY", fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(1.dp)
                                        .background(color = Raisin)
                                )
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight.dp)
                        .padding(horizontal = 15.dp, vertical = 5.dp)

                        .clickable { navController.navigate(NavRoutes.AvatarPicker.route + "/false") },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.illustration2),
                            contentDescription = "illustration",
                            modifier = Modifier
                                .fillMaxSize(),
                            alignment = Alignment.CenterEnd
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(32.dp)
                        ) {
                            Column {
                                Text(text = "JOIN\nLOBBY", fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(1.dp)
                                        .background(color = Raisin)
                                )
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((screenHeight * 0.5).dp)
                        .padding(horizontal = 15.dp, vertical = 5.dp)
                        .clickable { showTutorial = true },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.tutorial_icon),
                            contentDescription = "tutorial",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            alignment = Alignment.CenterEnd
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(text = "QUICK-START\nGUIDE", fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(1.dp)
                                        .background(color = Raisin)
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
            ) {
                LogOutButton(text = "Log out") {
                    showLogOutDialog = true
                }
            }
        }
        if (showTutorial) {
            TutorialDialog() {
                showTutorial = false
            }
        }
    }
    if (showLogOutDialog) {
        LogOutDialog(onDismissRequest = { showLogOutDialog = false }, onConfirm = {
            Firebase.auth.signOut()
            println("logged user: ${Firebase.auth.currentUser}")
            navController.navigate(NavRoutes.MainScreen.route)
        })
    }
    BackHandler(enabled = true) {
        showLogOutDialog = true
    }
}

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
                colors = ButtonDefaults.buttonColors(backgroundColor = SizzlingRed, contentColor = Color.White)
            ) {
                Text(text = "Log Out")
            }
        }
    )
}

@OptIn(ExperimentalPagerApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp * .9
    val screenWidth = LocalConfiguration.current.screenWidthDp * .9
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Powder)) {
            Column(
                Modifier
                    .background(Powder)
                    .fillMaxSize()) {
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

                // Display 10 items
                HorizontalPager(
                    count = 5,
                    state = pagerState,
                    // contentPadding to show previous/next pages slightly
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
internal fun PagerSampleItem(
    page: Int,
    modifier: Modifier = Modifier,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    if(page == 0) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Create a lobby to host your own game as the Seeker, and share the QR code for all your friends!", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
            Image(
                painter = painterResource(id = R.drawable.tutorial1),
                contentDescription = "tutorial",
                alignment = Alignment.Center,
                modifier = Modifier.height((screenHeight*0.5).dp),
            )
            Text(text = "Or join a lobby of a friend with a QR code and get ready to hide!", modifier = Modifier.padding(5.dp), fontSize = 18.sp)

        }
    } else if(page == 1) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Remember to pick a funny nickname so your friends can recognize you!", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
            Image(
                painter = painterResource(id = R.drawable.tutorial2_1),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "And most importantly, a cute avatar", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
            Image(
                painter = painterResource(id = R.drawable.tutorial2_2),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
        }
    } else if(page == 2) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Set the rules for your game", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
            Image(
                painter = painterResource(id = R.drawable.tutorial3),
                contentDescription = "tutorial",
                alignment = Alignment.Center,
                modifier = Modifier.height((screenHeight*0.5).dp),
                )
            Text(text = "And don't forget to define the play area before creating the lobby!", modifier = Modifier.padding(5.dp), fontSize = 18.sp)

        }
    } else if(page == 3) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Setting the play area is simple", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
            Image(
                painter = painterResource(id = R.drawable.tutorial4),
                contentDescription = "tutorial",
                alignment = Alignment.Center,
                modifier = Modifier.height((screenHeight*0.5).dp),
                )
            Text(text = "Just move the camera to a favorable position, set the radius with the vertical slider and you're good to go!", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
        }
    } else if(page == 4) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "While waiting for your friends, open up the QR code for them to scan from your phone", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
            Image(
                painter = painterResource(id = R.drawable.tutorial5_1),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "In case you want to change the rules, here's the option to do that!", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
            Image(
                painter = painterResource(id = R.drawable.tutorial5_3),
                contentDescription = "tutorial",
                alignment = Alignment.Center
            )
            Text(text = "Now start the game and off you go, hide quickly!", modifier = Modifier.padding(5.dp), fontSize = 18.sp)
        }
    }
}
