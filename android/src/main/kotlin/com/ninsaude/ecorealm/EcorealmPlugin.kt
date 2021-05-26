package com.ninsaude.ecorealm

import com.ninsaude.ecorealm.customer
import com.ninsaude.ecorealm.appointment
import com.ninsaude.ecorealm.configuration
import com.ninsaude.ecorealm.record
import com.ninsaude.ecorealm.record_content
import com.ninsaude.ecorealm.text_suggestion

import androidx.annotation.NonNull
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.Log

import io.realm.OrderedRealmCollectionChangeListener
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import io.realm.kotlin.where
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import io.realm.mongodb.User
import io.realm.mongodb.sync.SyncConfiguration
import io.realm.mongodb.sync.Sync
import io.realm.mongodb.sync.SyncSession
import io.realm.mongodb.ErrorCode
import io.realm.mongodb.auth.GoogleAuthType
import io.realm.RealmList
import io.realm.RealmConfiguration
import io.realm.RealmQuery

import java.util.concurrent.FutureTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Date
import org.bson.types.ObjectId

/** EcorealmPlugin */
class EcorealmPlugin: FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var app: App
    private lateinit var context: Context
    private var objectIdProperties: List<String> = listOf("_id", "customer")
    private var appId: String = "ecodoc-wgion"

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "ecorealm")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.getApplicationContext()
    }


    /*
        MethodCall - where the parameters are stored
        Result - where the returned value going to be inputted
    */
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.setLogLevel(Log.DEBUG)
        when (call.method) {
            "getConnectionType" -> getConnectionType(result)
            "init" -> init(result)
            "stopSync" -> stopSync(result)
            "startSync" -> startSync(result)
            "register" -> register(call, result)
            "isLoggedIn" -> {
                Log.d("isLoggedIn", "teste")
                if(isLoggedIn()) 
                    result.success(true)
                else
                    result.success(false)
            }
            "logIn" -> logIn(call, result)
            "logInGoogle" -> logInGoogle(call, result)
            "logOut" -> logOut(result)
            "addAppointment" -> addAppointment(call, result)
            "updateAppointment" -> updateAppointment(call, result)
            "deleteAppointment" -> deleteAppointment(call, result)
            "listAppointment" -> listAppointment(call, result)
            "addCustomer" -> addCustomer(call, result)
            "updateCustomer" -> updateCustomer(call, result)
            "deleteCustomer" -> deleteCustomer(call, result)
            "listCustomer" -> listCustomer(call, result)
            "addConfiguration" -> addConfiguration(call, result)
            "updateConfiguration" -> updateConfiguration(call, result)
            "deleteConfiguration" -> deleteConfiguration(call, result)
            "listConfiguration" -> listConfiguration(call, result)
            "addTextSuggestion" -> addTextSuggestion(call, result)
            "updateTextSuggestion" -> updateTextSuggestion(call, result)
            "deleteTextSuggestion" -> deleteTextSuggestion(call, result)
            "listTextSuggestion" -> listTextSuggestion(call, result)
            "addRecord" -> addRecord(call, result)
            "updateRecord" -> updateRecord(call, result)
            "deleteRecord" -> deleteRecord(call, result)
            "listRecord" -> listRecord(call, result)
            else -> result.notImplemented()
        }
    }

    /* 
        Verify which connection type is currently being used

        Return Int (0 = not connected, 1 - Wi-fi, 2 - Mobile)
    */
    fun getConnectionType(result: Result) {
        val connectivityManager : ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var connection : Network? = connectivityManager.activeNetwork
        var capabilities : NetworkCapabilities? = connectivityManager.getNetworkCapabilities(connection)

        if (connection != null && capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> result.success(1)
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> result.success(2)
                else -> result.success(0)
            }
        } else {
            result.success(0)
        }
    }

    /* 
        Initialize Realm

        Return Bool
    */
    fun init(result: Result) {
        Realm.init(context)
        app = App(AppConfiguration.Builder(appId).build())

        if (isLoggedIn()) {
            Log.d("gdc", Realm.getDefaultConfiguration()?.path + ":(")
            result.success(true)
        } else {
            result.success(false)
        }
    }

    /* 
        Stop realm sync (data will be saved only on device)

        Return Bool
    */
    fun stopSync(result: Result) {
        if (sessionInit() != null) {
            try { 
                app.sync.getSession(SyncConfiguration.Builder(app.currentUser(), app.currentUser()!!.id).build()).stop()
                result.success(true)
            } catch (e : Exception) {
                result.success(false)
            }
        } else {
            result.success(false)
        }
    }

    /* 
        Restart realm sync (data will be saved on device and cloud)

        Return Bool
    */
    fun startSync(result: Result) {
        if (sessionInit() != null) {
            try {
                val syncSession = app.sync.getOrCreateSession(SyncConfiguration.Builder(app.currentUser(), app.currentUser()!!.id).build());
                syncSession.start();
                DownloadChanges(syncSession);
                UploadChanges(syncSession);
                result.success(true)
            } catch (e : Exception) {
                result.success(false)
            }
        } else {
            result.success(false)
        }
    }

    /* 
        Register a realm user

        Parameters
            email : String
            password : String

        Return Bool
    */
    fun register(call: MethodCall, result: Result) {
        val username: String? = call.argument("username")
        val password: String? = call.argument("password")

        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            result.error("403", "Nenhuma credencial informada", "Informe email e senha do usuario")
        } else {
            app.emailPassword.registerUserAsync(username, password) {
                if (it.isSuccess) {
                    result.success(true)
                } else {
                    if (it.error.errorCode == ErrorCode.NETWORK_IO_EXCEPTION) {
                        result.error("408","Sem conexão", "Não há conexão com a internet")
                    } else {
                        result.success(false)
                    }
                }
            }
        }
    }

    /* 
        Returns if the are any users logged in on device

        Return Bool
    */
    fun isLoggedIn() : Boolean {
        return app.currentUser() != null
    }

    /* 
        Log in on realm

        Parameters
            email : String
            password : String

        Return Bool
    */
    fun logIn(call: MethodCall, result: Result) {
        val username: String? = call.argument("username")
        val password: String? = call.argument("password")
        Log.d("usuario", username + " olhaai");
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            result.error("403", "Nenhuma credencial informada", "Informe email e senha do usuario")
        } else {
            app.loginAsync(
                Credentials.emailPassword(
                    username,
                    password
                )
            ) {
                if (it.isSuccess) {
                    Log.d("Usuario atual", app.currentUser()?.id + " :|")
                    Realm.setDefaultConfiguration(SyncConfiguration.Builder(app.currentUser(), app.currentUser()!!.id).build())
                    result.success(sessionInit() != null)
                } else {
                    Log.d("Erro Login", it.error.toString());
                    if (it.error.errorCode == ErrorCode.NETWORK_IO_EXCEPTION) {
                        result.error("408","Sem conexão", "Não há conexão com a internet")
                    } else {
                        Log.d("erroLogin", it.error.toString())
                        result.success(false)
                    }
                }
            }
        }
    }

    /* 
        Log out from realm

        Return Bool
    */
    fun logOut(result: Result) {
        app.currentUser()?.logOutAsync() {
            if (it.isSuccess) {
                Realm.removeDefaultConfiguration();
                result.success(true)
            } else {
                Log.d("erroLogout", it.error.toString())
                Log.d("logout", it.error.toString())
                result.success(false)
            }
        }
    }

    /*
        TODO: Log in on realm using google account

        Parameters
            authToken : String

        Return Bool
    */
    fun logInGoogle(call: MethodCall, result: Result) {
        val token: String? = call.argument("token")
        val method: String? = call.argument("method")
        if (!token.isNullOrBlank()){
            Log.d("Token", token)
            if (method == "id") {
                Log.d("id",":D")
                app.loginAsync(Credentials.google(token, GoogleAuthType.ID_TOKEN)) {
                    if (it.isSuccess) {
                        sessionInit()
                        val partitionKey = app.currentUser()?.id
                        result.success(true)
                    } else {
                        if (it.error.errorCode == ErrorCode.NETWORK_IO_EXCEPTION) {
                            result.error("408","Sem conexão", "Não há conexão com a internet")
                        } else {
                            result.success(false)
                        }
                    }
                }
            } else {
                Log.d("o outro",":D")
                app.loginAsync(Credentials.google(token, GoogleAuthType.AUTH_CODE)) {
                    if (it.isSuccess) {
                        sessionInit()
                        val partitionKey = app.currentUser()?.id
                        result.success(true)
                    } else {
                        if (it.error.errorCode == ErrorCode.NETWORK_IO_EXCEPTION) {
                            result.error("408","Sem conexão", "Não há conexão com a internet")
                        } else {
                            result.success(false)
                        }
                    }
                }
            }
            // app.emailPassword.regi
            //     .requestEmail()
            //     .build()
            // val googleSignInClient = GoogleSignIn.getClient(this, gso)
            // val signInIntent: Intent = googleSignInClient.signInIntent
            // startActivityForResult(signInIntent, RC_SIGN_IN) 
        } else {
            result.success(false)
        }
    }


    /* 
        Add customer

        Parameters
            firstName : String
            lastName : String
            avatar : List<Int>? - list of int parsed bytes
            birthday : Long? - date in unix timestamp
            email : String?
            observation: String?
            phone : String?
            sex : String?
            socialName : String

        Return String (new Customer id)
    */
    fun addCustomer(call: MethodCall, result: Result) {
        Log.d("addcust", "initou")
        if (sessionInit() != null) {
            val realmThread = Realm.getInstance(sessionInit()!!)
            val partitionKey = app.currentUser()?.id.toString()
            var cust: customer = customer()
            
            val first_name: String? = call.argument("firstName")
            val last_name: String? = call.argument("lastName")
            val avatar: List<Int>? = call.argument("avatar")
            val avatarByte: ArrayList<Byte> = arrayListOf()
            val birthday: Long? = call.argument("birthday")
            val email: String? = call.argument("email")
            val observation: String? = call.argument("observation")
            val phone: String? = call.argument("phone")
            val sex: String? = call.argument("sex")
            val social_name: String? = call.argument("socialName")

            realmThread.executeTransactionAsync( Realm.Transaction { transactionRealm ->
                cust.`_id` = ObjectId()
                cust.`_partition` = partitionKey
                cust.first_name = first_name!!
                cust.last_name = last_name!!
                if (avatar != null) {
                    avatar.forEach {
                        avatarByte.add(it.toByte())
                    }
                    cust.avatar = avatarByte.toByteArray()
                }
                if (birthday != null) cust.birthday = Date(birthday)
                cust.email = email
                cust.observation = observation
                cust.phone = phone
                cust.sex = sex
                cust.social_name = social_name

                transactionRealm.insert(cust) 
            }, Realm.Transaction.OnSuccess {
                Log.d("onSuccess","teste")
                realmThread.close()
                result.success(cust.`_id`.toString())
            }, Realm.Transaction.OnError { e ->
                Log.d("onError", "teste")
                Log.d("onError", e.message!!)
                realmThread.close()
                result.success("")
            })
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("addcust", "acabou de inita")
    }

    /* 
        List customer

        Parameters
            campo : String? - field to filter
            logicalOperator : String? - filter method
            valor : Any? - value to filter

        Return List<Customer>
    */
    fun listCustomer(call: MethodCall, result: Result) {
        Log.d("listcust", "initou")
        if (sessionInit() != null) {
            val realm = Realm.getInstance(sessionInit()!!)
            var list = arrayListOf<Any>()
            val campo: String? = call.argument("campo")
            lateinit var listResult: RealmResults<customer>
            
            if (campo.isNullOrBlank()) {
                listResult = realm.where<customer>().findAll()
            } else {
                listResult = filterResults(call, realm.where<customer>()) as RealmResults<customer>
            }

            listResult.forEach {
                var avatarByte = it.avatar
                val avatarInt = arrayListOf<Any>()
                
                if (avatarByte != null) {
                    val avatarByteList = avatarByte.toList()
                    avatarByteList.forEach {
                        avatarInt.add(it.toInt())
                    }
                }
                list.add(hashMapOf(
                    "id" to it.`_id`.toString(),
                    "firstName" to it.first_name,
                    "lastName" to it.last_name,
                    "avatar" to avatarInt,
                    "birthday" to it.birthday?.time,
                    "email" to it.email,
                    "observation" to it.observation,
                    "phone" to it.phone,
                    "sex" to it.sex,
                    "socialName" to it.social_name
                ))
            }

            result.success(list)
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("listcust", "cabou de initar")
    }

    /* 
        Update customer

        Parameters
            id : String - customer indentifier
            firstName : String?
            lastName : String?
            avatar : List<Int>? - list of int parsed bytes
            birthday : Long? - date in unix timestamp
            email : String?
            observation: String?
            phone : String?
            sex : String?
            socialName : String?

        Return Bool
    */
    fun updateCustomer(call: MethodCall, result: Result) {
        Log.d("updatecust", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)
                
                val first_name: String? = call.argument("firstName")
                val last_name: String? = call.argument("lastName")
                val avatar: List<Int>? = call.argument("avatar")
                val avatarByte: ArrayList<Byte> = arrayListOf()
                val birthday: Long? = call.argument("birthday")
                val email: String? = call.argument("email")
                val observation: String? = call.argument("observation")
                val phone: String? = call.argument("phone")
                val sex: String? = call.argument("sex")
                val social_name: String? = call.argument("socialName")

                realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                    var cust: customer? = transactionRealm.where<customer>().equalTo("_id", ObjectId(id)).findFirst()
                    if (cust != null) {
                        if (!first_name.isNullOrBlank()) cust.first_name = first_name
                        if (!last_name.isNullOrBlank()) cust.last_name = last_name 
                        if (avatar != null) {
                            avatar.forEach {
                                avatarByte.add(it.toByte())
                            }
                            cust.avatar = avatarByte.toByteArray()
                        }
                        if (birthday != null) cust.birthday = Date(birthday)
                        if (!email.isNullOrBlank()) cust.email = email
                        if (!observation.isNullOrBlank()) cust.observation = observation
                        if (!phone.isNullOrBlank()) cust.phone = phone
                        if (!sex.isNullOrBlank()) cust.sex = sex
                        if (!social_name.isNullOrBlank()) cust.social_name = social_name

                        transactionRealm.insertOrUpdate(cust)
                    } else {
                        transactionRealm.close()
                        result.success(false)
                    } 
                }, Realm.Transaction.OnSuccess {
                    Log.d("onSuccess","teste")
                    realmThread.close()
                    result.success(true)
                }, Realm.Transaction.OnError { e ->
                    Log.d("onError", "teste")
                    Log.d("onError", e.message!!)
                    realmThread.close()
                    result.success(false)
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("updatecust", "acabou de inita")
    }

    /* 
        Delete customer

        Parameters
            id : String - customer indentifier

        Return Bool
    */
    fun deleteCustomer(call: MethodCall, result: Result) {
        Log.d("deletecust", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)

                realmThread.executeTransaction(Realm.Transaction { transactionRealm ->
                    var cust: customer? = transactionRealm.where<customer>().equalTo("_id", ObjectId(id)).findFirst()
                    if (cust != null) {
                        try {
                            cust.deleteFromRealm()
                            result.success(true)
                        } catch (e: Exception) {
                            Log.d("erro", e.toString())
                            result.success(false)
                        }
                    } else {
                        result.success(false)
                    }
                    transactionRealm.close()
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("deletecust", "acabou de inita")
    }



    /* 
        Add appointment

        Parameters
            date : Long - date in unix timestamp
            duration : Long
            customer : String? - customer indentifier
            observation : String?
            status : String?

        Return String (new Appointment id)
    */
    fun addAppointment(call: MethodCall, result: Result) {
        Log.d("addapp", "initou")
        if (sessionInit() != null) {
            val realmThread = Realm.getInstance(sessionInit()!!)
            val partitionKey = app.currentUser()?.id.toString()
            var appoint: appointment = appointment()

            val customer: String? = call.argument("customer")
            val date_time: Long? = call.argument("date")
            val observation: String? = call.argument("observation")
            var duration: Long?
            try {
                duration = call.argument("duration")
            } catch (e: Exception) {
                val durationInt: Int? = call.argument("duration")
                duration = durationInt!!.toLong()
            }
            val status: String? = call.argument("status")

            realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                appoint.`_partition` = partitionKey
                appoint.`_id` = ObjectId()
                if (!customer.isNullOrBlank()) appoint.customer = transactionRealm.where<customer>().equalTo("_id", ObjectId(customer)).findFirst()
                appoint.date_time = Date(date_time!!)
                appoint.duration = duration!!.toLong()
                appoint.observation = observation
                appoint.status = status

                transactionRealm.insert(appoint) 
            }, Realm.Transaction.OnSuccess {
                Log.d("onSuccess","teste")
                realmThread.close()
                result.success(appoint.`_id`.toString())
            }, Realm.Transaction.OnError { e ->
                Log.d("onError", "teste")
                Log.d("onError", e.message!!)
                realmThread.close()
                result.success("")
            })
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        
        Log.d("addapp", "cabou de initar")
    }

    /* 
        List appointment

        Parameters
            campo : String? - field to filter
            logicalOperator : String? - filter method
            valor : Any? - value to filter

        Return List<Appointment>
    */
    fun listAppointment(call: MethodCall, result: Result) {
        Log.d("listapp", "initou")
        if (sessionInit() != null) {
            val realm = Realm.getInstance(sessionInit()!!)
            var list = arrayListOf<Any>()
            val campo: String? = call.argument("campo")
            lateinit var listResult: RealmResults<appointment>
            if (campo.isNullOrBlank()) {
                listResult = realm.where<appointment>().findAll()
            } else {
                listResult = filterResults(call, realm.where<appointment>()) as RealmResults<appointment>
            }

            listResult.forEach {
                list.add(hashMapOf(
                    "id" to it.`_id`.toString(),
                    "customer" to it.customer?.`_id`.toString(),
                    "date" to it.date_time.toString(),
                    "duration" to it.duration,
                    "observation" to it.observation,
                    "status" to it.status
                ))
            }

            result.success(list)
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("listapp", "cabou de initar")
    }

    /* 
        Update appointment

        Parameters
            id : String - appointment indentifier
            date : Long? - date in unix timestamp
            duration : Long?
            customer : String? - customer indentifier
            observation : String?
            status : String?

        Return Bool
    */
    fun updateAppointment(call: MethodCall, result: Result) {
        Log.d("updateappoint", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)
                
                val customer: String? = call.argument("customer")
                val date_time: Long? = call.argument("date")
                val observation: String? = call.argument("observation")
                var duration: Long?
                try {
                    duration = call.argument("duration")
                } catch (e: Exception) {
                    val durationInt: Int? = call.argument("duration")
                    duration = durationInt!!.toLong()
                }
                val status: String? = call.argument("status")

                realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                    var appoint: appointment? = transactionRealm.where<appointment>().equalTo("_id", ObjectId(id)).findFirst()
                    if (appoint != null) { 
                        if (!customer.isNullOrBlank()) appoint.customer = transactionRealm.where<customer>().equalTo("_id", ObjectId(customer)).findFirst()
                        if (date_time != null) appoint.date_time = Date(date_time)
                        if (!observation.isNullOrBlank()) appoint.observation = observation
                        if (duration != null) appoint.duration = duration.toLong()
                        if (!status.isNullOrBlank()) appoint.status = status

                        transactionRealm.insertOrUpdate(appoint)
                    } else {
                        transactionRealm.close()
                        result.success(false)
                    } 
                }, Realm.Transaction.OnSuccess {
                    Log.d("onSuccess","teste")
                    realmThread.close()
                    result.success(true)
                }, Realm.Transaction.OnError { e ->
                    Log.d("onError", "teste")
                    Log.d("onError", e.message!!)
                    realmThread.close()
                    result.success(false)
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("updateappoint", "acabou de inita")
    }

    /* 
        Delete appointment

        Parameters
            id : String - appointment indentifier

        Return Bool
    */
    fun deleteAppointment(call: MethodCall, result: Result) {
        Log.d("deleteappoint", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)

                realmThread.executeTransaction(Realm.Transaction { transactionRealm ->
                    var appoint: appointment? = transactionRealm.where<appointment>().equalTo("_id", ObjectId(id)).findFirst()
                    if (appoint != null) {
                        try {
                            appoint.deleteFromRealm()
                            result.success(true)
                        } catch (e: Exception) {
                            Log.d("erro", e.toString())
                            result.success(false)
                        }
                    } else {
                        result.success(false)
                    }
                    transactionRealm.close()
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("deleteappoint", "acabou de inita")
    }



    /* 
        Add record

        Parameters
            dateTime : long - date in unix timestamp
            description : String
            source : String
            customer : String? - customer indentifier
            contentText : String?
            contentBin : List<Int>? - list of int parsed bytes
            tags : List<String>?

        Return String (new Record id)
    */
    fun addRecord(call: MethodCall, result: Result) {
        Log.d("addrecor", "intou")
        if (sessionInit() != null) {
            val realmThread = Realm.getInstance(sessionInit()!!)
            val partitionKey = app.currentUser()?.id.toString()
            var recor: record = record()

            val date: Long? = call.argument("dateTime")
            val description: String? = call.argument("description")
            val source: String? = call.argument("source")
            val tags: List<String>? = call.argument("tags")
            val realm_tags: RealmList<String> = RealmList()
            val content_text: String? = call.argument("contentText")
            val content_bin: List<Int>? = call.argument("contentBin")
            val content_bin_byte: ArrayList<Byte> = arrayListOf()
            val customer: String? = call.argument("customer")

            realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                tags?.forEach {
                    realm_tags.add(it)
                }
    
                recor.`_id` = ObjectId()
                recor.`_partition` = partitionKey
                recor.date_time = Date(date!!)
                recor.description = description!!
                recor.source = source!!
                recor.tags = realm_tags
                if (content_bin != null) {
                    content_bin.forEach {
                        content_bin_byte.add(it.toByte())
                    }
                    
                    var content: record_content = record_content()
                    content.binary = content_bin_byte.toByteArray()
                    if (!content_text.isNullOrBlank()) content.text = content_text 
                    
                    recor.content = content
                }
                if (!customer.isNullOrBlank()) {
                    recor.customer = transactionRealm.where<customer>().equalTo("_id", ObjectId(customer)).findFirst()
                }

                transactionRealm.insert(recor) 
            }, Realm.Transaction.OnSuccess {
                Log.d("onSuccess","teste")
                realmThread.close()
                result.success(recor.`_id`.toString())
            }, Realm.Transaction.OnError { e ->
                Log.d("onError", "teste")
                Log.d("onError", e.message!!)
                realmThread.close()
                result.success("")
            })
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("addrecor", "acabou de inita")
    }

    /* 
        List record

        Parameters
            campo : String? - field to filter
            logicalOperator : String? - filter method
            valor : Any? - value to filter

        Return List<Record>
    */
    fun listRecord(call: MethodCall ,result: Result) {
        Log.d("listrecor", "initou")
        if (sessionInit() != null) {
            val realm = Realm.getInstance(sessionInit()!!)
            var list = arrayListOf<Any>()
            val campo: String? = call.argument("campo")
            lateinit var listResult: RealmResults<record>
            
            if (campo.isNullOrBlank()) {
                listResult = realm.where<record>().findAll()
            } else {
                listResult = filterResults(call, realm.where<record>()) as RealmResults<record>
            }

            listResult.forEach {
                var avatarByte: List<Byte>? = null
                val content = it.content

                if (content != null){
                    val bin  = content.binary
                    if (bin != null) {
                        avatarByte = bin.toList()
                        val avatarInt = arrayListOf<Any>()
                        avatarByte.forEach {
                            avatarInt.add(it.toInt())
                        }
                    }
                }

                list.add(hashMapOf(
                    "id" to it.`_id`.toString(),
                    "customer" to it.customer?.first_name,
                    "description" to it.description,
                    "tags" to it.tags,
                    "date_time" to it.date_time.toString(),
                    "soucer" to it.source,
                    "content_binary" to avatarByte,
                    "content_text" to content?.text
                ))
            }

            result.success(list)
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("listrecor", "cabou de initar")
    }

    /* 
        Update record

        Parameters
            id : String - record indentifier
            dateTime : long? - date in unix timestamp
            description : String?
            source : String?
            customer : String? - customer indentifier
            contentText : String?
            contentBin : List<Int>? - list of int parsed bytes
            tags : List<String>?

        Return Bool
    */
    fun updateRecord(call: MethodCall, result: Result) {
        Log.d("updaterecor", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)
                
                val date: Long? = call.argument("dateTime")
                val description: String? = call.argument("description")
                val source: String? = call.argument("source")
                val tags: List<String>? = call.argument("tags")
                val realm_tags: RealmList<String> = RealmList()
                val content_text: String? = call.argument("contentText")
                val content_bin: List<Int>? = call.argument("contentBin")
                val content_bin_byte: ArrayList<Byte> = arrayListOf()
                val customer: String? = call.argument("customer")

                realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                    var recor: record? = transactionRealm.where<record>().equalTo("_id", ObjectId(id)).findFirst()
                    if (recor != null) {
                        if (date != null) recor.date_time = Date(date)
                        if (!description.isNullOrBlank()) recor.description = description
                        if (!source.isNullOrBlank()) recor.source = source
                        if (tags != null) {
                            tags.forEach {
                                realm_tags.add(it)
                            }

                            recor.tags = realm_tags
                        }
                        if (content_bin != null) {
                            content_bin.forEach {
                                content_bin_byte.add(it.toByte())
                            }
                            
                            var content: record_content = record_content()
                            content.binary = content_bin_byte.toByteArray()
                            if (!content_text.isNullOrBlank()) content.text = content_text 
                            
                            recor.content = content
                        }
                        if (!customer.isNullOrBlank()) {
                            recor.customer = transactionRealm.where<customer>().equalTo("_id", ObjectId(customer)).findFirst()
                        } 

                        transactionRealm.insertOrUpdate(recor)
                    } else {
                        transactionRealm.close()
                        result.success(false)
                    } 
                }, Realm.Transaction.OnSuccess {
                    Log.d("onSuccess","teste")
                    realmThread.close()
                    result.success(true)
                }, Realm.Transaction.OnError { e ->
                    Log.d("onError", "teste")
                    Log.d("onError", e.message!!)
                    realmThread.close()
                    result.success(false)
                    })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("updaterecor", "acabou de inita")
    }

    /* 
        Delete record

        Parameters
            id : String - record indentifier

        Return Bool
    */
    fun deleteRecord(call: MethodCall, result: Result) {
        Log.d("deleterecor", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)
                realmThread.executeTransaction(Realm.Transaction { transactionRealm ->
                    var recor: record? = transactionRealm.where<record>().equalTo("_id", ObjectId(id)).findFirst()
                    if (recor != null) {
                        try {
                            recor.deleteFromRealm()
                            result.success(true)
                        } catch (e: Exception) {
                            Log.d("erro", e.toString())
                            result.success(false)
                        }
                    } else {
                        result.success(false)
                    }
                    transactionRealm.close()
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("deleterecor", "acabou de inita")
    }



    /* 
        Add configuration

        Parameters
            email : String
            firstName : String
            lastName : String
            language : String
            timezone : String
            subscription : List<List<String>>? - Stripe subscription level
            socialName : String?

        Return String (new Configuration id)
    */
    fun addConfiguration(call: MethodCall, result: Result) {
        Log.d("addconfig", "intou")
        if (sessionInit() != null) {
            val partitionKey = app.currentUser()?.id.toString()
            val realmThread = Realm.getInstance(sessionInit()!!)
            var confi: configuration = configuration()

            val email: String? = call.argument("email")
            val first_name: String? = call.argument("firstName")
            val last_name: String? = call.argument("lastName")
            val language: String? = call.argument("language")
            val subscription: List<List<String>>? = call.argument("subscription")
            val realm_list: RealmList<configuration_subscription> = RealmList()
            val timezone: String? = call.argument("timezone")
            val social_name: String? = call.argument("socialName")

            realmThread.executeTransactionAsync( Realm.Transaction { transactionRealm ->
                subscription?.forEach {
                    realm_list.add(
                        configuration_subscription(it[0],it[1])
                    )
                }
    
                confi.`_id` = ObjectId()
                confi.`_partition` = partitionKey
                confi.email = email!!
                confi.first_name = first_name!!
                confi.last_name = last_name!!
                confi.language = language!!
                confi.timezone = timezone!!
                confi.subscription = realm_list
                confi.social_name = social_name

                transactionRealm.insert(confi) 
            }, Realm.Transaction.OnSuccess {
                Log.d("onSuccess","teste")
                realmThread.close()
                result.success(confi.`_id`.toString())
            }, Realm.Transaction.OnError { e ->
                Log.d("onError", "teste")
                Log.d("onError", e.message!!)
                realmThread.close()
                result.success("")
            })
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("addconfig", "acabou de inita")
    }

    /* 
        List configuration

        Parameters
            campo : String? - field to filter
            logicalOperator : String? - filter method
            valor : Any? - value to filter

        Return List<Configuration>
    */
    fun listConfiguration(call: MethodCall ,result: Result) {
        Log.d("listconfig", "initou")
        if (sessionInit() != null) {
            val realm = Realm.getInstance(sessionInit()!!)
            var list = arrayListOf<Any>()
            val campo: String? = call.argument("campo")
            lateinit var listResult: RealmResults<configuration>
            
            if (campo.isNullOrBlank()) {
                listResult = realm.where<configuration>().findAll()
            } else {
                listResult = filterResults(call, realm.where<configuration>()) as RealmResults<configuration>
            }

            listResult.forEach {
                var subscription: ArrayList<List<String>> = arrayListOf()
                it.subscription.forEach {
                    subscription.add(listOf(it.id, it.name))
                }
                list.add(hashMapOf(
                    "id" to it.`_id`.toString(),
                    "email" to it.email,
                    "first_name" to it.first_name,
                    "last_name" to it.last_name,
                    "language" to it.language,
                    "timezone" to it.timezone,
                    "social_name" to it.social_name,
                    "subscription" to subscription
                ))
            }

            result.success(list)
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("listconfig", "cabou de initar")
    }

    /* 
        Update configuration

        Parameters
            id : String - configuration indentifier
            email : String?
            firstName : String?
            lastName : String?
            language : String?
            timezone : String?
            subscription : List<List<String>>? - Stripe subscription level
            socialName : String?

        Return Bool
    */
    fun updateConfiguration(call: MethodCall, result: Result) {
        Log.d("updateconfig", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)

                val email: String? = call.argument("email")
                val first_name: String? = call.argument("firstName")
                val last_name: String? = call.argument("lastName")
                val language: String? = call.argument("language")
                val subscription: List<List<String>>? = call.argument("subscription")
                val realm_list: RealmList<configuration_subscription> = RealmList()
                val timezone: String? = call.argument("timezone")
                val social_name: String? = call.argument("socialName")

                realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                    var confi: configuration? = transactionRealm.where<configuration>().equalTo("_id", ObjectId(id)).findFirst()
                    if (confi != null) {
                        if (!email.isNullOrBlank()) confi.email = email
                        if (!first_name.isNullOrBlank()) confi.first_name = first_name
                        if (!last_name.isNullOrBlank()) confi.last_name = last_name
                        if (!language.isNullOrBlank()) confi.language = language
                        if (!timezone.isNullOrBlank()) confi.timezone = timezone
                        if (subscription != null) {
                            subscription.forEach {
                                realm_list.add(
                                    configuration_subscription(it[0],it[1])
                                )
                            }
                            confi.subscription = realm_list
                        }
                        if (!social_name.isNullOrBlank()) confi.social_name = social_name 

                        transactionRealm.insertOrUpdate(confi)
                    } else {
                        transactionRealm.close()
                        result.success(false)
                    }
                }, Realm.Transaction.OnSuccess {
                    Log.d("onSuccess","teste")
                    realmThread.close()
                    result.success(true)
                }, Realm.Transaction.OnError { e ->
                    Log.d("onError", "teste")
                    Log.d("onError", e.message!!)
                    realmThread.close()
                    result.success(false)
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("updateconfig", "acabou de inita")
    }

    /* 
        Delete configuration

        Parameters
            id : String - configuration indentifier

        Return Bool
    */
    fun deleteConfiguration(call: MethodCall, result: Result) {
        Log.d("deleteconfig", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)

                realmThread.executeTransaction(Realm.Transaction { transactionRealm ->
                    var confi: configuration? = transactionRealm.where<configuration>().equalTo("_id", ObjectId(id)).findFirst()
                    if (confi != null) {
                        try {
                            confi.deleteFromRealm()
                            result.success(true)
                        } catch (e: Exception) {
                            Log.d("erro", e.toString())
                            result.success(false)
                        }
                    } else {
                        result.success(false)
                    }
                    transactionRealm.close()
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("deleteconfig", "acabou de inita")
    }



    /* 
        Add text suggestion

        Parameters
            from : String?
            to : String?
            counter : Long?

        Return String (new TextSuggestion id)
    */
    fun addTextSuggestion(call: MethodCall, result: Result) {
        Log.d("addsuggestion", "intou")
        if (sessionInit() != null) {
            val realmThread = Realm.getInstance(sessionInit()!!)
            val partitionKey = app.currentUser()?.id.toString()
            var textsugg: text_suggestion = text_suggestion()
            val from: String? = call.argument("from")
            val to: String? = call.argument("to")
            var counter: Long?
            try {
                counter = call.argument("counter")
            } catch (e: Exception) {
                val counterInt: Int? = call.argument("counter")
                counter = counterInt!!.toLong()
            }

            realmThread.executeTransactionAsync( Realm.Transaction { transactionRealm ->               
                textsugg.`_id` = ObjectId()
                textsugg.`_partition` = partitionKey
                textsugg.from = from!!
                textsugg.to = to!!
                textsugg.counter = counter!!.toLong()

                transactionRealm.insert(textsugg) 
            }, Realm.Transaction.OnSuccess {
                Log.d("onSuccess","teste")
                realmThread.close()
                result.success(textsugg.`_id`.toString())
            }, Realm.Transaction.OnError { e ->
                Log.d("onError", "teste")
                Log.d("onError", e.message!!)
                realmThread.close()
                result.success("")
            })
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("addsuggestion", "acabou de inita")
    }

    /* 
        List text suggestion

        Parameters
            campo : String? - field to filter
            logicalOperator : String? - filter method
            valor : Any? - value to filter

        Return List<TextSuggestion>
    */
    fun listTextSuggestion(call: MethodCall ,result: Result) {
        Log.d("listsuggestion", "initou")
        if (sessionInit() != null) {
            val realm = Realm.getInstance(sessionInit()!!)
            var list = arrayListOf<Any>()
            val campo: String? = call.argument("campo")
            lateinit var listResult: RealmResults<text_suggestion>
            
            if (campo.isNullOrBlank()) {
                listResult = realm.where<text_suggestion>().findAll()
            } else {
                listResult = filterResults(call, realm.where<text_suggestion>()) as RealmResults<text_suggestion>
            }

            listResult.forEach {
                list.add(hashMapOf(
                    "id" to it.`_id`.toString(),
                    "from" to it.from,
                    "to" to it.to,
                    "counter" to it.counter
                ))
            }

            result.success(list)
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("listsuggestion", "cabou de initar")
    }

    /* 
        Update text suggestion

        Parameters
            id : String - text suggestion indentifier
            from : String?
            to : String?
            counter : Long?

        Return Bool
    */
    fun updateTextSuggestion(call: MethodCall, result: Result) {
        Log.d("updatesuggestion", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)

                val from: String? = call.argument("from")
                val to: String? = call.argument("to")
                var counter: Long?
                try {
                    counter = call.argument("counter")
                } catch (e: Exception) {
                    val counterInt: Int? = call.argument("counter")
                    counter = counterInt!!.toLong()
                }

                realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                    var textsugg: text_suggestion? = transactionRealm.where<text_suggestion>().equalTo("_id", ObjectId(id)).findFirst()
                    if (textsugg != null) {
                        if (!from.isNullOrBlank()) textsugg.from = from
                        if (!to.isNullOrBlank()) textsugg.to = to
                        if (counter != null) textsugg.counter = counter.toLong()
                        transactionRealm.insertOrUpdate(textsugg)
                    } else {
                        transactionRealm.close()
                        result.success(false) 
                    } 
                }, Realm.Transaction.OnSuccess {
                    Log.d("onSuccess","teste")
                    realmThread.close()
                    result.success(true)
                }, Realm.Transaction.OnError { e ->
                    Log.d("onError", "teste")
                    Log.d("onError", e.message!!)
                    realmThread.close()
                    result.success(false)
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("updatesuggestion", "acabou de inita")
    }

    
    /* 
        Delete text suggestion

        Parameters
            id : String - text suggestion indentifier

        Return Bool
    */
    fun deleteTextSuggestion(call: MethodCall, result: Result) {
        Log.d("deletesuggestion", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(sessionInit()!!)

                realmThread.executeTransaction(Realm.Transaction { transactionRealm ->
                    var textsugg: text_suggestion? = transactionRealm.where<text_suggestion>().equalTo("_id", ObjectId(id)).findFirst()
                    if (textsugg != null) {
                        try {
                            textsugg.deleteFromRealm()
                            result.success(true)
                        } catch (e: Exception) {
                            Log.d("erro", e.toString())
                            result.success(false)
                        }
                    } else {
                        result.success(false)
                    }
                    transactionRealm.close()
                })
            }
        } else {
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario")
        }
        Log.d("deletesuggestion", "acabou de inita")
    }


    /* 
        Filter results

        Parameters
            MethodCall - where the parameters are stored
                campo : String - field to filter
                logicalOperator : String - filter method
                valor : Any? - value to filter
            RealmQuery<Any> - results to be filtered 

        Return RealmResults<Any>
    */
    fun filterResults(call: MethodCall, query: RealmQuery<*>) : RealmResults<*> {
        val campo: String? = call.argument("campo")
        val operadorLogico: String? = call.argument("logicalOperator")
        
        Log.d("Campo", "D: " + campo)
        Log.d("OperadorLógico", "D: " + operadorLogico)

        if (campo.isNullOrBlank()) return query.findAll()
        
        try {
            when(operadorLogico) {
                "equals" -> {
                    val valor: String? = call.argument("valor")
                    Log.d("Valor", "D: " + valor)
                    if (!valor.isNullOrBlank()) {
                        if (objectIdProperties.contains(campo)) return query.equalTo(campo, ObjectId(valor)).findAll()

                        return query.equalTo(campo, valor).findAll()
                    }
                }
                "like" -> {
                    val valor: String? = call.argument("valor")
                    Log.d("Valor", "D: " + valor)
                    if (!valor.isNullOrBlank()) {
                        return query.like(campo, valor).findAll()
                    }
                }
                "greater" -> {
                    val valor: Int? = call.argument("valor")
                    Log.d("Valor", "D: " + valor.toString())
                    if (valor != null) {
                        return query.greaterThan(campo, valor).findAll()
                    }
                }
                "lesser" -> {
                    val valor: Int? = call.argument("valor")
                    Log.d("Valor", "D: " + valor.toString())
                    if (valor != null) {
                        return query.lessThan(campo, valor).findAll()
                    }
                }
                "beetwen" -> {
                    val valor: List<Int>? = call.argument("valor")
                    Log.d("Valor", "D: " + valor.toString())
                    if (valor != null && !valor.isEmpty()) {
                        return query.between(campo, valor[0], valor[1]).findAll()
                    }
                }
                "notNull" -> {
                    return query.isNotNull(campo).findAll()
                }
                // "inList" -> {
                //     val valor: List<String>? = call.argument("valor")
                //     if (valor != null && !valor.isEmpty()) {
                //         return query.`in`(campo, valor).findAll()
                //     }
                // }
                else -> return query.findAll()
            }
            return query.findAll()
        } catch (E : Exception) {
            Log.d("Erro", E.toString())
            return query.findAll()
        }
    }

    /* 
        Get the current RealmConfiguration

        Return RealmConfiguration|null
    */
    fun sessionInit() : RealmConfiguration? {
        if (!isLoggedIn())
            return null
            
        if (Realm.getDefaultConfiguration() == null)
            Realm.setDefaultConfiguration(SyncConfiguration.Builder(app.currentUser(), app.currentUser()!!.id).build())

        return Realm.getDefaultConfiguration()
    }

    /* 
        Download all server changes in a sub Thread

        Parameters
            session : SyncSession - current sync session 

        Return Runnable
    */
    class DownloadChanges(val session: SyncSession) : Runnable {
        override fun run() {
            session.downloadAllServerChanges()
        }
    }

    /* 
        Upload all local changes in a sub Thread

        Parameters
            SyncSession - current sync session 

        Return Runnable
    */
    class UploadChanges(val session: SyncSession) : Runnable {
        override fun run() {
            session.uploadAllLocalChanges()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}