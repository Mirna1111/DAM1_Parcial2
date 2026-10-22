package com.example.pracial2dam.ui
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pracial2dam.data.Equipo
import com.example.pracial2dam.databinding.ItemEquipoBinding

class EquipoAdapter(
    private var equipos: List<Equipo>,
    private val onEditClick: (Equipo) -> Unit,
    private val onDeleteClick: (Equipo) -> Unit
) : RecyclerView.Adapter<EquipoAdapter.EquipoViewHolder>() {

    inner class EquipoViewHolder(val binding: ItemEquipoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipoViewHolder {
        val binding = ItemEquipoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EquipoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EquipoViewHolder, position: Int) {
        val equipo = equipos[position]
        with(holder.binding) {
            tvNombreEquipo.text = equipo.nombre
            tvEstadoEquipo.text = equipo.estado
            tvSerieCodigo.text = "Serie: ${equipo.numeroSerie} | Activo: ${equipo.codigoActivo}"
            tvUbicacion.text = "Ubicación: ${equipo.ubicacion}"

            // Mostrar u ocultar la alerta si está pendiente de subir a Firestore
            if (equipo.syncPending) {
                tvSyncStatus.visibility = View.VISIBLE
            } else {
                tvSyncStatus.visibility = View.GONE
            }

            btnEdit.setOnClickListener { onEditClick(equipo) }
            btnDelete.setOnClickListener { onDeleteClick(equipo) }
        }
    }

    override fun getItemCount(): Int = equipos.size

    fun updateEquipos(newEquipos: List<Equipo>) {
        equipos = newEquipos
        notifyDataSetChanged()
    }
}