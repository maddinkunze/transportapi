package com.maddin.transportapi

interface StationCache {
    fun addSearch(search: String, results: List<Station>)
    fun getSearch(search: String) : List<Station>?
}

class DefaultStationCache : StationCache {
    private val sessionCache = mutableMapOf<String, List<Station>>()
    override fun addSearch(search: String, results: List<Station>) {
        sessionCache[search] = results
    }

    override fun getSearch(search: String): List<Station>? {
        return sessionCache[search]
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
        /*if (stationCache != null) {
            stations = stationCache!!.extendSearch(search, stations)
        }*/ // TODO

        return stations
    }
}