package com.ninsaude.ecorealm

import io.realm.RealmObject
import java.util.Date
import org.bson.types.ObjectId
import io.realm.RealmList
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import io.realm.annotations.RealmClass

open class customer(
    @PrimaryKey var _id: ObjectId? = null,
    var _partition: String = "",
    var active: Boolean? = true,
    var avatar: ByteArray? = null,
    var birthday: Date? = null,
    var created_at: Date? = null,
    var email: String? = null,
    var first_name: String = "",
    var last_name: String = "",
    var modified_at: Date? = null,
    var observation: String? = null,
    var phone: String? = null,
    var sex: String? = null,
    var social_name: String? = null
): RealmObject() {}

open class appointment(
    @PrimaryKey var _id: ObjectId? = null,
    var _partition: String = "",
    var created_at: Date? = null,
    var customer: customer? = null,
    var date_time: Date = Date(),
    var duration: Long = 0,
    var modified_at: Date? = null,
    var observation: String? = null,
    var status: String? = null
): RealmObject() {}

open class configuration(
    @PrimaryKey var _id: ObjectId? = null,
    var _partition: String = "",
    var created_at: Date? = null,
    var email: String = "",
    var first_name: String = "",
    var language: String = "",
    var last_name: String = "",
    var modified_at: Date? = null,
    var social_name: String? = null,
    var subscription: RealmList<configuration_subscription> = RealmList(),
    var timezone: String = ""
): RealmObject() {}

@RealmClass(embedded = true)
open class configuration_subscription(
    var id: String = "",
    var name: String = ""
): RealmObject() {}

open class record(
    @PrimaryKey var _id: ObjectId? = null,
    var _partition: String = "",
    var content: record_content? = null,
    var created_at: Date? = null,
    var customer: customer? = null,
    var date_time: Date = Date(),
    var description: String = "",
    var modified_at: Date? = null,
    var source: String = "",
    @Required
    var tags: RealmList<String> = RealmList()
): RealmObject() {}

@RealmClass(embedded = true)
open class record_content(
    var binary: ByteArray? = null,
    var text: String = ""
): RealmObject() {}

open class text_suggestion(
    @PrimaryKey var _id: ObjectId? = null,
    var _partition: String = "",
    var counter: Long? = 1,
    var from: String = "",
    var to: String = ""
): RealmObject() {}
