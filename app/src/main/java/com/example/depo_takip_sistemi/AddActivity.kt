package com.example.depo_takip_sistemi

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                val context = LocalContext.current
                AddScreen(context)

            }
        }
    }
}


@Composable
fun AddScreen(context: Context) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var _ad by remember { mutableStateOf("") }
    var _Raf by remember { mutableStateOf("") }
    var _urunSapNo by remember { mutableStateOf("") }
    var _marka by remember { mutableStateOf("") }
    var _bitmap: Bitmap? by remember { mutableStateOf(null) }
    var _model by remember { mutableStateOf("") }



    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = _ad,
            onValueChange = { _ad = it },
            label = { Text("İsim") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = _marka,
            onValueChange = { _marka = it },
            label = { Text("marka") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = _model,
            onValueChange = { _model = it },
            label = { Text("model") },
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            try {
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
                        sap_no = _urunSapNo,


                        )

                    urunnEkle(ürün, context)
                    _bitmap = Qr_generate().generateQRCode(ürünId)
                    QR_print().printBitmap(context, _bitmap!!)

                    stokGuncellemeVeyaEkleme(
                        ürün_adı = _ad, marka = _marka, model = _model, eklemeAdeti = 1, context
                    )


                }


            } catch (e: Exception) {
                Toast.makeText(context, "ürün eklenemedi:${e.message}", Toast.LENGTH_LONG).show()

            }

        }) {
            Text(" Ekle", fontSize = 20.sp)
        }
    }
}

suspend fun stokGuncellemeVeyaEkleme(
    ürün_adı: String,
    marka: String,
    model: String,
    eklemeAdeti: Int,
    context: Context,
) {
    val db = FirebaseFirestore.getInstance()
    val ürünStokCollection = db.collection("Urun_stok")
    val ürünId = "$ürün_adı+$marka+$model"

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
                Toast.makeText(context, "Yeni ürün başarıyla eklendi.", Toast.LENGTH_LONG).show()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Yeni ürün ekleme sırasında hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Stok güncelleme sırasında hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun urunnEkle(urun: Urun, context: Context) {
    val db = FirebaseFirestore.getInstance()
    // "Ürün" koleksiyonuna referans al
    val ürünCollection = db.collection("Urun")
    // Ürünü Firestore'a ekle
    try {
        // Ürünü "Ürün" koleksiyonuna ekleyin
        urun.urun_ID?.let {
            ürünCollection.document(it).set(urun)
        }
    } catch (e: Exception) {
        Toast.makeText(
            context, "Ürün ekleme sırasında hata oluştu: ${e.message}", Toast.LENGTH_LONG
        ).show()
    }
}



