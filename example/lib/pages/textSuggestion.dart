import 'dart:async';
import 'dart:math';

import 'package:ecorealm/ecorealm.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class TextSuggestionPage extends StatefulWidget {
    @override
    _TextSuggestionPageState createState() => _TextSuggestionPageState();
}

class _TextSuggestionPageState extends State<TextSuggestionPage> {
    List listTextSuggestion = [{}];
    var rng = new Random();
    String textsuggestion_id = '';
    int offset = 0;
    int limit = 10;
    
    @override
    void dispose() {
        super.dispose();
    }

    @override
    void initState() {
        Ecorealm.getTextSuggestions(
            limit: limit
        ).then((value) => setState(() {listTextSuggestion = value;}));

        super.initState();
    }

    @override
    Widget build(BuildContext context) {
        return Column(
            children: [
                Expanded(
                    child: Container(
                        color: Colors.deepOrange,
                        child: SingleChildScrollView(
                            padding: EdgeInsets.all(5),
                            child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: listTextSuggestion.map((e) {
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
                                textsuggestion_id = await Ecorealm.addTextSuggestion(
                                    from: "teste" + rng.nextInt(50).toString(),
                                    to: "teste to" "teste" + rng.nextInt(50).toString(),
                                    counter: rng.nextInt(50)
                                );
                                print(textsuggestion_id);
                            }, 
                            child: Text("Adicionar"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.lightGreen)
                            ),
                        ),
                        ElevatedButton(
                            onPressed: () async {
                                List result = await Ecorealm.getTextSuggestions(
                                    limit: 1
                                );

                                print(result);

                                if (result.length > 0) {
                                    textsuggestion_id = result[0]['id'];
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
                                if (textsuggestion_id != "") {
                                    print(await Ecorealm.updateTextSuggestion(
                                        id: textsuggestion_id,
                                        counter: rng.nextInt(100)
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
                                if (textsuggestion_id != "") {
                                    print(await Ecorealm.deleteTextSuggestion(textsuggestion_id));
                                }
                            }, 
                            child: Text("Deletar ultimo"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red[900])
                            ),
                        ),
                        ElevatedButton(
                            onPressed: () async {
                                offset += 10;
                                Ecorealm.getTextSuggestions(
                                    limit: limit,
                                    offset: offset
                                ).then((value) => setState(() {listTextSuggestion = value;}));
                            }, 
                            child: Text("Próximos"),
                            style: ButtonStyle(
                                backgroundColor: MaterialStateColor.resolveWith((states) => Colors.red[900])
                            ),
                        )
                    ],
                )
            ],
        );
    }
}