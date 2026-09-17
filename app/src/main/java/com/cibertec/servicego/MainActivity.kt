package com.cibertec.servicego

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.cibertec.servicego.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // ViewBinding permite conectar el activity_main.xml completo con Kotlin
    // sin usar findViewById. En clase se programará la lógica del Checkpoint 01.
    private lateinit var binding: ActivityMainBinding

    private var siguienteCodigo = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Punto de inicio de la clase:
        // aquí se agregarán las llamadas a las funciones del Checkpoint 01.

        mostrarEstadoInicial()
        configurarEventosDeTexto()

        binding.buttonRegistrarServicio.setOnClickListener {
            registrarServicio()
        }
    }

    private fun mostrarEstadoInicial() {
        binding.textViewResumenTitulo.text = getString(R.string.resumen_inicial_titulo)
        binding.textViewResumenCliente.text = getString(R.string.resumen_cliente_placeholder)
        binding.textViewResumenDescripcion.text = getString(R.string.resumen_descripcion_placeholder)
        binding.textViewResumenDireccion.text = getString(R.string.resumen_direccion_placeholder)
        binding.textViewEstadoRegistro.text = getString(R.string.estado_pendiente)
        binding.textViewMensajeVisible.text = getString(R.string.mensaje_inicial)
        binding.textViewCodigoPreliminar.text =
            getString(R.string.codigo_preliminar_formato, generarCodigoCorrelativo())
        binding.textViewCostoEstimado.text =
            getString(R.string.costo_estimado_formato, getString(R.string.costo_base_inicial))
        binding.textViewIndicadoresTecnicos.text = getString(R.string.indicadores_iniciales)
        binding.buttonRegistrarServicio.isEnabled = false
    }

    private fun configurarEventosDeTexto() {
        binding.editTextCliente.doAfterTextChanged {
            binding.editTextCliente.error = null
            actualizarIndicadoresBasicos()
        }
        binding.editTextDescripcion.doAfterTextChanged {
            binding.editTextDescripcion.error = null
            actualizarIndicadoresBasicos()
        }
        binding.editTextDireccion.doAfterTextChanged {
            binding.editTextDireccion.error = null
            actualizarIndicadoresBasicos()
        }
    }

    private fun actualizarIndicadoresBasicos() {
        val cliente = binding.editTextCliente.text.toString().trim()
        val descripcion = binding.editTextDescripcion.text.toString().trim()
        val direccion = binding.editTextDireccion.text.toString().trim()
        val clienteValido = cliente.isNotBlank()
        val descripcionValida = descripcion.length >= 8
        val direccionValida = direccion.isNotBlank()
        val listoParaRegistrar = clienteValido && descripcionValida && direccionValida

        binding.textViewCodigoPreliminar.text =
            getString(R.string.codigo_preliminar_formato, generarCodigoCorrelativo())

        if (descripcion.isBlank() || direccion.isBlank()) {
            // Sin dirección y descripción no hay datos suficientes para estimar el servicio
            binding.textViewCostoEstimado.text =
                getString(R.string.costo_estimado_formato, getString(R.string.costo_base_inicial))
            binding.textViewIndicadoresTecnicos.text = getString(R.string.indicadores_iniciales)
            binding.textViewMensajeVisible.text = getString(R.string.mensaje_inicial)
        } else {
            // la descripción se clasifica primero y luego se usa para calcular el costo, tiempo y modalidad con reglas simples.
            val tipoServicio = obtenerTipoServicio(descripcion)
            val modalidad = obtenerModalidad(descripcion)
            val costoEstimado = calcularCostoEstimado(tipoServicio, modalidad)
            val tiempoEstimado = calcularTiempoEstimado(tipoServicio, modalidad)

            binding.textViewCostoEstimado.text =
                getString(R.string.costo_estimado_formato, "S/ ${"%.2f".format(costoEstimado)}")
            binding.textViewIndicadoresTecnicos.text =
                getString(
                    R.string.indicadores_operativos_formato,
                    "%.1f".format(tiempoEstimado),
                    tipoServicio,
                    modalidad
                )
            binding.textViewMensajeVisible.text =
                getString(R.string.mensaje_estimacion_previa, tipoServicio.lowercase())
        }

        binding.buttonRegistrarServicio.isEnabled = listoParaRegistrar

        if (listoParaRegistrar) {
            binding.textViewEstadoRegistro.text = getString(R.string.estado_listo)
        } else {
            binding.textViewEstadoRegistro.text = getString(R.string.estado_pendiente)
        }
    }

    private fun generarCodigoCorrelativo(): String {
        return "SG-%04d".format(siguienteCodigo)
    }

    private fun contieneAlgunTermino(texto: String, terminos: List<String>): Boolean {
        return terminos.any() { termino -> termino in texto}
    }

    private fun obtenerTipoServicio(descripcion: String): String {
        // La descripción se normaliza a minúsculas para comparar las palabras claves

        val descripcionNormalizada = descripcion.lowercase()
        val atiendeRed = contieneAlgunTermino(
            descripcionNormalizada,
            listOf("red", "wifi", "router", "internet")
        )
        val atiendeImpresora = contieneAlgunTermino(
            descripcionNormalizada,
            listOf("impresora", "toner", "tinta", "escaner")
        )
        val requiereInstalacion = contieneAlgunTermino(
            descripcionNormalizada,
            listOf("instalacion", "instalar", "configurar", "montaje")
        )

        return when {
            atiendeRed && atiendeImpresora -> getString(R.string.tipo_servicio_mixto)
            atiendeRed -> getString(R.string.tipo_servicio_red)
            atiendeImpresora -> getString(R.string.tipo_servicio_impresion)
            requiereInstalacion -> getString(R.string.tipo_servicio_instalacion)
            else -> getString(R.string.tipo_servicio_diagnostico)
        }
    }

    private fun obtenerModalidad(descripcion: String): String {
        // La modalidad pasa a prioritaria cuando la descripción contiene los términos de urgencia
        val descripcionNormalizada = descripcion.lowercase()
        val servicioUrgente = contieneAlgunTermino(
            descripcionNormalizada,
            listOf("urgente", "caido", "sin servicio", "no enciende")
        )

        return if (servicioUrgente) {
            getString(R.string.modalidad_prioritaria)
        } else {
            getString(R.string.modalidad_programada)
        }
    }

    private fun calcularCostoEstimado(tipoServicio: String, modalidad: String): Double {
        // El costo base cambia según el tipo de trabajo identificado. La modalidad prioritaria agrega un recargo fijo.

        val costoBase = when (tipoServicio) {
            getString(R.string.tipo_servicio_red) -> 70.0
            getString(R.string.tipo_servicio_impresion) -> 60.0
            getString(R.string.tipo_servicio_instalacion) -> 90.0
            getString(R.string.tipo_servicio_mixto) -> 110.0
            else -> 45.0
        }
        val recargoPrioridad = if (modalidad == getString(R.string.modalidad_prioritaria)) 25.0 else 0.0
        return costoBase + recargoPrioridad
    }

    private fun calcularTiempoEstimado(tipoServicio: String, modalidad: String): Double {
        // El tiempo base sigue la misma clasificación del costo para mantener coherencia entre esfuerzo técnico y estimación mostrada.

        val tiempoBase = when (tipoServicio) {
            getString(R.string.tipo_servicio_red) -> 2.0
            getString(R.string.tipo_servicio_impresion) -> 1.5
            getString(R.string.tipo_servicio_instalacion) -> 3.0
            getString(R.string.tipo_servicio_mixto) -> 3.5
            else -> 1.0
        }
        val recargoPrioridad = if (modalidad == getString(R.string.modalidad_prioritaria)) 0.5 else 0.0
        return tiempoBase + recargoPrioridad
    }




}
