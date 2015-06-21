package com.timepath.steam.webapi

import com.timepath.Logger
import com.timepath.web.api.base.RequestBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Field
import javax.xml.bind.DatatypeConverter
import kotlin.properties.Delegates

fun Field.getPrivate(obj: Any?): Any? {
    val access = isAccessible()
    setAccessible(true)
    try {
        return get(obj)
    } finally {
        setAccessible(access)
    }
}

fun JSONObject.toMap() = javaClass.getDeclaredField("map").getPrivate(this) as Map<String, *>

fun JSONArray.toList() = javaClass.getDeclaredField("myArrayList").getPrivate(this) as List<*>

val LOG = Logger()

object Endpoint {
    val API = SteamConnection("https://api.steampowered.com/")
    val COMMUNITY = SteamConnection("https://steamcommunity.com/")
}

/** oauth: 3638BFB1, DE45CD61 */
val CLIENTID = "DE45CD61"

fun dologin(username: String,
            cipherData: ByteArray,
            rsatimestamp: String,
            emailsteamid: String,
            eauth: String,
            captchagid: Long,
            captcha: String): JSONObject = RequestBuilder.fromArray(arrayOf(
        // Credentials
        arrayOf("username", username),
        arrayOf("password", DatatypeConverter.printBase64Binary(cipherData)),
        arrayOf("rsatimestamp", rsatimestamp),
        // Steamguard
        arrayOf("emailauth", eauth),
        arrayOf("emailsteamid", emailsteamid),
        // Captcha
        arrayOf("captchagid", captchagid),
        arrayOf("captcha_text", captcha),
        // User preferences
        arrayOf("loginfriendlyname", "TimePath java test"),
        arrayOf("remember_login", true),
        // Required
        arrayOf("oauth_client_id", CLIENTID),
        arrayOf("oauth_scope", arrayOf("read_profile", "write_profile", "read_client", "write_client").join(" ")),
        arrayOf("donotcache", System.currentTimeMillis())
)).let {
    Endpoint.COMMUNITY.postget("mobilelogin/dologin", it.toString())
}.let { JSONObject(it) }

val umqid: Long /* get() */ = (Math.random() * Long.MAX_VALUE.toDouble()).toLong()

class Logon(val json: Map<String, *>) {
    override fun toString() = json.toString()

    val steamid by Delegates.mapVal<String>(json)
    val error by Delegates.mapVal<String>(json)
    val umqid by Delegates.mapVal<String>(json)
    val timestamp by Delegates.mapVal<Long>(json)
    val utc_timestamp by Delegates.mapVal<Long>(json)
    val message by Delegates.mapVal<Int>(json)
    val push by Delegates.mapVal<Int>(json)
}

fun logon(token: String): Logon = RequestBuilder.fromArray(arrayOf(
        arrayOf("access_token", token),
        arrayOf("umqid", umqid),
        arrayOf("ui_mode", "web")
)).let {
    Endpoint.API.postget("ISteamWebUserPresenceOAuth/Logon/v0001", it.toString())
}.let {
    Logon(JSONObject(it).toMap())
}

class Poll(val json: Map<String, *>) {
    override fun toString() = json.toString()
    val pollid by Delegates.mapVal<Int>(json)

    class Message(val json: Map<String, *>) {
        override fun toString() = json.toString()
        val type by Delegates.mapVal<String>(json)
        val timestamp by Delegates.mapVal<Long>(json)
        val utc_timestamp by Delegates.mapVal<Long>(json)
        val steamid_from by Delegates.mapVal<String>(json)
        val status_flags by Delegates.mapVal<Int>(json)
        val persona_state by Delegates.mapVal<Int>(json)
        val persona_name by Delegates.mapVal<String>(json)
    }

    val messages: List<Message> by Delegates.lazy {
        val field = json.get("messages")
        (field as JSONArray).toList().map {
            Message((it as JSONObject).toMap())
        }
    }
    val messagelast by Delegates.mapVal<Int>(json)
    val timestamp by Delegates.mapVal<Long>(json)
    val utc_timestamp by Delegates.mapVal<Long>(json)
    val messagebase by Delegates.mapVal<Int>(json)
    val sectimeout by Delegates.mapVal<Int>(json)
    val error by Delegates.mapVal<String>(json)
}

fun poll(token: String, lmid: Int) = RequestBuilder.fromArray(arrayOf(
        arrayOf("access_token", token),
        arrayOf("umqid", umqid),
        arrayOf("message", lmid),
        arrayOf("sectimeout", 30),
        arrayOf("secidletime", 0) // AWAY: 600+, SNOOZE: 8000+
)).let {
    Endpoint.API.postget("ISteamWebUserPresenceOAuth/Poll/v0001", it.toString())
}.let {
    LOG.info { it!! }
    Poll(JSONObject(it).toMap())
}
