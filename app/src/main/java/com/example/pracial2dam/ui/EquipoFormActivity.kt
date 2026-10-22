package com.example.pracial2dam.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pracial2dam.data.AppDatabase
import com.example.pracial2dam.data.Equipo
import com.example.pracial2dam.databinding.ActivityEquipoFormBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class EquipoFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEquipoFormBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var equipoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipoFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        equipoId = intent.getStringExtra("EQUIPO_ID")

        if (equipoId != null) {
            binding.tvFormTitle.text = "Editar Equipo"
            loadEquipoData(equipoId!!)
        }

        binding.btnSaveEquipo.setOnClickListener {
            saveEquipo()
        }
    }

    private fun loadEquipoData(id: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@EquipoFormActivity)
            val equipo = withContext(Dispatchers.IO) {
                db.equipoDao().getEquipoById(id)
            }
            equipo?.let {
                binding.etNombre.setText(it.nombre)
                binding.etNumeroSerie.setText(it.numeroSerie)
                binding.etCodigoActivo.setText(it.codigoActivo)
                binding.etUbicacion.setText(it.ubicacion)

                when (it.estado) {
                    "EN_MANTENIMIENTO" -> binding.rbMantenimiento.isChecked = true
                    "INACTIVO" -> binding.rbInactivo.isChecked = true
                    else -> binding.rbOperativo.isChecked = true
                }
            }
        }
    }

    private fun saveEquipo() {
        val nombre = binding.etNombre.text.toString().trim()
        val numeroSerie = binding.etNumeroSerie.text.toString().trim()
        val codigoActivo = binding.etCodigoActivo.text.toString().trim()
        val ubicacion = binding.etUbicacion.text.toString().trim()

        if (nombre.isEmpty() || numeroSerie.isEmpty() || codigoActivo.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val estado = when (binding.rgEstado.checkedRadioButtonId) {
            binding.rbMantenimiento.id -> "EN_MANTENIMIENTO"
            binding.rbInactivo.id -> "INACTIVO"
            else -> "OPERATIVO"
        }

        val id = equipoId ?: UUID.randomUUID().toString()

        binding.btnSaveEquipo.isEnabled = false

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@EquipoFormActivity)

            val equipoMap = mapOf(
                "id" to id,
                "nombre" to nombre,
                "numeroSerie" to numeroSerie,
                "codigoActivo" to codigoActivo,
                "ubicacion" to ubicacion,
                "estado" to estado
            )

            // Intentar enviar a firestore con tiempo límite de 2.5 segundos
            val syncSuccess = withTimeoutOrNull(2500) {
                try {
                    firestore.collection("equipos")
                        .document(id)
                        .set(equipoMap)
                        .await()
                    true
                } catch (e: Exception) {
                    false
                }
            } ?: false // Si sobrepasa el tiempo (offline), syncSuccess será false

            // Crea objeto Equipo con el flag correspondiente
            val equipo = Equipo(
                id = id,
                nombre = nombre,
                numeroSerie = numeroSerie,
                codigoActivo = codigoActivo,
                estado = estado,
                ubicacion = ubicacion,
                syncPending = !syncSuccess
            )

            // Guardar o actualizar en la DB Local
            withContext(Dispatchers.IO) {
                db.equipoDao().insertEquipo(equipo)
            }

            val msg = if (syncSuccess) {
                "Guardado en la nube y local"
            } else {
                "Guardado en local (Pendiente de sincronizar)"
            }

            Toast.makeText(this@EquipoFormActivity, msg, Toast.LENGTH_LONG).show()
            finish() // Regresa a MainActivity de inmediato
        }
    }
}