package com.example.seekers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.ui.theme.avatarBackground
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream

@Composable
fun MainQRScreen(viewModel: QRViewmodel = viewModel()) {
    val gameId = ""
    viewModel.getQR(gameId)
    val context = LocalContext.current
    val qrBitmap by viewModel.qrBitmap.observeAsState()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Scan to join!", fontSize = 20.sp, modifier = Modifier.padding(15.dp))
        qrBitmap?.let {
            QRCodeComponent(it)
        }
        Text(text = "Participants", fontSize = 20.sp, modifier = Modifier.padding(15.dp))
        Participants()
        CustomButton(text = "Start Game") {
            Toast.makeText(context, "You have started the game", Toast.LENGTH_SHORT ).show()
        }
    }
}

@Composable
fun QRCodeComponent(bitmap: Bitmap) {

    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(250.dp))
}

fun generateQRCode(gameId: String): Bitmap {
    val fileOut = ByteArrayOutputStream()

    QRCode(gameId)
        .render(cellSize = 50, margin = 25)
        .writeImage(fileOut)

    val imageBytes = fileOut.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

@Composable
fun Participants() {
    val player1 = Player("Sam", 1, "playerId1")
    val player2 = Player("Souly", 2, "playerId2")
    val player3 = Player("Mikko", 3, "playerId3")
    val player4 = Player("Miro", 4, "playerId4")
    val player5 = Player("Nam", 5, "playerId5")
    val player6 = Player("Jarkko", 6, "playerId6")
    val playerList = listOf<Player>(player1,player2,player3,player4,player5,player6)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(playerList) { player ->
            PlayerCard(player = player)
        }
    }
    
}

@Composable
fun PlayerCard(player: Player) {

    val avaratID = when(player.avatarId) {
        0 -> R.drawable.bee
        1 -> R.drawable.chameleon
        2 -> R.drawable.chick
        3 -> R.drawable.cow
        4 -> R.drawable.crab
        5 -> R.drawable.dog
        6 -> R.drawable.elephant
        7 -> R.drawable.fox
        8 -> R.drawable.koala
        9 -> R.drawable.lion
        10 -> R.drawable.penguin
        else -> R.drawable.whale


    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
        elevation = 10.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                backgroundColor = avatarBackground,
                modifier = Modifier.padding(10.dp)

            ) {
                Image(
                    painter = painterResource(id = avaratID),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(25.dp))
            Text(text = player.nickname)
        }
    }
}

class QRViewmodel(): ViewModel() {
    val storage = FirebaseStorageHelper
    val qrBitmap = MutableLiveData<Bitmap>()

    fun getQR(gameId: String) {
        storage.getQRCode(gameId).addOnSuccessListener {
        val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            qrBitmap.postValue(bitmap)
        }
    }
}