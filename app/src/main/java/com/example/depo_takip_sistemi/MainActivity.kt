package com.example.depo_takip_sistemi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.depo_takip_sistemi.ui.theme.Depo_takip_sistemiTheme
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Depo_takip_sistemiTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Icon(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ana Ekran", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFFB0BEC5)) // Pastel gri
            )
        },
        containerColor = Color(0xFFF0F4F8) // Soluk gri-mavi arka plan rengi
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Butonlar arasında daha fazla boşluk
        ) {
            Text(
                text = "Depo Takip Sistemi",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF37474F) // Koyu gri renk
            )
            Text(
                text = "Seçim yaparak devam edebilirsiniz:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF616161) // Açık gri renk
            )
            NavigationButton("Listele", context, listActivity::class.java)
            NavigationButton("Ekle", context, AddActivity::class.java)
            NavigationButton("Al", context, UrunAlActivity::class.java)
            NavigationButton("Teslim Et", context, Urun_teslimActivity::class.java)
        }
    }
}

@Composable
fun NavigationButton(text: String, context: Context, activityClass: Class<*>) {
    Button(
        onClick = {
            val intent = Intent(context, activityClass)
            context.startActivity(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp), // Orta yuvarlak köşeler
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFC0CED9), // Soluk pastel mavi
            contentColor = Color.Black // Metin rengi
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 4.dp, // Normal durumda daha belirgin gölge
            pressedElevation = 8.dp // Basılıyken daha belirgin gölge
        )
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
