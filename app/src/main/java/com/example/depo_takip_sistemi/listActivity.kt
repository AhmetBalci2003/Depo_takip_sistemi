package com.example.depo_takip_sistemi

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.depo_takip_sistemi.ui.theme.Depo_takip_sistemiTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class listActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Depo_takip_sistemiTheme {
                Scaffold {
                    ListScreen()
                }






            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen() {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val urunCollection = db.collection("Urun")
    val urunStokCollection = db.collection("Urun_stok")
    val urunKullanimCollection = db.collection("Urun_kullanim")

    var urunler by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var stokBilgileri by remember { mutableStateOf<List<Urun_stok>>(emptyList()) }
    var kullaniciBilgileri by remember { mutableStateOf<List<Urun_kullanim>>(emptyList()) }
    var selectedStok by remember { mutableStateOf<Urun_stok?>(null) }
    var isDataLoaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    Scaffold { innerPadding->
        LaunchedEffect(Unit) {
            scope.launch {
                try {
                    val urunSnapshot = urunCollection.get().await()
                    val urunList = urunSnapshot.documents.mapNotNull { it.toObject(Urun::class.java) }

                    val stokSnapshot = urunStokCollection.get().await()
                    val stokList = stokSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Urun_stok::class.java)
                    }

                    val kullanimSnapshot = urunKullanimCollection.get().await()
                    val kullanimlist = kullanimSnapshot.documents.mapNotNull { it.toObject(Urun_kullanim::class.java) }

                    urunler = urunList
                    stokBilgileri = stokList
                    kullaniciBilgileri = kullanimlist
                    isDataLoaded = true
                } catch (e: Exception) {
                    errorMessage = e.message
                    isDataLoaded = true
                }
            }
        }

        if (!isDataLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Hata: $errorMessage", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            if (selectedStok != null) {
                val urunBilgi = urunler.find { it.urun_ad == selectedStok!!.Urun_ad && it.marka == selectedStok!!.marka && it.model == selectedStok!!.model }
                DetailScreen(
                    stokBilgi = selectedStok!!,
                    urunBilgi = urunBilgi,
                    urunler = urunler,
                    kullaniciBilgileri = kullaniciBilgileri,
                    onBack = { selectedStok = null }
                )
            } else {
                LazyColumn(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                ) {
                    items(stokBilgileri) { stokBilgi ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { selectedStok = stokBilgi },
                            colors = CardDefaults.cardColors(containerColor = Color.White), // Beyaz arka plan rengi
                            elevation = CardDefaults.cardElevation(8.dp) // Hafif gölge efekti
                        ) {
                            Row(modifier = Modifier.padding(16.dp)) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text =  "${stokBilgi.Urun_ad}", style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Depo Adeti: ${stokBilgi.depo_adet}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Kullanım  Adeti: ${stokBilgi.kullanim_adet}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(

                                ) {
                                    Text(text = "${stokBilgi.marka } -", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "${stokBilgi.model}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

    }


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    stokBilgi: Urun_stok,
    urunBilgi: Urun?,
    urunler: List<Urun>,
    kullaniciBilgileri: List<Urun_kullanim>,
    onBack: () -> Unit
) {
    val marka = stokBilgi.marka
    val model = stokBilgi.model

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Ürün Detayı")
                },
                navigationIcon = {
                    IconButton(onClick = { onBack()}) {

                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription ="geri" )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFFB0BEC5)) // Pastel gri
            )
        },
        containerColor = Color(0xFFF0F4F8) // Soluk gri-mavi arka plan rengi
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(urunler.filter { it.marka == marka && it.model == model }) { urun ->
                    val kullaniciBilgiList = kullaniciBilgileri.filter { it.urunID == urun.urun_ID && it.teslim_trh == null }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White), // Beyaz arka plan rengi
                        elevation = CardDefaults.cardElevation(8.dp) // Hafif gölge efekti
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Ürün Adı: ${urun.urun_ad}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Marka   : ${urun.marka}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Model   : ${urun.model}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Raf       : ${urun.raf}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Sap no  : ${urun.sap_no}", style = MaterialTheme.typography.bodyMedium)

                            // Kullanıcı bilgilerini listele
                            if (kullaniciBilgiList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                kullaniciBilgiList.forEach { kullaniciBilgi ->
                                    Text(text = "Kullanıcı Adı     : ${kullaniciBilgi.kul_ad}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Kullanıcı Soyadı  : ${kullaniciBilgi.kul_soyad}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Kullanıcı Sicil No: ${kullaniciBilgi.kul_sicil_no}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Birimi            : ${kullaniciBilgi.kul_dep}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Verilme Tarihi    : ${kullaniciBilgi.verilme_trh?.toDate()}", style = MaterialTheme.typography.bodyMedium)

                                    Spacer(modifier = Modifier.height(8.dp))
                                }
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
