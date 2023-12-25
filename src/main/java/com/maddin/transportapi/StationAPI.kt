package com.maddin.transportapi

import java.time.Duration
import java.time.LocalDateTime

interface StationCache {
    fun addSearch(search: String, results: List<Station>)
    fun getSearch(search: String) : List<Station>?
    fun searchStations(search: String) : List<Station>
    fun extendSearch(search: String, existing: List<Station>) : List<Station> {
        val additional = searchStations(search)
        val extended = existing.toMutableList()
        for (item in additional) {
            if (extended.find { it.id == item.id } != null) { continue }
            extended.add(item)
        }
        return extended
    }
}

@Suppress("NewApi")
data class CachedItem<T>(val station: T, var updated: LocalDateTime) {
    constructor(station: T) : this(station, LocalDateTime.now())
    fun update() {
        updated = LocalDateTime.now()
    }

    fun isValid(expiresAfter: Duration) : Boolean {
        return LocalDateTime.now() <= updated + expiresAfter
    }
}

open class DefaultStationCache(private val expiresAfter: Duration) : StationCache {
    @Suppress("NewApi")
    constructor() : this(Duration.ofDays(7))
    private val sessionCache = mutableMapOf<String, List<Station>>()
    private val stationCache = mutableListOf<CachedItem<SearchableStation>>()
    override fun addSearch(search: String, results: List<Station>) {
        sessionCache[search] = results

        for (item in results) {
            val existing = stationCache.find { it.station.id == item.id }
            if (existing != null) {
                existing.update()
            } else if (item is SearchableStation) {
                stationCache.add(CachedItem(item))
            }
        }
    }

    override fun getSearch(search: String): List<Station>? {
        return sessionCache[search]
    }

    private fun stationMatchesSearchAndIsNotTooOld(station: CachedItem<SearchableStation>, search: String) : Boolean {
        return station.isValid(expiresAfter) && station.station.matches(search)
    }

    override fun searchStations(search: String): List<Station> {
        return stationCache.mapNotNull { if (stationMatchesSearchAndIsNotTooOld(it, search)) { it.station } else { null } }
    }
}

interface StationAPI {
    fun searchStations(search: String) : List<Station>
}

interface CachedStationAPI : StationAPI {
    val stationCache: StationCache?
    fun searchStationsAPI(search: String) : List<Station>
    override fun searchStations(search: String) : List<Station> {
        // if we have a session cache and we already searched for this specific station, use it
        var stations: List<Station>? = stationCache?.getSearch(search)

        // if we did not find some stations until here, call the api (might be expensive)
        if (stations == null) {
            stations = searchStationsAPI(search)
            stationCache?.addSearch(search, stations)
        }

        // if we have cached other stations that would match the search, add them
        if (stationCache != null) {
            stations = stationCache!!.extendSearch(search, stations)
        }

        return stations
    }
}