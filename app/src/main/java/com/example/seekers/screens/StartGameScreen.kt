package com.example.seekers.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.seekers.PermissionsViewModel
import com.example.seekers.R
import com.example.seekers.composables.LogOutDialog
import com.example.seekers.composables.TutorialDialog
import com.example.seekers.general.LogOutButton
import com.example.seekers.general.NavRoutes
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun StartGameScreen(
    navController: NavController,
    permissionVM: PermissionsViewModel
) {
    val context = LocalContext.current
    var showLogOutDialog by remember { mutableStateOf(false) }
    val buttonHeight = LocalConfiguration.current.screenHeightDp * 0.3
    var showTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
    }

    Surface {
        Box(
            Modifier
                .fillMaxSize()
                .background(Powder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StartScreenButton(
                    buttonHeight = buttonHeight,
                    onClick = { navController.navigate(NavRoutes.AvatarPicker.route + "/true") },
                    painterResource = painterResource(R.drawable.illustration1),
                    textAlignedRight = true,
                    text = "CREATE\nLOBBY"
                )
                StartScreenButton(
                    buttonHeight = buttonHeight,
                    onClick = { navController.navigate(NavRoutes.AvatarPicker.route + "/false") },
                    painterResource = painterResource(R.drawable.illustration2),
                    textAlignedRight = false,
                    text = "JOIN\nLOBBY"
                )
                StartScreenButton(
                    buttonHeight = buttonHeight,
                    onClick = { showTutorial = true },
                    painterResource = painterResource(R.drawable.tutorial_icon),
                    isTutorial = true,
                    textAlignedRight = false,
                    text = "QUICK-START\nGUIDE"
                )
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
            TutorialDialog {
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
fun StartScreenButton(
    buttonHeight: Double,
    onClick: () -> Unit,
    painterResource: Painter,
    isTutorial: Boolean = false,
    textAlignedRight: Boolean,
    text: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (!isTutorial) buttonHeight.dp else buttonHeight.times(0.5).dp)
            .padding(horizontal = 15.dp, vertical = 5.dp)
            .clickable { onClick() },

        elevation = 10.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource,
                contentDescription = "illustration",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isTutorial) 16.dp else 0.dp),
                alignment = if (textAlignedRight) Alignment.CenterStart else Alignment.CenterEnd
            )
            Box(
                modifier = Modifier
                    .align(if (textAlignedRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(if (isTutorial) 16.dp else 32.dp)
            ) {
                Column {
                    Text(text = text, fontSize = 22.sp)
                    Box(
                        modifier = Modifier
                            .width(if (isTutorial) 80.dp else 50.dp)
                            .height(1.dp)
                            .background(color = Raisin)
                    )
                }
            }
        }
    }
}

