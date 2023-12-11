package com.example.sample_x200

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonOpenPagecall = findViewById<Button>(R.id.buttonOpenPagecall)
        buttonOpenPagecall.setOnClickListener {
            val intent = Intent(this, PagecallActivity::class.java)
            startActivity(intent)
        }
    }
}
