package com.example.seekers

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.IconButton
import com.example.seekers.ui.theme.Emerald
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.ui.theme.SizzlingRed
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.*

@Composable
fun LobbyCreationScreen(
    vm: LobbyCreationScreenViewModel = viewModel(),
    navController: NavController,
    nickname: String,
    avatarId: Int,
    permissionVM: PermissionsViewModel
) {
    val context = LocalContext.current
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val radius by vm.radius.observeAsState()
    val countdown by vm.countdown.observeAsState()
    val center by vm.center.observeAsState()
    val currentLocation by vm.currentLocation.observeAsState()
    val showMap by vm.showMap.observeAsState(false)
    val isLocationAllowed by permissionVM.fineLocPerm.observeAsState(false)

    var initialLocationSet by remember { mutableStateOf(false) }
    val cameraState = rememberCameraPositionState()
    val screenHeight = LocalConfiguration.current.screenHeightDp * 0.3

    LaunchedEffect(Unit) {
        permissionVM.checkAllPermissions(context)
    }

    LaunchedEffect(isLocationAllowed) {
        if (isLocationAllowed) {
            vm.requestLoc()
        }
    }

    if (!initialLocationSet) {
        LaunchedEffect(currentLocation) {
            currentLocation?.let {
                cameraState.position = CameraPosition.fromLatLngZoom(it, 16f)
                initialLocationSet = true
            }
        }
    }

    if (!showMap) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(3f), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(id = R.string.lobby_creation).uppercase(Locale.ROOT),
                        fontSize = 32.sp
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(1.dp)
                            .background(color = Raisin)
                    )
                }

            }

            Column(Modifier.fillMaxWidth()) {
                CreationForm(vm = vm)
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable {
                            vm.updateShowMap(true)
                        },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.map),
                            contentDescription = "map",
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(text = "DEFINE PLAY AREA", fontSize = 22.sp)
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

            Spacer(modifier = Modifier.weight(1f))
            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.create_lobby)
            ) {
                if (maxPlayers!! >= 2 && timeLimit!! >= 10 && radius != null && countdown!! >= 30) {
                    if (center == null) {
                        Toast.makeText(
                            context,
                            "Please set a location for the game",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        vm.removeLocationUpdates()
                        val geoPoint = GeoPoint(center!!.latitude, center!!.longitude)
                        val lobby = Lobby(
                            id = "",
                            center = geoPoint,
                            maxPlayers = maxPlayers!!,
                            timeLimit = timeLimit!!,
                            radius = radius!!,
                            status = LobbyStatus.CREATED.ordinal,
                            countdown = countdown!!
                        )
                        val gameId = vm.addLobby(lobby)
                        val player = Player(
                            nickname = nickname,
                            avatarId = avatarId,
                            playerId = FirebaseHelper.uid!!,
                            inLobbyStatus = InLobbyStatus.CREATOR.ordinal,
                            inGameStatus = InGameStatus.SEEKER.ordinal
                        )
                        vm.addPlayer(player, gameId)
                        vm.updateUser(
                            FirebaseHelper.uid!!,
                            mapOf(Pair("currentGameId", gameId))
                        )
                        navController.navigate(NavRoutes.LobbyQR.route + "/$gameId")
                    }
                } else {
                    if (maxPlayers!! < 2) {
                        vm.showMaxPlayersError.value = true
                        vm.maxPlayersError.value = true
                    }
                    if (timeLimit!! < 10) {
                        vm.showTimeLimitError.value = true
                        vm.timeLimitError.value = true
                    }
                    if(countdown!! < 30) {
                        vm.showCountDownError.value = true
                        vm.countDownError.value = true
                    }
                }
            }
        }
    } else {
        if (isLocationAllowed) {
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
                        isMyLocationEnabled = true
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

        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Please allow location to set a playing area")
            }
        }
    }
}

@Composable
fun CreationForm(modifier: Modifier = Modifier, vm: LobbyCreationScreenViewModel) {
    val maxPlayers by vm.maxPlayers.observeAsState()
    val timeLimit by vm.timeLimit.observeAsState()
    val countdown by vm.countdown.observeAsState()
    val maxPlayersError by vm.maxPlayersError.observeAsState(false)
    val showMaxPlayersError by vm.maxPlayersError.observeAsState(false)
    val timeLimitError by vm.timeLimitError.observeAsState(false)
    val showTimeLimitError by vm.timeLimitError.observeAsState(false)
    val countDownError by vm.countDownError.observeAsState(false)
    val showCountDownError by vm.countDownError.observeAsState(false)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column() {
            Input(
                title = stringResource(id = R.string.max_players),
                value = maxPlayers?.toString() ?: "",
                isError = maxPlayersError,
                keyboardType = KeyboardType.Number,
                onChangeValue = { vm.updateMaxPlayers(it.toIntOrNull()) })
            if (showMaxPlayersError) ValidationErrorRow(text = "Minimum 2 players", modifier = Modifier.padding(top = 5.dp))
        }
        Column() {
            Input(
                title = stringResource(id = R.string.time_limit),
                value = timeLimit?.toString() ?: "",
                isError = timeLimitError,
                keyboardType = KeyboardType.Number,
                onChangeValue = { vm.updateTimeLimit(it.toIntOrNull()) })
            if (showTimeLimitError) ValidationErrorRow(text = "Minimum 10 minutes", modifier = Modifier.padding(top = 5.dp))
        }
        Column() {
            Input(
                title = stringResource(id = R.string.countdown),
                value = countdown?.toString() ?: "",
                isError = countDownError,
                keyboardType = KeyboardType.Number,
                onChangeValue = { vm.updateCountdown(it.toIntOrNull()) })
            if (showCountDownError) ValidationErrorRow(text = "Minimum 30 seconds", modifier = Modifier.padding(top = 5.dp))
        }

    }
}

@Composable
fun Input(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    onChangeValue: (String) -> Unit
) {
    Column(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onChangeValue,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                label = { Text(text = title) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    errorBorderColor = SizzlingRed,
                    errorLabelColor = SizzlingRed,
                    errorCursorColor = SizzlingRed,
                    errorLeadingIconColor = SizzlingRed,
                    errorTrailingIconColor = SizzlingRed,
                    focusedBorderColor = Raisin,
                    focusedLabelColor = Raisin,
                    unfocusedBorderColor = Raisin,
                    unfocusedLabelColor = Raisin,
                    trailingIconColor = Raisin
                ),
                singleLine = true
            )
        }
    }
}

class LobbyCreationScreenViewModel(application: Application) : AndroidViewModel(application) {

    val TAG = "LobbyVM"

    // Firebase
    val firestore = FirebaseHelper

    // Players
    val maxPlayers = MutableLiveData<Int>()
    val players = MutableLiveData(listOf<Player>())
    val isCreator = MutableLiveData<Boolean>()

    // Lobby
    val lobby = MutableLiveData<Lobby>()
    val timeLimit = MutableLiveData<Int>()
    val countdown = MutableLiveData<Int>()
    val center: MutableLiveData<LatLng> = MutableLiveData<LatLng>(null)
    val radius = MutableLiveData(50)

    // Map
    val showMap = MutableLiveData(false)
    val currentLocation = MutableLiveData<LatLng>()
    private val client = LocationServices.getFusedLocationProviderClient(application)
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            p0.lastLocation?.let {
                currentLocation.postValue(LatLng(it.latitude, it.longitude))
            }
        }
    }

    // Other
    val maxPlayersError = MutableLiveData(false)
    val timeLimitError = MutableLiveData(false)
    val countDownError = MutableLiveData(false)
    val showMaxPlayersError = MutableLiveData(false)
    val showTimeLimitError = MutableLiveData(false)
    val showCountDownError = MutableLiveData(false)

    // Map functions

    fun updateCenter(location: LatLng) {
        center.value = location
    }

    fun updateRadius(newVal: Int) {
        radius.value = newVal
    }

    fun updateShowMap(newVal: Boolean) {
        showMap.value = newVal
    }

    fun requestLoc() {
        LocationHelper.requestLocationUpdates(
            client = client,
            locationCallback = locationCallback
        )
    }

    fun removeLocationUpdates() {
        LocationHelper.removeLocationUpdates(client, locationCallback)
    }

    // Player functions

    fun addPlayer(player: Player, gameId: String) = firestore.addPlayer(player, gameId)

    fun removePlayer(gameId: String, playerId: String) =
        firestore.removePlayer(gameId = gameId, playerId = playerId)

    fun getPlayers(gameId: String) {
        firestore.getPlayers(gameId)
            .addSnapshotListener { list, e ->
                list ?: run {
                    Log.e(TAG, "getPlayers: ", e)
                    return@addSnapshotListener
                }
                val playerList = list.toObjects(Player::class.java)
                players.postValue(playerList)
            }
    }

    fun getPlayer(gameId: String, playerId: String) {
        firestore.getPlayer(gameId, playerId).get()
            .addOnSuccessListener { data ->
                val player = data.toObject(Player::class.java)
                player?.let {
                    isCreator.postValue(it.inLobbyStatus == InLobbyStatus.CREATOR.ordinal)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "getPlayer: ", it)
            }
    }

    fun updateUser(userId: String, changeMap: Map<String, Any>) =
        firestore.updateUser(userId, changeMap)

    fun updateMaxPlayers(newVal: Int?) {
        maxPlayers.value = newVal
    }

    // Lobby functions

    fun addLobby(lobby: Lobby) = firestore.addLobby(lobby)

    fun getLobby(gameId: String) {
        firestore.getLobby(gameId).addSnapshotListener { data, e ->
            data?.let {
                val lobbyFetched = it.toObject(Lobby::class.java)
                if (lobbyFetched != null) {
                    lobby.postValue(lobbyFetched)
                    maxPlayers.postValue(lobbyFetched.maxPlayers)
                    timeLimit.postValue(lobbyFetched.timeLimit)
                    countdown.postValue(lobbyFetched.countdown)
                    radius.postValue(lobbyFetched.radius)
                    center.postValue(LatLng(lobbyFetched.center.latitude, lobbyFetched.center.longitude))
                }
            }
        }
    }

    fun updateLobby(changeMap: Map<String, Any>, gameId: String) =
        firestore.updateLobby(changeMap, gameId)

    fun updateTimeLimit(newVal: Int?) {
        timeLimit.value = newVal
    }

    fun updateCountdown(newVal: Int?) {
        countdown.value = newVal
    }

}