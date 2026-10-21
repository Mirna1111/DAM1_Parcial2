package com.example.pracial2dam.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pracial2dam.data.AppDatabase
import com.example.pracial2dam.data.User
import com.example.pracial2dam.databinding.ActivityRegisterBinding
import com.example.pracial2dam.utils.Validators
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegisterSubmit.setOnClickListener {

            val name = binding.etRegisterName.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString()
            val confirmPassword =
                binding.etRegisterConfirmPassword.text.toString()

            // Validar nombre
            if (!Validators.isValidName(name)) {
                binding.etRegisterName.error =
                    "El nombre debe tener al menos 7 caracteres"
                binding.etRegisterName.requestFocus()
                return@setOnClickListener
            }

            // Validar correo
            if (!Validators.isValidEmail(email)) {
                binding.etRegisterEmail.error =
                    "Ingrese un correo válido"
                binding.etRegisterEmail.requestFocus()
                return@setOnClickListener
            }

            // Validar contraseña
            if (!Validators.isValidPassword(password)) {
                binding.etRegisterPassword.error =
                    "La contraseña debe tener al menos 8 caracteres"
                binding.etRegisterPassword.requestFocus()
                return@setOnClickListener
            }

            // Validar confirmación
            if (confirmPassword.isEmpty()) {
                binding.etRegisterConfirmPassword.error =
                    "Confirma tu contraseña"
                binding.etRegisterConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.etRegisterConfirmPassword.error =
                    "Las contraseñas no coinciden"
                binding.etRegisterConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            registerUser(name, email, password)
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser(
        name: String,
        email: String,
        password: String
    ) {
        binding.btnRegisterSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                // Crear cuenta en Firebase Authentication
                val result = auth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val firebaseUser = result.user
                    ?: throw Exception("No se pudo crear la cuenta")

                // Guardar usuario en Room
                val db = AppDatabase.getDatabase(
                    this@RegisterActivity
                )

                val newUser = User(
                    email = email,
                    name = name,
                    isSynced = false
                )

                withContext(Dispatchers.IO) {
                    db.userDao().insertUser(newUser)
                }

                // Sincronizar perfil con Firestore
                var synchronized = false

                try {
                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .set(
                            mapOf(
                                "name" to name,
                                "email" to email
                            ),
                            SetOptions.merge()
                        )
                        .await()

                    synchronized = true

                } catch (e: Exception) {
                    synchronized = false
                }

                if (synchronized) {
                    withContext(Dispatchers.IO) {
                        db.userDao().updateUser(
                            newUser.copy(isSynced = true)
                        )
                    }
                }

                val message = if (synchronized) {
                    "Cuenta creada correctamente"
                } else {
                    "Cuenta creada. Perfil pendiente de sincronización"
                }

                Toast.makeText(
                    this@RegisterActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()

                // Regresar al Login
                auth.signOut()
                finish()

            } catch (e: Exception) {

                val message = when (e) {
                    is FirebaseAuthUserCollisionException ->
                        "Este correo ya está registrado"

                    is FirebaseAuthWeakPasswordException ->
                        "La contraseña es demasiado débil"

                    is FirebaseAuthInvalidCredentialsException ->
                        "El correo electrónico no es válido"

                    else ->
                        "Error al registrar: ${e.localizedMessage}"
                }

                Toast.makeText(
                    this@RegisterActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                binding.btnRegisterSubmit.isEnabled = true
            }
        }
    }
}
