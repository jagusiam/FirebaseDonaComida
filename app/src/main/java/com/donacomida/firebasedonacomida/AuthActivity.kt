package com.donacomida.firebasedonacomida

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.donacomida.firebasedonacomida.databinding.ActivityAuthBinding
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
//import com.google.android.gms.auth.GoogleAuthUtil.getToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
//import com.google.firebase.messaging.ktx.messaging
//import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

//import kotlinx.coroutines.scheduling.DefaultIoScheduler.default


class AuthActivity : AppCompatActivity() {

    //VARIABLES GLOBALES

    private val GOOGLE_SIGN_IN = 100 //constante de id de Google
    private val callBackManger = CallbackManager.Factory.create() //

    //declarar una variable de la clase de vinculación para la actividad que se usará
    private lateinit var binding: ActivityAuthBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Llama al método inflate() incld. en clase de vinculación generada - esto crea una instancia de clase de vinculación.
        binding = ActivityAuthBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(R.layout.activity_auth)

        //Remote config
        val configSettings : FirebaseRemoteConfigSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 60
        }
        val firebaseConfig : FirebaseRemoteConfig = Firebase.remoteConfig
        firebaseConfig.setConfigSettingsAsync(configSettings)
        firebaseConfig.setDefaultsAsync(mapOf("show_error_button" to false, "error_button_text" to "Forzar error"))

        //Setup
        //para accede a token de egistro de dispositivo identif para notificacions
        notification()
        setup()
        session() //llamada a la funcion session para comprobar de que se ha iniciado una

    }

    //Que se invoca cada vez que se vuelve a mostrar la pantalla
    override fun onStart() {
        super.onStart()
        //para que vuelva a estar visible
        binding.authLayout.visibility = View.VISIBLE
    }

    private fun notification() {
            //version MoureDev -no funciona, FirebaseInstanceId obsoleto
/*            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
                it.result?.token?.let {
                    println("Este es el token del dispositivo: ${it}")
                }
            }*/

        //nueva version actual segun la doc ofi
       // FirebaseMessaging.getInstance().getToken().addOnCompleteListener(OnCompleteListener { task ->

        //FirebaseMessaging.getInstance().getToken().addOnCompleteListener {
/*        FirebaseMessaging.getInstance().token.addOnCompleteListener {
                //val token = task.result?.token

                it.result?.token?.let {
                    println("Este es el token del dispositivo: ${it}")
                }
        }*/
        //)

        //Version Ofi
        //FirebaseMessaging.getInstance().getToken.addOnCompleteListener(
        //FirebaseMessaging.getInstance().token.addOnCompleteListener(
        //Firebase.messaging.token.addOnCompleteListener {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result


                // Log and toast
                //val msg = getString(R.string.msg_token_fmt, token) - da error en token
                //val msg = token
                Log.d(TAG, token)
                Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
                println("Este es el token del dispositivo: $token")
        })

    }

    //Para comprobarsi tenemos guardado un email y un proveedor - si se ha inciado una sesión
    private fun session() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE) //sin edit(), aqui solo recuperamos
        val email = prefs.getString("email", null) //valor or defecto: nulo
        val proveedor = prefs.getString("proveedor", null)
        //si es distinto de nulo - podemos navegar a la pantalla HomeActivity
        if (email != null && proveedor != null) {
            //authLayout solo visible cuando hay sesion iniciada
            binding.authLayout.visibility = View.INVISIBLE
            showHome(email, ProviderType.valueOf(proveedor))
        }

    }

    private fun setup() {
        title = "Autenticación"
        binding.signUpButton.setOnClickListener {
            if (binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()) {

                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(
                        binding.emailEditText.text.toString(),
                        binding.passwordEditText.text.toString()
                    ).addOnCompleteListener {
                        if (it.isSuccessful) {
                            showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                        } else {
                            showAlert()
                        }
                    }

            }
        }
        binding.loginButton.setOnClickListener {
            if (binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()) {

                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        binding.emailEditText.text.toString(),
                        binding.passwordEditText.text.toString()
                    ).addOnCompleteListener {
                        if (it.isSuccessful) {
                            showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                        } else {
                            showAlert()
                        }
                    }

            }
        }

        //acción sobre el boton de Google
        binding.googleButton.setOnClickListener {
            //Configuracion de login a traves de Google
            //DEFAULT_SIGN_IN - login por defecto
            val googleConfig = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) //
                .requestEmail()
                .build()

            //Cliente de autenticacion de Google
            val googleClient = GoogleSignIn.getClient(this, googleConfig)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }

        binding.facebookButton.setOnClickListener() {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
            LoginManager.getInstance().registerCallback(callBackManger,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        result?.let {
                            val token = it.accessToken
                            val credential = FacebookAuthProvider.getCredential(token.token)
                            FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    showHome(it.result?.user?.email ?: "", ProviderType.FACEBOOK)
                                } else {
                                    showAlert()
                                }
                            }
                        }
                    }

                    override fun onCancel() {
                        TODO("Not yet implemented")
                    }

                    override fun onError(error: FacebookException) {
                        showAlert()
                    }
                })
        }

    }

    //error autenticando
    private fun showAlert() {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Error")
            builder.setMessage("Se ha producido un error autenticando al usuario")
            builder.setPositiveButton("Aceptar", null)
            val dialog: AlertDialog = builder.create()
            dialog.show()
    }

    //navegar a la otra pantalla
    private fun showHome(email: String, proveedor: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", proveedor.name)
        }
        startActivity(homeIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callBackManger.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful) {
                            showHome(account.email ?: "", ProviderType.GOOGLE)
                        } else {
                            showAlert()
                        }
                    }
                }
            } catch (e: ApiException) {
                showAlert()
            }


        }
    }



}