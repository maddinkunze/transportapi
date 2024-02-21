package com.maddin.transportapi.utils

import java.io.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale

// this file contains shared interfaces that describe how any component/artefact (station, trip, poi, ...) can be interacted with


// API should be implemented on all classes that implements and fills api endpoints
// usage example: class BostonTransport : API, SearchStationAPI, TripAPI
interface API
interface APIRequest
interface APIResponse {
    val request: APIRequest
    val exceptions: List<IThoughtOfThePossibilityException>?; get() = null
}

interface Identifier : Serializable {
    val uuid: String // universally unique id, only ever exists once
    val uid: String; get() = uuid // unique id, exists once in a given circumstance
                                  // for example, a platform id can only exist once for every station,
                                  // but there may be different stations that have platforms with the same id
    companion object {
        const val SAFE_CONCAT = ":::~:::" // it is safe to assume that no id will ever naturally contain this
        const val SAFE_EMPTY = "[[[NULL]]]"
    }
    fun concat(vararg parts: Any?): String {
        return parts.joinToString(SAFE_CONCAT) {
            when (it) {
                is Identifier -> it.uuid
                is String -> it
                else -> SAFE_EMPTY
            }
        }
    }
}

open class IdentifierImpl(override val uuid: String) : Identifier {
    override fun equals(other: Any?): Boolean { return when (other) {
        is Identifier -> other.uuid == uuid
        is String -> other == uuid
        else -> false
    } }

    override fun toString(): String {
        return uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}

interface MaybeIdentifiable {
    val id: Identifier?
}

interface Identifiable : MaybeIdentifiable {
    override val id: Identifier
}

interface MaybeNamed {
    val name: String?
}

interface Named : MaybeNamed {
    override val name: String
}

interface Searchable {
    fun matches(search: String) : Boolean
}

typealias Translations = Map<String, String>
typealias MutableTranslations = MutableMap<String, String>
interface Translatable {
    fun translate(locale: Locale? = null, translations: Translations? = null): String
}

fun String.toTranslatable() = object: Translatable {
    override fun translate(locale: Locale?, translations: Translations?): String = this@toTranslatable
}

private fun durationBetween(s: LocalDateTime?, e: LocalDateTime?) = if (s == null || e == null) null else Duration.between(s, e)

interface MaybeHasArrivalDeparture {
    val arrivalPlanned: LocalDateTime?
    val arrivalActual: LocalDateTime?
    val departurePlanned: LocalDateTime?
    val departureActual: LocalDateTime?

    fun estimateArrivalPlanned(fallbackToDeparture: Boolean=true): LocalDateTime? = arrivalPlanned ?: arrivalActual ?: if (fallbackToDeparture) { estimateDeparturePlanned(false) } else { null }
    fun estimateArrivalActual(fallbackToDeparture: Boolean=true): LocalDateTime? = arrivalActual ?: arrivalPlanned ?: if (fallbackToDeparture) { estimateDepartureActual(false) } else { null }
    fun estimateDeparturePlanned(fallbackToArrival: Boolean=true): LocalDateTime? = departurePlanned ?: departureActual ?: if (fallbackToArrival) { estimateArrivalPlanned(false) } else { null }
    fun estimateDepartureActual(fallbackToArrival: Boolean=true): LocalDateTime? = departureActual ?: departurePlanned ?: if (fallbackToArrival) { estimateArrivalActual(false) } else { null }

    fun departsOrArrivesIn(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, estimateDepartureActual())
    fun departsIn(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, estimateDepartureActual(false))
    fun departsInPlanned(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, departurePlanned)
    fun departsInActual(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, departureActual)
    fun arrivesOrDepartsIn(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, estimateArrivalActual())
    fun arrivesIn(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, estimateArrivalActual(false))
    fun arrivesInPlanned(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, arrivalPlanned)
    fun arrivesInActual(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, arrivalActual)
}

interface MaybeHasStartAndEnd {
    val startPlanned: LocalDateTime?
    val startActual: LocalDateTime?
    val endPlanned: LocalDateTime?
    val endActual: LocalDateTime?

    fun estimateStartPlanned(fallbackToEnd: Boolean=true): LocalDateTime? = startPlanned ?: startActual ?: if (fallbackToEnd) { estimateEndPlanned(false) } else { null }
    fun estimateStartActual(fallbackToEnd: Boolean=true): LocalDateTime? = startActual ?: startPlanned ?: if (fallbackToEnd) { estimateEndActual(false) } else { null }
    fun estimateEndPlanned(fallbackToStart: Boolean=true): LocalDateTime? = endPlanned ?: endActual ?: if (fallbackToStart) { estimateStartPlanned(false) } else { null }
    fun estimateEndActual(fallbackToStart: Boolean=true): LocalDateTime? = endActual ?: endPlanned ?: if (fallbackToStart) { estimateStartActual(false) } else { null }

    fun startsIn(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, estimateStartActual())
    fun startsInPlanned(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, startPlanned)
    fun startsInActual(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, startActual)
    fun endsIn(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, estimateEndActual())
    fun endsInPlanned(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, endPlanned)
    fun endsInActual(from: LocalDateTime=LocalDateTime.now()) = durationBetween(from, endActual)

    val duration; get() = durationBetween(estimateStartActual(), estimateEndActual())
}