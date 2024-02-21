package com.maddin.transportapi.utils

import org.json.JSONException
import org.json.JSONObject

fun JSONObject.optStringNull(key: String): String? {
    return try { getString(key) } catch (_: JSONException) { null }
}

fun JSONObject.optIntNull(key: String): Int? {
    return try { getInt(key) } catch (_: JSONException) { null }
}

fun JSONObject.optLongNull(key: String): Long? {
    return try { getLong(key) } catch (_: JSONException) { null }
}