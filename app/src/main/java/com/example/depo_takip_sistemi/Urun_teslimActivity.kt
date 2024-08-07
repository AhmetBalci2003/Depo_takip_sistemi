package com.example.depo_takip_sistemi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.depo_takip_sistemi.ui.theme.Depo_takip_sistemiTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Urun_teslimActivity : ComponentActivity() {
    private lateinit var qrScanner: QR_Scanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        qrScanner = QR_Scanner(this)
        enableEdgeToEdge()
        setContent {
            Depo_takip_sistemiTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var qrCode by remember { mutableStateOf("") }


                    qrScanner.checkCameraPermission()
                    qrCode = qrScanner.getcode()
                    if (qrCode.isNotEmpty()) {
                        teslimet(input = qrCode, context = context, scope = scope)
                    } else {
                        Toast.makeText(context, "QR kod taranamadı", Toast.LENGTH_LONG).show()
                    }

            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun teslimet(input: String, context: Context, scope: CoroutineScope) {
    val db = FirebaseFirestore.getInstance()
    val urunCollection = db.collection("Urun")
    val urunstokcollection = db.collection("Urun_stok")
    val urunkullanimcollection = db.collection("Urun_kullanim")
    var stok_id by remember { mutableStateOf("") }

    scope.launch {
        try {
            val doc = urunCollection.document(input).get().await()
            val veri = doc.toObject(Urun::class.java)
            if (veri != null && veri.urun_ID != null) {
                urunkullanimcollection.whereEqualTo("UrunID", veri.urun_ID).get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot) {
                            urunkullanimcollection.document(document.id)
                                .update("teslim_trh", Timestamp.now())
                        }
                    }
                urunCollection.document(veri.urun_ID).update("kullanim_durumu", "depoda").await()
                stok_id = "${veri.urun_ad}+${veri.marka}+${veri.model}"
                val docref = urunstokcollection.document(stok_id)
                val docsnapshot = docref.get().await()
                val mevcutStok = docsnapshot.toObject(Urun_stok::class.java)
                    ?: throw Exception("Stok bilgisi alınamadı.")
                val yenidepoAdet = (mevcutStok.depo_adet ?: 0) + 1
                val yenikulAdet = (mevcutStok.kullanim_adet ?: 0) - 1

                try {
                    docref.update("depo_adet", yenidepoAdet).await()
                    docref.update("kullanim_adet", yenikulAdet).await()
                    Toast.makeText(context, "Stok güncellemeleri başarıyla yapıldı.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Stok güncelleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Ürün bulunamadı", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Veri çekme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
