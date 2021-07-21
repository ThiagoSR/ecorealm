import 'dart:async';
import 'dart:math';

import 'package:ecorealm/ecorealm.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart';

class CustomerPage extends StatefulWidget {
    @override
    _CustomerPageState createState() => _CustomerPageState();
}

class _CustomerPageState extends State<CustomerPage> {
    List listCustomer = [];
    var rng = new Random();
    String customer_id = '';

    @override
    void dispose() {
        super.dispose();
    }

    @override
    void initState() {
        Ecorealm.getCustomers().then((value) => setState(() {listCustomer = value;}));
        super.initState();
    }

    @override
    Widget build(BuildContext context) {
        return Column(
            children: [
                Expanded(
                    child: Container(
                        color: Colors.black,
                        child: SingleChildScrollView(
                            padding: EdgeInsets.all(5),
                            child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: listCustomer.map((e) {
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
                                Response response = await get(Uri.parse('https://cdn.discordapp.com/emojis/816804094794268673.png'));
                                customer_id = await Ecorealm.addCustomer(
                                    firstName: "Thiago" + rng.nextInt(100).toString(),
                                    lastName: "Rossi" + rng.nextInt(100).toString(),
                                    birthday: DateTime.now().toUtc(),
                                    email: "thiagorossi@teste.com" + rng.nextInt(100).toString(),
                                    observation: "teste",
                                    phone: "999999999",
                                    sex: "male",
                                    socialName: "teste" + rng.nextInt(100).toString(),
                                    avatar: response.bodyBytes.toList()
                                );
                                print(customer_id);
                            }, 
                            child: Text("Adicionar"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.lightGreen)
                            ),
                        ),
                        ElevatedButton(
                            onPressed: () async {
                                if (customer_id != "") {
                                    print(await Ecorealm.getCustomers(
                                        field: '_id',
                                        logicalOperator: RealmLogicalOperator.equals,
                                        value: customer_id,
                                        valueType: RealmValueTypes.objectId
                                    ));
                                }
                            }, 
                            child: Text("Pegar ultimo"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.lightBlue)
                            ),
                        )
                    ],
                ),
                Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
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
                            child: Text("Editar ultimo"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.yellow[900])
                            ),
                        ),
                        ElevatedButton(
                            onPressed: () async {
                                if (customer_id != "") {
                                    print(await Ecorealm.deleteCustomer(customer_id));
                                }
                            }, 
                            child: Text("Deletar ultimo"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red[900])
                            ),
                        ),
                    ],
                ),
            ],
        );
    }
}