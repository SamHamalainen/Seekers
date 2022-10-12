package com.example.seekers

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.LogOutButton
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.general.getPermissionLauncher
import com.example.seekers.general.isPermissionGranted
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun StartGameScreen(navController: NavController, vm: PermissionsViewModel = viewModel()) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showLogOutDialog by remember { mutableStateOf(false) }
    val screenHeight = LocalConfiguration.current.screenHeightDp * 0.3

    LaunchedEffect(Unit) {
        vm.checkAllPermissions(context)
        if (!vm.allPermissionsAllowed()) {
            showPermissionDialog = true
        }
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
                // Spacer(modifier = Modifier.height(50.dp))
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

                    /*
                    CustomButton(text = "Join lobby") {
                        navController.navigate(NavRoutes.AvatarPicker.route + "/false")
                    } */
                }

            }
            Text(
                text = "${FirebaseHelper.uid}", modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            ) {
                LogOutButton(text = "Log out") {
                    Firebase.auth.signOut()
                    println("logged user: ${Firebase.auth.currentUser}")
                    navController.navigate(NavRoutes.MainScreen.route)
                }
            }
        }
        if (showPermissionDialog) {
            PermissionsDialog(onDismiss = { showPermissionDialog = false }, vm = vm)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PermissionsDialog(onDismiss: () -> Unit, vm: PermissionsViewModel) {
    val coarseLocPerm by vm.coarseLocPerm.observeAsState()
    val fineLocPerm by vm.fineLocPerm.observeAsState()
    val backgroundLocPerm by vm.backgroundLocPerm.observeAsState()
    val cameraPerm by vm.cameraPerm.observeAsState()
    val activityRecPerm by vm.activityRecPerm.observeAsState()
    val foregroundServPerm by vm.foregroundServPerm.observeAsState()
    val allPermissionsOK by remember {
        derivedStateOf {
            coarseLocPerm == true
                    && fineLocPerm == true
                    && backgroundLocPerm == true
                    && cameraPerm == true
                    && activityRecPerm == true
                    && foregroundServPerm == true
        }
    }

    var permLaunchers: List<ManagedActivityResultLauncher<String, Boolean>> by remember {
        mutableStateOf(
            listOf()
        )
    }
    permLaunchers = getLauncherList(vm = vm)

    fun getPermissionValue(requiredPermission: RequiredPermission): Boolean? {
        return when (requiredPermission) {
            RequiredPermission.COARSE_LOCATION -> coarseLocPerm
            RequiredPermission.FINE_LOCATION -> fineLocPerm
            RequiredPermission.BACKGROUND_LOCATION -> backgroundLocPerm
            RequiredPermission.CAMERA -> cameraPerm
            RequiredPermission.ACTIVITY_RECOGNITION -> activityRecPerm
            RequiredPermission.FOREGROUND_SERVICE -> foregroundServPerm
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(backgroundColor = Color.White, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permissions needed",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Please press on the missing permissions to allow them",
                        fontSize = 16.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.White,
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                    ) {
                        RequiredPermission.values().forEach {
                            PermissionTile(
                                permission = it.text,
                                isAllowed = getPermissionValue(it) == true,
                                onClick = {
                                    permLaunchers[it.ordinal].launch(it.value)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
                CustomButton(
                    modifier = Modifier.alpha(if (!allPermissionsOK) 0f else 1f),
                    text = "Continue"
                ) {
                    if (allPermissionsOK) {
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
fun getLauncherList(vm: PermissionsViewModel): MutableList<ManagedActivityResultLauncher<String, Boolean>> {
    val launcherList = mutableListOf<ManagedActivityResultLauncher<String, Boolean>>()
    RequiredPermission.values().forEach { perm ->
        val launcher = getPermissionLauncher(onResult = { vm.getLiveData(perm).value = it })
        launcherList.add(launcher)
    }
    return launcherList
}

@Composable
fun PermissionTile(permission: String, isAllowed: Boolean, onClick: () -> Unit) {
    val iconVector =
        if (isAllowed) Icons.Filled.CheckCircleOutline else Icons.Filled.ErrorOutline
    val color = if (isAllowed) Color.Green else Color.Red
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = permission)
            Icon(
                imageVector = iconVector,
                contentDescription = "isAllowed: $isAllowed",
                tint = color
            )
        }
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.LightGray))
    }

}