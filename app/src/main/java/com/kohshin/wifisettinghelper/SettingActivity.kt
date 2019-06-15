package com.kohshin.wifisettinghelper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import java.nio.file.Files.exists
import android.content.Intent
import com.googlecode.tesseract.android.TessBaseAPI
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.view.View
import android.widget.Button
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import android.widget.ImageView
import java.io.*
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_WRITE


class SettingActivity : AppCompatActivity(){
    val DEFAULT_LANGUAGE = "eng"

    lateinit var filepath: String
    lateinit var bitmap: Bitmap
    lateinit var tessBaseAPI: TessBaseAPI

    val appDir = File(Environment.getExternalStorageDirectory(), "WiFiSettingHelper")
    val filename = "wifi.jpg"

    /**
     * Boolean that tells me how to treat a transparent pixel (Should it be black?)
     */
    private val TRASNPARENT_IS_BLACK = false
    /**
     * This is a point that will break the space into Black or white
     * In real words, if the distance between WHITE and BLACK is D;
     * then we should be this percent far from WHITE to be in the black region.
     * Example: If this value is 0.5, the space is equally split.
     */
    private val SPACE_BREAKING_POINT = 20.0 / 30.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val loadfile = File(appDir, filename)
        bitmap = BitmapFactory.decodeFile(loadfile.toString())
//        bitmap = BitmapFactory.decodeResource(resources, R.drawable.image10)

        //２値化
        bitmap = processBinary(bitmap)

        filepath = filesDir.toString() + "/tesseract/"

        tessBaseAPI = TessBaseAPI()

        checkFile(File(filepath + "tessdata/"))

        tessBaseAPI.init(filepath, DEFAULT_LANGUAGE)

        val imageView = findViewById(R.id.image_view) as ImageView
        imageView.setImageBitmap(bitmap)

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

    private fun processBinary(bitmap: Bitmap) :Bitmap{
        //this is the image that is binarized
        val binarizedImage = convertToMutable(bitmap)
        // I will look at each pixel and use the function shouldBeBlack to decide
        // whether to make it black or otherwise white
        for (i in 0 until binarizedImage.width) {
            for (c in 0 until binarizedImage.height) {
                val pixel = binarizedImage.getPixel(i, c)
                if (shouldBeBlack(pixel))
                    binarizedImage.setPixel(i, c, Color.BLACK)
                else
                    binarizedImage.setPixel(i, c, Color.WHITE)
            }
        }

        return binarizedImage
    }


    /**
     * @param pixel the pixel that we need to decide on
     * @return boolean indicating whether this pixel should be black
     */
    private fun shouldBeBlack(pixel: Int): Boolean {
        val alpha = Color.alpha(pixel)
        val redValue = Color.red(pixel)
        val blueValue = Color.blue(pixel)
        val greenValue = Color.green(pixel)
        if (alpha == 0x00)
        //if this pixel is transparent let me use TRASNPARENT_IS_BLACK
            return TRASNPARENT_IS_BLACK
        // distance from the white extreme
        val distanceFromWhite = Math.sqrt(
            Math.pow((0xff - redValue).toDouble(), 2.0) + Math.pow(
                (0xff - blueValue).toDouble(),
                2.0
            ) + Math.pow((0xff - greenValue).toDouble(), 2.0)
        )
        // distance from the black extreme //this should not be computed and might be as well a function of distanceFromWhite and the whole distance
        val distanceFromBlack = Math.sqrt(
            Math.pow((0x00 - redValue).toDouble(), 2.0) + Math.pow(
                (0x00 - blueValue).toDouble(),
                2.0
            ) + Math.pow((0x00 - greenValue).toDouble(), 2.0)
        )
        // distance between the extremes //this is a constant that should not be computed :p
        val distance = distanceFromBlack + distanceFromWhite
        // distance between the extremes
        return distanceFromWhite / distance > SPACE_BREAKING_POINT
    }

    /**
     * @author Derzu
     *
     * @see https://stackoverflow.com/a/9194259/833622
     *
     * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
     * more memory that there is already allocated.
     *
     *
     * @param imgIn - Source image. It will be released, and should not be used more
     * @return a copy of imgIn, but muttable.
     */
    fun convertToMutable(imgIn: Bitmap): Bitmap {
        var imgIn = imgIn
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            val file = File(Environment.getExternalStorageDirectory().toString() + File.separator + "temp.tmp")

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            val randomAccessFile = RandomAccessFile(file, "rw")

            // get the width and height of the source bitmap.
            val width = imgIn.width
            val height = imgIn.height
            val type = imgIn.config

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            val channel = randomAccessFile.getChannel()
            val map = channel.map(FileChannel.MapMode.READ_WRITE, 0, (imgIn.rowBytes * height).toLong())
            imgIn.copyPixelsToBuffer(map)
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle()
            System.gc()// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type)
            map.position(0)
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map)
            //close the temporary file and channel , then delete that also
            channel.close()
            randomAccessFile.close()

            // delete the temp file
            file.delete()

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return imgIn
    }

}
