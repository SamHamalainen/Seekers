package com.example.seekers.composables

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.general.CustomButton
import com.example.seekers.general.VerticalSlider
import com.example.seekers.ui.theme.*
import com.example.seekers.viewModels.LobbyCreationScreenViewModel
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

/**
 * AreaSelectionMap: GoogleMap for selecting the playing area during the creation of a lobby.
 */

@SuppressLint("MissingPermission")
@Composable
fun AreaSelectionMap(
    vm: LobbyCreationScreenViewModel = viewModel(),
    properties: MapProperties,
    settings: MapUiSettings,
    state: CameraPositionState
) {
    // Radius shown as a circle on the map
    val radius by vm.radius.observeAsState(50)
    Box(
        Modifier.fillMaxSize()
    ) {
        GoogleMap(
            cameraPositionState = state,
            properties = properties,
            uiSettings = settings
        ) {
            val center = LatLng(
                state.position.target.latitude,
                state.position.target.longitude
            )
            // Circle that represents the playing area
            Circle(
                center = center,
                radius = radius.toDouble(),
                fillColor = EmeraldTransparent2,
                strokeColor = EmeraldTransparent1,
                clickable = true
            )
        }
        Text(
            text = "$radius",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(0.dp, (-20).dp),
            color = SizzlingRed,
            fontWeight = FontWeight.Bold
        )

        // Button to set the center coordinate of the playing area and its radius
        Column(Modifier.align(Alignment.BottomCenter)) {
            CustomButton(
                modifier = Modifier.width(150.dp),
                text = "Define Area"
            ) {
                vm.updateCenter(
                    LatLng(
                        state.position.target.latitude,
                        state.position.target.longitude
                    )
                )
                vm.updateShowMap(false)
            }
            Spacer(Modifier.height(15.dp))
        }

        // Slider to change the radius of the playing area in meters
        Row(Modifier.align(Alignment.CenterEnd)) {
            VerticalSlider(
                value = radius.toFloat(),
                onValueChange = {
                    vm.updateRadius(it.toInt())
                },
                valueRange = 20f..500f,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            )
        }
    }
}
