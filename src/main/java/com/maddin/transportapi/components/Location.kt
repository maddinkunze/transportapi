package com.maddin.transportapi.components

import java.io.Serializable

interface Location : Serializable

interface LocationLatLon : Location {
    val lat: Double;
    val lon: Double
}

open class LocationLatLonImpl(override val lat: Double, override val lon: Double) : LocationLatLon


interface LocationArea {
    fun toPolygon() = toPolygon(0.01)
    fun toPolygon(precision: Double) : LocationAreaPolygon
    fun toCircle() = toCircle(1.0)
    fun toCircle(contains: Double) {

    }

    private fun toOuterCircle() {
        // TODO
    }

    private fun toInnerCircle() {
        // TODO
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
        val center = LocationLatLonImpl(minX + height/2, minY + width/2)
        return LocationAreaRect(center, width, height)
    }

    private fun toInnerRect() : LocationAreaRect {
        return toOuterRect() // TODO
    }

    private fun interpolate(a: LocationLatLon, b: LocationLatLon, f: Double) : LocationLatLon {
        return LocationLatLonImpl(interpolate(a.lat, b.lon, f), interpolate(a.lat, b.lon, f))
    }

    private fun interpolate(a: Double, b: Double, f: Double) : Double {
        return a * (f-1) + b * f
    }
}

open class LocationAreaPolygon(val points: List<LocationLatLon>) : LocationArea {
    override fun toPolygon(precision: Double) : LocationAreaPolygon {
        return this
    }
}

open class LocationAreaRect(val center: LocationLatLon, val width: Double, val height: Double) : LocationArea {
    val topLeft = LocationLatLonImpl(center.lat-height/2, center.lon-width/2)
    val topRight = LocationLatLonImpl(center.lat+height/2, center.lon-width/2)
    val bottomLeft = LocationLatLonImpl(center.lat-height/2, center.lon+width/2)
    val bottomRight = LocationLatLonImpl(center.lat+height/2, center.lon+width/2)
    override fun toPolygon(precision: Double): LocationAreaPolygon {
        return LocationAreaPolygon(listOf(topLeft, topRight, bottomRight, bottomLeft))
    }

    override fun toRect(contains: Double): LocationAreaRect {
        return this
    }
}

open class LocationAreaSquare(center: LocationLatLon, val size: Double) : LocationAreaRect(center, size, size)

open class LocationAreaEllipse(val center: LocationLatLon, val r1: Double, val r2: Double) // TODO

open class LocationAreaCircle(center: LocationLatLon, val radius: Double) : LocationAreaEllipse(center, radius, radius)