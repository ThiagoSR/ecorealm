package com.ninsaude.ecorealm

import com.ninsaude.ecorealm.customer
import com.ninsaude.ecorealm.appointment
import com.ninsaude.ecorealm.configuration
import com.ninsaude.ecorealm.record
import com.ninsaude.ecorealm.record_content
import com.ninsaude.ecorealm.text_suggestion

import androidx.annotation.NonNull
import android.content.Context

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
    private var partitionKey: String? = null
    private var config: SyncConfiguration? = null
    private var appId: String = "ecodoc-wgion"

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "ecorealm")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.getApplicationContext()
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.setLogLevel(Log.DEBUG);
        when (call.method) {
            "init" -> init(result)
            "logIn" -> logIn(call, result)
            "googleLogIn" -> googleLogIn(call, result)
            "logOut" -> logOut(result)
            "upload" -> upload(result)
            "download" -> download(result)
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

    // Instancia o realm e define a partition key e retorna se o usuario está logado
    fun init(result: Result) {
        Log.d("init", "initou");
        Realm.init(context)
        app = App(AppConfiguration.Builder(appId).build())
        partitionKey = app.currentUser()?.id
        result.success(app.currentUser() != null)
        Log.d("init", "cabou de initar");
    }

    fun sessionInit() : SyncConfiguration? {
        Log.d("session", "initou");
        if (app.currentUser() == null) 
            return null

        if (config == null) { 
            if (partitionKey.isNullOrEmpty()) partitionKey = app.currentUser()?.id.toString()
            config = SyncConfiguration.Builder(app.currentUser(), partitionKey)
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .build()
        }
        
        Log.d("session", "cabou de initar");
        return config;
    }

    fun logIn(call: MethodCall, result: Result) {
        Log.d("login", "initou");
        val username: String? = call.argument("username")
        val password: String? = call.argument("password")

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
                    sessionInit()
                    partitionKey = app.currentUser()?.id
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
        Log.d("login", "cabou de initar");
    }

    fun logOut(result: Result) {
        Log.d("logout", "initou");
        app.currentUser()?.logOutAsync() {
            if (it.isSuccess) {
                config = null
                partitionKey = null
                result.success(true)
            } else {
                result.success(false)
            }
        }
        Log.d("logout", "cabou de initar");
    }

    fun googleLogIn(call: MethodCall, result: Result) {
        // val a = app.loginAsync(Credentials.google(idToken ,GoogleAuthType.ID_TOKEN)) {

        // }
        //     .requestEmail()
        //     .build()
        // val googleSignInClient = GoogleSignIn.getClient(this, gso)
        // val signInIntent: Intent = googleSignInClient.signInIntent
        // startActivityForResult(signInIntent, RC_SIGN_IN) 
    }

    fun upload(result: Result) {
        val session: SyncSession = app.getSync().getSession(config)
        val task: FutureTask<String> = FutureTask(UploadChanges(session), "test")
        val executorService: ExecutorService = Executors.newFixedThreadPool(2)
        executorService.execute(task)
        result.success(true)
    }

    fun download(result: Result) {
        val session: SyncSession = app.getSync().getSession(config)
        val task: FutureTask<String> = FutureTask(DownloadChanges(session), "test")
        val executorService: ExecutorService = Executors.newFixedThreadPool(2)
        executorService.execute(task)
        result.success(true)
    }




    fun addCustomer(call: MethodCall, result: Result) {
        Log.d("addcust", "initou")
        if (sessionInit() != null) {
            val realmThread = Realm.getInstance(config)
            if (partitionKey.isNullOrEmpty()) partitionKey = app.currentUser()?.id.toString()
            var cust: customer = customer()
            
            val first_name: String? = call.argument("firstName")
            val last_name: String? = call.argument("lastName")
            val avatar: List<Int>? = call.argument("avatar")
            val avatarByte: ArrayList<Byte> = arrayListOf();
            val birthday: Long? = call.argument("birthday")
            val email: String? = call.argument("email")
            val observation: String? = call.argument("observation")
            val phone: String? = call.argument("phone")
            val sex: String? = call.argument("sex")
            val social_name: String? = call.argument("socialName")

            realmThread.executeTransactionAsync( Realm.Transaction { transactionRealm ->
                cust.`_id` = ObjectId()
                cust.`_partition` = partitionKey!!
                cust.first_name = first_name!!
                cust.last_name = last_name!!
                if (!avatar.isNullOrEmpty()) {
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("addcust", "acabou de inita")
    }

    fun listCustomer(call: MethodCall, result: Result) {
        Log.d("listcust", "initou");
        if (sessionInit() != null) {
            val realm = Realm.getInstance(config)
            var list = arrayListOf<Any>()
            val filtro: String? = call.argument("id")
            lateinit var listResult: RealmResults<customer>;
            
            if(filtro.isNullOrEmpty()) {
                listResult = realm.where<customer>().findAll()
            } else {
                listResult = realm.where<customer>().equalTo("_id", ObjectId(filtro)).findAll()
            }

            listResult.forEach {
                var avatarByte = it.avatar;
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("listcust", "cabou de initar");
    }

    fun updateCustomer(call: MethodCall, result: Result) {
        Log.d("updatecust", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)
                
                val first_name: String? = call.argument("firstName")
                val last_name: String? = call.argument("lastName")
                val avatar: List<Int>? = call.argument("avatar")
                val avatarByte: ArrayList<Byte> = arrayListOf();
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
                        if (!avatar.isNullOrEmpty()) {
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("updatecust", "acabou de inita")
    }

    fun deleteCustomer(call: MethodCall, result: Result) {
        Log.d("deletecust", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)

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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("deletecust", "acabou de inita")
    }




    fun addAppointment(call: MethodCall, result: Result) {
        Log.d("addapp", "initou");
        if (sessionInit() != null) {
            val realmThread = Realm.getInstance(config)
            if (partitionKey.isNullOrEmpty()) partitionKey = app.currentUser()?.id.toString()
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
                appoint.`_partition` = partitionKey!!
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        
        Log.d("addapp", "cabou de initar");
    }

    fun listAppointment(call: MethodCall, result: Result) {
        Log.d("listapp", "initou");
        if (sessionInit() != null) {
            val realm = Realm.getInstance(config)
            var list = arrayListOf<Any>()
            val filtro: String? = call.argument("id")
            lateinit var listResult: RealmResults<appointment>;
            
            if(filtro.isNullOrEmpty()) {
                listResult = realm.where<appointment>().findAll()
                
                Log.d("length1", listResult.size.toString())
            } else {
                listResult = realm.where<appointment>().equalTo("_id", ObjectId(filtro)).findAll()
                
                Log.d("length2", listResult.size.toString())
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("listapp", "cabou de initar");
    }

    fun updateAppointment(call: MethodCall, result: Result) {
        Log.d("updateappoint", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)
                
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("updateappoint", "acabou de inita")
    }

    fun deleteAppointment(call: MethodCall, result: Result) {
        Log.d("deleteappoint", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)

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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("deleteappoint", "acabou de inita")
    }




    fun addRecord(call: MethodCall, result: Result) {
        Log.d("addrecor", "intou")
        if (sessionInit() != null) {
            val realmThread = Realm.getInstance(config)
            if (partitionKey.isNullOrEmpty()) partitionKey = app.currentUser()?.id.toString()
            var recor: record = record()

            val date: Long? = call.argument("dateTime")
            val description: String? = call.argument("description")
            val source: String? = call.argument("source")
            val tags: List<String>? = call.argument("tags")
            val realm_tags: RealmList<String> = RealmList()
            val content_text: String? = call.argument("contentText")
            val content_bin: List<Int>? = call.argument("contentBin")
            val content_bin_byte: ArrayList<Byte> = arrayListOf();
            val customer: String? = call.argument("customer")

            realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                tags?.forEach {
                    realm_tags.add(it)
                }
    
                recor.`_id` = ObjectId()
                recor.`_partition` = partitionKey!!
                recor.date_time = Date(date!!)
                recor.description = description!!
                recor.source = source!!
                recor.tags = realm_tags
                if (!content_bin.isNullOrEmpty()) {
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("addrecor", "acabou de inita")
    }

    fun listRecord(call: MethodCall ,result: Result) {
        Log.d("listrecor", "initou");
        if (sessionInit() != null) {
            val realm = Realm.getInstance(config)
            var list = arrayListOf<Any>()
            val filtro: String? = call.argument("id")
            lateinit var listResult: RealmResults<record>;
            
            if(filtro.isNullOrEmpty()) {
                listResult = realm.where<record>().findAll()
            } else {
                listResult = realm.where<record>().equalTo("_id", ObjectId(filtro)).findAll()
            }

            listResult.forEach {
                var avatarByte: List<Byte>? = null
                val content = it.content;

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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("listrecor", "cabou de initar");
    }

    fun updateRecord(call: MethodCall, result: Result) {
        Log.d("updaterecor", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)
                
                val date: Long? = call.argument("dateTime")
                val description: String? = call.argument("description")
                val source: String? = call.argument("source")
                val tags: List<String>? = call.argument("tags")
                val realm_tags: RealmList<String> = RealmList()
                val content_text: String? = call.argument("contentText")
                val content_bin: List<Int>? = call.argument("contentBin")
                val content_bin_byte: ArrayList<Byte> = arrayListOf();
                val customer: String? = call.argument("customer")

                realmThread.executeTransactionAsync(Realm.Transaction { transactionRealm ->
                    var recor: record? = transactionRealm.where<record>().equalTo("_id", ObjectId(id)).findFirst()
                    if (recor != null) {
                        if (date != null) recor.date_time = Date(date)
                        if (!description.isNullOrBlank()) recor.description = description
                        if (!source.isNullOrBlank()) recor.source = source
                        if (!tags.isNullOrEmpty()) {
                            tags.forEach {
                                realm_tags.add(it)
                            }

                            recor.tags = realm_tags
                        }
                        if (!content_bin.isNullOrEmpty()) {
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("updaterecor", "acabou de inita")
    }

    fun deleteRecord(call: MethodCall, result: Result) {
        Log.d("deleterecor", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("deleterecor", "acabou de inita")
    }




    fun addConfiguration(call: MethodCall, result: Result) {
        Log.d("addconfig", "intou")
        if (sessionInit() != null) {
            if (partitionKey.isNullOrEmpty()) partitionKey = app.currentUser()?.id.toString()
            val realmThread = Realm.getInstance(config)
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
                confi.`_partition` = partitionKey!!
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("addconfig", "acabou de inita")
    }

    fun listConfiguration(call: MethodCall ,result: Result) {
        Log.d("listconfig", "initou");
        if (sessionInit() != null) {
            val realm = Realm.getInstance(config)
            var list = arrayListOf<Any>()
            val filtro: String? = call.argument("id")
            lateinit var listResult: RealmResults<configuration>;
            
            if(filtro.isNullOrEmpty()) {
                listResult = realm.where<configuration>().findAll()
            } else {
                listResult = realm.where<configuration>().equalTo("_id", ObjectId(filtro)).findAll()
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("listconfig", "cabou de initar");
    }

    fun updateConfiguration(call: MethodCall, result: Result) {
        Log.d("updateconfig", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)

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
                        if (!subscription.isNullOrEmpty()) {
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("updateconfig", "acabou de inita")
    }

    fun deleteConfiguration(call: MethodCall, result: Result) {
        Log.d("deleteconfig", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)

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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("deleteconfig", "acabou de inita")
    }




    fun addTextSuggestion(call: MethodCall, result: Result) {
        Log.d("addsuggestion", "intou")
        if (sessionInit() != null) {
            val realmThread = Realm.getInstance(config)
            if (partitionKey.isNullOrEmpty()) partitionKey = app.currentUser()?.id.toString()
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
                textsugg.`_partition` = partitionKey!!
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("addsuggestion", "acabou de inita")
    }

    fun listTextSuggestion(call: MethodCall ,result: Result) {
        Log.d("listsuggestion", "initou");
        if (sessionInit() != null) {
            val realm = Realm.getInstance(config)
            var list = arrayListOf<Any>()
            val filtro: String? = call.argument("id")
            lateinit var listResult: RealmResults<text_suggestion>;
            
            if(filtro.isNullOrEmpty()) {
                listResult = realm.where<text_suggestion>().findAll()
            } else {
                listResult = realm.where<text_suggestion>().equalTo("_id", ObjectId(filtro)).findAll()
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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("listsuggestion", "cabou de initar");
    }

    fun updateTextSuggestion(call: MethodCall, result: Result) {
        Log.d("updatesuggestion", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)

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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("updatesuggestion", "acabou de inita")
    }

    fun deleteTextSuggestion(call: MethodCall, result: Result) {
        Log.d("deletesuggestion", "initou")
        if (sessionInit() != null) {
            val id: String? = call.argument("id")
            if (id.isNullOrBlank()) {
                result.success(false)
            } else {
                val realmThread = Realm.getInstance(config)

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
            result.error("403", "Nenhum usuário conectado", "Conecte um usuario");
        }
        Log.d("deletesuggestion", "acabou de inita")
    }




    class DownloadChanges(val session: SyncSession) : Runnable {
        override fun run() {
            session.downloadAllServerChanges()
        }
    }

    class UploadChanges(val session: SyncSession) : Runnable {
        override fun run() {
            session.uploadAllLocalChanges()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}