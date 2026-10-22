package com.example.pracial2dam.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pracial2dam.R
import com.example.pracial2dam.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapToolbar.title = "Ubicación EquipStock"
        setSupportActionBar(binding.mapToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.mapToolbar.setNavigationOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Coordenadas de EquipStock (San Salvador, Zona Alameda M. E. Araujo)
        val equipStockLocation = LatLng(13.6925, -89.2272)

        googleMap.addMarker(
            MarkerOptions()
                .position(equipStockLocation)
                .title("EquipStock - Oficina Central")
                .snippet("Gestión e Inventario de Equipos Tecnológicos (San Salvador)")
        )

        // Centra con un zoom adecuado
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(equipStockLocation, 16f))
    }
}