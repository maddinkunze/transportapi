package com.maddin.transportapi.endpoints

import com.maddin.transportapi.caches.POICache
import com.maddin.transportapi.caches.SearchPOICache
import com.maddin.transportapi.components.LocationArea
import com.maddin.transportapi.components.POI
import com.maddin.transportapi.components.POIIdentifier
import com.maddin.transportapi.utils.APIRequest
import com.maddin.transportapi.utils.APIResponse
import com.maddin.transportapi.utils.IThoughtOfThePossibilityException
import kotlin.reflect.KClass

interface POIRequest : APIRequest {
    val poiId: POIIdentifier
    val requiredFeatures: Int; get() = POIAPI.FEATURE_POI_NONE
}

class POIRequestImpl(override val poiId: POIIdentifier): POIRequest {
    constructor(poi: POI) : this(poiId=poi.id!!)
}

interface POIResponse : APIResponse {
    override val request: POIRequest
    val poi: POI?
}

class POIResponseImpl(
    override val request: POIRequest,
    override val poi: POI?,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : POIResponse

interface POIAPI {
    companion object {
        const val FEATURE_POI_NONE = 0
    }

    fun getPOI(request: POIRequest) : POIResponse
    val poiFeatures; get(): Int = FEATURE_POI_NONE
    fun supportsStationFeature(feature: Int): Boolean = (poiFeatures and feature) != 0
    fun getMissingStationFeatures(request: POIRequest): Int = request.requiredFeatures and poiFeatures.inv()
}

interface CachedPOIAPI : POIAPI {
    val poiCache: POICache
    fun getPOIAPI(request: POIRequest) : POIResponse
    override fun getPOI(request: POIRequest): POIResponse {
        poiCache.getItem(request.poiId.uuid)?.let {
            return POIResponseImpl(request=request, poi=it)
        }

        val response = getPOIAPI(request)
        response.poi?.let { poiCache.addItem(it) }
        return response
    }
}


enum class POIUseCase {
    REALTIME,
    TRIP,
}

interface SearchPOIRequest : APIRequest {
    val search: String

    // use this to indicate what you are going to use the results for (i.e. some apis may return
    // loads of POIs but only support trips from Station to Station
    val useCase: POIUseCase?; get() = null

    // use this to indicate that you are only interested in certain types of POI (i.e. only Stations or only Streets)
    // WARNING: the result is not guaranteed to only contain those classes, it is only an indicator for implementations
    // to maybe only request POIs of that type, but if the underlying api does not support that type of distinction
    // it may simply return everything
    val onlySearch: List<KClass<out POI>>?; get() = null
    val requiredFeatures: Int; get() = SearchPOIAPI.FEATURE_SEARCH_POI_NONE
}

class SearchPOIRequestImpl(override val search: String) : SearchPOIRequest

interface SearchPOIResponse : APIResponse {
    override val request: SearchPOIRequest
    val pois: List<POI>
}

class SearchPOIResponseImpl(
    override val request: SearchPOIRequest,
    override val pois: List<POI>,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : SearchPOIResponse

interface SearchPOIAPI {
    companion object {
        const val FEATURE_SEARCH_POI_NONE = 0
    }

    fun searchPOIs(request: SearchPOIRequest) : SearchPOIResponse
    val searchPOIFeatures; get() = FEATURE_SEARCH_POI_NONE
    fun supportsSearchPOIFeature(feature: Int): Boolean = (searchPOIFeatures and feature) != 0
    fun getMissingSearchPOIFeatures(request: SearchPOIRequest): Int = request.requiredFeatures and searchPOIFeatures.inv()
}

interface CachedSearchPOIAPI : SearchPOIAPI {
    val searchPOICache: SearchPOICache?
    fun searchPOIsAPI(request: SearchPOIRequest) : SearchPOIResponse

    // return a mask of all types of poi to be included (and other custom binary search parameters)
    // example: your custom api may wants to implement SearchPOIAPI, SearchTripsAPI and RealtimeAPI
    //          and your RealtimeAPI accepts any type of POI, but your TripAPI only accepts Stations.
    //          You can use the SearchPOIRequest::useCase and SearchPOIRequest::onlySearch parameters
    //          to return the following masks: XX = [Everything except Stations|Stations]
    //          when (request.useCase) {
    //              POIUseCase.TRIP -> 01  // 01 = [Not everything except Stations|Stations] = only Stations
    //              else -> 11             // 11 = [Everything except Stations|Stations] = everything
    //          }
    fun getSearchPOIsMask(request: SearchPOIRequest) : Int = 0

    // filter the (cached) search results so that they are matching the search request
    fun filterSearchPOIs(request: SearchPOIRequest, pois: List<POI>, mask: Int) : List<POI> {
        return pois.filter { poi -> request.onlySearch?.any { cls -> cls.isInstance(poi) } ?: true }
    }
    override fun searchPOIs(request: SearchPOIRequest) : SearchPOIResponse {
        var exceptions: List<IThoughtOfThePossibilityException>? = null

        val mask = getSearchPOIsMask(request)

        // if we have a session cache and we already searched for this specific station, use it
        var pois: List<POI> = searchPOICache?.getSearch(request.search, mask) ?: run {
            // otherwise call api (might be expensive)
            val apiResponse = searchPOIsAPI(request)
            exceptions = apiResponse.exceptions
            searchPOICache?.addSearch(request.search, apiResponse.pois, mask)
            apiResponse.pois
        }

        // if we have cached other stations that would match the search, add them
        // searchPOICache?.let { stations = it.extendSearch(request.search, stations) }

        pois = filterSearchPOIs(request, pois, mask)

        return SearchPOIResponseImpl(request=request, pois=pois, exceptions=exceptions)
    }
}


interface LocatePOIRequest : APIRequest {
    val location: LocationArea

    // use this to indicate what you are going to use the results for (i.e. some apis may return
    // loads of POIs but only support trips from Station to Station
    val useCase: POIUseCase?; get() = null

    // use this to indicate that you are only interested in certain types of POI (i.e. only Stations or only Streets)
    // WARNING: the result is not guaranteed to only contain those classes, it is only an indicator for implementations
    // to maybe only request POIs of that type, but if the underlying api does not support that type of distinction
    // it may simply return everything
    val onlyLocate: List<KClass<out POI>>?; get() = null
    val requiredFeatures: Int; get() = LocatePOIAPI.FEATURE_LOCATE_POI_NONE
}

class LocatePOIRequestImpl(override val location: LocationArea) : LocatePOIRequest

interface LocatePOIResponse : APIResponse {
    override val request: LocatePOIRequest
    val pois: List<POI>
}
class LocatePOIResponseImpl(
    override val request: LocatePOIRequest,
    override val pois: List<POI>,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : LocatePOIResponse

interface LocatePOIAPI {
    companion object {
        const val FEATURE_LOCATE_POI_NONE = 0
    }

    fun locatePOIs(request: LocatePOIRequest) : LocatePOIResponse
    val locatePOIFeatures; get(): Int = FEATURE_LOCATE_POI_NONE
    fun supportsLocatePOIFeature(feature: Int): Boolean = (locatePOIFeatures and feature) != 0
    fun getMissingLocatePOIFeatures(request: LocatePOIRequest): Int = request.requiredFeatures and locatePOIFeatures.inv()
}