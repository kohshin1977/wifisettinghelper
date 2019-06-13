package com.kohshin.wifisettinghelper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import java.nio.file.Files.exists
import android.content.Intent
import com.googlecode.tesseract.android.TessBaseAPI
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.view.View
import android.widget.Button
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream


class SettingActivity : AppCompatActivity(){
    val DEFAULT_LANGUAGE = "eng"

    lateinit var filepath: String
    lateinit var bitmap: Bitmap
    lateinit var tessBaseAPI: TessBaseAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        bitmap = BitmapFactory.decodeResource(resources, R.drawable.image2)

        filepath = filesDir.toString() + "/tesseract/"

        tessBaseAPI = TessBaseAPI()

        checkFile(File(filepath + "tessdata/"))

        tessBaseAPI.init(filepath, DEFAULT_LANGUAGE)

        val button = findViewById<View>(R.id.button) as Button
        button.setOnClickListener {
            tessBaseAPI.setImage(bitmap)
            val result = tessBaseAPI.utF8Text
            val intent = Intent(this@SettingActivity, NextActivity::class.java)
            intent.putExtra("result", result)
            startActivity(intent)
        }

    }

    private fun checkFile(file: File) {
        if (!file.exists() && file.mkdirs()) {
            copyFiles()
        }
        if (file.exists()) {
            val datafilepath = "$filepath" + "tessdata/eng.traineddata"
            val datafile = File(datafilepath)

            if (!datafile.exists()) {
                copyFiles()
            }
        }
    }

    private fun copyFiles() {
        try {
            val datapath = "$filepath" + "tessdata/eng.traineddata"
            val instream = assets.open("tessdata/eng.traineddata")
            val outstream = FileOutputStream(datapath)

            val buffer = ByteArray(1024)
            var read: Int
            do{
                read = instream.read(buffer)
                if(read == -1)
                    break
                outstream.write(buffer, 0, read)
            }while(true)
//            while ((read = instream.read(buffer)) != -1) {
//                outstream.write(buffer, 0, read)
//            }

            outstream.flush()
            outstream.close()
            instream.close()

            val file = File(datapath)
            if (!file.exists()) {
                throw FileNotFoundException()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

}