package com.maddin.transportapi.utils

import java.net.HttpURLConnection
import java.net.URL

typealias typefun<T> = (T) -> String?

class APIEndpoint<S, T> {
    internal lateinit var method: typefun<S>
    internal lateinit var base: typefun<S>
    internal val paths = mutableListOf<typefun<S>>()
    internal val params = mutableListOf<typefun<S>>()

    fun setMethod(method: String): APIEndpoint<S, T> {
        this.method = { _ -> method }
        return this
    }

    fun setMethod(method: typefun<S>): APIEndpoint<S, T> {
        this.method = method
        return this
    }

    fun setBase(base: String): APIEndpoint<S, T> {
        this.base = { _ -> base }
        return this
    }

    fun setBase(base: typefun<S>): APIEndpoint<S, T> {
        this.base = base
        return this
    }

    fun addPath(path: String): APIEndpoint<S, T> {
        paths.add { _ -> path }
        return this
    }

    fun addPath(path: typefun<S>): APIEndpoint<S, T> {
        paths.add(path)
        return this
    }
    fun addParam(key: String, value: String): APIEndpoint<S, T> {

        return this
    }

    fun <S> call(): T? {
        URL("")
        return null
    }
}