package com.example.seekers

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seekers.general.NavRoutes
import com.example.seekers.screens.*
import com.example.seekers.ui.theme.SeekersTheme
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Google
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build()
            )
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.your_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                e.localizedMessage?.let { Log.d(TAG, it) }
            }

        FirebaseApp.initializeApp(this)

        setContent {
            SeekersTheme {
                MyAppNavHost()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MyAppNavHost(permissionVM: PermissionsViewModel = viewModel()) {

    val navController = rememberNavController()
    val auth = Firebase.auth
    val showPermissionDialog by permissionVM.showDialog.observeAsState(false)

    NavHost(
        navController = navController,
        startDestination = NavRoutes.MainScreen.route
    ) {
        // Login or Sign up
        composable(NavRoutes.MainScreen.route) {
            MainScreen(navController = navController)
        }

        // Create Account screen
        composable(NavRoutes.CreateAccount.route) {
            CreateUserScreen(auth = auth, navController = navController)
        }

        // Create lobby or join game screen
        composable(NavRoutes.StartGame.route) {
            StartGameScreen(navController, permissionVM = permissionVM)
        }
        // Avatar picker screen
        composable(
            NavRoutes.AvatarPicker.route + "/{isCreator}",
            arguments = listOf(
                navArgument("isCreator") { type = NavType.BoolType }
            )
        ) {
            val isCreator = it.arguments!!.getBoolean("isCreator")
            AvatarPickerScreen(navController = navController, isCreator = isCreator)
        }

        //Lobby rules screen
        composable(
            NavRoutes.LobbyCreation.route + "/{nickname}/{avatarId}",
            arguments = listOf(
                navArgument("nickname") {
                    type = NavType.StringType
                },
                navArgument("avatarId") {
                    type = NavType.IntType
                }
            )) {
            val nickname = it.arguments!!.getString("nickname")!!
            val avatarId = it.arguments!!.getInt("avatarId")
            LobbyCreationScreen(
                navController = navController,
                nickname = nickname,
                avatarId = avatarId,
                permissionVM = permissionVM
            )
        }

        //Lobby screen with QR
        composable(
            NavRoutes.LobbyQR.route + "/{gameId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            )
        ) {
            val gameId = it.arguments!!.getString("gameId")!!
            LobbyQRScreen(
                navController = navController,
                gameId = gameId,
                permissionVM = permissionVM
            )
        }
        //QR Scanner
        composable(
            NavRoutes.Scanner.route + "/{nickname}/{avatarId}",
            arguments = listOf(
                navArgument("nickname") {
                    type = NavType.StringType
                },
                navArgument("avatarId") {
                    type = NavType.IntType
                }
            )
        ) {
            val nickname = it.arguments!!.getString("nickname")!!
            val avatarId = it.arguments!!.getInt("avatarId")
            QrScannerScreen(
                navController,
                nickname = nickname,
                avatarId = avatarId,
                permissionVM = permissionVM
            )
        }

        //Countdown
        composable(
            NavRoutes.Countdown.route + "/{gameId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            )
        ) {
            val gameId = it.arguments!!.getString("gameId")!!
            CountdownScreen(gameId = gameId, navController = navController)
        }

        //Heatmap
        composable(
            NavRoutes.Heatmap.route + "/{gameId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            )
        ) {
            val gameId = it.arguments!!.getString("gameId")!!
            HeatMapScreen(
                mapControl = true,
                navController = navController,
                gameId = gameId,
                permissionVM = permissionVM
            )
        }
        //Game end screen
        composable(
            NavRoutes.EndGame.route + "/{gameId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            )
        ) {
            val gameId = it.arguments!!.getString("gameId")!!
            GameEndScreen(navController = navController, gameId = gameId)
        }
    }
    if (showPermissionDialog) {
        PermissionsDialog(onDismiss = { permissionVM.updateShowDialog(false) }, vm = permissionVM)
    }
}

@Composable
fun googleRememberFirebaseAuthLauncher(
    onAuthComplete: (AuthResult) -> Unit,
    onAuthError: (ApiException) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            scope.launch {
                val authResult = async { Firebase.auth.signInWithCredential(credential) }
                delay(2000)
                onAuthComplete(authResult.await().result)
            }
        } catch (e: ApiException) {
            onAuthError(e)
            Log.d("onAuthError", e.toString())
        }
    }
}
