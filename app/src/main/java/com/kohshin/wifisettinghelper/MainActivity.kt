package com.kohshin.wifisettinghelper


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.*
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

class MainActivity : AppCompatActivity() {
    val PERMISION_CAMERA        = 200
    val PERMISION_WRITE_STORAGE = 1000
    val PERMISION_READ_STORAGE  = 1001

    /**
     * 各レイアウトオブジェクト変数を生成
     */
    private lateinit var shutterButton : ImageButton
    private lateinit var previewView   : TextureView
    private lateinit var imageReader   : ImageReader
    private lateinit var sizeBitmap : EditText

    /**
     * 各種変数初期化
     */
    private lateinit var previewRequestBuilder : CaptureRequest.Builder
    private lateinit var previewRequest        : CaptureRequest
    private var backgroundHandler              : Handler?                = null
    private var backgroundThread               : HandlerThread?          = null
    private var cameraDevice                   : CameraDevice?           = null
    private lateinit var captureSession        : CameraCaptureSession
    private lateinit var progressBar: ProgressBar

        val DEFAULT_LANGUAGE = "eng"

    lateinit var filepath: String
    lateinit var tessBaseAPI: TessBaseAPI

    val appDir = File(Environment.getExternalStorageDirectory(), "WiFiSettingHelper")
    val filename = "wifi.png"

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
        setContentView(R.layout.activity_main)


        if(!OpenCVLoader.initDebug()){
            Log.i("OpenCV", "Failed");
        }else{
            Log.i("OpenCV", "successfully built !");
        }

        progressBar = findViewById(R.id.progressbar)
        shutterButton = findViewById(R.id.Shutter);

        Thread( object: Runnable {
            override fun run() {
                //読み込み処理
                filepath = filesDir.toString() + "/tesseract/"

                tessBaseAPI = TessBaseAPI()

                checkFile(File(filepath + "tessdata/"))

                shutterButton.visibility = View.VISIBLE

                println( "hello" )
            }
        }).start()

        previewView = findViewById(R.id.mySurfaceView)
        previewView.surfaceTextureListener = surfaceTextureListener
        startBackgroundThread()

        sizeBitmap = findViewById(R.id.editText)

        /**
         * シャッターボタンにイベント生成
         */
        shutterButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE

            // フォルダーを使用する場合、あるかを確認
            if (!appDir.exists()) {
                // なければ、フォルダーを作る
                appDir.mkdirs()
            }

            try {
                var savefile : File? = null

                /**
                 * プレビューの更新を止める
                 */
                captureSession.stopRepeating()
                if (previewView.isAvailable) {

                    savefile = File(appDir, filename)
                    val fos = FileOutputStream(savefile)
                    val bitmap: Bitmap = previewView.bitmap
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    getResizedBitmap(bitmap, Integer.parseInt(sizeBitmap.text.toString())).compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.close()
                }

                if (savefile != null) {
                    Log.d("edulog", "Image Saved On: $savefile")
//                    Toast.makeText(this, "Saved: $savefile", Toast.LENGTH_SHORT).show()
                }

               //文字読み込み処理
                val result = readCharacter()


                //ここで次のインテントに移動
                val intent = Intent(this@MainActivity, NextActivity::class.java)
                intent.putExtra("result", result)
                startActivity(intent)
//                val intent = Intent(this, SettingActivity::class.java)
//                startActivity(intent)

            } catch (e: CameraAccessException) {
                Log.d("edulog", "CameraAccessException_Error: $e")
            } catch (e: FileNotFoundException) {
                Log.d("edulog", "FileNotFoundException_Error: $e")
            } catch (e: IOException) {
                Log.d("edulog", "IOException_Error: $e")
            }

            /**
             * プレビューを再開
             */
            captureSession.setRepeatingRequest(previewRequest, null, null)
        }


    }

    override fun onResume(){
        super.onResume()
        progressBar.visibility = View.INVISIBLE
    }

    private fun readCharacter(): String{
        val loadfile = File(appDir, filename)
        var bitmap = BitmapFactory.decodeFile(loadfile.toString())

        //２値化処理
//        bitmap = processBinary(bitmap)
        bitmap = grayScale(bitmap)

        val savefile = File(appDir, "wifi_binary.png")
        val fos = FileOutputStream(savefile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()


        tessBaseAPI.init(filepath, DEFAULT_LANGUAGE)

        tessBaseAPI.setImage(bitmap)
        val result = tessBaseAPI.utF8Text
        return result


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


    /**
     * カメラをバックグラウンドで実行
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    /**
     * TextureView Listener
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener
    {
        // TextureViewが有効になった
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int)
        {
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG,2)
            openCamera()
        }

        // TextureViewのサイズが変わった
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) { }

        // TextureViewが更新された
        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) { }

        // TextureViewが破棄された
        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean
        {
            return false
        }
    }

    /**
     * カメラ起動処理関数
     */
    private fun openCamera() {
        /**
         * カメラマネジャーの取得
         */
        val manager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            /**
             * カメラIDの取得
             */
            val camerId: String = manager.cameraIdList[0]

            /**
             * カメラ起動パーミッションの確認
             */
            val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission()
                return
            }

            /**
             * カメラ起動
             */
            manager.openCamera(camerId, stateCallback, null)


        /**
         * ストレージ読み書きパーミッションの確認
         */
        val writePermission  = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission  = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        if ( (writePermission != PackageManager.PERMISSION_GRANTED) || (readPermission != PackageManager.PERMISSION_GRANTED) ) {
            requestStoragePermission()
        }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * カメラ利用許可取得ダイアログを表示
     */
    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder(baseContext)
                .setMessage("Permission Check")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISION_CAMERA)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .show()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISION_CAMERA)
        }
    }

    /**
     * カメラ状態取得コールバック関数
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        /**
         * カメラ接続完了
         */
        override fun onOpened(cameraDevice: CameraDevice) {
            this@MainActivity.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        /**
         * カメラ切断
         */
        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
            this@MainActivity.cameraDevice = null
        }

        /**
         * カメラエラー
         */
        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
            finish()
        }
    }

    /**
     * カメラ画像生成許可取得ダイアログを表示
     */
    private fun createCameraPreviewSession()
    {
        try
        {
            val texture = previewView.surfaceTexture
            texture.setDefaultBufferSize(previewView.width, previewView.height)

            val surface = Surface(texture)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(Arrays.asList(surface, imageReader.surface),
                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                object : CameraCaptureSession.StateCallback()
                {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession)
                    {
                        if (cameraDevice == null) return
                        try
                        {
                            captureSession = cameraCaptureSession
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            previewRequest = previewRequestBuilder.build()
                            cameraCaptureSession.setRepeatingRequest(previewRequest, null, Handler(backgroundThread?.looper))
                        } catch (e: CameraAccessException) {
                            Log.e("erfs", e.toString())
                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        //Tools.makeToast(baseContext, "Failed")
                    }
                }, null)
        } catch (e: CameraAccessException) {
            Log.e("erf", e.toString())
        }
    }

    private fun requestStoragePermission() {
        /**
         * 書き込み権限
         */
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder(baseContext)
                .setMessage("Permission Here")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISION_WRITE_STORAGE)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .create()
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISION_WRITE_STORAGE)
        }

        /**
         * 読み込み権限
         */
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            AlertDialog.Builder(baseContext)
                .setMessage("Permission Here")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISION_READ_STORAGE)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .create()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISION_READ_STORAGE)
        }
    }

    /**
     * reduces the size of the image
     * @param image
     * @param maxSize
     * @return
     */
    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.getWidth()
        var height = image.getHeight()

        val bitmapRatio: Float = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize;
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
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


    private fun grayScale(bitmap: Bitmap) :Bitmap{
        // リソースから読み込み
        var bmp = bitmap

        // bmp → OpenCVへ
        var mat = Mat()
        Utils.bitmapToMat(bmp, mat) // OpenCVの行列へ

        var mat_gray = Mat()
//        var mat_gaussian = Mat()
        var mat_result = Mat()

//        Imgproc.GaussianBlur(mat, mat_gaussian, Size(9.0, 9.0), 8.0, 6.0 )
        Imgproc.cvtColor(mat, mat_gray, Imgproc.COLOR_RGB2GRAY)  // まずグレースケールへ(明るさだけの形式)
        Imgproc.adaptiveThreshold(mat_gray, mat_result, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 51, 10.0 )

        // OpenCV → bmpへ
        var output = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat_result, output)

        return output
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
