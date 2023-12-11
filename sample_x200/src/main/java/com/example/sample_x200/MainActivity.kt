package com.example.sample_x200

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    var mainWebView: WebView? = null
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 결과가 OK일 때 처리할 로직
            val intent = result.data
            // 인텐트에서 데이터 처리
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainWebView = findViewById<WebView>(R.id.webView)
        val customHtml = """
            <html>
                <body>
                    <h1>Hello World!</h1>
                    <a href="https://app.pagecall.com/meet?room_id=6572e3bc435814217603baa1">Go to pagecall room</a>
                </body>
            </html>""".trimIndent()
        mainWebView?.loadData(customHtml, "text/html", "UTF-8")
        mainWebView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (Uri.parse(url).host == "app.pagecall.com") {
                    Log.d("Ryan123", "Trying to go to pagecall room")
                    val intent = Intent(this@MainActivity, PagecallActivity::class.java).apply {
                        putExtra("URL", url)
                    }
                    startForResult.launch(intent)
                    return true // WebView가 URL을 로드하지 않도록 함
                }
                return false // 다른 URL은 WebView가 로드하도록 함
            }
        }
    }
}
