package com.kohshin.wifisettinghelper

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import java.io.File

class NextActivity : AppCompatActivity(){
    val appDir = File(Environment.getExternalStorageDirectory(), "WiFiSettingHelper")
    val filename = "wifi_binary.png"

    lateinit var imageViewWifi: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)

        imageViewWifi = findViewById(R.id.image_view)

        val loadfile = File(appDir, filename)
        val bitmap = BitmapFactory.decodeFile(loadfile.toString())
        imageViewWifi.setImageBitmap(bitmap)

        //受け取った変数を入れる
        val text = intent.getStringExtra("result")

    }


}