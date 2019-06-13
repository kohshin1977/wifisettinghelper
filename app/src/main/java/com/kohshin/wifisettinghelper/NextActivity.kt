package com.kohshin.wifisettinghelper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

class NextActivity : AppCompatActivity(){
    lateinit var textView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)

        textView = findViewById(R.id.textView)

        //受け取った変数を入れる
        val text = intent.getStringExtra("result")

        textView.setText(text)
    }


}