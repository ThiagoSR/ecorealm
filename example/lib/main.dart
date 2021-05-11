import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:ecorealm/ecorealm.dart';
// import 'package:http/http.dart' as http;

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
    bool _booLogado = false;
    String customer_id = "";
    String appointment_id = "";
    String record_id = "";
    String configuration_id = "";
    String textsuggestion_id = "";
    var rng = new Random();

    @override
    void initState() {
        super.initState();
        initRealm();
    }

    // Platform messages are asynchronous, so we initialize in an async method.
    Future<void> initRealm() async {
        bool logado = await Ecorealm.init();
        setState(() {
            _booLogado = logado;
        });
    }

    // Future<void> teste() async{
    //     http.Response response = await http
    //         .get(Uri.parse('https://cdn.discordapp.com/emojis/806895966341824534.png'));
    //     print(await Ecorealm.addCustomer(
    //         firstName: "João2",
    //         lastName: "Silva2",
    //         avatar: response.bodyBytes.toList()
    //     ));
    // }

    Future<void> teste2() async{
        print(await Ecorealm.addRecord(
            dateTime: DateTime.now().toUtc(),
            description: "teste",
            source: "teste",
            tags: ['1', '2', '3'],
        ));
    }

    @override
    Widget build(BuildContext context) {
        return MaterialApp(
            home: Scaffold(
                floatingActionButton: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                        Column(
                            children: [
                                FloatingActionButton(
                                    onPressed: () async {
                                        if (_booLogado) {
                                            bool deslogado = await Ecorealm.logOut();
                                            setState(() {
                                                _booLogado = !deslogado;
                                            });
                                        } else { 
                                            bool logado = await Ecorealm.logIn(
                                                username: 'thiagospindolarossi159@gmail.com',
                                                password: 'chopgelado'
                                            );
                                            setState(() {
                                                _booLogado = logado;
                                            });
                                        }
                                    },
                                    child: _booLogado ? Text('Deslogar') : Text('Logar'),
                                )
                            ]
                        )
                    ]
                ),
                appBar: AppBar(
                    title: const Text('Plugin example app'),
                ),
                body: Center(
                    child: SingleChildScrollView(
                        child: Column(
                            children: [
                                ElevatedButton(
                                    onPressed: () async {
                                        print(await Ecorealm.getCustomers());
                                    }, 
                                    child: Text("Listar customer"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blue)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        customer_id = await Ecorealm.addCustomer(
                                            firstName: "Thiago" + rng.nextInt(100).toString(),
                                            lastName: "Rossi" + rng.nextInt(100).toString(),
                                            birthday: DateTime.now().toUtc(),
                                            email: "thiagorossi@teste.com" + rng.nextInt(100).toString(),
                                            observation: "teste",
                                            phone: "999999999",
                                            sex: "male",
                                            socialName: "teste" + rng.nextInt(100).toString()
                                        );
                                        print(customer_id);
                                    }, 
                                    child: Text("Adicionar customer"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blue)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (customer_id != "") {
                                            print(await Ecorealm.getCustomers(customer_id));
                                        }
                                    }, 
                                    child: Text("Pegar ultimo customer"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blue)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (customer_id != "") {
                                            print(await Ecorealm.updateCustomer(
                                                id: customer_id,
                                                firstName: "alterado",
                                                observation: "alterado"
                                            ));
                                        }
                                    }, 
                                    child: Text("Editar ultimo customer"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blue)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (customer_id != "") {
                                            print(await Ecorealm.deleteCustomer(customer_id));
                                        }
                                    }, 
                                    child: Text("Deletar ultimo customer"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.blue)
                                    ),
                                ),
                                SizedBox(height: 20),
                                ElevatedButton(
                                    onPressed: () async {
                                        print(await Ecorealm.getAppointments());
                                    }, 
                                    child: Text("Listar appointment"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        appointment_id = await Ecorealm.addAppointment(
                                            customer: customer_id,
                                            observation: "Rossi" + rng.nextInt(100).toString(),
                                            date: DateTime.now().toUtc(),
                                            duration: 5000000000000000000,
                                            status: "Marcado"
                                        );
                                        print(appointment_id);
                                    }, 
                                    child: Text("Adicionar appointment"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (appointment_id != "") {
                                            print(await Ecorealm.getAppointments(appointment_id));
                                        }
                                    }, 
                                    child: Text("Pegar ultimo appointment"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (appointment_id != "") {
                                            print(await Ecorealm.updateAppointment(
                                                id: appointment_id,
                                                status: "alterado",
                                                observation: "alterado"
                                            ));
                                        }
                                    }, 
                                    child: Text("Editar ultimo appointment"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (appointment_id != "") {
                                            print(await Ecorealm.deleteAppointment(appointment_id));
                                        }
                                    }, 
                                    child: Text("Deletar ultimo appointment"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red)
                                    ),
                                ),
                                SizedBox(height: 20),
                                ElevatedButton(
                                    onPressed: () async {
                                        print(await Ecorealm.getRecords());
                                    }, 
                                    child: Text("Listar record"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.green)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        record_id = await Ecorealm.addRecord(
                                            customer: customer_id,
                                            description: "desc" + rng.nextInt(100).toString(),
                                            dateTime: DateTime.now().toUtc(),
                                            source: "teste" + rng.nextInt(200).toString(),
                                            tags: ["teste1", "teste2", rng.nextInt(300).toString()],
                                            contentBin: [1,2,3,4,5,6,7,8],
                                            contentText: "teste" + rng.nextInt(100).toString()
                                        );
                                        print(record_id);
                                    }, 
                                    child: Text("Adicionar record"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.green)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (record_id != "") {
                                            print(await Ecorealm.getRecords(record_id));
                                        }
                                    }, 
                                    child: Text("Pegar ultimo record"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.green)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (record_id != "") {
                                            print(await Ecorealm.updateRecord(
                                                id: record_id,
                                                description: "alterado",
                                                dateTime: DateTime.now().toUtc(),
                                            ));
                                        }
                                    }, 
                                    child: Text("Editar ultimo record"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.green)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (record_id != "") {
                                            print(await Ecorealm.deleteRecord(record_id));
                                        }
                                    }, 
                                    child: Text("Deletar ultimo record"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.green)
                                    ),
                                ),
                                SizedBox(height: 20),
                                ElevatedButton(
                                    onPressed: () async {
                                        print(await Ecorealm.getConfigurations());
                                    }, 
                                    child: Text("Listar configuration"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.purple)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        configuration_id = await Ecorealm.addConfiguration(
                                            firstName: "Thiago" + rng.nextInt(100).toString(),
                                            lastName: "Rossi" + rng.nextInt(100).toString(),
                                            email: "thiagorossi@teste.com" + rng.nextInt(100).toString(),
                                            language: "teste",
                                            timezone: "999999999",
                                            socialName: "teste" + rng.nextInt(100).toString(),
                                            subscription: [
                                                ['teste', '1'],
                                                ['teste2', '2'],
                                                ['teste' + rng.nextInt(50).toString(), rng.nextInt(50).toString()]
                                            ]
                                        );
                                        print(configuration_id);
                                    }, 
                                    child: Text("Adicionar configuration"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.purple)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (configuration_id != "") {
                                            print(await Ecorealm.getConfigurations(configuration_id));
                                        }
                                    }, 
                                    child: Text("Pegar ultimo configuration"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.purple)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (configuration_id != "") {
                                            print(await Ecorealm.updateConfiguration(
                                                id: configuration_id,
                                                firstName: "alterado",
                                                socialName: "alterado"
                                            ));
                                        }
                                    }, 
                                    child: Text("Editar ultimo configuration"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.purple)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (configuration_id != "") {
                                            print(await Ecorealm.deleteConfiguration(configuration_id));
                                        }
                                    }, 
                                    child: Text("Deletar ultimo configuration"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.purple)
                                    ),
                                ),
                                SizedBox(height: 20),
                                ElevatedButton(
                                    onPressed: () async {
                                        print(await Ecorealm.getTextSuggestions());
                                    }, 
                                    child: Text("Listar textsuggestion"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.orange)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        textsuggestion_id = await Ecorealm.addTextSuggestion(
                                            from: "teste" + rng.nextInt(50).toString(),
                                            to: "teste to" "teste" + rng.nextInt(50).toString(),
                                            counter: rng.nextInt(50)
                                        );
                                        print(textsuggestion_id);
                                    }, 
                                    child: Text("Adicionar TextSuggestion"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.orange)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (textsuggestion_id != "") {
                                            print(await Ecorealm.getTextSuggestions(textsuggestion_id));
                                        }
                                    }, 
                                    child: Text("Pegar ultimo TextSuggestion"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.orange)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (textsuggestion_id != "") {
                                            print(await Ecorealm.updateTextSuggestion(
                                                id: textsuggestion_id,
                                                counter: rng.nextInt(100)
                                            ));
                                        }
                                    }, 
                                    child: Text("Editar ultimo TextSuggestion"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.orange)
                                    ),
                                ),
                                ElevatedButton(
                                    onPressed: () async {
                                        if (textsuggestion_id != "") {
                                            print(await Ecorealm.deleteTextSuggestion(textsuggestion_id));
                                        }
                                    }, 
                                    child: Text("Deletar ultimo TextSuggestion"),
                                    style: ButtonStyle(
                                        backgroundColor: MaterialStateColor.resolveWith((states) => Colors.orange)
                                    ),
                                ),
                                SizedBox(height: 20),
                            ]
                        ),
                    ),
                ),
            )
        );
    }
}