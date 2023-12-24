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
data class CachedStation(val station: Station, var updated: LocalDateTime) {
    constructor(station: Station) : this(station, LocalDateTime.now())
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
    private val stationCache = mutableListOf<CachedStation>()
    override fun addSearch(search: String, results: List<Station>) {
        sessionCache[search] = results

        for (item in results) {
            val existing = stationCache.find { it.station.id == item.id }
            if (existing != null) {
                existing.update()
            } else {
                stationCache.add(CachedStation(item))
            }
        }
    }

    override fun getSearch(search: String): List<Station>? {
        return sessionCache[search]
    }

    private fun stationMatchesSearch(station: Station, search: String) : Boolean {
        return station.name.contains(search, ignoreCase=true)
    }
    private fun stationMatchesSearchAndIsNotTooOld(station: CachedStation, search: String) : Boolean {
        return station.isValid(expiresAfter) && stationMatchesSearch(station.station, search)
    }

    override fun searchStations(search: String): List<Station> {
        return stationCache.mapNotNull { if (stationMatchesSearchAndIsNotTooOld(it, search)) { it.station } else { null } }
    }
}

interface StationAPI {
    fun getStations(search: String) : List<Station>
}

interface CachedStationAPI : StationAPI {
    val stationCache: StationCache?
    fun getStationsAPI(search: String) : List<Station>
    override fun getStations(search: String) : List<Station> {
        // if we have a session cache and we already searched for this specific station, use it
        var stations: List<Station>? = stationCache?.getSearch(search)

        // if we did not find some stations until here, call the api (might be expensive)
        if (stations == null) {
            stations = getStationsAPI(search)
            stationCache?.addSearch(search, stations)
        }

        // if we have cached other stations that would match the search, add them
        if (stationCache != null) {
            stations = stationCache!!.extendSearch(search, stations)
        }

        return stations
    }
}