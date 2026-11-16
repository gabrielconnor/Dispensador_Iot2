package com.example.dispensador_iot

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador_iot.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 1. Botón Ingresar
        binding.btnIngresar.setOnClickListener {
            val usuario = binding.etUsuario.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convertimos el usuario a formato correo para Firebase
            val emailFormato = if (usuario.contains("@")) usuario else "$usuario@dispensador.com"

            auth.signInWithEmailAndPassword(emailFormato, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Bienvenido $usuario", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ModelSelectionActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error: Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // 2. Botón Registrar Nuevo Usuario en la nube
        binding.btnRegistrar.setOnClickListener {
            val usuario = binding.etUsuario.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (usuario.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Ingrese usuario y contraseña (mínimo 6 caracteres)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val emailFormato = if (usuario.contains("@")) usuario else "$usuario@dispensador.com"

            auth.createUserWithEmailAndPassword(emailFormato, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "¡Usuario '$usuario' guardado en Firebase!", Toast.LENGTH_SHORT).show()
                        binding.etUsuario.text?.clear()
                        binding.etPassword.text?.clear()
                    } else {
                        Toast.makeText(this, "Error al registrar: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // 3. Botón Cancelar
        binding.btnCancelar.setOnClickListener {
            binding.etUsuario.text?.clear()
            binding.etPassword.text?.clear()
            Toast.makeText(this, "Proceso cancelado", Toast.LENGTH_SHORT).show()
        }

        // 4. Botón Regresar
        binding.btnRegresar.setOnClickListener {
            finish()
        }
    }
}