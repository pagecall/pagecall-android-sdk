package com.example.sample_x200

import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.pagecall.PagecallWebView
import com.pagecall.TerminationReason

class PagecallActivity : AppCompatActivity() {
    private lateinit var pagecallView: PagecallWebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagecall)
        PagecallWebView.setWebContentsDebuggingEnabled(true)

        initWebView();
    }
    private fun initWebView() {
        pagecallView = findViewById<PagecallWebView>(R.id.pagecall_view)
        pagecallView.load("6572e3bc435814217603baa1", "", PagecallWebView.PagecallMode.MEET, HashMap<String, String>().apply {
            put("chime", "0")
            put("logLevel", "1")
        })

        pagecallView.setListener(object : PagecallWebView.Listener {
            override fun onLoaded() {
                Log.d("Ryan123", "loaded");
            }

            override fun onMessage(message: String?) {
                Log.d("Ryan123", "onMessage")
            }

            override fun onError(error: WebResourceError?) {
                Log.e("Ryan123", error.toString() + " " + error?.description);
            }
            override fun onTerminated(reason: TerminationReason?) {
                finish()
            }
        })
    }

    override fun onDestroy() {
        val pagecallLayout = findViewById<LinearLayout>(R.id.pagecall_layout)
        pagecallLayout.removeAllViews()
        pagecallView.destroy()
        super.onDestroy()
    }
}
