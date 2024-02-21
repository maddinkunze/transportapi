package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.components.DirectionImpl
import com.maddin.transportapi.components.LineImpl
import com.maddin.transportapi.endpoints.RealtimeAPI
import com.maddin.transportapi.components.RealtimeConnection
import com.maddin.transportapi.components.RealtimeConnectionImpl
import com.maddin.transportapi.endpoints.RealtimeRequest
import com.maddin.transportapi.components.StationImpl
import com.maddin.transportapi.components.StopImpl
import com.maddin.transportapi.components.VehicleImpl
import com.maddin.transportapi.components.toLineId
import com.maddin.transportapi.components.toStaId
import com.maddin.transportapi.endpoints.RealtimeResponse
import com.maddin.transportapi.endpoints.RealtimeResponseImpl
import com.maddin.transportapi.endpoints.SearchPOIAPI
import com.maddin.transportapi.endpoints.SearchPOIRequest
import com.maddin.transportapi.endpoints.SearchPOIResponse
import com.maddin.transportapi.endpoints.SearchPOIResponseImpl
import com.maddin.transportapi.utils.InvalidRequestPOIException
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId


@Deprecated("This API has been discontinued by CVAG and will not work anymore. Please use the VMS API with an area limit instead, it will receive updates and implements newer features anyways.", replaceWith= ReplaceWith("VMS(\"Chemnitz\")", "com.maddin.transportapi.impl.germany.VMS"))
class CVAG : SearchPOIAPI, RealtimeAPI {
    override fun searchPOIs(request: SearchPOIRequest): SearchPOIResponse {
        val stations = JSONObject(URL("https://www.cvag.de/eza/mis/stations?like=${URLEncoder.encode(request.search, "UTF-8")}").readText()).getJSONArray("stations")
        return SearchPOIResponseImpl(request=request, pois=List(stations.length()) { i ->
            val station = stations.getJSONObject(i)
            val stationId = station.getString("mandator") + "-" + station.getString("number")
            val stationName = station.getString("displayName")
            StationImpl(stationId.toStaId(), stationName)
        })
    }

    @Suppress("NewApi")
    override fun getRealtimeInformation(request: RealtimeRequest): RealtimeResponse {
        val stationId = request.poi.id?.uuid ?: throw InvalidRequestPOIException("Provided POI has no valid id")
        val stationInfo = JSONObject(URL("https://www.cvag.de/eza/mis/stops/station/${URLEncoder.encode(stationId, "UTF-8")}").readText())
        val zoneOffset = ZoneId.of("Europe/Berlin").rules.getOffset(LocalDateTime.now())
        val stops = stationInfo.getJSONArray("stops")
        val connections = List(stops.length()) { i ->
            val stop = stops.getJSONObject(i)

            val vName = stop.getString("line")
            val vDirection = stop.getString("direction")
            val vehicle = VehicleImpl(line= LineImpl(vName.toLineId(), vName), direction= DirectionImpl(vDirection))

            val departureActual = LocalDateTime.ofEpochSecond(stop.getLong("actualDeparture"), 0, zoneOffset)
            RealtimeConnectionImpl(stop=StopImpl(poi=request.poi, departurePlanned=departureActual, departureActual=departureActual), vehicle=vehicle) // TODO: add id
        }

        return RealtimeResponseImpl(request=request, connections=connections)
    }
}