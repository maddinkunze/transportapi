package com.maddin.transportapi

interface StationAPI {
    val sessionCache: List<String>?
    fun getStationsAPI(search: String) : List<Station>
    fun getStations(search: String) : List<Station> {
        return getStationsAPI(search)
    }
}