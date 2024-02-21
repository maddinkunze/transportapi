package com.maddin.transportapi.utils

import java.net.URL
import java.net.URLEncoder

fun String.url(): String {
    return URLEncoder.encode(this, "UTF-8")
}

class URLBuilder<T> {
    private lateinit var host: (T) -> String
    private val path = mutableListOf<(T) -> String?>()
    private val params = mutableListOf<Pair<String, (T) -> String?>>()

    fun setHost(host: String): URLBuilder<T> {
        this.host = { _ -> host}
        return this
    }

    fun setHost(evaluator: (T) -> String): URLBuilder<T> {
        host = evaluator
        return this
    }

    fun addPath(path: String): URLBuilder<T> {
        this.path.add { _ -> path }
        return this
    }

    fun addPaths(vararg paths: String): URLBuilder<T> {
        for (path in paths) {
            addPath(path)
        }
        return this
    }

    fun addPath(evaluator: (T) -> String?): URLBuilder<T> {
        path.add(evaluator)
        return this
    }

    fun addParam(param: String, value: String): URLBuilder<T> {
        params.add(Pair(param) { _ -> value })
        return this
    }

    fun addParam(param: String, evaluator: (T) -> String?): URLBuilder<T> {
        params.add(Pair(param, evaluator))
        return this
    }

    fun addParams(vararg params: String): URLBuilder<T> {
        for (param in params) {
            val split = param.split("=", limit=2)
            addParam(split[0], split[1])
        }
        return this
    }

    fun build(value: T): String {
        val host = host(value)
        val path = path.mapNotNull { it(value)?.url() }.joinToString("/")
        var params = params.mapNotNull { p -> p.second(value)?.url()?.let { "${p.first}=${it}" } }.joinToString("&")
        if (params.isNotBlank()) { params = "?$params" }
        return "$host/$path$params"
    }
}