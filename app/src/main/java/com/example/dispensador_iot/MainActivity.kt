package com.example.dispensador_iot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redirige inmediatamente a LoginActivity al abrir la app
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
