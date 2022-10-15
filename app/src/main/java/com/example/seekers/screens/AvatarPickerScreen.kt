package com.example.seekers.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.R
import com.example.seekers.general.*
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.ui.theme.avatarBackground
import com.example.seekers.viewModels.AvatarViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AvatarPickerScreen(
    vm: AvatarViewModel = viewModel(),
    navController: NavHostController,
    isCreator: Boolean,
) {
    val context = LocalContext.current
    val avatarId by vm.avatarId.observeAsState(R.drawable.avatar_empty)
    val nickname by vm.nickname.observeAsState("")
    val nicknameError by vm.nicknameError.observeAsState(false)
    val showNicknameEmpty by vm.showNicknameEmpty.observeAsState(false)
    val keyboardController = LocalSoftwareKeyboardController.current
    adjustContentWithKB(context, isPan = false)

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded }
    )
    val coroutineScope = rememberCoroutineScope()

    BackHandler(sheetState.isVisible) {
        coroutineScope.launch { sheetState.hide() }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            BottomSheet(onPick = {
                vm.avatarId.value = it
                coroutineScope.launch {
                    sheetState.hide()
                }
            })
        },
        modifier = Modifier
            .fillMaxSize()
            .background(color = Powder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "PLAYER CREATION", fontSize = 26.sp)
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(1.dp)
                    .background(color = Raisin)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, Raisin),
                backgroundColor = avatarBackground,
                modifier = Modifier
                    .clickable {
                        keyboardController?.hide()
                        coroutineScope.launch {
                            sheetState.show()
                        }
                    }
            ) {
                Image(
                    painter = painterResource(avatarId),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .padding(30.dp)
                        .size(150.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Input(
                modifier = Modifier.fillMaxWidth(),
                title = "Nickname",
                value = nickname,
                isError = nicknameError,
                onChangeValue = {
                    vm.nickname.value = it
                })
            if (showNicknameEmpty) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                    ValidationErrorRow(text = "Please enter a nickname")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            CustomButton(text = if (isCreator) "Continue" else "Join lobby") {
                val avatarIndex = avatarList.indexOf(avatarId)
                if (nickname.isNotBlank()) {
                    if (!isCreator) {
                        navController.navigate(NavRoutes.Scanner.route + "/$nickname/$avatarIndex")
                    } else {
                        navController.navigate(NavRoutes.LobbyCreation.route + "/$nickname/$avatarIndex")
                    }
                } else {
                    vm.nicknameError.value = true
                    vm.showNicknameEmpty.value = true
                }

            }
        }
    }
}

@Composable
fun BottomSheet(onPick: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "CHOOSE AVATAR", fontSize = 30.sp)
        Spacer(modifier = Modifier.height(32.dp))
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(avatarList) { avatar ->
                Card(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Raisin),
                    backgroundColor = avatarBackground,
                    modifier = Modifier
                        .padding(10.dp)
                        .clickable {
                            onPick(avatar)
                        }
                ) {
                    Image(
                        painter = painterResource(id = avatar),
                        contentDescription = "avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(20.dp)
                    )
                }
            }
        }
    }
}
