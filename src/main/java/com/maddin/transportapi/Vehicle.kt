package com.maddin.transportapi

interface VehicleType

enum class VehicleTypes : VehicleType {
    BUS,
    TRAM,
}

open class Line(val id: String, val name: String)

open class Direction(open val name: String) {
    protected constructor() : this("")
}

open class StationDirection(val station: Station) : Direction() {
    override val name: String
        get() { return station.name }
}

open class Vehicle(val type: VehicleType?, val line: Line?, val direction: Direction?)