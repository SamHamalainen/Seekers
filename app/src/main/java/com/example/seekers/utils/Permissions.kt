package com.example.seekers.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seekers.general.CustomButton

/**
 * Permissions: Contains all the utilities necessary to check that a user has granted all the permissions
 * required in the app: 
 * - Location: coarse, fine and background
 * - Camera
 * - Activity recognition
 * - Foreground service
 */

class PermissionsViewModel : ViewModel() {
    val fineLocPerm = MutableLiveData<Boolean>()
    val backgroundLocPerm = MutableLiveData<Boolean>()
    val cameraPerm = MutableLiveData<Boolean>()
    val activityRecPerm = MutableLiveData<Boolean>()
    val foregroundServPerm = MutableLiveData<Boolean>()
    val showDialog = MutableLiveData(false)

    fun getLiveData(requiredPermission: RequiredPermission): MutableLiveData<Boolean> {
        return when (requiredPermission) {
            RequiredPermission.FINE_LOCATION -> fineLocPerm
            RequiredPermission.BACKGROUND_LOCATION -> backgroundLocPerm
            RequiredPermission.CAMERA -> cameraPerm
            RequiredPermission.ACTIVITY_RECOGNITION -> activityRecPerm
            RequiredPermission.FOREGROUND_SERVICE -> foregroundServPerm
        }
    }

    // Checks all current permissions and updates the corresponding livedata
    fun checkAllPermissions(context: Context) {
        RequiredPermission.values().forEach {
            getLiveData(it).value = isPermissionGranted(context, it.value)
        }
        showDialog.value = !allPermissionsgranted()
        println("showDialog: ${!allPermissionsgranted()}")
    }

    // return true if all permissions are granted
    private fun allPermissionsgranted(): Boolean {
        return fineLocPerm.value == true
                && backgroundLocPerm.value == true
                && cameraPerm.value == true
                && activityRecPerm.value == true
                && foregroundServPerm.value == true

    }

    // shows full screen dialog if all the permissions are not granted
    fun updateShowDialog(newVal: Boolean) {
        showDialog.value = newVal
    }
}

// enum class with all the required permissions in this app
enum class RequiredPermission(val value: String, val text: String) {
    FINE_LOCATION(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "\uD83D\uDCCD Fine location"
    ),
    BACKGROUND_LOCATION(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        "\uD83D\uDCCD Background location (All the time)"
    ),
    CAMERA(
        Manifest.permission.CAMERA, "\uD83D\uDCF7 Camera"
    ),
    ACTIVITY_RECOGNITION(
        Manifest.permission.ACTIVITY_RECOGNITION,
        "\uD83D\uDC5F Activity recognition"
    ),
    FOREGROUND_SERVICE(
        Manifest.permission.FOREGROUND_SERVICE, "\uD83D\uDD14 Foreground Service"
    ),
}

// Get a list of all the launchers corresponding to the required permissions.
// OnResult, they update the corresponding livedata
@Composable
fun getLauncherList(vm: PermissionsViewModel): MutableList<ManagedActivityResultLauncher<String, Boolean>> {
    val launcherList = mutableListOf<ManagedActivityResultLauncher<String, Boolean>>()
    RequiredPermission.values().forEach { perm ->
        val launcher = getPermissionLauncher(onResult = { vm.getLiveData(perm).value = it })
        launcherList.add(launcher)
    }
    return launcherList
}

// Tile which represent a permission and its current status (accepted or not).
// Pressing one of them will prompt the user to allow a permission if it hasn't been yet.
@Composable
fun PermissionTile(permission: String, isGranted: Boolean, onClick: () -> Unit) {
    val iconVector =
        if (isGranted) Icons.Filled.CheckCircleOutline else Icons.Filled.ErrorOutline
    val color = if (isGranted) Color.Green else Color.Red
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
                contentDescription = "isGranted: $isGranted",
                tint = color
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.LightGray)
        )
    }

}

// Full screen dialog which prompts the user to accept all the required permissions for the app to work
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PermissionsDialog(onDismiss: () -> Unit, vm: PermissionsViewModel) {
    val fineLocPerm by vm.fineLocPerm.observeAsState()
    val backgroundLocPerm by vm.backgroundLocPerm.observeAsState()
    val cameraPerm by vm.cameraPerm.observeAsState()
    val activityRecPerm by vm.activityRecPerm.observeAsState()
    val foregroundServPerm by vm.foregroundServPerm.observeAsState()
    val allPermissionsOK by remember {
        derivedStateOf {
                    fineLocPerm == true
                    && backgroundLocPerm == true
                    && cameraPerm == true
                    && activityRecPerm == true
                    && foregroundServPerm == true
        }
    }

    // List of all the permission request launchers
    var permLaunchers: List<ManagedActivityResultLauncher<String, Boolean>> by remember {
        mutableStateOf(
            listOf()
        )
    }
    permLaunchers = getLauncherList(vm = vm)

    fun getPermissionValue(requiredPermission: RequiredPermission): Boolean? {
        return when (requiredPermission) {
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
                                isGranted = getPermissionValue(it) == true,
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

// Get a permission request launcher
@Composable
fun getPermissionLauncher(onResult: (Boolean) -> Unit): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult
    )
}

// Check if a permission is currently granted
fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}
