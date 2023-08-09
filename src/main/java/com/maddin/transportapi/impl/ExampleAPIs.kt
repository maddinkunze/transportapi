package com.maddin.transportapi.impl

import com.maddin.transportapi.RealtimeAPI
import com.maddin.transportapi.RealtimeConnection
import com.maddin.transportapi.RealtimeInfo
import com.maddin.transportapi.Station
import com.maddin.transportapi.StationAPI
import com.maddin.transportapi.Vehicle

class ExampleAPI(private var connectionsPerStation: Int) : StationAPI, RealtimeAPI {
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
    private val cachedConnections = mutableMapOf<String, RealtimeInfo>()

    @Suppress("NewApi")
    override fun getRealtimeInformation(station: Station): RealtimeInfo {
        val curStationId = station.id
        if (!cachedConnections.contains(curStationId)) {
            cachedConnections[curStationId] = RealtimeInfo(station, mutableListOf())
        }

        val connections = cachedConnections[curStationId]?.connections as MutableList? ?: mutableListOf()
        for (connection in connections) {
            if (connection.departsIn() > -30) { continue }
            connections.remove(connection)
        }

        var departsIn = connections.lastOrNull()?.departsIn() ?: 0L
        for (i in connections.size..connectionsPerStation) {
            departsIn += (10 + Math.random() * 100).toLong()
            val vIndex = (Math.random() * vehicleNames.size).toInt().coerceAtMost(vehicleNames.size-1)
            val dIndex = (Math.random() * stationNames.size).toInt().coerceAtMost(stationNames.size-1)
            val vName = vehicleNames[vIndex]
            val dName = stationNames[dIndex]
            val vehicle = Vehicle(vName, vName, dName)
            val connection = RealtimeConnection(station, departsIn, vehicle)
            connections.add(connection)
        }

        return getRealtimeInfoCopy(curStationId)
    }

    @Suppress("NewApi")
    private fun getRealtimeInfoCopy(stationId: String): RealtimeInfo {
        val connections = mutableListOf<RealtimeConnection>()
        val cachedInfo = cachedConnections[stationId] ?: return RealtimeInfo(Station("", ""), emptyList())
        for (connection in cachedInfo.connections) {
            connections.add(connection)
        }
        return RealtimeInfo(cachedInfo.station, connections)
    }

    override fun getStations(search: String): List<Station> {
        val stations = mutableListOf<Station>()
        for (stationIndex in stationNames.indices) {
            val stationName = stationNames[stationIndex]
            if (!stationName.startsWith(search, ignoreCase = true)) {
                continue
            }
            stations.add(Station(stationIndex.toString(), stationName))
        }
        return stations
    }

}

class EmptyAPI : StationAPI, RealtimeAPI {
    override fun getRealtimeInformation(station: Station): RealtimeInfo { TODO("Not yet implemented") }
    override fun getStations(search: String): List<Station> { TODO("Not yet implemented") }
}