package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.RealtimeAPI
import com.maddin.transportapi.RealtimeConnection
import com.maddin.transportapi.RealtimeInfo
import com.maddin.transportapi.Station
import com.maddin.transportapi.StationAPI
import com.maddin.transportapi.Vehicle
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset


class CVAG : StationAPI, RealtimeAPI {
    override fun getStations(search: String): List<Station> {
        val stations = JSONObject(URL("https://www.cvag.de/eza/mis/stations?like=${URLEncoder.encode(search, "UTF-8")}").readText()).getJSONArray("stations")
        return List(stations.length()) { i ->
            val station = stations.getJSONObject(i)
            val stationId = station.getString("mandator") + "-" + station.getString("number")
            val stationName = station.getString("displayName")
            Station(stationId, stationName)
        }
    }

    override fun getRealtimeInformation(station: Station): RealtimeInfo {
        val stationInfo = JSONObject(URL("https://www.cvag.de/eza/mis/stops/station/${URLEncoder.encode(station.id, "UTF-8")}").readText())
        val zoneOffset = ZoneId.of("Europe/Berlin").rules.getOffset(LocalDateTime.now())
        val timeNow = LocalDateTime.ofEpochSecond(stationInfo.getLong("now"), 0, zoneOffset)
        val stops = stationInfo.getJSONArray("stops")
        val connections = List(stops.length()) { i ->
            val stop = stops.getJSONObject(i)

            val vName = stop.getString("line")
            val vDirection = stop.getString("direction")
            val vehicle = Vehicle(vName, vName, vDirection)

            val departsIn = LocalDateTime.ofEpochSecond(stop.getLong("actualDeparture"), 0, zoneOffset)
            RealtimeConnection(station, departsIn, vehicle)
        }

        return RealtimeInfo(station, timeNow, connections)
    }
}