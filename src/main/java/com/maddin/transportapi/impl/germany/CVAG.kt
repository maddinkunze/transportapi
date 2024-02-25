package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.caches.SearchPOICache
import com.maddin.transportapi.caches.SearchPOICacheImpl
import com.maddin.transportapi.components.DirectionImpl
import com.maddin.transportapi.components.LineImpl
import com.maddin.transportapi.components.LineMOTImpl
import com.maddin.transportapi.components.LineVariantImpl
import com.maddin.transportapi.components.MOTTypes
import com.maddin.transportapi.components.PlatformImpl
import com.maddin.transportapi.components.RealtimeConnection
import com.maddin.transportapi.endpoints.RealtimeAPI
import com.maddin.transportapi.components.RealtimeConnectionImpl
import com.maddin.transportapi.components.Station
import com.maddin.transportapi.components.StationIdentifier
import com.maddin.transportapi.components.StationIdentifierImpl
import com.maddin.transportapi.endpoints.RealtimeRequest
import com.maddin.transportapi.components.StationImpl
import com.maddin.transportapi.components.StopImpl
import com.maddin.transportapi.components.toLineId
import com.maddin.transportapi.components.toStaId
import com.maddin.transportapi.endpoints.CachedSearchPOIAPI
import com.maddin.transportapi.endpoints.RealtimeResponse
import com.maddin.transportapi.endpoints.RealtimeResponseImpl
import com.maddin.transportapi.endpoints.SearchPOIAPI
import com.maddin.transportapi.endpoints.SearchPOIRequest
import com.maddin.transportapi.endpoints.SearchPOIResponse
import com.maddin.transportapi.endpoints.SearchPOIResponseImpl
import com.maddin.transportapi.utils.InvalidRequestPOIException
import com.maddin.transportapi.utils.InvalidResponseContentException
import com.maddin.transportapi.utils.optLongNull
import com.maddin.transportapi.utils.optStringNull
import com.maddin.transportapi.utils.wrapExceptions
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId

interface CVAGStationId {
    val cvagId: String; get() = "$cvagMandator-$cvagNumber"
    val cvagMandator: String
    val cvagNumber: String
}
class CVAGStationIdImpl(override val cvagMandator: String, override val cvagNumber: String) : StationIdentifier, CVAGStationId {
    override val uuid = cvagId
}

class CVAG : CachedSearchPOIAPI, RealtimeAPI {
    override val searchPOICache = SearchPOICacheImpl()

    override fun searchPOIsAPI(request: SearchPOIRequest): SearchPOIResponse = wrapExceptions { handler ->
        handler.default = { SearchPOIResponseImpl(request=request, pois=emptyList(), exceptions=handler.combineExceptions()) }

        val requestUrl = "https://www.cvag.de/eza/mis/stations?like=${URLEncoder.encode(request.search, "UTF-8")}"
        handler.add("url: $requestUrl")
        val stationsRaw = JSONObject(URL(requestUrl).readText()).getJSONArray("stations")
        val stations = mutableListOf<Station>()
        for (i in 0 until stationsRaw.length()) {
            val stationRaw = stationsRaw.getJSONObject(i)

            val stationIdM = stationRaw.optStringNull("mandator")
            val stationIdN = stationRaw.optStringNull("number")
            if (stationIdM == null || stationIdN == null) {
                handler.raise(InvalidResponseContentException("Station has no valid id (missing \"mandator\" and/or \"number\")"))
                continue
            }
            val stationId = CVAGStationIdImpl(stationIdM, stationIdN)

            val stationName = stationRaw.optStringNull("displayName")
            if (stationName == null) {
                handler.raise(InvalidResponseContentException("Station has no valid name (missing \"displayName\")"))
                continue
            }

            stations.add(StationImpl(stationId, stationName))
        }

        return SearchPOIResponseImpl(request=request, pois=stations, exceptions=handler.combineExceptions())
    }

    @Suppress("NewApi")
    override fun getRealtimeInformation(request: RealtimeRequest): RealtimeResponse = wrapExceptions { handler ->
        handler.default = { RealtimeResponseImpl(request=request, connections=emptyList(), exceptions=handler.combineExceptions()) }

        val stationId = (request.poi.id as? CVAGStationId)?.cvagId ?:
            throw InvalidRequestPOIException("Provided POI has no valid id")

        val requestUrl = "https://www.cvag.de/eza/mis/stops/station/${URLEncoder.encode(stationId, "UTF-8")}"
        val stationInfo = JSONObject(URL(requestUrl).readText())

        val zoneOffset = ZoneId.of("Europe/Berlin").rules.getOffset(LocalDateTime.now())

        val connectionsRaw = stationInfo.getJSONArray("stops")
        val connections = mutableListOf<RealtimeConnection>()
        for (i in 0 until connectionsRaw.length()) {
            val connectionRaw = connectionsRaw.getJSONObject(i)

            val vName = connectionRaw.getString("line")
            val vDirection = connectionRaw.getString("destination")
            val vType = when (connectionRaw.optStringNull("serviceType")) {
                "BUS" -> MOTTypes.BUS
                "TRAM" -> MOTTypes.TRAM
                "CHEMNITZBAHN" -> VMS.Companion.CHEMNITZ_BAHN
                "SCHIENENERSATZVERKEHR" -> MOTTypes.BUS_REPLACES_TRAM
                else -> null
            }
            val line = LineImpl(vName.toLineId(), vName)
            val variant = LineVariantImpl(direction=DirectionImpl(vDirection))
            val vehicle = LineMOTImpl(motType=vType, line=line, variant=variant)
            val platform = connectionRaw.optStringNull("platform")?.let { PlatformImpl(name=it) }

            val departureRaw = connectionRaw.optLongNull("actualDeparture") ?: connectionRaw.optLongNull("plannedDeparture") ?: connectionRaw.optLongNull("departure")
            if (departureRaw == null) {
                handler.raise(InvalidResponseContentException("Connection has no keys relating to departure (i.e. \"actualDeparture\")"))
                continue
            }
            val departureActual = LocalDateTime.ofEpochSecond(departureRaw/1000, 0, zoneOffset)

            val stop = StopImpl(poi=request.poi, platformPlanned=platform, platformActual=platform,
                departureActual=departureActual)

            connections.add(RealtimeConnectionImpl(stop=stop, modeOfTransport=vehicle))
        }

        return RealtimeResponseImpl(request=request, connections=connections)
    }
}