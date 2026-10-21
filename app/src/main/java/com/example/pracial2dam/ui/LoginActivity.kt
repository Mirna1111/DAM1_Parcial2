
package com.example.pracial2dam.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pracial2dam.data.SessionManager
import com.example.pracial2dam.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.pracial2dam.ui.MainActivity
import com.example.pracial2dam.utils.Validators

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this)

        cargarDatosGuardados()
        configurarBotones()
    }

    private fun cargarDatosGuardados() {
        val recordar = sessionManager.isRememberEnabled()

        binding.cbRememberMe.isChecked = recordar

        if (recordar) {
            binding.etEmail.setText(
                sessionManager.getSavedEmail()
            )
        }
    }

    private fun configurarBotones() {

        binding.btnLogin.setOnClickListener {
            iniciarSesion()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    private fun iniciarSesion() {

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Validar correo
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Ingrese un correo válido"
            binding.etEmail.requestFocus()
            return
        }

        // Validar contraseña
        if (!Validators.isValidPassword(password)) {
            binding.etPassword.error =
                "Debe tener al menos 8 caracteres, con letras y números"
            binding.etPassword.requestFocus()
            return
        }

        binding.btnLogin.isEnabled = false

        // Autenticación con Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                binding.btnLogin.isEnabled = true

                if (task.isSuccessful) {

                    sessionManager.saveCredentials(
                        email,
                        binding.cbRememberMe.isChecked
                    )

                    Toast.makeText(
                        this,
                        "¡Bienvenido!",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        )
                    )

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        "Correo o contraseña incorrectos, o no hay conexión",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}

