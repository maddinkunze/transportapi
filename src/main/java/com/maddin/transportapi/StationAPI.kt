package com.maddin.transportapi

import java.time.Duration
import java.time.LocalDateTime

interface StationAPI {
    fun getStation(id: String): Station
}

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
    private val stationCache = mutableListOf<CachedItem<Station>>()
    override fun addSearch(search: String, results: List<Station>) {
        sessionCache[search] = results

        for (item in results) {
            val existing = stationCache.find { it.station.id == item.id }
            if (existing != null) {
                existing.update()
            } else {
                stationCache.add(CachedItem(item))
            }
        }
    }

    override fun getSearch(search: String): List<Station>? {
        return sessionCache[search]
    }

    private fun stationMatchesSearchAndIsNotTooOld(station: CachedItem<Station>, search: String) : Boolean {
        return station.isValid(expiresAfter) && station.station.matches(search)
    }

    override fun searchStations(search: String): List<Station> {
        return stationCache.mapNotNull { if (stationMatchesSearchAndIsNotTooOld(it, search)) { it.station } else { null } }
    }
}

interface SearchStationAPI {
    fun searchStations(search: String) : List<Station>
}

interface CachedSearchStationAPI : SearchStationAPI {
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

interface LocationArea {

    fun toPolygon() : LocationAreaPolygon {
        return toPolygon(0.01)
    }

    fun toPolygon(precision: Double) : LocationAreaPolygon

    fun toCircle() {
        return toCircle(1.0)
    }

    fun toCircle(contains: Double) {

    }

    private fun toOuterCircle() {

    }

    private fun toInnerCircle() {

    }

    fun toRect() : LocationAreaRect {
        return toOuterRect()
    }

    fun toRect(contains: Double) : LocationAreaRect {
        val outerRect = toOuterRect()
        val innerRect = toInnerRect()

        val center = interpolate(outerRect.center, innerRect.center, contains)
        val width = interpolate(outerRect.width, innerRect.width, contains)
        val height = interpolate(outerRect.height, innerRect.height, contains)
        return LocationAreaRect(center, width, height)
    }

    private fun toOuterRect() : LocationAreaRect {
        val points = toPolygon().points
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (point in points) {
            if (point.lat < minX) { minX = point.lat }
            if (point.lat > maxX) { maxX = point.lat }
            if (point.lon < minY) { minY = point.lon }
            if (point.lon > maxY) { maxY = point.lon }
        }

        val width = maxX - minX
        val height = maxY - minY
        val center = LocationLatLon(minX + height/2, minY + width/2)
        return LocationAreaRect(center, width, height)
    }

    private fun toInnerRect() : LocationAreaRect {
        return toOuterRect()
    }

    private fun interpolate(a: LocationLatLon, b: LocationLatLon, f: Double) : LocationLatLon {
        return LocationLatLon(interpolate(a.lat, b.lon, f), interpolate(a.lat, b.lon, f))
    }

    private fun interpolate(a: Double, b: Double, f: Double) : Double {
        return a * (f-1) + b * f
    }
}


open class LocationAreaRect(val center: LocationLatLon, val width: Double, val height: Double) : LocationArea {
    val topLeft = LocationLatLon(center.lat-height/2, center.lon-width/2)
    val topRight = LocationLatLon(center.lat+height/2, center.lon-width/2)
    val bottomLeft = LocationLatLon(center.lat-height/2, center.lon+width/2)
    val bottomRight = LocationLatLon(center.lat+height/2, center.lon+width/2)
    override fun toPolygon(precision: Double): LocationAreaPolygon {
        return LocationAreaPolygon(listOf(topLeft, topRight, bottomRight, bottomLeft))
    }

    override fun toRect(contains: Double): LocationAreaRect {
        return this
    }
}

open class LocationAreaSquare(center: LocationLatLon, val size: Double) : LocationAreaRect(center, size, size) {

}

open class LocationAreaEllipse(val center: LocationLatLon, val r1: Double, val r2: Double) {

}

open class LocationAreaCircle(center: LocationLatLon, val radius: Double) : LocationAreaEllipse(center, radius, radius)

open class LocationAreaPolygon(val points: List<LocationLatLon>) : LocationArea {
    override fun toPolygon(precision: Double) : LocationAreaPolygon {
        return this
    }
}

interface LocationStationAPI {
    fun locateStations(location: LocationArea) : List<Station>
}