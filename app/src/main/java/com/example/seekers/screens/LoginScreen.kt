package com.example.seekers

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.*
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.viewModels.AuthenticationViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun LoginForm(
    vm: AuthenticationViewModel,
    navController: NavController,
) {
    var invalidCredentials by remember { mutableStateOf(false) }
    // Email
    var email by remember { mutableStateOf(TextFieldValue("")) }

    // Password
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    adjustContentWithKB(context, isPan = true)
    val height = LocalConfiguration.current.screenHeightDp

    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Image(
                painter = painterResource(R.drawable.seekers_ver3),
                contentDescription = "seekers",
                modifier = Modifier.height((height * 0.2).dp)
            )

        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Welcome back!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(text = "Sign in to continue", fontSize = 16.sp, color = Raisin)
        }

        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if(invalidCredentials) {
                Text(text = "Invalid email or password", color = Color.Red)
            }
            CustomOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                focusManager = focusManager,
                label = "Email",
                placeholder = "Email",
                keyboardType = KeyboardType.Email,
                modifier = Modifier.width(width.dp)
            )
            // Spacer(modifier = Modifier.height(10.dp))
            CustomOutlinedTextField(
                value = password,
                onValueChange = { password = it },
                focusManager = focusManager,
                label = "Password",
                placeholder = "Password",
                trailingIcon = {
                    val image = if (passwordVisible) {
                        Icons.Filled.Visibility
                    } else {
                        Icons.Filled.VisibilityOff
                    }
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                },
                keyboardType = KeyboardType.Password,
                passwordVisible = passwordVisible,
                modifier = Modifier.width(width.dp)
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                CustomButton(
                    onClick = {
                        if (email.text == "" || password.text == "") {
                            Toast.makeText(
                                context,
                                "Please give an email and password!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            vm.fireBaseAuth.signInWithEmailAndPassword(
                                email.text,
                                password.text
                            )
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        vm.setUser(it.result.user)
                                        println("logged in as: ${vm.fireBaseAuth.currentUser}")
                                        invalidCredentials = false
                                        navController.navigate(NavRoutes.StartGame.route)
                                    }
                                }
                                .addOnFailureListener {
                                    invalidCredentials = true
                                }
                        }
                    }, text = "Login"
                )
            }
        }
        Text(text = "Or", fontSize = 16.sp, color = Raisin)
        GoogleButton(context = context, vm = vm)
        Row {
            Text(text = "Don't have an account?", fontSize = 12.sp, color = Raisin)
            Text(
                text = " Create one now",
                fontSize = 12.sp,
                color = Color.Blue,
                modifier = Modifier
                    .clickable {
                        navController.navigate(NavRoutes.CreateAccount.route)
                    }
            )
        }
    }
}

@Composable
fun GoogleButton(
    context: Context,
    vm: AuthenticationViewModel
) {
    val token = stringResource(id = R.string.your_web_client_id)
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    val googleLauncher = googleRememberFirebaseAuthLauncher(
        onAuthComplete = {
            vm.setUser(it.user)
            Log.d("authenticated", "MainScreen: ${vm.fireBaseAuth.currentUser}")
        },
        onAuthError = {
            vm.setUser(null)
            Log.d("authenticated", "MainScreen: ${it.message}")
        }
    )
    Button(
        modifier = Modifier.width(width.dp),
        onClick = {
            val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(token)
                    .requestEmail()
                    .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleLauncher.launch(googleSignInClient.signInIntent)
        },
        colors = ButtonDefaults.buttonColors(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.google_logo),
            contentDescription = "Google logo"
        )
        Text(
            "Sign in with Google", fontSize = 17.sp
        )
    }
}