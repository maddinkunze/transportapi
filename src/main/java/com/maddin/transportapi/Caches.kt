package com.maddin.transportapi

import java.io.Serializable
import java.time.LocalDateTime

class SessionCache<T> {
    private val cache: MutableMap<String, List<T>> = mutableMapOf()
    fun getSearch(search: String) : List<T>? {
        return cache[search]
    }
    fun addSearch(search: String, result: List<T>) {
        cache[search] = result
    }
}

/*interface StaticCachable {
    val cacheId: String
}


@Suppress("NewApi")
class StaticCacheItem<T>(val item: T, var updated: LocalDateTime) {
    constructor(item: T) : this(item, LocalDateTime.now())
    fun update() {
        updated = LocalDateTime.now()
    }
}

abstract class CacheSearch<T> {
    abstract fun search(query: String) : List<T>
}

typealias StaticCacheMap<T> = MutableMap<String, StaticCacheItem<T>>

class DefaultCacheSearch<T>(cache: StaticCacheMap<T>) : CacheSearch<T>() {
    override fun search(query: String) : List<T> {

        return emptyList()
    }
}

class StaticCache<T: StaticCachable>(val searchAlgorithm: CacheSearch<T>) {
    constructor() : this(DefaultCacheSearch<T>(cache))
    private var cache: StaticCacheMap<T> = mutableMapOf()
    fun addOrUpdateItems(items: List<T>) {
        for (item in items) {
            cache.getOrPut(item.cacheId) { StaticCacheItem(item) }.update()
        }
    }
    fun extendSearch(search: String, existing: List<T>) : List<T> {
        addOrUpdateItems(existing)
        searchAlgorithm.search(cache.values, search)
        return emptyList()
    }
}*/

interface SearchCache<T> {
    fun getSearch(search: String) : List<T>? { return null }
    fun addSearch(search: String, results: List<T>) {}
    fun extendSearch(search: String, existing: List<T>) : List<T> { return existing }
}