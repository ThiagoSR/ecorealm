
import 'dart:async';
                       
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

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
                "value": valores[1],
                "valueType": valores[2] 
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
                "value": valores[1],
                "valueType": valores[2]
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
                "value": valores[1],
                "valueType": valores[2] 
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
                "value": valores[1],
                "valueType": valores[2]
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
                "value": valores[1],
                "valueType": valores[2]
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

    static String _logicalOperator(RealmLogicalOperator operador) {
        String lOperador = '';
        switch(operador) {
            case RealmLogicalOperator.equals:
                lOperador = 'equals';
            break;
            case RealmLogicalOperator.like:
                lOperador = 'like';
            break;
            case RealmLogicalOperator.greater:
                lOperador = 'greater';
            break;
            case RealmLogicalOperator.lesser:
                lOperador = 'lesser';
            break;
            case RealmLogicalOperator.beetwen:
                lOperador = 'beetwen';
            break;
            case RealmLogicalOperator.notNull:
                lOperador = 'notNull';
            break;
            default:
                lOperador = 'equals';
            break; 
        }
        return lOperador;
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
                return [];
            break;
        }
    }
}
