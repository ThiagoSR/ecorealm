import 'dart:async';
import 'dart:math';

import 'package:ecorealm/ecorealm.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class PlansPage extends StatefulWidget {
    @override
    _PlansPageState createState() => _PlansPageState();
}

class _PlansPageState extends State<PlansPage> {
    List listPlans = [{}];
    var rng = new Random();
    String configuration_id = '';

    @override
    void dispose() {
        super.dispose();
    }

    @override
    void initState() {
        Ecorealm.getConfigurations().then((value) => setState(() {listPlans = value;}));

        setState(() {});

        super.initState();
    }

    @override
    Widget build(BuildContext context) {
        return Column(
            children: [
                Expanded(
                    child: Container(
                        color: Colors.blue,
                        child: SingleChildScrollView(
                            padding: EdgeInsets.all(5),
                            child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: listPlans.map((e) {
                                    return Column(
                                        children:[
                                            Container(
                                                color: Colors.white70,
                                                child: e is Map 
                                                    ? Column(children: e.keys.map((i) => Text(i.toString() + ': ' + e[i].toString())).toList()) 
                                                    : SizedBox.shrink()
                                            ),
                                            SizedBox(height: 15)
                                        ]
                                    );
                                }).toList(),
                            ),
                        ),
                    ),
                ),
                Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
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
                            child: Text("Adicionar"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.lightGreen)
                            ),
                        ),
                        ElevatedButton(
                            onPressed: () async {
                                if (configuration_id != "") {
                                    print(await Ecorealm.getConfigurations(
                                        field: '_id',
                                        logicalOperator: RealmLogicalOperator.equals,
                                        value: configuration_id,
                                        valueType: RealmValueTypes.objectId
                                    ));
                                }
                            }, 
                            child: Text("Pegar ultimo"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.lightBlue)
                            ),
                        ),
                    ],
                ),
                Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
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
                            child: Text("Editar ultimo"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.yellow[900])
                            ),
                        ),
                        ElevatedButton(
                            onPressed: () async {
                                if (configuration_id != "") {
                                    print(await Ecorealm.deleteConfiguration(configuration_id));
                                }
                            }, 
                            child: Text("Deletar ultimo"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red[900])
                            ),
                        )
                    ],
                ),
            ],
        );
    }
}