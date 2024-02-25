package com.maddin.transportapi.utils

import com.maddin.transportapi.components.Stop
import com.maddin.transportapi.components.StopImpl
import com.maddin.transportapi.components.Trip
import com.maddin.transportapi.components.TripConnectionImpl
import com.maddin.transportapi.components.TripImpl
import com.maddin.transportapi.endpoints.ConnectionAPI
import com.maddin.transportapi.endpoints.ConnectionRequestImpl
import com.maddin.transportapi.endpoints.TripAPI
import com.maddin.transportapi.endpoints.TripRequestImpl
import com.maddin.transportapi.endpoints.TripResponse

interface APIHelperResponse {
    val realResponse: APIResponse?
    val exceptions: List<IThoughtOfThePossibilityException>?
}

class HelperTripResponse(
    val trip: Trip?,
    override val realResponse: TripResponse?=null,
    override val exceptions: List<IThoughtOfThePossibilityException>?=null
) : APIHelperResponse {
    constructor(response: TripResponse) : this(response.trip, response, response.exceptions)
}


@Suppress("unused", "MemberVisibilityCanBePrivate")
class APIHelper { companion object {
    fun updateTrip(api: API, trip: Trip): HelperTripResponse {
        val tripId = trip.id
        if ((api is TripAPI) && (tripId != null)) {
            val request = TripRequestImpl(tripId=tripId)
            val response = api.getTrip(request)
            return HelperTripResponse(response=response)
        }

        if (api is ConnectionAPI) {
            return estimateTripUpdateUsingConnections(api, trip)
        }

        return HelperTripResponse(trip=trip)
    }

    fun estimateTripUpdateUsingConnections(api: ConnectionAPI, trip: Trip): HelperTripResponse {
        val exceptions = mutableListOf<IThoughtOfThePossibilityException>()
        val connections = trip.connections.map { connO ->
            val connId = connO.id ?: return@map connO

            val request = ConnectionRequestImpl(connectionId=connId)
            val response = api.getConnection(request)
            val conn = response.connection ?: return@map connO
            response.exceptions?.let { exceptions.addAll(it) }
            val stops = conn.stops.toMutableList()

            // try to guess the new "from" and "to" stops
            // find the stop that has the same poi (may be dangerous for circular connections) if the original from/to had an id
            // if there originally was a stop with id but it could not be found in this connection, assume that it has been cancelled
            val stopFrom = trip.from.poi.id?.let { id ->
                stops.find { st -> id == st.poi.id } ?: makeStopCopyCancelled(trip.from)
            } ?: trip.from
            insertMissingStopStart(stops, stopFrom)

            val stopTo = trip.to.poi.id?.let { id ->
                stops.find { st -> id == st.poi.id } ?: makeStopCopyCancelled(trip.to)
            } ?: trip.to
            insertMissingStopEnd(stops, stopTo)

            TripConnectionImpl(
                id=conn.id?:connId, stops=stops, from=stopFrom, to=stopTo,
                modeOfTransport=conn.modeOfTransport?:connO.modeOfTransport,
                path=conn.path?:connO.path,
                flags=conn.flags
            )
        }

        val tripN = TripImpl(id=trip.id, connections=connections)
        return HelperTripResponse(trip=tripN, exceptions=exceptions)
    }

    private fun makeStopCopyCancelled(stop: Stop): Stop {
        return StopImpl(
            poi=stop.poi,
            platformPlanned=stop.platformPlanned,
            departurePlanned=stop.departurePlanned,
            departureActual=stop.departureActual,
            arrivalPlanned=stop.arrivalPlanned,
            arrivalActual=stop.arrivalActual,
            flags=stop.flags or Stop.FLAG_CANCELLED,
            additionalNotes=stop.additionalNotes.toMutableList()
        )
    }

    private fun insertMissingStopStart(stops: MutableList<Stop>, stop: Stop) {
        if (stops.contains(stop)) { return }
        val indexFirst = 0
        val index = stop.estimateArrivalActual()?.let { arrival ->
            stops.indexOfFirst {
                arrival > (it.estimateDepartureActual() ?: return@indexOfFirst true)
            }.coerceAtLeast(indexFirst)
        } ?: indexFirst
        stops.add(index, stop)
    }

    private fun insertMissingStopEnd(stops: MutableList<Stop>, stop: Stop) {
        if (stops.contains(stop)) { return }
        val indexLast = stops.size+1
        val index = stop.estimateDepartureActual()?.let { departure ->
            stops.indexOfLast {
                departure < (it.estimateArrivalActual() ?: return@indexOfLast true)
            }.mod(indexLast)
        } ?: indexLast
        stops.add(index, stop)
    }
}}