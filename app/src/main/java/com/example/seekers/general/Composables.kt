package com.example.seekers.general

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.seekers.ui.theme.*
import java.util.*

@Composable
fun CustomOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusManager: FocusManager,
    label: String,
    placeholder: String,
    trailingIcon: @Composable() (() -> Unit)? = null,
    keyboardType: KeyboardType,
    passwordVisible: Boolean? = null,
    isError: Boolean = false,
    emailIsAvailable: Boolean? = null,
    modifier: Modifier? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        isError = isError,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            errorBorderColor = SizzlingRed,
            errorLabelColor = SizzlingRed,
            errorCursorColor = SizzlingRed,
            errorLeadingIconColor = SizzlingRed,
            errorTrailingIconColor = SizzlingRed,
            focusedBorderColor = if (emailIsAvailable == true) emailAvailable else Raisin,
            focusedLabelColor = if (emailIsAvailable == true) emailAvailable else Raisin,
            unfocusedBorderColor = if (emailIsAvailable == true) emailAvailable else Raisin,
            unfocusedLabelColor = if (emailIsAvailable == true) emailAvailable else Raisin,
            trailingIconColor = if (emailIsAvailable == true) emailAvailable else Raisin
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }),
        label = { Text(text = label) },
        placeholder = { Text(text = placeholder) },
        visualTransformation = if (passwordVisible == true || passwordVisible == null) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = trailingIcon,
        modifier = modifier ?: Modifier
    )
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


@Composable
fun QRScanner(context: Context, onScanned: (String) -> Unit) {
    val scannerView = CodeScannerView(context)
    val codeScanner = CodeScanner(context, scannerView)
    codeScanner.camera = CodeScanner.CAMERA_BACK
    codeScanner.formats = CodeScanner.ALL_FORMATS
    codeScanner.autoFocusMode = AutoFocusMode.SAFE
    codeScanner.scanMode = ScanMode.SINGLE
    codeScanner.isAutoFocusEnabled = true
    codeScanner.isFlashEnabled = false
    codeScanner.decodeCallback = DecodeCallback {
        codeScanner.stopPreview()
        codeScanner.releaseResources()
        onScanned(it.text)
    }
    codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
        Log.e("qrScanner", "QrScannerScreen: ", it)
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val layout = FrameLayout(it)
            layout.addView(scannerView)
            codeScanner.startPreview()
            layout
        })
}

@Composable
fun CustomButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    Button(
        border = BorderStroke(1.dp, Raisin),
        onClick = onClick,
        modifier = modifier
            .width(width.dp)
            .height(50.dp),
        shape = RoundedCornerShape(15),
        colors = ButtonDefaults.outlinedButtonColors(Emerald, contentColor = Raisin)
    ) {
        Text(text = text.uppercase(Locale.ROOT))
    }
}

@Composable
fun LogOutButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    Button(
        border = BorderStroke(1.dp, Color.White),
        onClick = onClick,
        modifier = modifier
            .width(150.dp)
            .height(50.dp),
        shape = RoundedCornerShape(15),
        colors = ButtonDefaults.outlinedButtonColors(SizzlingRed, contentColor = Color.White)
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "", tint = Color.White)
        Text(text = text.uppercase(Locale.ROOT))
    }
}

@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    resourceId: Int,
    buttonText: String,
    buttonColor: Color,
    onClick: () -> Unit
) {

    Button(
        border = BorderStroke(1.dp, Raisin),
        onClick = onClick,
        modifier = modifier
            .height(50.dp),
        shape = RoundedCornerShape(15),
        colors = ButtonDefaults.buttonColors(buttonColor, contentColor = Raisin),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = resourceId),
                    modifier = Modifier
                        .size(18.dp),
                    contentDescription = "drawable_icons",
                    tint = Color.Unspecified
                )
            }
            Text(text = buttonText.uppercase(Locale.ROOT))
        }
    }
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 50f..200f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    Column() {
        // Text(text = value.toString(), fontSize = 10.sp)
        Slider(
            colors = SliderDefaults.colors(
                thumbColor = Emerald,
                activeTrackColor = Emerald,
                inactiveTrackColor = Raisin
            ),
            onValueChangeFinished = onValueChangeFinished,
            steps = steps,
            valueRange = valueRange,
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .then(modifier)
        )
    }
}

@Composable
fun QRCodeComponent(modifier: Modifier = Modifier, bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR",
        modifier = modifier.size(250.dp)
    )
}

@Composable
fun AvatarIcon(modifier: Modifier = Modifier, imgModifier: Modifier = Modifier, resourceId: Int) {
    Card(
        shape = CircleShape,
        border = BorderStroke(2.dp, Color.Black),
        backgroundColor = avatarBackground,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = "avatar",
            modifier = imgModifier
        )
    }
}