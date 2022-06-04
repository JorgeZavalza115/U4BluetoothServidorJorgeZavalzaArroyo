package mx.edu.ittepic.ladm_u4_proyectobluetooth_jorgezavalzaarroyo

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import mx.edu.ittepic.ladm_u4_proyectobluetooth_jorgezavalzaarroyo.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    // Request
    val location_permition_request = 101
    val select_device = 102

    // Adaptadores
    lateinit var bluetoothAdapter: BluetoothAdapter

    // Valores
    companion object {
        var myUUID : UUID = UUID.fromString("b50b9ce3-5a2f-47e3-8853-4cd9be15df5e")
        lateinit var m_address : String
    }

    // Datos para los archivos
    var noControl : String = "17401333"
    lateinit var hora : String
    lateinit var hoy : String
    val formato = SimpleDateFormat("dd-MM-yyyy")
    var arreglo= ArrayList<String>()
    var asistencia : String = ""
    lateinit var baseRemota : FirebaseFirestore

    init {
        hoy = formato.format(Date())
        hora = "Horario: 11:00 - 12:00"
        try {
            asistencia = obtenerLista( hora )
        } catch ( e:Exception ) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG)
                .show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView( binding.root )

        // Instanciar Base Firestore (Perezosa)
        baseRemota = FirebaseFirestore.getInstance()

        initBluetooth()
        // Imagen inicial
        binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24)

        if ( bluetoothAdapter.isEnabled ) {
            binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth)
        }



        // Cambio de horario
        binding.switchHora.setOnCheckedChangeListener { compoundButton, b ->
            if ( b ) {
                hora = "Horario: 12:00 - 13:00"
                compoundButton.setText(hora)
                asistencia= obtenerLista(hora)
            } else {
                hora = "Horario: 11:00 - 12:00"
                binding.switchHora.setText(hora)
                asistencia= obtenerLista(hora)
            }
        }

        // Test de guardado en Firestore
        binding.test.setOnClickListener {
            var texto = asistencia + noControl + ", "
            baseRemota.collection(hora).document(hoy)
                .set(hashMapOf("NoControl" to texto))
                .addOnSuccessListener {
                    Toast.makeText(this, "${noControl} se presentó", Toast.LENGTH_SHORT)
                        .show()
                    asistencia = obtenerLista(hora)
                }.addOnFailureListener {
                    AlertDialog.Builder(this)
                        .setTitle("Atención")
                        .setMessage("No se ha podido ingresar la asistencia en el Firestore")
                        .show()
                }

        }

        binding.listaHoy.setOnClickListener {
            val texto = generarLista()
            val archivo = OutputStreamWriter( openFileOutput("ListaAsistencia.txt", MODE_PRIVATE ))
            archivo.write( texto )
            archivo.flush()
            archivo.close()
            Toast.makeText(this, "Se ha guardado la lista de asistencia de hoy", Toast.LENGTH_SHORT)
                .show()
        }

        binding.generarLista.setOnClickListener {
            val archivo = BufferedReader( InputStreamReader( openFileInput( "ListaAsistencia.txt" ) ) )
            var list = archivo.readLines()
            var cad = ""
            list.forEach {
                cad = cad + it + "\n"
            }
            binding.listaArchivo.setText( cad )
            archivo.close()
        }


    }

    // Verifica si el dispositivo tiene bluetooth
    private fun initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if ( bluetoothAdapter == null ) {
            Toast.makeText(this, "No Bluetooth Found", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun obtenerLista(a:String ) : String {
        try {
            FirebaseFirestore.getInstance().collection(a).document(hoy)
                .get()
                .addOnSuccessListener {
                    if ( it.get("NoControl") == null ) {
                        asistencia = ""
                    }
                    asistencia = it.get("NoControl").toString()
                }
                .addOnFailureListener {
                    AlertDialog.Builder(this)
                        .setTitle("Atención")
                        .setMessage("No se ha podido recuperar la información de Firestore")
                        .show()
                }
        } catch ( e:Exception ) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG)
                .show()
        }

        return asistencia
    }

    private fun generarLista() : String {
        var cad = " -- ${hoy} -- \n - Horario: 11:00 - 12:00 - \n"
        FirebaseFirestore.getInstance().collection("Horario: 11:00 - 12:00").document(hoy)
            .get()
            .addOnSuccessListener {
                var res = it.getString("NoControl") as String
                cad += res
            }
            .addOnFailureListener {
                AlertDialog.Builder(this)
                    .setTitle("Atención")
                    .setMessage("No se ha podido recuperar la información de Firestore" + it.message )
                    .show()
            }
        cad += "\n\n - Horario: 12:00 - 13-00 - \n"
        try {
            FirebaseFirestore.getInstance().collection("Horario: 11:00 - 12:00").document(hoy)
                .get()
                .addOnSuccessListener {
                    var res = it.getString("NoControl") as String
                    cad += res
                }
                .addOnFailureListener {
                    AlertDialog.Builder(this)
                        .setTitle("Atención")
                        .setMessage("No se ha podido recuperar la información de Firestore")
                        .show()
                }
        } catch ( e:Exception ) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG)
                .show()
        }
        return cad
    }

    // Vincular el menú
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return true
    }

    // Selección del item del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when ( item.itemId ) {
            R.id.menuBluetooth -> {
                checkPermitions()
                return true
            }
        }
        return true
    }

    // Analiza permisos
    private fun checkPermitions() {
        if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, arrayOf( android.Manifest.permission.ACCESS_FINE_LOCATION ), location_permition_request)
        } else {
            // Activa Bluetooth
            enableBluetooth()
            binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth)
        }
    }

    // Activa el bluetooth
    @SuppressLint("MissingPermission")
    private fun enableBluetooth() {
        if ( !bluetoothAdapter.isEnabled ) {
            bluetoothAdapter.enable()
        }

        // Activa la visibilidad
        if ( bluetoothAdapter.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE ) {
            var discoveryIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoveryIntent)
            binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth_searching)
        }
    }

    // Receptor de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if ( requestCode == location_permition_request ) {
            if ( grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                Toast.makeText(this, "Permisos Otorgados", Toast.LENGTH_SHORT)
                    .show()
                binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth)
            } else {
                AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("Location permition is required\nPlease grant")
                    .setPositiveButton("Grant") { d, i ->
                        checkPermitions()
                    }
                    .setNegativeButton("Deny") { d, i ->
                        finish()
                    }
                    .show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // Server :/
    @SuppressLint("MissingPermission")
    private inner class AcceptThread() : Thread() {
        private val mmServerSocket : BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Servidor", myUUID )
        }

        override fun run() {
            super.run()
            var shouldLoop = true
            while ( shouldLoop ) {
                val socket : BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch ( err: IOException) {
                    Log.e("Server","Socket Accept() method failed", err )
                    shouldLoop = false
                    null
                }
                socket?.also {
                    binding.imagen.setImageResource(R.drawable.ic_baseline_bluetooth_connected)
                    //manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch ( err: IOException) {
                Log.e("Server","Could not close the connect socket")
            }
        }
    }
}