package com.example.depo_takip_sistemi

data class Urun_stok(
    var stok_ID: String = "",
    var Urun_ad: String = "",
    var marka: String = "",
    var model: String = "",
    var depo_adet: Int = 0,
    var kullanim_adet: Int = 0,
)
