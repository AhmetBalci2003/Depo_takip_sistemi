package com.example.depo_takip_sistemi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.depo_takip_sistemi.ui.theme.Depo_takip_sistemiTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
                if(qrCode!="")
                { teslimet(input = qrCode, context = context, scope = scope)}



            }
        }
    }
}@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun teslimet(input: String, context: Context, scope: CoroutineScope) {
    val db = FirebaseFirestore.getInstance()
    val urunCollection = db.collection("Urun")
    val urunstokcollection = db.collection("Urun_stok")
    val urunkullanimcollection = db.collection("Urun_kullanim")
    var stok_id by remember { mutableStateOf("") }

    scope.launch {
        try {
            // Ürün belgesini al
            if (input.isNotEmpty()) {
                try {
                    val docref = urunCollection.document(input)
                    val doc = docref.get().await()

                    if (doc.exists()) {
                        val veri = doc.toObject(Urun::class.java)

                        if (veri != null && veri.urun_ID != null) {
                            // Kullanım tablosunda belgeyi sorgula
                            try {
                                val query= urunkullanimcollection.whereEqualTo("urunID",veri.urun_ID.toString()).get().await()




                                val documentsToUpdate = query.documents.filter { it.getTimestamp("teslim_trh") == null }

                                if (documentsToUpdate.isNotEmpty()) {
                                    for (document in documentsToUpdate) {
                                        document.reference.update("teslim_trh", Timestamp.now()).await()
                                    }
                                    Toast.makeText(context, "Teslim tarihleri başarıyla güncellendi.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Güncellenecek belge bulunamadı.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Kullanım tablosu güncelleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                            // Ürün durumunu güncelle
                            try {
                                urunCollection.document(veri.urun_ID).update("kullanim_durumu", "depoda").await()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ürün durumu güncelleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                            // Stok bilgilerini güncelle
                            stok_id = "${veri.urun_ad}+${veri.marka}+${veri.model}"
                            val stokDocRef = urunstokcollection.document(stok_id)
                            try {
                                val docsnapshot = stokDocRef.get().await()
                                val mevcutStok = docsnapshot.toObject(Urun_stok::class.java)
                                    ?: throw Exception("Stok bilgisi alınamadı.")
                                val yenidepoAdet = (mevcutStok.depo_adet ?: 0) + 1
                                val yenikulAdet = (mevcutStok.kullanim_adet ?: 0) - 1

                                stokDocRef.update("depo_adet", yenidepoAdet).await()
                                stokDocRef.update("kullanim_adet", yenikulAdet).await()
                                Toast.makeText(context, "Stok güncellemeleri başarıyla yapıldı.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Stok güncelleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Ürün verileri geçersiz veya eksik.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Ürün bulunamadı.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ürün belgesi getirme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Geçersiz ürün ID.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Veri çekme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
