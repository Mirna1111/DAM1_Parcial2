package com.example.pracial2dam.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pracial2dam.data.AppDatabase
import com.example.pracial2dam.data.SessionManager
import com.example.pracial2dam.data.User
import com.example.pracial2dam.databinding.ActivityProfileBinding
import com.example.pracial2dam.utils.Validators
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarProfile.setNavigationIcon(
            androidx.appcompat.R.drawable.abc_ic_ab_back_material
        )

        binding.toolbarProfile.navigationIcon?.setTint(
            android.graphics.Color.WHITE
        )

        binding.toolbarProfile.setNavigationOnClickListener {
            finish()
        }



        sessionManager = SessionManager(this)

        val email = auth.currentUser?.email

        if (email.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.tvProfileEmail.text = "Correo: $email"

        loadUserData(email)

        binding.btnUpdateProfile.setOnClickListener {
            confirmarActualizacion()
        }

        binding.btnSyncData.setOnClickListener {
            syncPendingData()
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.logout()

            val intent = Intent(
                this,
                LoginActivity::class.java
            )

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }

    private fun loadUserData(email: String) {
        lifecycleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(
                        this@ProfileActivity
                    ).userDao().getUserByEmail(email)
                }

                currentUser = user

                if (user != null) {
                    binding.etProfileName.setText(user.name)
                    updateSyncBadge(user.isSynced)
                } else {
                    binding.tvSyncStatus.text =
                        "No se encontró el perfil local"
                }

                binding.etProfilePassword.text?.clear()

            } catch (e: Exception) {
                mostrarMensaje(
                    "Error al cargar perfil: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun confirmarActualizacion() {
        AlertDialog.Builder(this)
            .setTitle("Actualizar perfil")
            .setMessage(
                "¿Deseas guardar los cambios de tu perfil?"
            )
            .setPositiveButton("Confirmar") { _, _ ->
                updateUserData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateSyncBadge(isSynced: Boolean) {
        if (isSynced) {
            binding.tvSyncStatus.text =
                "Estado: Sincronizado en la nube"

            binding.tvSyncStatus.setTextColor(
                Color.rgb(0, 130, 0)
            )
        } else {
            binding.tvSyncStatus.text =
                "Estado: Pendiente de sincronización"

            binding.tvSyncStatus.setTextColor(Color.RED)
        }
    }

    private fun updateUserData() {
        val newName =
            binding.etProfileName.text.toString().trim()

        val newPassword =
            binding.etProfilePassword.text.toString()

        if (!Validators.isValidName(newName)) {
            binding.etProfileName.error =
                "El nombre debe tener al menos 7 caracteres"
            return
        }

        if (newPassword.isNotEmpty() &&
            !Validators.isValidPassword(newPassword)
        ) {
            binding.etProfilePassword.error =
                "La contraseña debe ser alfanumérica y tener mínimo 8 caracteres"
            return
        }

        val firebaseUser = auth.currentUser ?: return
        val email = firebaseUser.email ?: return

        binding.btnUpdateProfile.isEnabled = false

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(
                    this@ProfileActivity
                )

                val updatedUser = User(
                    email = email,
                    name = newName,
                    isSynced = false
                )

                // Guardar el nombre localmente.
                withContext(Dispatchers.IO) {
                    db.userDao().insertUser(updatedUser)
                }

                currentUser = updatedUser
                updateSyncBadge(false)

                // Sincronizar solo si hay internet.
                if (Validators.isNetworkAvailable(
                        this@ProfileActivity
                    )
                ) {
                    try {
                        firestore.collection("users")
                            .document(firebaseUser.uid)
                            .set(
                                mapOf(
                                    "name" to newName,
                                    "email" to email
                                ),
                                SetOptions.merge()
                            )
                            .await()

                        val syncedUser =
                            updatedUser.copy(isSynced = true)

                        withContext(Dispatchers.IO) {
                            db.userDao().updateUser(syncedUser)
                        }

                        currentUser = syncedUser
                        updateSyncBadge(true)

                    } catch (e: Exception) {
                        updateSyncBadge(false)
                        mostrarMensaje(
                            "Nombre guardado localmente. " +
                                    "Sincronización pendiente."
                        )
                    }
                } else {
                    updateSyncBadge(false)
                    mostrarMensaje(
                        "Sin internet. Nombre guardado localmente."
                    )
                }

                // La contraseña se gestiona con Firebase Auth.
                if (newPassword.isNotEmpty()) {
                    if (!Validators.isNetworkAvailable(
                            this@ProfileActivity
                        )
                    ) {
                        mostrarMensaje(
                            "Para cambiar la contraseña necesitas internet."
                        )
                    } else {
                        try {
                            firebaseUser.updatePassword(
                                newPassword
                            ).await()

                            binding.etProfilePassword.text?.clear()

                            mostrarMensaje(
                                "Contraseña actualizada correctamente"
                            )

                        } catch (e: Exception) {
                            mostrarMensaje(
                                "No se pudo cambiar la contraseña. " +
                                        "Puede ser necesario iniciar sesión nuevamente."
                            )
                        }
                    }
                } else {
                    mostrarMensaje("Perfil guardado")
                }

            } catch (e: Exception) {
                mostrarMensaje(
                    "Error al actualizar perfil: ${e.localizedMessage}"
                )

            } finally {
                binding.btnUpdateProfile.isEnabled = true
            }
        }
    }

    private fun syncPendingData() {
        val firebaseUser = auth.currentUser ?: return
        val email = firebaseUser.email ?: return

        // No intentar enviar datos sin internet.
        if (!Validators.isNetworkAvailable(this)) {
            mostrarMensaje(
                "Sin conexión a internet. Intenta sincronizar más tarde."
            )
            return
        }

        binding.btnSyncData.isEnabled = false

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(
                    this@ProfileActivity
                )

                // 1. Sincronizar Usuario Pending si aplica
                val user = withContext(Dispatchers.IO) {
                    db.userDao().getUserByEmail(email)
                }

                if (user != null && !user.isSynced) {
                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .set(
                            mapOf(
                                "name" to user.name,
                                "email" to user.email
                            ),
                            SetOptions.merge()
                        )
                        .await()

                    val syncedUser = user.copy(isSynced = true)
                    withContext(Dispatchers.IO) {
                        db.userDao().updateUser(syncedUser)
                    }
                    currentUser = syncedUser
                    updateSyncBadge(true)
                }

                // 2. Sincronizar Equipos Pendientes de Room -> Firestore
                val pendingEquipos = withContext(Dispatchers.IO) {
                    db.equipoDao().getPendingEquipos()
                }

                var equiposSincronizadosCount = 0

                for (equipo in pendingEquipos) {
                    try {
                        val equipoMap = mapOf(
                            "id" to equipo.id,
                            "nombre" to equipo.nombre,
                            "numeroSerie" to equipo.numeroSerie,
                            "codigoActivo" to equipo.codigoActivo,
                            "ubicacion" to equipo.ubicacion,
                            "estado" to equipo.estado
                        )

                        firestore.collection("equipos")
                            .document(equipo.id)
                            .set(equipoMap, SetOptions.merge())
                            .await()

                        withContext(Dispatchers.IO) {
                            db.equipoDao().markAsSynced(equipo.id)
                        }

                        equiposSincronizadosCount++
                    } catch (_: Exception) {
                        // Si falla un registro individual, continúa con los demás
                    }
                }

                val userIsSynced = currentUser?.isSynced ?: true
                if (userIsSynced) {
                    updateSyncBadge(true)
                }

                if (equiposSincronizadosCount > 0) {
                    mostrarMensaje("Sincronización completada ($equiposSincronizadosCount equipos actualizados)")
                } else {
                    mostrarMensaje("Todo está sincronizado")
                }

            } catch (e: Exception) {
                updateSyncBadge(false)
                mostrarMensaje(
                    "Error al sincronizar: ${e.localizedMessage}"
                )

            } finally {
                binding.btnSyncData.isEnabled = true
            }
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}
