package com.maddin.transportapi

interface Location {
    fun describe() : String
}

interface LocationLongLat : Location {
    val lat: Double
    val lon: Double

    override fun describe(): String {
        return "($lat, $lon)"
    }
}
data class DefaultLocationLongLat(override val lat: Double, override val lon: Double) : LocationLongLat

interface Station {
    val id: String
    val name: String
    val location: Location?
}

interface SearchableStation : Station {
    fun matches(search: String) : Boolean {
        return name.contains(search, ignoreCase=true)
    }
}

interface LocatableStation : Station {
    override val location: LocationLongLat
}

data class MinimalStation(
    override val id: String, override var name: String, override val location: Location?) : Station {
    constructor(id: String, name: String) : this(id, name, null)
}

data class DefaultStation(override val id: String, override val name: String, override val location: LocationLongLat) : Station, SearchableStation, LocatableStation {

}