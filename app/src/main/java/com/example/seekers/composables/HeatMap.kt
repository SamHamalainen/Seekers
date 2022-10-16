package com.example.seekers.composables

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.seekers.general.*
import com.example.seekers.viewModels.HeatMapViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.HeatmapTileProvider

/**
 * HeatMap: GoogleMap with a delimited playing area, players shown as heat markers when hiding
 * and as icons when on the move.
 */

@Composable
fun HeatMap(
    state: CameraPositionState,
    vm: HeatMapViewModel
) {
    val context = LocalContext.current
    val lobby by vm.lobby.observeAsState()
    val seekers by vm.currentSeekers.observeAsState(listOf())
    val movingPlayers by vm.movingPlayers.observeAsState(listOf())
    val heatPositions by vm.heatPositions.observeAsState(listOf())
    val canSeeSeeker by vm.canSeeSeeker.observeAsState(false)

    // Tile provider for the heat markers
    val tileProvider by remember {
        derivedStateOf {
            var provider: HeatmapTileProvider? = null
            if (heatPositions.isNotEmpty()) {
                provider = HeatmapTileProvider.Builder()
                    .data(heatPositions)
                    .build()
                provider.setRadius(200)
            }
            provider
        }
    }

    // Setting the center and radius of the playing area to display it on the GoogleMap.
    // Defining the bounds and the MapProperties of the GoogleMap
    lobby?.let {
        val density = LocalDensity.current
        val width = with(density) {
            LocalConfiguration.current.screenWidthDp.dp.toPx()
        }.times(0.5).toInt()

        val center = LatLng(
            it.center.latitude,
            it.center.longitude
        )
        val radius = it.radius

        val mapBounds = getBounds(center, radius)

        val minZoom = getBoundsZoomLevel(
            mapBounds,
            Size(width, width)
        ).toFloat()

        val properties = MapProperties(
            mapType = MapType.SATELLITE,
            isMyLocationEnabled = true,
            maxZoomPreference = 17.5F,
            minZoomPreference = minZoom,
            latLngBoundsForCameraTarget = mapBounds
        )

        val circleCoords = getCircleCoords(center, radius)

        // Centering the map on the playing area on launch
        LaunchedEffect(Unit) {
            state.position = CameraPosition.fromLatLngZoom(center, minZoom)
        }

        GoogleMap(
            cameraPositionState = state,
            properties = properties,
            uiSettings = uiSettings,
        ) {
            tileProvider?.let { provider ->
                TileOverlay(
                    tileProvider = provider,
                    transparency = 0.3f
                )
            }

            // Showing moving players by their avatar icon
            movingPlayers.forEach { movingPlayer ->
                val res = avatarListWithBg[movingPlayer.avatarId]
                val bitmap = BitmapFactory.decodeResource(context.resources, res)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
                Marker(
                    state = MarkerState(
                        position = LatLng(
                            movingPlayer.location.latitude,
                            movingPlayer.location.longitude
                        )
                    ),
                    icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap),
                    title = movingPlayer.nickname,
                    visible = true,
                    anchor = Offset(0.5f, 0.5f)
                )
            }

            // Showing seeker positions if the current player is using their reveal ability
            if (canSeeSeeker) {
                seekers.forEach { seeker ->
                    val res = avatarListWithBg[seeker.avatarId]
                    val bitmap = BitmapFactory.decodeResource(context.resources, res)
                    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
                    Marker(
                        state = MarkerState(
                            position = LatLng(
                                seeker.location.latitude,
                                seeker.location.longitude
                            )
                        ),
                        icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap),
                        title = seeker.nickname,
                        visible = true,
                        anchor = Offset(0.5f, 0.5f)
                    )
                }
            }

            // Darkened background around the playing area
            Polygon(
                points = getCornerCoords(center, radius),
                fillColor = Color(0x8D000000),
                holes = listOf(circleCoords),
                strokeWidth = 0f,
            )

            // Border around the playing area
            Circle(
                center = center,
                radius = radius.toDouble(),
                strokeColor = Color(0x8DBDA500),
            )
        }
    }
}

// settings for the GoogleMap
val uiSettings = MapUiSettings(
    compassEnabled = true,
    indoorLevelPickerEnabled = true,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = true,
    scrollGesturesEnabled = true,
    scrollGesturesEnabledDuringRotateOrZoom = true,
    tiltGesturesEnabled = true,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = true
)