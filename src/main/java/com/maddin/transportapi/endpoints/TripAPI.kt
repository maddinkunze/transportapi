package com.maddin.transportapi.endpoints

import com.maddin.transportapi.components.POI
import com.maddin.transportapi.components.Trip
import com.maddin.transportapi.components.TripIdentifier
import com.maddin.transportapi.utils.APIRequest
import com.maddin.transportapi.utils.APIResponse
import com.maddin.transportapi.utils.IThoughtOfThePossibilityException
import java.time.LocalDateTime

interface TripRequest : APIRequest {
    val tripId: TripIdentifier
}

open class TripRequestImpl(override val tripId: TripIdentifier) : TripRequest {
    constructor(trip: Trip) : this(trip.id!!)
}

interface TripResponse : APIResponse {
    val trip: Trip?
    override val request: TripRequest
}

open class TripResponseImpl(
    override val trip: Trip?,
    override val request: TripRequest,
    override val exceptions: List<IThoughtOfThePossibilityException>?=null,
) : TripResponse

interface TripAPI {
    fun getTrip(request: TripRequest) : TripResponse
}


interface TripSearchRequest : APIRequest {
    val waypoints: List<POI>
    val waypointsIdentifiable: List<POI>; get() = waypoints.filter { it.id != null }
    val time: LocalDateTime?; get() = null
    val timeSpec: String?; get() = null

    companion object {
        const val TRIP_TIME_DEPARTURE = "depart"
        const val TRIP_TIME_ARRIVAL = "arrive"
    }

    val start; get() = waypoints[0]
    val identifiableStart; get() = waypointsIdentifiable[0]
    val identifiableStartId; get() = identifiableStart.id!!

    val firstVia; get() = if (waypoints.size < 3) null else waypoints[1]
    val firstIdentifiableVia; get() = if (waypointsIdentifiable.size < 3) null else waypointsIdentifiable[1]
    val firstIdentifiableViaId; get() = firstIdentifiableVia?.id

    val secondVia; get() = if (waypoints.size < 4) null else waypoints[2]
    val secondIdentifiableVia; get() = if (waypointsIdentifiable.size < 4) null else waypointsIdentifiable[2]
    val secondIdentifiableViaId; get() = secondIdentifiableVia?.id

    val lastVia; get() = if (waypoints.size < 3) null else waypoints[waypoints.size-2]
    val lastIdentifiableVia; get() = if (waypointsIdentifiable.size < 3) null else waypointsIdentifiable[waypointsIdentifiable.size-2]
    val lastIdentifiableViaId; get() = lastIdentifiableVia?.id

    val end; get() = waypoints.last()
    val identifiableEnd; get() = waypointsIdentifiable.last()
    val identifiableEndId; get() = identifiableEnd.id!!

    fun getRequiredFeatures(): Int {
        var features = TripSearchAPI.FEATURE_SEARCH_TRIP_NONE
        if ((time != null) && (timeSpec == TRIP_TIME_DEPARTURE)) { features = features or TripSearchAPI.FEATURE_SEARCH_TRIP_CUSTOM_DEPARTURE }
        if ((time != null) && (timeSpec == TRIP_TIME_ARRIVAL)) { features = features or TripSearchAPI.FEATURE_SEARCH_TRIP_CUSTOM_ARRIVAL }
        if (waypoints.size != waypointsIdentifiable.size) { features = features or TripSearchAPI.FEATURE_SEARCH_TRIP_UNIDENTIFIABLE_POIS }
        return features
    }
}
class TripSearchRequestImpl(
    override val waypoints: List<POI>,
    override var time: LocalDateTime?=null,
    override var timeSpec: String?=null
) : TripSearchRequest

interface TripSearchResponse : APIResponse {
    override val request: TripSearchRequest
    val trips: List<Trip>
}
class TripSearchResponseImpl(
    override val request: TripSearchRequest,
    override val trips: List<Trip>,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : TripSearchResponse

interface TripSearchAPI {
    companion object {
        protected fun getSearchTripWaypointCount(allowedVias: Int) = 2 + allowedVias
        val TRIP_SEARCH_WAYPOINT_COUNT_FROM_TO = getSearchTripWaypointCount(0)
        val TRIP_SEARCH_WAYPOINT_COUNT_FROM_VIA_TO = getSearchTripWaypointCount(1)
        const val TRIP_SEARCH_WAYPOINT_COUNT_INFINITE = -1

        const val FEATURE_SEARCH_TRIP_NONE = 0
        const val FEATURE_SEARCH_TRIP_CUSTOM_DEPARTURE = 1
        const val FEATURE_SEARCH_TRIP_CUSTOM_ARRIVAL = 2
        const val FEATURE_SEARCH_TRIP_UNIDENTIFIABLE_POIS = 4
    }
    fun searchTrips(request: TripSearchRequest) : TripSearchResponse
    fun getSearchTripWaypointCount() : Int = TRIP_SEARCH_WAYPOINT_COUNT_FROM_TO
    fun getSearchTripFeatures(): Int = FEATURE_SEARCH_TRIP_NONE
    fun supportsSearchTripFeature(feature: Int): Boolean = (getSearchTripFeatures() and feature) != 0
    fun getMissingSearchTripFeatures(request: TripSearchRequest): Int = request.getRequiredFeatures() and getSearchTripFeatures().inv()
}