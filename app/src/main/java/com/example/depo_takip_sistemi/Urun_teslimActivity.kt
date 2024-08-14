package com.example.depo_takip_sistemi

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("CoroutineCreationDuringComposition", "SuspiciousIndentation")
@Composable
fun teslimet(input: String, context: Context, scope: CoroutineScope) {
    val db = FirebaseFirestore.getInstance()
    val urunCollection = db.collection("Urun")
    val urunstokcollection = db.collection("Urun_stok")
    val urunkullanimcollection = db.collection("Urun_kullanim")

    var stok_id by remember { mutableStateOf("") }
    var rafBilgisi by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var urunBilgileri by remember { mutableStateOf<Urun?>(null) }
    var showRafInput by remember { mutableStateOf(true) }

    if (showRafInput) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row {
                            Icon(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Teslim Et", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFFB0BEC5)) // Pastel gri
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)) {
                Text("Raf giriniz")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rafBilgisi,
                    onValueChange = { rafBilgisi = it },
                    label = { Text("Raf Bilgisi") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                try {
                                    val doc = urunCollection.document(input).get().await()
                                    val veri = doc.toObject(Urun::class.java)
                                    if (veri != null && veri.urun_ID != null) {
                                        val querySnapshot = urunkullanimcollection.whereEqualTo("urunID", veri.urun_ID).get().await()
                                        for (document in querySnapshot.documents) {
                                            urunkullanimcollection.document(document.id)
                                                .update("teslim_trh", Timestamp.now())
                                                .await()
                                        }
                                        urunCollection.document(veri.urun_ID).update("kullanim_durumu", "depoda").await()
                                        urunCollection.document(veri.urun_ID).update("raf", rafBilgisi).await()
                                        stok_id = "${veri.urun_ad}-${veri.marka}-${veri.model}"
                                        val docref = urunstokcollection.document(stok_id)
                                        val docsnapshot = docref.get().await()
                                        val mevcutStok = docsnapshot.toObject(Urun_stok::class.java)
                                            ?: throw Exception("Stok bilgisi alınamadı.")
                                        val yenidepoAdet = (mevcutStok.depo_adet ?: 0) + 1
                                        val yenikulAdet = (mevcutStok.kullanim_adet ?: 0) - 1
                                        docref.update("depo_adet", yenidepoAdet).await()
                                        docref.update("kullanim_adet", yenikulAdet).await()
                                        docref.update("raf_bilgisi", rafBilgisi).await() // Raf bilgisini güncelle

                                        // Başarı durumunda ürün bilgilerini tekrar al
                                        urunBilgileri = urunCollection.document(veri.urun_ID).get().await().toObject(Urun::class.java)
                                        showRafInput = false
                                        showAlertDialog = true
                                    } else {
                                        Toast.makeText(context, "Ürün bulunamadı", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC0CED9),
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Güncelle")
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // İptal butonuna basıldığında ana menüye dön
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                        }, modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC0CED9),
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text("İptal")
                    }
                }
            }
        }
    }

    // Başarı mesajı ve ürün bilgilerini içeren alert dialog
    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = { Text("İşlem Başarılı") },
            text = {
                urunBilgileri?.let {
                    Column {
                        Text("İşlem başarılı")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ürün Adı       : ${it.urun_ad}")
                        Text("Marka          : ${it.marka}")
                        Text("Model          : ${it.model}")
                        Text("Sap No         : ${it.sap_no}")
                        Text("Raf            : ${it.raf}")
                        Text("Kullanım durumu: ${it.kullanim_durumu}")
                    }
                } ?: Text("Ürün bilgileri bulunamadı.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAlertDialog = false
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC0CED9),
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text("Tamam")
                }
            }
        )
    }
}
