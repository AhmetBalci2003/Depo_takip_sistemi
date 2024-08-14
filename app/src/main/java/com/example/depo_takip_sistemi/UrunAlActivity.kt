package com.example.depo_takip_sistemi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.depo_takip_sistemi.ui.theme.Depo_takip_sistemiTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UrunAlActivity : ComponentActivity() {
    private lateinit var qrScanner: QR_Scanner
    private var qr_code by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        qrScanner = QR_Scanner(this)
        enableEdgeToEdge()
        setContent {
            Depo_takip_sistemiTheme {
                qrScanner.checkCameraPermission()
                qr_code = qrScanner.getcode()
                if (qr_code != "") {
                    UrunAl(input = qr_code)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrunAl(input: String) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val urunCollection = db.collection("Urun")
    val urunstokcollection = db.collection("Urun_stok")
    val urunkullanimcollection = db.collection("Urun_kullanim")
    val context = LocalContext.current
    var kullaniciAdi by remember { mutableStateOf("") }
    var kullaniciSoyadi by remember { mutableStateOf("") }
    var kullaniciSicilNo by remember { mutableStateOf("") }
    var kullaniciDept by remember { mutableStateOf("") }
    var stok_id by remember { mutableStateOf("") }

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
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFFB0BEC5) // Pastel gri
                )
            )
        },
        containerColor = Color(0xFFF0F4F8) // Soluk gri-mavi arka plan rengi
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ürün Al",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = kullaniciAdi,
                onValueChange = { kullaniciAdi = it },
                label = { Text("Adınızı giriniz") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = kullaniciSoyadi,
                onValueChange = { kullaniciSoyadi = it },
                label = { Text("Soyadınızı giriniz") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = kullaniciDept,
                onValueChange = { kullaniciDept = it },
                label = { Text("Biriminizi giriniz") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = kullaniciSicilNo,
                onValueChange = { kullaniciSicilNo = it },
                label = { Text("Sicil no giriniz") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val doc = urunCollection.document(input).get().await()
                            val veri = doc.toObject(Urun::class.java)

                            if (veri != null) {
                                if (veri.urun_ID != null) {
                                    val urunkullanim = Urun_kullanim(
                                        urunID = veri.urun_ID,
                                        kul_ad = kullaniciAdi,
                                        kul_dep = kullaniciDept,
                                        kul_soyad = kullaniciSoyadi,
                                        kul_sicil_no = kullaniciSicilNo,
                                        verilme_trh = Timestamp.now()
                                    )

                                    urunkullanimcollection.document().set(urunkullanim).await()
                                    urunCollection.document(veri.urun_ID)
                                        .update("kullanim_durumu", "kullanımda").await()
                                }
                                stok_id = "${veri.urun_ad}-${veri.marka}-${veri.model}"
                                val docref = urunstokcollection.document(stok_id)
                                val docsnapshot = docref.get().await()
                                val mevcutStok = docsnapshot.toObject(Urun_stok::class.java)
                                    ?: throw Exception("Stok bilgisi alınamadı.")
                                val yenidepoAdet = mevcutStok.depo_adet - 1
                                val yenikulAdet = mevcutStok.kullanim_adet + 1
                                docref.update("depo_adet", yenidepoAdet)
                                docref.update("kullanim_adet", yenikulAdet)

                                Toast.makeText(context, "Başarılı", Toast.LENGTH_SHORT).show()
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Ürün bulunamadı.", Toast.LENGTH_LONG).show()
                            }

                        } catch (e: Exception) {
                            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }, modifier = Modifier
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
                Text("Ürünü Al", fontSize = 18.sp)
            }
        }
    }
}
