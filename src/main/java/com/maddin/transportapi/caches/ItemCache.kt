package com.maddin.transportapi.caches

import com.maddin.transportapi.utils.MaybeIdentifiable

interface ItemCache<T : MaybeIdentifiable> {
    fun addItem(item: T)
    fun getItem(id: String): T?
}

open class ItemCacheImpl<T: MaybeIdentifiable> : ItemCache<T> {
    protected val sessionCache = mutableMapOf<String, T>()
    override fun addItem(item: T) {
        sessionCache[item.id?.uuid ?: return] = item
    }
    override fun getItem(id: String): T? {
        return sessionCache[id]
    }
}