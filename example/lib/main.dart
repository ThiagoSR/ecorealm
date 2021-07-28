import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

import 'package:ecorealm_example/pages/customer.dart';
import 'package:ecorealm_example/pages/planos.dart';
import 'package:ecorealm_example/pages/record.dart';
import 'package:ecorealm_example/pages/schedule.dart';
import 'package:ecorealm_example/pages/textSuggestion.dart';
import 'package:ecorealm_example/pages/user.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:ecorealm/ecorealm.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart';
import 'package:path_provider/path_provider.dart';
// import 'package:google_sign_in/google_sign_in.dart';
// import 'package:google';
// import 'package:http/http.dart' as http;

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

enum States {
    customer,
    planos,
    record,
    schedule,
    textSuggestion,
    user
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
    States state = States.customer;
    bool _booLogado = false;
    var rng = new Random();
    GoogleSignIn _googleSignIn = GoogleSignIn(
        // Optional clientId
        // clientId: '479882132969-9i9aqik3jfjd7qhci1nqf0bm2g71rm1u.apps.googleusercontent.com',
        scopes: <String>[
            'email'
        ],
    );



    @override
    void initState() {
        super.initState();
        initRealm();
        Ecorealm.downloadState = AppLifecycleState.resumed;
        WidgetsBinding.instance.addObserver(this);
    }

    @override
    void dispose() {
        WidgetsBinding.instance.removeObserver(this);
        print('disposado');
        super.dispose();
    }

    @override
    void didChangeAppLifecycleState(AppLifecycleState state) {
        super.didChangeAppLifecycleState(state);
        
        Ecorealm.downloadState = state;
    }

    // Platform messages are asynchronous, so we initialize in an async method.
    Future<void> initRealm() async {
        bool logado = await Ecorealm.init();
        setState(() {
            _booLogado = logado;
        });
    }

    // profile	View your basic profile info
    // email	View your email address
    // openid	Authenticate using OpenID Connect

    @override
    Widget build(BuildContext context) {
        return MaterialApp(
            home: Scaffold(
                // floatingActionButton: Column(
                //     mainAxisAlignment: MainAxisAlignment.end,
                //     crossAxisAlignment: CrossAxisAlignment.end,
                //     children: [
                //         Column(
                //             children: [
                                // SizedBox(height: 10,),
                                // FloatingActionButton(
                                //     onPressed: () async {
                                //         print(await Ecorealm.connectionType);
                                //     },
                                //     child: Text('Type'),
                                // ),
                                // SizedBox(height: 10,),
                                // FloatingActionButton(
                                //     onPressed: () async {
                                //         await Ecorealm.exportData(RealmExportType.xml, await getApplicationDocumentsDirectory());
                                //         print('exportou');
                                //     },
                                //     child: Text('Exportar'),
                                // )
                //             ]
                //         )
                //     ]
                // ),
                appBar: AppBar(
                    title: const Text('Plugin example app'),
                ),
                body: Padding(
                    padding: EdgeInsets.all(5),
                    child: Column(
                        children: [
                            Row(
                                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                children: [
                                    Text(_booLogado ? 'Logado' : 'Deslogado'),
                                    SizedBox(width: 10),
                                    ElevatedButton(
                                        onPressed: () async {
                                            if (_booLogado) {
                                                print('Deslogando');
                                                bool deslogado = await Ecorealm.logOut();
                                                print(deslogado);
                                                setState(() {
                                                    _booLogado = !deslogado;
                                                });
                                            } else { 
                                                print('Logando');
                                                bool logado = await Ecorealm.logIn(
                                                    username: 'thiagospindolarossi15409@gmail.com',
                                                    password: '123456'
                                                );
                                                print(logado);
                                                setState(() {
                                                    _booLogado = logado;
                                                });
                                            }
                                        }, 
                                        child: _booLogado ? Text('Deslogar') : Text('Logar'),
                                    ),
                                    ElevatedButton(
                                        onPressed: () {
                                            try {
                                                _googleSignIn.signIn().then((value) {
                                                    String email = value.email.toString();
                                                    String password = value.id.toString() + 'ap31';

                                                    if (email != '' && password != '') {
                                                        password = base64.encode(utf8.encode(password));
                                                        Ecorealm.logIn(
                                                            username: email,
                                                            password: password
                                                        ).then((value) {
                                                            if (!value) {
                                                                print('registrando');
                                                                Ecorealm.register(
                                                                    username: email,
                                                                    password: password
                                                                ).then((value) {
                                                                    if (value) {
                                                                        Ecorealm.logIn(
                                                                            username: email,
                                                                            password: password
                                                                        ).then((value) {
                                                                            setState(() {
                                                                                _booLogado = value;
                                                                            });
                                                                        });
                                                                    } else {    
                                                                        print('Usuário já registrado');
                                                                    }
                                                                });
                                                            } else {
                                                                setState(() {
                                                                    _booLogado = value;
                                                                });
                                                            }
                                                        });
                                                    }
                                                });
                                            } catch (error) {
                                                print(error);
                                            }
                                        },
                                        child: Text('Google')
                                    )
                                ]
                            ),
                            SizedBox(height: 5),
                            Row(
                                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                children: [
                                    ElevatedButton(
                                        onPressed: () async {
                                            print('Começando Sync');
                                            print(await Ecorealm.startSync());
                                        },
                                        child: Text('Start'),
                                        style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.green))
                                    ),
                                    SizedBox(width: 10),
                                    ElevatedButton(
                                        onPressed: () async {
                                            print('Parando Sync');
                                            print(await Ecorealm.stopSync());
                                        },
                                        child: Text('Stop'),
                                        style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red))
                                    ),
                                ],
                            ),
                            SizedBox(height: 5),
                            Container(
                                color: Colors.grey[300],
                                child: Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                        ElevatedButton(onPressed: () {
                                            setState(() {
                                                state = States.customer;
                                            });
                                        }, child: Text('cust'), style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blueGrey))),
                                        ElevatedButton(onPressed: () {
                                            setState(() {
                                                state = States.planos;
                                            });
                                        }, child: Text('plan'), style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blueGrey))),
                                        ElevatedButton(onPressed: () {
                                            setState(() {
                                                state = States.record;
                                            });
                                        }, child: Text('reco'), style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blueGrey))),
                                        ElevatedButton(onPressed: () {
                                            setState(() {
                                                state = States.schedule;
                                            });
                                        }, child: Text('sche'), style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blueGrey))),
                                        ElevatedButton(onPressed: () {
                                            setState(() {
                                                state = States.textSuggestion;
                                            });
                                        }, child: Text('text'), style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blueGrey))),
                                        ElevatedButton(onPressed: () {
                                            setState(() {
                                                state = States.user;
                                            });
                                        }, child: Text('user'), style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blueGrey))),
                                    ],
                                )
                            ),
                            Expanded(
                                child: LayoutBuilder(
                                    builder: (context, _) {
                                        switch (state) {
                                            case States.customer: return CustomerPage();
                                            case States.planos: return PlansPage();
                                            case States.record: return RecordPage();
                                            case States.schedule: return SchedulePage();
                                            case States.textSuggestion: return TextSuggestionPage();
                                            case States.user: return UserPage();
                                        }
                                        
                                        return SizedBox.shrink();
                                    },
                                )
                            ),
                        ],
                    ),
                )
            )
        );
    }
}