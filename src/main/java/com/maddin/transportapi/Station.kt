package com.maddin.transportapi

import java.io.Serializable

open class Location : Serializable

open class LocationLatLon(val lat: Double, val lon: Double) : Location()

interface Searchable {
    fun matches(search: String) : Boolean
}

interface Locatable {
    val location: LocationLatLon
}

open class Station(open val id: String, open val name: String, open val location: Location?) : Serializable, Searchable {
    constructor(id: String, name: String) : this(id, name, null)
    override fun matches(search: String): Boolean {
        return name.contains(search, ignoreCase=true)
    }
}

open class LocatableStation(id: String, name: String, override val location: LocationLatLon) : Station(id, name, location), Locatable