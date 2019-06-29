package com.kohshin.wifisettinghelper

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import java.io.File
import android.net.wifi.WifiManager
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiConfiguration
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.Toast
import android.support.v4.app.ActivityCompat
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_next.*
import kotlinx.android.synthetic.main.activity_setting.*


class NextActivity : AppCompatActivity(){
    val TAG: String = "NextActivity"
    val PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION: Int = 0

    val appDir = File(Environment.getExternalStorageDirectory(), "WiFiSettingHelper")
    val filename = "wifi.png"

    lateinit var imageViewWifi: ImageView

    lateinit var scrollViewSsid: ScrollView
    lateinit var scrollViewPassword: ScrollView

    lateinit var buttonSsid: ImageButton
    lateinit var buttonPassword: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)

        checkPermission();

        imageViewWifi = findViewById(R.id.image_view)

        val loadfile = File(appDir, filename)
        val bitmap = BitmapFactory.decodeFile(loadfile.toString())
        imageViewWifi.setImageBitmap(bitmap)

        //受け取った変数を入れる
        val result = intent.getStringExtra("result")


        val linearLayoutSsid: LinearLayout = findViewById(R.id.linear_layout_ssid)
        val linearLayoutPassword: LinearLayout = findViewById(R.id.linear_layout_password)

        val editTextSsid: EditText = findViewById(R.id.editTextSsid)
        val editTextPass: EditText = findViewById(R.id.editTextPass)

        scrollViewSsid = findViewById(R.id.scroll_view_ssid)
        scrollViewPassword = findViewById(R.id.scroll_view_password)

        // Activity 等の Context の中で
        val wm = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val scanResults = wm.scanResults

        //SSIDの読み込み
        for (scanResult in scanResults) {
            Log.d(TAG, scanResult.toString())

            if(scanResult.SSID == ""){
                continue
            }

            //SSIDのTextViewをLinearLayoutに追加
            val buttonSsidSelected: Button = Button(this)
            buttonSsidSelected.setText(scanResult.SSID)

            val textLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            buttonSsidSelected.setBackgroundColor(Color.parseColor("#cccccc"))
            buttonSsidSelected.setTextSize(18F)
            buttonSsidSelected.setLayoutParams(textLayoutParams)
            linearLayoutSsid.addView(buttonSsidSelected)

            buttonSsidSelected.setOnClickListener{
                editTextSsid.setText(buttonSsidSelected.text)
                buttonSsid.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp)
                scrollViewSsid.visibility = View.INVISIBLE
            }
        }

        buttonSsid = findViewById(R.id.imageButtonSsid)
        buttonSsid.setOnClickListener{
            //SSIDの選択
            pressSsid()
        }

        //OCR結果の読み込み
        var ocr_result: List<String> = result.split("\n")

        for( line in ocr_result){
            if(line == ""){
                continue
            }

            val line_replace_space = line.replace(" ", "")

            //SSIDのTextViewをLinearLayoutに追加
            val buttonPasswordSelected: Button = Button(this)
            buttonPasswordSelected.setText(line_replace_space)

            val textLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            buttonPasswordSelected.setBackgroundColor(Color.parseColor("#cccccc"))
            buttonPasswordSelected.setTextSize(18F)
            buttonPasswordSelected.setLayoutParams(textLayoutParams)
            linearLayoutPassword.addView(buttonPasswordSelected)

            buttonPasswordSelected.setOnClickListener{
                editTextPass.setText(buttonPasswordSelected.text)
                buttonPassword.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp)
                scrollViewPassword.visibility = View.INVISIBLE
            }
        }

        buttonPassword = findViewById(R.id.imageButtonPass)
        buttonPassword.setOnClickListener{
            //PASSWORDの選択
            pressPassword()
        }

        //設定ボタン押下処理
        val buttonSet: Button = findViewById(R.id.buttonSet)
        buttonSet.setOnClickListener{
            // Activity 等の Context の中で
            val  wm: WifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager

            // targetSsid が未登録の場合は新規登録を先に行う
            // 以下は WPA の例
            val targetConfig: WifiConfiguration = WifiConfiguration()
            targetConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            targetConfig.SSID = '"' + editTextSsid.text.toString() + '"'
            targetConfig.preSharedKey = '"' + editTextPass.text.toString() + '"'

            // 以下を実行しても targetConfig.networkId は更新されないので
            // 返り値の networkId を使う
            val networkId:Int = wm.addNetwork(targetConfig)

            if (networkId != -1) {
                wm.enableNetwork(networkId, true)
                finish()
            } else {
                // 登録失敗
                Toast.makeText(this, "登録が失敗しました", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun checkPermission() {
        // ・現在地取得のパーミッションの許可確認
        // 許可されていない
        if (ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            // パーミッションの許可をリクエスト
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION)
        }
    }

    // requestPermissionsのコールバック
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION-> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "パーミッション追加しました", Toast.LENGTH_SHORT).show()
                // 以下通常処理等に飛ばす・・・
            } else {
//                Toast.makeText(this, "パーミッション追加できませんでした", Toast.LENGTH_SHORT).show()
            }
            else -> {
            }
        }
    }


    override fun onResume() {
        super.onResume()


        // 検索時に呼ばれる BroadcastReceiver を登録
        registerReceiver(
            mScanResultsReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        // startScan() は呼ばなくても scan が裏で走っていることが多いけど念の為
        val wm = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.startScan()
    }

    override fun onPause() {
        super.onPause()

        try {
            unregisterReceiver(mScanResultsReceiver)
        } catch (e: IllegalArgumentException) {
            // 既に登録解除されている場合
            // 事前に知るための API は用意されていない
        }

    }

    private val mScanResultsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // intent に情報が乗っているわけではないので、
            // WifiManager の getScanResults で結果を取得
            val wm = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            val scanResults = wm.scanResults

            for (scanResult in scanResults) {
                Log.d(TAG, scanResult.toString())
            }

            // onReceive() は何度も呼ばれるので、
            // 1度で終了させたい場合はここで unregister する
            try {
                unregisterReceiver(this)
            } catch (e: IllegalArgumentException) {
                // 既に登録解除されている場合
                // 事前に知るための API は用意されていない
            }

        }
    }

    private fun pressSsid(){
        if(scrollViewSsid.visibility == View.INVISIBLE && scrollViewPassword.visibility == View.INVISIBLE) {
            scrollViewSsid.visibility = View.VISIBLE
            buttonSsid.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp)
        }
        else if(scrollViewSsid.visibility == View.VISIBLE && scrollViewPassword.visibility == View.INVISIBLE){
            scrollViewSsid.visibility = View.INVISIBLE
            buttonSsid.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp)
        }
        else if(scrollViewSsid.visibility == View.INVISIBLE && scrollViewPassword.visibility == View.VISIBLE){
            scrollViewSsid.visibility = View.VISIBLE
            scrollViewPassword.visibility = View.INVISIBLE
            buttonSsid.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp)
            buttonPassword.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp)
        }
    }

    private fun pressPassword(){
        if(scrollViewSsid.visibility == View.INVISIBLE && scrollViewPassword.visibility == View.INVISIBLE) {
            scrollViewPassword.visibility = View.VISIBLE
            buttonPassword.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp)
        }
        else if(scrollViewSsid.visibility == View.VISIBLE && scrollViewPassword.visibility == View.INVISIBLE){
            scrollViewSsid.visibility = View.INVISIBLE
            scrollViewPassword.visibility = View.VISIBLE
            buttonSsid.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp)
            buttonPassword.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp)

        }
        else if(scrollViewSsid.visibility == View.INVISIBLE && scrollViewPassword.visibility == View.VISIBLE){
            scrollViewPassword.visibility = View.INVISIBLE
            buttonPassword.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp)
        }
    }

}