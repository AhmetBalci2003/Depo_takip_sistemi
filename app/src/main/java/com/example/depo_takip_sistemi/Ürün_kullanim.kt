package com.example.depo_takip_sistemi

import com.google.firebase.Timestamp

data class Urun_kullanim(
    var UrunID:String="",
    var kul_ad:String="",
    var kul_soyad:String="",
    var kul_dep:String="",
    var kul_sicil_no:String="",
    var verilme_trh:Timestamp?=null,
    var teslim_trh:Timestamp?=null
)
