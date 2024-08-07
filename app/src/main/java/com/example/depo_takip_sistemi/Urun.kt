package com.example.depo_takip_sistemi

import com.google.firebase.Timestamp

data class Urun(
    val urun_ad: String? = null,
    val urun_ID: String? = null,
    var raf: String? = null,
    var sap_no: String? = null,
    var eklenme_tarihi: Timestamp? = null,
    var marka: String? = null,
    var model: String? = null,
    var kullanim_durumu: String? = null,
)
