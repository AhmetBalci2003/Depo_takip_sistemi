package com.example.depo_takip_sistemi

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.util.UUID

class AddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Depo_takip_sistemiTheme {
                // Ekranı ortalamak için padding ekleyelim
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    AddScreen(context)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(context: Context) {
    val scope = rememberCoroutineScope()
    var _ad by remember { mutableStateOf("") }
    var _Raf by remember { mutableStateOf("") }
    var _urunSapNo by remember { mutableStateOf("") }
    var _marka by remember { mutableStateOf("") }
    var _model by remember { mutableStateOf("") }
    var _bitmap: Bitmap? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Icon(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ürün Ekle", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFFB0BEC5)
                )
            )
        },
        containerColor = Color(0xFFF0F4F8) // Soluk gri-mavi arka plan rengi
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = _ad,
                onValueChange = { _ad = it },
                label = { Text("Ürün ismi") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = _marka,
                onValueChange = { _marka = it },
                label = { Text("Marka") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = _model,
                onValueChange = { _model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = _Raf,
                onValueChange = { _Raf = it },
                label = { Text("Raf") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = _urunSapNo,
                onValueChange = { _urunSapNo = it },
                label = { Text("Ürün SAP No") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val ürünId = UUID.randomUUID().toString()
                        val ürün = Urun(
                            urun_ad = _ad,
                            urun_ID = ürünId,
                            eklenme_tarihi = Timestamp.now(),
                            kullanim_durumu = "depoda",
                            marka = _marka,
                            model = _model,
                            raf = _Raf,
                            sap_no = _urunSapNo
                        )
                        try {
                            urunnEkle(ürün, context)
                            _bitmap = Qr_generate().generateQRCode(ürünId)
                            QR_print().printBitmap(context, _bitmap!!)

                            stokGuncellemeVeyaEkleme(
                                ürün_adı = _ad, marka = _marka, model = _model, eklemeAdeti = 1, context
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ürün eklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
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
                Text("Ekle", fontSize = 18.sp, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

suspend fun stokGuncellemeVeyaEkleme(
    ürün_adı: String,
    marka: String,
    model: String,
    eklemeAdeti: Int,
    context: Context
) {
    val db = FirebaseFirestore.getInstance()
    val ürünStokCollection = db.collection("Urun_stok")
    val ürünId = "$ürün_adı-$marka-$model"

    try {
        val docRef = ürünStokCollection.document(ürünId)
        val docSnapshot = docRef.get().await()
        if (docSnapshot.exists()) {
            val mevcutStok = docSnapshot.toObject(Urun_stok::class.java)
            val yeniAdet = (mevcutStok?.depo_adet ?: 0) + eklemeAdeti
            docRef.update("depo_adet", yeniAdet).addOnSuccessListener {
                Toast.makeText(context, "Stok durumu başarıyla güncellendi.", Toast.LENGTH_LONG).show()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Stok güncelleme sırasında hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            val yeniÜrün = Urun_stok(
                stok_ID = ürünId,
                Urun_ad = ürün_adı,
                marka = marka,
                model = model,
                depo_adet = eklemeAdeti
            )
            docRef.set(yeniÜrün).addOnSuccessListener {
                Toast.makeText(context, "Yeni stok başarıyla eklendi.", Toast.LENGTH_LONG).show()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Stok ekleme sırasında hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Stok güncelleme sırasında hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun urunnEkle(urun: Urun, context: Context) {
    val db = FirebaseFirestore.getInstance()
    val ürünCollection = db.collection("Urun")
    try {
        urun.urun_ID?.let {
            ürünCollection.document(it).set(urun).addOnSuccessListener {
                Toast.makeText(context, "Ürün başarıyla eklendi.", Toast.LENGTH_LONG).show()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Ürün ekleme sırasında hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Ürün ekleme sırasında hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
