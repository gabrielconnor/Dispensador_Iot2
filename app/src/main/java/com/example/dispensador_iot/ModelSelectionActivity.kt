package com.example.dispensador_iot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador_iot.databinding.ActivityModeSelectionBinding

class ModelSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModeSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOwnerMode.setOnClickListener {
            val intent = Intent(this, DuenioActivity::class.java)
            startActivity(intent)
        }

        binding.btnDispenserMode.setOnClickListener {
            val intent = Intent(this, DispensadorActivity::class.java)
            startActivity(intent)
        }
    }
}