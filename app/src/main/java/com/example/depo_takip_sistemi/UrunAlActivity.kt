package com.example.depo_takip_sistemi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.depo_takip_sistemi.ui.theme.Depo_takip_sistemiTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UrunAlActivity : ComponentActivity() {
    lateinit var qrScanner: QR_Scanner
    var qr_code by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        qrScanner = QR_Scanner(this)
        enableEdgeToEdge()
        setContent {
            Depo_takip_sistemiTheme {
                qrScanner.checkCameraPermission()
                qr_code = qrScanner.getcode()
                UrunAl(input = qr_code)
            }
        }
    }
}

@Composable
fun UrunAl(input: String) {
    println(input)
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
    var gelenÜrün by remember { mutableStateOf<Urun?>(null) }
    var stok_id by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(value = kullaniciAdi,
            onValueChange = { kullaniciAdi = it },
            label = { Text(text = "Adınızı giriniz") })
        OutlinedTextField(value = kullaniciSoyadi,
            onValueChange = { kullaniciSoyadi = it },
            label = { Text(text = "Soyadınızı giriniz") })
        OutlinedTextField(value = kullaniciDept,
            onValueChange = { kullaniciDept = it },
            label = { Text(text = "Biriminizi giriniz") })
        OutlinedTextField(value = kullaniciSicilNo,
            onValueChange = { kullaniciSicilNo = it },
            label = { Text(text = "Sicil no giriniz") })
        Button(onClick = {
            scope.launch {
                try {
                    val doc = urunCollection.document(input).get().await()
                    val veri = doc.toObject(Urun::class.java)

                    if (veri != null) {
                        if (veri.urun_ID != null) {
                            val urunkullanim = Urun_kullanim(
                                UrunID = veri.urun_ID,
                                kul_ad = kullaniciAdi,
                                kul_dep = kullaniciDept,
                                kul_soyad = kullaniciSoyadi,
                                kul_sicil_no = kullaniciSicilNo,
                                verilme_trh = Timestamp.now(),
                                teslim_trh = null
                            )

                            urunkullanimcollection.add(urunkullanim).await()
                            urunCollection.document(veri.urun_ID)
                                .update("kullanim_durumu", "kullanımda").await()
                        }
                        stok_id = "${veri.urun_ad}+${veri.marka}+${veri.model}"
                        val docref = urunstokcollection.document(stok_id)
                        val docsnapshot = docref.get().await()
                        val mevcutStok = docsnapshot.toObject(Urun_stok::class.java)
                            ?: throw Exception("Stok bilgisi alınamadı.")
                        val yenidepoAdet = (mevcutStok.depo_adet ?: 0) - 1
                        val yenikulAdet = (mevcutStok.kullanim_adet ?: 0) + 1
                        docref.update("depo_adet", yenidepoAdet)
                        docref.update("kullanim_adet", yenikulAdet)

                    } else {
                    Toast.makeText(context,"Ürün bulunamadı.",Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(context, "Hata:${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }) {
            Text(text = "Ürünü Al")
        }
    }
}
