package com.maddin.transportapi

data class Vehicle(val name: String, val identifier: String, val directionName: String, val directionId: Int) {
    constructor(name: String, identifier: String, direction: String)
            : this(name, identifier, direction, 0)
}