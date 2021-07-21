
import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
                       
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:xml/xml.dart';

enum RealmLogicalOperator {
    equals,
    like,
    greater,
    lesser,
    beetwen,
    notNull
}

enum RealmValueTypes {
    objectId,
    boolean,
    integer,
    string,
    list,
    date
}

enum RealmConnectionType {
    wifi,
    mobile,
    unconnected
}

enum RealmExportType {
    json,
    xml,
    csv
}

class Ecorealm {
    static const MethodChannel _channel =
        const MethodChannel('ecorealm');

    static Future<RealmConnectionType> get connectionType async {
        switch(await _channel.invokeMethod('getConnectionType')) {
            case 0: 
                return RealmConnectionType.unconnected;
            case 1:
                return RealmConnectionType.wifi;
            case 2:
                return RealmConnectionType.mobile;
            default:
                return RealmConnectionType.unconnected;
        }
    }

    static Future<bool> init() async {
        return await _channel.invokeMethod("init");
    }

    static Future<bool> stopSync() async {
        return await _channel.invokeMethod("stopSync");
    }

    static Future<bool> startSync() async {
        return await _channel.invokeMethod("startSync");
    }

    static Future<bool> register({
        String username,
        String password
    }) async {
        return await _channel.invokeMethod(
            "register",
            {
                "username": username,
                "password": password
            }
        ).onError((error, stackTrace) {
            print("Sem conexão");
            return false;
        });
    }

    static Future<bool> isLoggedIn() async {
        return await _channel.invokeMethod("isLoggedIn");
    }

    static Future<bool> logIn({
        String username,
        String password
    }) async {
        print('loginlib');
        return await _channel.invokeMethod(
            "logIn",
            {
                "username": username,
                "password": password
            }
        ).onError((error, stackTrace) {
            print("Sem conexão");
            return false;
        });
    }

    static Future<bool> logInGoogle({
        String token
    }) async {
        return await _channel.invokeMethod(
            "logInGoogle",
            {
                "token": token,
                "method": "auth"
            }
        ) || await _channel.invokeMethod(
            "logInGoogle",
            {
                "token": token,
                "method": "id"
            }
        );
        // ).onError((error, stackTrace) {
        //     print("Sem conexão");
        //     return false;
        // });
    }

    static Future<bool> logOut() async {
        return await _channel.invokeMethod("logOut");
    }

    static Future<bool> downloadChanges() async {
        return await _channel.invokeMethod("download");
    }

    static Future<bool> uploadChanges() async {
        return await _channel.invokeMethod("upload");
    }
    
    static Future<List> getCustomers({
        String field,
        RealmLogicalOperator logicalOperator,
        dynamic value,
        RealmValueTypes valueType
    }) async {
        var valores = _valueType(valueType, value);
        return await _channel.invokeMethod(
            'listCustomer',
            {
                "field": field,
                "logicalOperator": _logicalOperator(logicalOperator),
                "value": valores[0],
                "valueType": valores[1] 
            }
            )
            .onError((error, stackTrace) {
                print('erro');
                return [];
            });
    }

    static Future<String> addCustomer({
        @required String firstName,
        @required String lastName,
        List<int> avatar,
        DateTime birthday,
        String email,
        String observation,
        String phone,
        String sex,
        String socialName
    }) async {
        return await _channel.invokeMethod(
            "addCustomer",
            {
                "firstName": firstName,
                "lastName": lastName,
                "avatar": avatar,
                "birthday": birthday.millisecondsSinceEpoch,
                "email": email,
                "observation": observation,
                "phone": phone,
                "sex": sex,
                "socialName": socialName
            }
        );
    }

    static Future<bool> updateCustomer({
        @required String id,
        String firstName,
        String lastName,
        List<int> avatar,
        DateTime birthday,
        String email,
        String observation,
        String phone,
        String sex,
        String socialName
    }) async {
        return await _channel.invokeMethod(
            "updateCustomer",
            {
                "id": id,
                "firstName": firstName,
                "lastName": lastName,
                "avatar": avatar,
                "birthday": birthday != null ? birthday.millisecondsSinceEpoch : null,
                "email": email,
                "observation": observation,
                "phone": phone,
                "sex": sex,
                "socialName": socialName
            }
        );
    }

    static Future<bool> deleteCustomer(String id) async {
        return await _channel.invokeMethod(
            "deleteCustomer",
            {
                "id": id
            }
        );
    }


    static Future<List> getAppointments({
        String field,
        RealmLogicalOperator logicalOperator,
        dynamic value,
        RealmValueTypes valueType
    }) async {
        var valores = _valueType(valueType, value);
        return await _channel.invokeMethod(
            'listAppointment',
            {
                "field": field,
                "logicalOperator": _logicalOperator(logicalOperator),
                "value": valores[0],
                "valueType": valores[1]
            }
            )
            .onError((error, stackTrace) {
                print(error);
                return [];
            });
    }

    static Future<String> addAppointment({
        @required DateTime date,
        @required int duration,
        String customer,
        String observation,
        String status
    }) async {
        return await _channel.invokeMethod(
            "addAppointment",
            {
                "date": date.millisecondsSinceEpoch,
                "duration": duration,
                "customer": customer,
                "observation": observation,
                "status": status,
            }
        );
    }

    static Future<bool> updateAppointment({
        @required String id,
        DateTime date,
        int duration,
        String customer,
        String observation,
        String status
    }) async {
        return await _channel.invokeMethod(
            "updateAppointment",
            {
                "id": id,
                "date": date != null ? date.millisecondsSinceEpoch : null,
                "duration": duration,
                "customer": customer,
                "observation": observation,
                "status": status,
            }
        );
    }

    static Future<bool> deleteAppointment(String id) async {
        return await _channel.invokeMethod(
            "deleteAppointment",
            {
                "id": id
            }
        );
    }


    static Future<List> getRecords({
        String field,
        RealmLogicalOperator logicalOperator,
        dynamic value,
        RealmValueTypes valueType
    }) async {
        var valores = _valueType(valueType, value);
        return await _channel.invokeMethod(
            'listRecord',
            {
                "field": field,
                "logicalOperator": _logicalOperator(logicalOperator),
                "value": valores[0],
                "valueType": valores[1] 
            }
            )
            .onError((error, stackTrace) {
                print('erro');
                return [];
            });
    }
    
    static Future<String> addRecord({
        @required DateTime dateTime,
        @required String description,
        @required String source,
        @required List<String> tags,
        String contentText,
        List<int> contentBin,
        String customer
    }) async {
        return await _channel.invokeMethod(
            "addRecord",
            {
                "dateTime": dateTime.millisecondsSinceEpoch,
                "description": description,
                "source": source,
                "tags": tags,
                "contentText": contentText,
                "contentBin": contentBin,
                "customer": customer
            }
        );
    }

    static Future<bool> updateRecord({
        @required String id,
        DateTime dateTime,
        String description,
        String source,
        List<String> tags,
        String contentText,
        List<int> contentBin,
        String customer
    }) async {
        return await _channel.invokeMethod(
            "updateRecord",
            {
                "id": id,
                "dateTime": dateTime != null ? dateTime.millisecondsSinceEpoch : null,
                "description": description,
                "source": source,
                "tags": tags,
                "contentText": contentText,
                "contentBin": contentBin,
                "customer": customer
            }
        );
    }

    static Future<bool> deleteRecord(String id) async {
        return await _channel.invokeMethod(
            "deleteRecord",
            {
                "id": id
            }
        );
    }


    static Future<List> getConfigurations({
        String field,
        RealmLogicalOperator logicalOperator,
        dynamic value,
        RealmValueTypes valueType
    }) async {
        var valores = _valueType(valueType, value);
        return await _channel.invokeMethod(
            'listConfiguration',
            {
                "field": field,
                "logicalOperator": _logicalOperator(logicalOperator),
                "value": valores[0],
                "valueType": valores[1]
            }
        )
        .onError((error, stackTrace) {
                print('erro');
                return [];
            });
    }
    
    static Future<String> addConfiguration({
        @required String email,
        @required String firstName,
        @required String lastName,
        @required String language,
        @required List<List<String>> subscription,
        @required String timezone,
        String socialName
    }) async {
        return await _channel.invokeMethod(
            "addConfiguration",
            {
                "email": email,
                "firstName": firstName,
                "lastName": lastName,
                "language": language,
                "subscription": subscription,
                "timezone": timezone,
                "socialName": socialName
            }
        );
    }

    static Future<bool> updateConfiguration({
        @required String id,
        String email,
        String firstName,
        String lastName,
        String language,
        List<List<String>> subscription,
        String timezone,
        String socialName
    }) async {
        return await _channel.invokeMethod(
            "updateConfiguration",
            {
                "id": id,
                "email": email,
                "firstName": firstName,
                "lastName": lastName,
                "language": language,
                "subscription": subscription,
                "timezone": timezone,
                "socialName": socialName
            }
        );
    }

    static Future<bool> deleteConfiguration(String id) async {
        return await _channel.invokeMethod(
            "deleteConfiguration",
            {
                "id": id
            }
        );
    }

    static Future<List> getTextSuggestions({
        String field,
        RealmLogicalOperator logicalOperator,
        dynamic value,
        RealmValueTypes valueType
    }) async {
        var valores = _valueType(valueType, value);
        return await _channel.invokeMethod(
            'listTextSuggestion',
            {
                "field": field,
                "logicalOperator": _logicalOperator(logicalOperator),
                "value": valores[0],
                "valueType": valores[1]
            }
            )
            .onError((error, stackTrace) {
                print('erro');
                return [];
            });
    }
    
    static Future<String> addTextSuggestion({
        @required String from,
        @required String to,
        int counter
    }) async {
        return await _channel.invokeMethod(
            "addTextSuggestion",
            {
                "from": from,
                "to": to,
                "counter": counter
            }
        );
    }

    static Future<bool> updateTextSuggestion({
        @required String id,
        String from,
        String to,
        int counter
    }) async {
        return await _channel.invokeMethod(
            "updateTextSuggestion",
            {
                "id": id,
                "from": from,
                "to": to,
                "counter": counter
            }
        );
    }

    static Future<bool> deleteTextSuggestion(String id) async {
        return await _channel.invokeMethod(
            "deleteTextSuggestion",
            {
                "id": id
            }
        );
    }


    static Future<String> exportData(RealmExportType type, Directory dir) async {
        List customers = await getCustomers();
        List appointments = await getAppointments();
        List configurations = await getConfigurations();
        List textSuggestions = await getTextSuggestions();
        List records = await getRecords();
        print(customers);
        print(appointments);
        print(configurations);
        print(textSuggestions);
        print(records);

        String customerXml = _generateXML(customers, 'customer');
        print(customerXml);
        File customerFile = File(dir.path + '/customer.xml');
        customerFile.createSync();
        customerFile.writeAsString(customerXml);

        String appointmentXml = _generateXML(appointments, 'appointment');
        print(appointmentXml);
        File appointmentFile = File(dir.path + '/appointment.xml');
        appointmentFile.createSync();
        appointmentFile.writeAsString(appointmentXml);

        String configurationXml = _generateXML(configurations, 'configuration');
        print(configurationXml);
        File configurationFile = File(dir.path + '/configuration.xml');
        configurationFile.createSync();
        configurationFile.writeAsString(configurationXml);

        String textSuggestionXml = _generateXML(textSuggestions, 'text-suggestion');
        print(textSuggestionXml);
        File textSuggestionFile = File(dir.path + '/textSuggestion.xml');
        textSuggestionFile.createSync();
        textSuggestionFile.writeAsString(textSuggestionXml);

        String recordXml = _generateXML(records, 'record');
        print(recordXml);
        File recordFile = File(dir.path + '/record.xml');
        recordFile.createSync();
        recordFile.writeAsString(recordXml);
    }

    static String _generateXML(List<dynamic> info, String name) {
        XmlBuilder xmlBuilder = XmlBuilder();
        xmlBuilder.processing('xml', 'version="1.0"');
        info.forEach((element) {
            if (element is Map) {
                xmlBuilder.element(name, nest: () {
                    element.forEach((key, value) {
                        xmlBuilder.element(key, nest: () {
                            if (value is List && value.length > 0) {
                                if (value[0] is int) {
                                    List<int> lista = [];

                                    value.forEach((element) {
                                        if (element is int) lista.add(element);
                                    });

                                    xmlBuilder.text(base64Encode(Uint8List.fromList(lista)));
                                } else if (value[0] is String) {
                                    value.forEach((element) {
                                        xmlBuilder.element(key.substring(0, key.length-1), nest: () {
                                            xmlBuilder.text(element);
                                        });
                                    });
                                } else if (value[0] is List) {
                                    value.forEach((element) {
                                        xmlBuilder.element(key+"-type", nest: () {
                                            String text = '';
                                            element.forEach((element) {
                                                text += element;
                                            });
                                            xmlBuilder.text(text);
                                        });
                                    });
                                }
                            } else {
                                xmlBuilder.text(value);
                            }
                        });
                    });
                });
            }
        });
        XmlDocument doc = xmlBuilder.buildDocument(); 
        return doc.toXmlString();
    }

    static String _logicalOperator(RealmLogicalOperator operador) {
        switch(operador) {
            case RealmLogicalOperator.equals:
                return 'equals';
            break;
            case RealmLogicalOperator.like:
                return 'like';
            break;
            case RealmLogicalOperator.greater:
                return 'greater';
            break;
            case RealmLogicalOperator.lesser:
                return 'lesser';
            break;
            case RealmLogicalOperator.beetwen:
                return 'beetwen';
            break;
            case RealmLogicalOperator.notNull:
                return 'notNull';
            break;
            default:
                return 'equals';
            break; 
        }
    }

    static List _valueType(RealmValueTypes valueType, dynamic value){
        switch(valueType) {
            case RealmValueTypes.objectId:
                return [
                    'ObjectId',
                    value
                ];
            break;
            case RealmValueTypes.boolean:
                return [
                    'Boolean',
                    value
                ];
            break;
            case RealmValueTypes.integer:
                return [
                    'Integer',
                    value
                ];
            break;
            case RealmValueTypes.string:
                return [
                    'String',
                    value
                ];
            break;
            case RealmValueTypes.date:
                return [
                    'Date',
                    value.millisecondsSinceEpoch
                ];
            break;
            case RealmValueTypes.list:
                return [
                    'List',
                    value
                ];
            break;
            default:
                return ['',''];
            break;
        }
    }
}
