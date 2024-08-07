package com.example.depo_takip_sistemi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.depo_takip_sistemi.ui.theme.Depo_takip_sistemiTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class listActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Depo_takip_sistemiTheme {
                ListScreen()




            }
        }
    }
}

@Composable
fun ListScreen() {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val urunCollection = db.collection("Urun")
    val urunStokCollection = db.collection("Urun_stok")
    val urunKullanimCollection = db.collection("Urun_kullanim")

    var urunler by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var stokBilgileri by remember { mutableStateOf<Map<String, Urun_stok>>(emptyMap()) }
    var kullanimBilgileri by remember { mutableStateOf<Map<String, Urun_kullanim>>(emptyMap()) }
    var isDataLoaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Ürünleri çek
                val urunSnapshot = urunCollection.get().await()
                val urunList = urunSnapshot.documents.mapNotNull { it.toObject(Urun::class.java) }

                // Stok bilgilerini çek
                val stokSnapshot = urunStokCollection.get().await()
                val stokMap = stokSnapshot.documents.mapNotNull { doc ->
                    val stok = doc.toObject(Urun_stok::class.java)
                    stok?.let { it.stok_ID to it }
                }.toMap()

                // Kullanıcı bilgilerini çek
                val kullanimSnapshot = urunKullanimCollection.get().await()
                val kullanimMap = kullanimSnapshot.documents.mapNotNull { doc ->
                    val kullanim = doc.toObject(Urun_kullanim::class.java)
                    kullanim?.let { it.UrunID to it }
                }.toMap()

                urunler = urunList
                stokBilgileri = stokMap
                kullanimBilgileri = kullanimMap
                isDataLoaded = true
            } catch (e: Exception) {
                errorMessage = e.message
                isDataLoaded = true
            }
        }
    }

    if (!isDataLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Hata: $errorMessage")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(urunler) { urun ->
                val stokBilgi=stokBilgileri["${urun.urun_ad}+${urun.marka}+${urun.model}"]
                val kullanimBilgi = kullanimBilgileri[urun.urun_ID]
                var expanded by remember { mutableStateOf(false) }


                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { expanded = !expanded }


                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Kısa özet
                        Text(text = "Ürün Adı: ${urun.urun_ad}", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Marka: ${urun.marka}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Model: ${urun.model}", style = MaterialTheme.typography.bodyMedium)

                        if (expanded) {
                            // Genişletilmiş kartın içeriği
                            Spacer(modifier = Modifier.height(8.dp))
                            if (stokBilgi != null) {
                                Text(text = "Stok Adeti: ${stokBilgi.depo_adet}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Kullanım Adeti: ${stokBilgi.kullanim_adet}", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(text = "Stok bilgisi bulunamadı", style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            if (kullanimBilgi != null) {
                                Text(text = "Kullanıcı Adı: ${kullanimBilgi.kul_ad}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Kullanıcı Soyadı: ${kullanimBilgi.kul_soyad}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Kullanıcı Sicil No: ${kullanimBilgi.kul_sicil_no}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Birimi: ${kullanimBilgi.kul_dep}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Verilme Tarihi: ${kullanimBilgi.verilme_trh?.toDate()}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Teslim Tarihi: ${kullanimBilgi.teslim_trh?.toDate()}", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(text = "Kullanıcı bilgisi bulunamadı", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

