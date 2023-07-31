package com.maddin.transportapi

interface StationAPI {
    fun getStations(search: String) : List<Station>
}