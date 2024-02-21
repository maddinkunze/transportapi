package com.maddin.transportapi.caches

import com.maddin.transportapi.caches.SearchCache.Companion.supersedesMask
import com.maddin.transportapi.utils.MaybeIdentifiable
import com.maddin.transportapi.utils.Named
import com.maddin.transportapi.utils.Searchable
import java.time.Duration
import java.time.LocalDateTime

interface SearchCache<T: Searchable> {
    companion object {
        // check if one containsMask completely contains the other, i.e.
        // cache contains:  10110         10100
        // user requests:   10010         10010
        //                  -----         -----
        // (i1^i2)&i2       00000         00010
        //                  valid        invalid
        fun Int.supersedesMask(other: Int) = this.xor(other).and(other) != 0
    }

    fun addSearch(search: String, results: List<T>, containsMask: Int=0)
    fun getSearch(search: String, mustContainMask: Int=0) : List<T>?
    fun searchItems(search: String) : List<T>? = null
    fun extendSearch(search: String, existing: List<T>, onlyAddCertainNonDuplicates: Boolean=true) : List<T> {
        val additional = searchItems(search) ?: return existing
        val extended = existing.toMutableList()
        for (item in additional) {
            val isDuplicate = !isNonDuplicate(item, extended, onlyAddCertainNonDuplicates) // check if the item is already in our extended list -> try to prevent duplicate additions
            if (isDuplicate) { continue }
            extended.add(item)
        }
        return extended
    }

    private fun isNonDuplicate(item: T, items: List<T>, checkForCertainNonDuplicates: Boolean=true) : Boolean {
        val checker = if (checkForCertainNonDuplicates) { ::isCertainNonDuplicate } else { ::isProbablyNonDuplicate }
        for (itemC in items) {
            if (!checker(item, itemC)) { return false }
        }
        return true
    }

    // two items are certainly no duplicates if at least one of them has an id and the ids are not the same
    // otherwise the items might be the same
    fun isCertainNonDuplicate(item1: T, item2: T) : Boolean {
        if (item1 == item2) { return false }

        val id1 = (item1 as? MaybeIdentifiable)?.id
        val id2 = (item2 as? MaybeIdentifiable)?.id

        if ((id1 == null) && (id2 == null)) { return false }
        return id1 != id2
    }

    // two items are probably duplicates if they are the same object
    // or have the same id (and ids != null) or if they have the same name (and names != null)
    fun isProbablyNonDuplicate(item1: T, item2: T) : Boolean {
        if (item1 == item2) { return false }
        val id1 = (item1 as? MaybeIdentifiable)?.id
        val id2 = (item2 as? MaybeIdentifiable)?.id

        if (id1 != id2) { return true }
        else if (id1 != null) { return false } // ids are the same and not null -> duplicate for sure

        val name1 = (item1 as? Named)?.name
        val name2 = (item2 as? Named)?.name

        if (name1 != name2) { return true }
        else if (name1 != null) { return false } // names are the same and not null -> duplicate for sure

        return true // assume they are not duplicates otherwise
    }
}

@Suppress("NewApi")
data class CachedItem<T>(val item: T, var updated: LocalDateTime) {
    constructor(station: T) : this(station, LocalDateTime.now())
    fun update() {
        updated = LocalDateTime.now()
    }

    fun isValid(expiresAfter: Duration) : Boolean {
        return LocalDateTime.now() <= updated + expiresAfter
    }
}

open class SearchCacheImpl<T: Searchable>(private val expiresAfter: Duration) : SearchCache<T> {
    @Suppress("NewApi")
    constructor() : this(Duration.ofDays(7))
    private val sessionCache = mutableMapOf<String, Pair<Int, List<T>>>()
    private val itemCache = mutableListOf<CachedItem<T>>()

    override fun addSearch(search: String, results: List<T>, containsMask: Int) {
        sessionCache[search] = sessionCache[search]?.let { previous ->
            val items = previous.second.toMutableList()
            val containsMaskN = containsMask or previous.first
            val prevSSCur = previous.first.supersedesMask(containsMask)
            val curSSPrev = containsMask.supersedesMask(previous.first)
            if (prevSSCur && curSSPrev) {
                // existing searches may not contain any elements of each other
                // ["S1", "B1", "S2"] + ["P1", "B1", "P2", "P3"] -> ? (TODO)
                // ["S1", "S2", "S3"] + ["P1", "P2", "P3", "P4"] -> ? (TODO)

                // TODO: look for a real solution (probably implement an algorithm for determining the result quality)
                // for now only append those two lists
                items.addAll(results)
            } else if (prevSSCur) {
                // existing search contains more (or more spread out) information
                // ["S1", "B1", "S2", "P1"] + ["S1", "S2", "S3", "S4"] -> ["S1", "B1", "S2", "P1", "S3", "S4"]

                // find the index where the items of the second list are no longer contained in the first list (i.e. S3 and S4)
                val index = results.indexOfFirst { !items.contains(it) }

                // add all items after index of the second list to the existing items
                if (index >= 0) results.listIterator(index).forEachRemaining { items.add(it) }
            } else {
                // existing search contains less (or more dense) information
                // ["S1", "S2", "S3", "S4"] + ["S1", "B1", "S2", "P1"] -> ["S1", "B1", "S2", "P1", "S3", "S4"]

                // go through each item in results and either skip it if it already exists or add it
                for ((index, item) in results.withIndex()) {
                    if (index >= items.size || isProbablyNonDuplicate(items[index], item)) {
                        items.add(index, item)
                    }
                }
            }

            Pair(containsMaskN, items)
        } ?: Pair(containsMask, results)

        for (item in results) {
            val existing = itemCache.find { !isProbablyNonDuplicate(item, it.item) }
            if (existing != null) {
                existing.update()
            } else {
                itemCache.add(CachedItem(item))
            }
        }
    }

    override fun getSearch(search: String, mustContainMask: Int): List<T>? {
        val result = sessionCache[search] ?: return null
        if (!result.first.supersedesMask(mustContainMask)) { return null }
        return result.second
    }

    private fun itemMatchesSearchAndIsNotTooOld(poi: CachedItem<T>, search: String) : Boolean {
        return poi.isValid(expiresAfter) && poi.item.matches(search)
    }

    override fun searchItems(search: String): List<T> {
        return itemCache.mapNotNull { if (itemMatchesSearchAndIsNotTooOld(it, search)) { it.item } else { null } }
    }
}