@file:Suppress("unused") // hopefully those apis go unused, especially in prod (ONLY use those apis, if even, for testing)

package com.maddin.transportapi.impl

import com.maddin.transportapi.components.DirectionImpl
import com.maddin.transportapi.components.LineImpl
import com.maddin.transportapi.endpoints.RealtimeAPI
import com.maddin.transportapi.components.RealtimeConnection
import com.maddin.transportapi.components.RealtimeConnectionImpl
import com.maddin.transportapi.endpoints.RealtimeRequest
import com.maddin.transportapi.components.StationImpl
import com.maddin.transportapi.components.StopImpl
import com.maddin.transportapi.components.VehicleImpl
import com.maddin.transportapi.components.toConId
import com.maddin.transportapi.components.toLineId
import com.maddin.transportapi.components.toStaId
import com.maddin.transportapi.endpoints.LocatePOIAPI
import com.maddin.transportapi.endpoints.LocatePOIRequest
import com.maddin.transportapi.endpoints.LocatePOIResponse
import com.maddin.transportapi.endpoints.RealtimeResponse
import com.maddin.transportapi.endpoints.RealtimeResponseImpl
import com.maddin.transportapi.endpoints.SearchPOIAPI
import com.maddin.transportapi.endpoints.SearchPOIRequest
import com.maddin.transportapi.endpoints.SearchPOIResponse
import com.maddin.transportapi.endpoints.SearchPOIResponseImpl
import com.maddin.transportapi.utils.InvalidRequestPOIException
import java.time.LocalDateTime

class ExampleAPI(private var connectionsPerStation: Int) : SearchPOIAPI, RealtimeAPI {
    constructor() : this(20)
    private val stationNames = arrayOf(
        "Student Station",
        "Climate Station",
        "Response Station",
        "Moment Station",
        "Union Station",
        "Bread Station",
        "Fishing Station",
        "City Station",
        "Week Station",
        "Breath Station",
        "Presentation Station",
        "Requirement Station",
        "Chemistry Station",
        "Elevator Station",
        "Application Station",
        "Year Station",
        "Impression Station",
        "Map Station",
        "Competition Station",
        "Army Station",
        "Art Station",
        "Painting Station",
        "Relation Station",
        "Atmosphere Station",
        "Confusion Station",
        "Literature Station",
        "Revenue Station",
        "Definition Station",
        "Hair Station",
        "Emphasis Station",
        "Perspective Station",
        "Region Station",
        "Trainer Station",
        "Assignment Station",
        "Assistance Station",
        "Organization Station",
        "Family Station",
        "Historian Station",
        "Dirt Station",
        "Preparation Station",
        "Entertainment Station",
        "Ability Station",
        "Weakness Station",
        "Chocolate Station",
        "Committee Station",
        "Decision Station",
        "Exam Station",
        "Tale Station",
        "Revolution Station",
        "Passion Station"
    )
    private val vehicleNames = arrayOf(
        "1",
        "2",
        "3",
        "4",
        "5",
        "21",
        "22",
        "23",
        "31",
        "32",
        "A7",
        "B8",
        "RE9",
        "RB30",
        "512",
        "507",
        "785",
        "82B"
    )
    private val cachedConnections = mutableMapOf<String, RealtimeResponse>()

    @Suppress("NewApi")
    override fun getRealtimeInformation(request: RealtimeRequest): RealtimeResponse {
        val curStationId = request.poi.id ?: throw InvalidRequestPOIException()
        if (!cachedConnections.contains(curStationId.uuid)) {
            cachedConnections[curStationId.uuid] = RealtimeResponseImpl(request=request, connections=mutableListOf())
        }

        val connections = cachedConnections[curStationId.uuid]?.connections as MutableList? ?: mutableListOf()
        for (connection in connections) {
            if ((connection.departsIn()?.seconds?:0) > -30) { continue }
            connections.remove(connection)
        }

        var departurePlanned = connections.lastOrNull()?.stop?.departurePlanned ?: LocalDateTime.now()
        for (i in connections.size..connectionsPerStation) {
            val cId = Math.random().toString()
            departurePlanned = departurePlanned.plusSeconds((10 + Math.random() * 100).toLong())
            val vIndex = (Math.random() * vehicleNames.size).toInt().coerceAtMost(vehicleNames.size-1)
            val dIndex = (Math.random() * stationNames.size).toInt().coerceAtMost(stationNames.size-1)
            val vName = vehicleNames[vIndex]
            val dName = stationNames[dIndex]
            val vehicle = VehicleImpl(line=LineImpl(id=vName.toLineId(), name=vName), direction=DirectionImpl(name=dName))
            val connection = RealtimeConnectionImpl(id=cId.toConId(), StopImpl(poi=request.poi, departurePlanned=departurePlanned), vehicle=vehicle)
            connections.add(connection)
        }

        return getRealtimeInfoCopy(request)
    }

    @Suppress("NewApi")
    private fun getRealtimeInfoCopy(request: RealtimeRequest): RealtimeResponse {
        val connections = mutableListOf<RealtimeConnection>()
        val cachedInfo = cachedConnections[request.poi.id?.uuid ?: throw InvalidRequestPOIException()] ?: return RealtimeResponseImpl(request=request, connections=emptyList())
        for (connection in cachedInfo.connections) {
            connections.add(connection)
        }
        return RealtimeResponseImpl(request=request, connections=connections)
    }

    override fun searchPOIs(request: SearchPOIRequest): SearchPOIResponse {
        val stations = mutableListOf<StationImpl>()
        for (stationIndex in stationNames.indices) {
            val stationName = stationNames[stationIndex]
            if (!stationName.startsWith(request.search, ignoreCase = true)) {
                continue
            }
            stations.add(StationImpl(stationIndex.toString().toStaId(), stationName))
        }
        return SearchPOIResponseImpl(request=request, pois=stations)
    }

}

class EmptyAPI : SearchPOIAPI, LocatePOIAPI, RealtimeAPI {
    override fun getRealtimeInformation(request: RealtimeRequest): RealtimeResponse { TODO("Not yet implemented") }
    override fun searchPOIs(request: SearchPOIRequest): SearchPOIResponse { TODO("Not yet implemented") }
    override fun locatePOIs(request: LocatePOIRequest): LocatePOIResponse { TODO("Not yet implemented") }
}