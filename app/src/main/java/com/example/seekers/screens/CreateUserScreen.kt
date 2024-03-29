package com.example.seekers.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navArgument
import com.example.seekers.general.CustomButton
import com.example.seekers.general.CustomOutlinedTextField
import com.example.seekers.general.NavRoutes
import com.example.seekers.general.ValidationErrorRow
import com.example.seekers.ui.theme.Raisin
import com.example.seekers.ui.theme.emailAvailable
import com.example.seekers.viewModels.AuthenticationViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * This screen is for creating a new account with email and password validation.
 */
@Composable
fun CreateUserScreen(
    vm: AuthenticationViewModel = viewModel(),
    auth: FirebaseAuth,
    navController: NavController
) {
    val email by vm.email.observeAsState(TextFieldValue(""))
    val password by vm.password.observeAsState(TextFieldValue(""))
    val passwordError by vm.passwordValidationError.observeAsState(null)

    BackArrowButton(navController)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(30.dp)
            .fillMaxSize(),
    ) {
        Text(text = "Create Account", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(text = "Create a new account", fontSize = 16.sp, color = Raisin)
        Spacer(modifier = Modifier.height(40.dp))
        EmailTextFieldWithValidation(vm)
        Spacer(modifier = Modifier.height(10.dp))
        PasswordTextFieldWithValidation(vm)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CustomButton(
                onClick = {
                    if (password.text != "" && email.text != "" && passwordError != false) {
                        val validated = vm.validatePassword(password.text)
                        if (validated) {
                            auth.createUserWithEmailAndPassword(
                                email.text,
                                password.text
                            )
                                .addOnCompleteListener() {
                                    navController.navigate(NavRoutes.MainScreen.route)
                                }
                        }
                    }
                }, text = "Create account"
            )
        }
    }
}

// Back button for navigating back to LoginScreen
@Composable
fun BackArrowButton(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        val backBtn = Icons.Filled.ArrowBack
        val desc = "Back Button"
        IconButton(
            onClick = { navController.navigate(NavRoutes.MainScreen.route) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) {
            Icon(imageVector = backBtn, desc, modifier = Modifier.size(32.dp))
        }
    }
}

// Password TextField with validation so users create users with stronger passwords.
@Composable
fun PasswordTextFieldWithValidation(vm: AuthenticationViewModel) {
    val password by vm.password.observeAsState(TextFieldValue(""))
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordError by vm.passwordValidationError.observeAsState(null)
    val focusManager = LocalFocusManager.current
    val width = LocalConfiguration.current.screenWidthDp * 0.8

    CustomOutlinedTextField(
        value = password,
        onValueChange = { vm.updatePasswordTextField(it) },
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
        isError = passwordError ?: false,
        modifier = Modifier
            .width(width.dp)
    )
    if (passwordError == true) {
        PasswordValidationErrorMessage()
    }
}

// Email TextField that checks if email is already in use and correct email format
@Composable
fun EmailTextFieldWithValidation(vm: AuthenticationViewModel) {
    val email by vm.email.observeAsState(TextFieldValue(""))
    val emailError by vm.emailValidationError.observeAsState(null)
    val emailIsAvailable by vm.emailIsAvailable.observeAsState(null)
    val focusManager = LocalFocusManager.current
    val width = LocalConfiguration.current.screenWidthDp * 0.8

    CustomOutlinedTextField(
        value = email,
        onValueChange = {
            vm.emailIsAvailable.value = null
            vm.updateEmailTextField(it)
            if (email.text.isNotBlank()) {
                vm.validateEmail(email.text)
            }
        },
        focusManager = focusManager,
        label = "Email",
        placeholder = "Email",
        trailingIcon = {
            if (emailIsAvailable == true && emailError == false) {
                val image = Icons.Filled.TaskAlt
                val description = "Email available"
                Icon(image, description)
            }
        },
        keyboardType = KeyboardType.Email,
        isError = emailError ?: false,
        emailIsAvailable = emailIsAvailable,
        modifier = Modifier
            .width(width.dp)
            .onFocusChanged {
                if (!it.isFocused && emailError == false && email.text != "") {
                    vm.checkEmailAvailability(email.text)
                }
            }
    )
    if (emailError == true) {
        Card(
            modifier = Modifier
                .width(width.dp)
                .padding(5.dp),
            elevation = 0.dp,
            backgroundColor = Color.Transparent
        ) {
            ValidationErrorRow(
                text = if (emailIsAvailable == false) "Email already in use"
                else "Please provide a valid email address"
            )
        }
    }
    if (emailIsAvailable == true && emailError == false) {
        EmailAvailableMessage()
    }
}

// Shows message under email TextField when email is available
@Composable
fun EmailAvailableMessage() {
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    Card(
        modifier = Modifier
            .width(width.dp)
            .padding(5.dp),
        elevation = 0.dp,
        backgroundColor = Color.Transparent
    ) {
        Text(text = "Available", color = emailAvailable)
    }
}

// Custom error message when password is not strong enough
@Composable
fun PasswordValidationErrorMessage() {
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    Card(
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(2.dp, Color.Red),
        modifier = Modifier
            .padding(5.dp)
            .width(width.dp),
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            ValidationErrorRow(
                text = "Password requires at least:",
                fontWeight = FontWeight.Bold
            )
            ValidationErrorRow(text = "\u2022 Minimum 8 characters long")
            ValidationErrorRow(text = "\u2022 1 Capital letter")
            ValidationErrorRow(text = "\u2022 1 Number")
        }
    }
}