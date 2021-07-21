import 'dart:async';
import 'dart:math';

import 'package:ecorealm/ecorealm.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class RecordPage extends StatefulWidget {
    @override
    _RecordPageState createState() => _RecordPageState();
}

class _RecordPageState extends State<RecordPage> {
    List listRecord = [{}];
    var rng = new Random();
    String record_id = '';
    String customer_id = '';

    @override
    void dispose() {
        super.dispose();
    }

    @override
    void initState() {
        Ecorealm.getRecords().then((value) => setState(() {listRecord = value;}));

        super.initState();
    }

    @override
    Widget build(BuildContext context) {
        return Column(
            children: [
                Expanded(
                    child: Container(
                        color: Colors.brown,
                        child: SingleChildScrollView(
                            padding: EdgeInsets.all(5),
                            child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: listRecord.map((e) {
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
                            child: Text("Adicionar"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.lightGreen)
                            ),
                        ),
                        ElevatedButton(
                            onPressed: () async {
                                if (record_id != "") {
                                    print(await Ecorealm.getRecords(
                                        field: '_id',
                                        logicalOperator: RealmLogicalOperator.equals,
                                        value: record_id,
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
                                if (record_id != "") {
                                    print(await Ecorealm.updateRecord(
                                        id: record_id,
                                        description: "alterado",
                                        dateTime: DateTime.now().toUtc(),
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
                                if (record_id != "") {
                                    print(await Ecorealm.deleteRecord(record_id));
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