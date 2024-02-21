package com.maddin.transportapi.endpoints

import com.maddin.transportapi.utils.APIRequest
import com.maddin.transportapi.utils.APIResponse
import com.maddin.transportapi.caches.SearchStationCache
import com.maddin.transportapi.caches.StationCache
import com.maddin.transportapi.components.LocationArea
import com.maddin.transportapi.components.Station
import com.maddin.transportapi.components.StationIdentifier
import com.maddin.transportapi.utils.IThoughtOfThePossibilityException
/*
interface StationRequest : APIRequest {
    val stationId: StationIdentifier
    val requiredFeatures: Int; get() = StationAPI.FEATURE_STATION_NONE
}

class StationRequestImpl(override val stationId: StationIdentifier): StationRequest {
    constructor(station: Station) : this(stationId=station.id)
}

interface StationResponse : APIResponse {
    override val request: StationRequest
    val station: Station?
}

class StationResponseImpl(
    override val request: StationRequest,
    override val station: Station?,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : StationResponse

interface StationAPI {
    companion object {
        const val FEATURE_STATION_NONE = 0
    }
    fun getStation(request: StationRequest): StationResponse
    fun getStationFeatures(): Int = FEATURE_STATION_NONE
    fun supportsStationFeature(feature: Int): Boolean = (getStationFeatures() and feature) != 0
    fun getMissingStationFeatures(request: StationRequest): Int = request.requiredFeatures and getStationFeatures().inv()
}

interface CachedStationAPI : StationAPI {
    val poiCache: StationCache
    fun getStationAPI(request: StationRequest) : StationResponse
    override fun getStation(request: StationRequest): StationResponse {
        poiCache.getItem(request.stationId.uuid)?.let { return StationResponseImpl(request=request, station=it) }

        val response = getStationAPI(request)
        response.station?.let { poiCache.addItem(it) }
        return response
    }
}


interface SearchStationsRequest : APIRequest {
    val search: String
    val requiredFeatures: Int; get() = SearchStationsAPI.FEATURE_SEARCH_STATION_NONE
}

class SearchStationRequestImpl(override val search: String) : SearchStationsRequest

interface SearchStationsResponse : APIResponse {
    override val request: SearchStationsRequest
    val stations: List<Station>
}

class SearchStationsResponseImpl(
    override val request: SearchStationsRequest,
    override val stations: List<Station>,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : SearchStationsResponse

interface SearchStationsAPI {
    companion object {
        const val FEATURE_SEARCH_STATION_NONE = 0
    }
    fun searchStations(request: SearchStationsRequest) : SearchStationsResponse
    fun getSearchStationFeatures() = FEATURE_SEARCH_STATION_NONE
    fun supportsSearchStationFeature(feature: Int): Boolean = (getSearchStationFeatures() and feature) != 0
    fun getMissingSearchStationFeatures(request: SearchStationsRequest): Int = request.requiredFeatures and getSearchStationFeatures().inv()
}

interface CachedSearchStationsAPI : SearchStationsAPI {
    val searchPOICache: SearchStationCache?
    fun searchStationsAPI(request: SearchStationsRequest) : SearchStationsResponse
    override fun searchStations(request: SearchStationsRequest) : SearchStationsResponse {
        var exceptions: List<IThoughtOfThePossibilityException>? = null

        // if we have a session cache and we already searched for this specific station, use it
        var stations: List<Station> = searchPOICache?.getSearch(request.search) ?: run {
            // otherwise call api (might be expensive)
            val apiResponse = searchStationsAPI(request)
            exceptions = apiResponse.exceptions
            searchPOICache?.addSearch(request.search, apiResponse.stations)
            apiResponse.stations
        }

        // if we have cached other stations that would match the search, add them
        searchPOICache?.let { stations = it.extendSearch(request.search, stations) }

        return SearchStationsResponseImpl(request=request, stations=stations, exceptions=exceptions)
    }
}

interface LocateStationsRequest : APIRequest {
    val location: LocationArea
    val requiredFeatures: Int; get() = LocateStationsAPI.FEATURE_LOCATE_STATIONS_NONE
}

class LocateStationsRequestImpl(override val location: LocationArea) : LocateStationsRequest

interface LocateStationsResponse : APIResponse {
    override val request: LocateStationsRequest
    val stations: List<Station>
}
class LocateStationsResponseImpl(
    override val request: LocateStationsRequest,
    override val stations: List<Station>,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : LocateStationsResponse

interface LocateStationsAPI {
    companion object {
        const val FEATURE_LOCATE_STATIONS_NONE = 0
    }

    fun locateStations(request: LocateStationsRequest) : LocateStationsResponse
    fun getLocationStationFeatures(): Int = FEATURE_LOCATE_STATIONS_NONE
    fun supportsLocationStationFeature(feature: Int): Boolean = (getLocationStationFeatures() and feature) != 0
    fun getMissingLocationStationFeatures(request: LocateStationsRequest): Int = request.requiredFeatures and getLocationStationFeatures().inv()
}*/