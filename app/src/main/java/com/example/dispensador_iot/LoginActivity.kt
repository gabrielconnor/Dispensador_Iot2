package com.example.dispensador_iot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador_iot.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UsuariosAppIoT", Context.MODE_PRIVATE)

        // 1. Ingresar
        binding.btnIngresar.setOnClickListener {
            val usuario = binding.etUsuario.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar cuentas estáticas por defecto
            val esDefecto = (usuario == "admin" && password == "1234") || (usuario == "duenio" && password == "1234")

            // Validar cuentas registradas dinámicamente con PBKDF2
            val hashGuardado = sharedPref.getString("${usuario}_hash", null)
            val saltGuardadaStr = sharedPref.getString("${usuario}_salt", null)
            var esValidoDinamico = false

            if (hashGuardado != null && saltGuardadaStr != null) {
                val salt = SecurityUtils.stringToSalt(saltGuardadaStr)
                val hashCalculado = SecurityUtils.hashPassword(password, salt)
                esValidoDinamico = (hashCalculado == hashGuardado)
            }

            if (esDefecto || esValidoDinamico) {
                Toast.makeText(this, "Bienvenido $usuario", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ModelSelectionActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Registrar usuario nuevo con Hash PBKDF2 + Sal
        binding.btnRegistrar.setOnClickListener {
            val usuario = binding.etUsuario.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val salt = SecurityUtils.generateSalt()
            val hash = SecurityUtils.hashPassword(password, salt)

            val editor = sharedPref.edit()
            editor.putString("${usuario}_hash", hash)
            editor.putString("${usuario}_salt", SecurityUtils.saltToString(salt))
            editor.apply()

            Toast.makeText(this, "Usuario '$usuario' registrado con éxito de forma segura", Toast.LENGTH_SHORT).show()
            binding.etUsuario.text?.clear()
            binding.etPassword.text?.clear()
        }

        binding.btnCancelar.setOnClickListener {
            binding.etUsuario.text?.clear()
            binding.etPassword.text?.clear()
        }

        binding.btnRegresar.setOnClickListener { finish() }
    }
}