package com.maddin.transportapi.components

import com.maddin.transportapi.utils.Translatable
import com.maddin.transportapi.utils.Translations
import java.io.Serializable
import java.util.Locale

interface Distance : Translatable {
    companion object {
        fun milesToKm(miles: Double) = 1.60934 * miles
        fun kmToMiles(km: Double) = 0.621371 * km
        fun kmToMeters(km: Double) = 1000 * km
        fun metersToKm(meters: Double) = meters / 1000
    }

    val meters; get() = kmToMeters(km)
    val km: Double
    val kilometers; get() = km
    val miles; get() = kmToMiles(km)

    override fun translate(locale: Locale?, translations: Translations?): String {
        return "$meters m" // TODO
    }
}

open class DistanceImpl private constructor(override val km: Double, override val miles: Double) : Serializable, Distance {
    constructor(km: Double) : this(km, Distance.kmToMiles(km))
    companion object {
        fun fromMiles(miles: Double) = DistanceImpl(Distance.milesToKm(miles), miles)
        fun fromKm(km: Double) = DistanceImpl(km)
        fun fromMeters(meters: Double) = DistanceImpl(Distance.metersToKm(meters))
    }
}

fun Double.toDistanceKm() = DistanceImpl.fromKm(this)
fun Double.toDistanceMeters() = DistanceImpl.fromMeters(this)
fun Double.toDistanceMiles() = DistanceImpl.fromMiles(this)
fun Float.toDistanceKm() = toDouble().toDistanceKm()
fun Float.toDistanceMeters() = toDouble().toDistanceMeters()
fun Float.toDistanceMiles() = toDouble().toDistanceMiles()
fun Int.toDistanceKm() = toDouble().toDistanceKm()
fun Int.toDistanceMeters() = toDouble().toDistanceMeters()
fun Int.toDistanceMiles() = toDouble().toDistanceMiles()


interface Speed : Serializable, Translatable {
    companion object {
        fun mphToKmh(mph: Double) = 1.60934 * mph
        fun kmhToMph(kmh: Double) = 0.621371 * kmh
    }
    val kmh: Double
    val kilometersPerHour; get() = kmh
    val mph: Double; get() = kmhToMph(kmh)
    val milesPerHour; get() = mph

    override fun translate(locale: Locale?, translations: Translations?): String {
        return "$kmh km/h" // TODO
    }
}
val Speed.isJogging; get() = kmh >= 6.5
val Speed.isRunning; get() = kmh >= 9.5

open class SpeedImpl private constructor(override val kmh: Double, override val mph: Double) : Speed {
    constructor(kmh: Double) : this(kmh, Speed.kmhToMph(kmh))

    companion object {
        fun fromMph(mph: Double) = SpeedImpl(Speed.mphToKmh(mph), mph)
        fun fromKmh(kmh: Double) = SpeedImpl(kmh)
    }
}
fun Double.toSpeedKmh() = SpeedImpl.fromKmh(this)
fun Double.toSpeedMph() = SpeedImpl.fromMph(this)
fun Float.toSpeedKmh() = toDouble().toSpeedKmh()
fun Float.toSpeedMph() = toDouble().toSpeedMph()
fun Int.toSpeedKmh() = toDouble().toSpeedKmh()
fun Int.toSpeedMph() = toDouble().toSpeedMph()