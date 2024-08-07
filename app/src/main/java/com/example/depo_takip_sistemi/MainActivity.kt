package com.example.depo_takip_sistemi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.depo_takip_sistemi.ui.theme.Depo_takip_sistemiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Depo_takip_sistemiTheme {

                mainscren()


            }
        }
    }
}

@Composable
fun mainscren() {
    val context = LocalContext.current
    Column {
        Button(onClick = {
            var intent = Intent(context, listActivity::class.java)
            context.startActivity(intent)
        }) {
            Text(text = "listele")
        }
        Button(onClick = {
            var intent = Intent(context, AddActivity::class.java)
            context.startActivity(intent)
        }) {
            Text(text = "ekle")

        }
        Button(onClick = {
            var intent = Intent(context, UrunAlActivity::class.java)
            context.startActivity(intent)
        })
        {
            Text(text = "al")

        }
        Button(onClick = {
            var intent = Intent(context, Urun_teslimActivity::class.java)
            context.startActivity(intent)
        })
        {
            Text(text = "teslim et")

        }

    }
}


