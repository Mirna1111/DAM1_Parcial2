package com.example.pracial2dam.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pracial2dam.R
import com.example.pracial2dam.data.AppDatabase
import com.example.pracial2dam.data.Equipo
import com.example.pracial2dam.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: EquipoAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la Toolbar con soporte para menu
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()

        binding.fabAddEquipo.setOnClickListener {
            startActivity(Intent(this, EquipoFormActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadEquipos()
    }

    private fun setupRecyclerView() {
        adapter = EquipoAdapter(
            equipos = emptyList(),
            onEditClick = { equipo ->
                val intent = Intent(this, EquipoFormActivity::class.java).apply {
                    putExtra("EQUIPO_ID", equipo.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { equipo ->
                deleteEquipo(equipo)
            }
        )
        binding.rvEquipos.layoutManager = LinearLayoutManager(this)
        binding.rvEquipos.adapter = adapter
    }

    private fun loadEquipos() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val list = withContext(Dispatchers.IO) {
                db.equipoDao().getAllEquipos()
            }
            adapter.updateEquipos(list)
        }
    }

    private fun deleteEquipo(equipo: Equipo) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)

            // elimina inmediatamente de la base de datos local
            withContext(Dispatchers.IO) {
                db.equipoDao().deleteEquipo(equipo)
            }

            Toast.makeText(this@MainActivity, "Equipo eliminado localmente", Toast.LENGTH_SHORT).show()
            loadEquipos() // Refresca la pantalla

            // Intentar eliminar de firestore con tiempo límite
            withTimeoutOrNull(2000) {
                try {
                    firestore.collection("equipos").document(equipo.id).delete().await()
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_map -> {
                startActivity(Intent(this, MapActivity::class.java)) // Comentado temporalmente
                Toast.makeText(this, "Ubicación de EquipStock", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}