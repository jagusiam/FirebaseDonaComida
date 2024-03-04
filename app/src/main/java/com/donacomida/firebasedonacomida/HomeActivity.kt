package com.donacomida.firebasedonacomida

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.donacomida.firebasedonacomida.databinding.ActivityHomeBinding
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
//import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig


//clase enum para enumerar los proveedores
enum class ProviderType {
    BASIC,
    GOOGLE,
    FACEBOOK
}

class HomeActivity : AppCompatActivity() {
    //constante global privada a db - asi tenemos una instancia definida en remoto desde la consola de Firebase
    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(R.layout.activity_home)

        //Setup
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val proveedor = bundle?.getString("proveedor")
        setup(email ?: "",proveedor ?: "")

        //Guardado de datos de usuario autenticado - para que quede guardado el estado de sesion
        //a traves de SharedPreferences de Android para guaradar de datos
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email) //guardar email
        prefs.putString("proveedor", proveedor) //guardar proveedor
        prefs.apply() //para que se apliquen los cambios de los nuevos datos

        //Remote Config
        binding.errorButton.visibility = View.INVISIBLE
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {
            task ->
            if(task.isSuccessful) {
                val showErrorButton: Boolean = Firebase.remoteConfig.getBoolean("show_error_button")
                val errorButtonText: String = Firebase.remoteConfig.getString("error_button_text")

                if (showErrorButton) {
                    binding.errorButton.visibility = View.VISIBLE
                }
                binding.errorButton.text = errorButtonText
            }
        }
    }

    private fun setup(email: String, proveedor: String) {
        title = "Inicio"
        binding.emailTextView.text = email
        binding.providerTextView.text = proveedor

        binding.logOutButton.setOnClickListener {
            //Borrado de datos - cuando vamos a cerrar la sesion las prefencias deberian de elminarse
            val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
            prefs.clear() //borrar todas las preferencias
            prefs.apply() //para que se efectue el borrado
            if (proveedor==ProviderType.FACEBOOK.name) {
                LoginManager.getInstance().logOut()
            }
            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }

        binding.errorButton.setOnClickListener {
            FirebaseCrashlytics.getInstance().setUserId(email)
            FirebaseCrashlytics.getInstance().setCustomKey("proveedor", proveedor)

            //Enviar log de contacto
            FirebaseCrashlytics.getInstance().log("Se ha pulsado el boton FORZAR ERROR")
            //Forzado de error
            //throw java.lang.RuntimeException("Forzado de error")
        }

        binding.saveButton.setOnClickListener {
            db.collection("users").document(email).set(
                hashMapOf("proveedor" to proveedor,
                "address" to binding.addressTextView.text.toString(),
                "phone" to binding.phoneTextView.text.toString())
            )

        }

        binding.getButton.setOnClickListener {
            db.collection("users").document(email).get().addOnSuccessListener {
                binding.addressTextView.setText(it.get("address") as String?)
                binding.phoneTextView.setText(it.get("phone") as String?)
            }

        }

        binding.deleteButton.setOnClickListener {
            db.collection("users").document(email).delete()

        }



    }
}