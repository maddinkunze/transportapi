package com.maddin.transportapi

interface Coordinate {
    val lat: Double
    val lon: Double
}
data class DefaultCoordinate(override val lat: Double, override val lon: Double) : Coordinate

interface Location {
    fun describe() : String
}

interface LocationLatLon : Location, Coordinate {
    override fun describe(): String {
        return "($lat, $lon)"
    }
}
data class DefaultLocationLatLon(override val lat: Double, override val lon: Double) : LocationLatLon

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
    override val location: LocationLatLon
}

data class MinimalStation(
    override val id: String, override var name: String, override val location: Location?) : Station {
    constructor(id: String, name: String) : this(id, name, null)
}

data class DefaultStation(override val id: String, override val name: String, override val location: LocationLatLon) : Station, SearchableStation, LocatableStation {

}