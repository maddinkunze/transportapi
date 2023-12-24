package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.InvalidResponseContentException
import com.maddin.transportapi.RealtimeAPI
import com.maddin.transportapi.RealtimeConnection
import com.maddin.transportapi.RealtimeInfo
import com.maddin.transportapi.Station
import com.maddin.transportapi.StationAPI
import com.maddin.transportapi.Vehicle
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar

@Suppress("unused")
class VMS(private val limitArea: String) : StationAPI, RealtimeAPI {
    private val limitAreaRegex = if (limitArea.isEmpty()) { null } else {Regex("(?:([\\s0-9a-zA-Z\\u00F0-\\u02AF]*)\\s)?\\($limitArea\\)(, )(.*)") }

    constructor() : this("")
    override fun getStations(search: String): List<Station> {
        val prefix = if (limitArea.isEmpty()) "" else "$limitArea, "
        val requestUrl = "https://efa.vvo-online.de/VMSSL3/XSLT_STOPFINDER_REQUEST?outputFormat=JSON&type_sf=any&name_sf=${URLEncoder.encode(prefix+search, "UTF-8")}"
        // TODO: throw InvalidResponseException when the URL fails to load and throw InvalidResponseFormatException when the JSONObject loader fails
        var stationsRaw = JSONObject(URL(requestUrl).readText())
        val stations = mutableListOf<Station>()
        if (!stationsRaw.has("stopFinder")) {
            throw InvalidResponseContentException("VMS.getStations() response had no attribute \"stopFinder\" after requesting $requestUrl")
        }
        stationsRaw = stationsRaw.getJSONObject("stopFinder")
        if (!stationsRaw.has("points")) {
            throw InvalidResponseContentException("VMS.getStations() response had no attribute \"stopFinder\".\"points\" after requesting $requestUrl")
        }

        var stationsArray = stationsRaw.optJSONArray("points")
        if (stationsArray == null) {
            stationsRaw = stationsRaw.optJSONObject("points") ?: throw InvalidResponseContentException("VMS.getStations() response contains \"stopFinder\".\"points\" which is neither JSONArray nor JSONObject after requesting $requestUrl")
            val onlyStation = stationsRaw.optJSONObject("point") ?: throw InvalidResponseContentException("VMS.getStations() response contains \"stopFinder\".\"points\" which is JSONObject but does not contain \"point\" after requesting $requestUrl")
            stationsArray = JSONArray().put(onlyStation)
        }

        println("MADDIN101: Station list")

        for (index in 0 until stationsArray.length()) {
            val station = stationsArray.optJSONObject(index) ?: continue
            if (station.optString("anyType") != "stop") { continue }
            if (limitArea.isNotEmpty() && station.optString("mainLoc", "") != limitArea) { continue }
            val stationName = cleanStationName(station.getString("name"))
            println("MADDIN101: ${station.getString("name")} -> $stationName")
            val stationId = station.getString("stateless")
            stations.add(Station(stationId, stationName))
        }

        println("MADDIN101: \nMADDIN101: ")
        return stations
    }

    private fun cleanStationName(stationName: String) : String {
        if (stationName.startsWith("$limitArea, ")) {
            return stationName.removePrefix("$limitArea, ")
        }

        if (limitAreaRegex == null) { return stationName }
        val results = limitAreaRegex.findAll(stationName)
        if (results.count() != 1) { return stationName }
        var result = results.elementAt(0).groupValues
        result = result.subList(1, result.size)
        return result.joinToString("")
    }

    @Suppress("SimpleDateFormat", "NewApi")
    override fun getRealtimeInformation(station: Station): RealtimeInfo {
        val stationId = URLEncoder.encode(station.id, "UTF-8")
        val timeNow = Calendar.getInstance().time
        val date = URLEncoder.encode(SimpleDateFormat("dd.MM.yyyy").format(timeNow), "UTF-8")
        val time = URLEncoder.encode(SimpleDateFormat("HH:mm").format(timeNow), "UTF-8")
        val requestUrl = "https://efa.vvo-online.de/VMSSL3/XSLT_DM_REQUEST?language=deincludeCompleteStopSeq=1&mode=direct&useAllStops=1&outputFormat=JSON&name_dm=${stationId}&type_dm=any&itdDateDayMonthYear=${date}&itdTime=${time}&useRealtime=1"
        val stopInfo = JSONObject(URL(requestUrl).readText())

        val areaPrefix = "$limitArea, "
        val connections = mutableListOf<RealtimeConnection>()

        if (!stopInfo.has("departureList")) {
            throw InvalidResponseContentException("VMS.getRealtimeInformation() response had no attribute \"departureList\" after requesting $requestUrl")
        }
        if (stopInfo.isNull("departureList")) { // departureList will be null (and not an empty array) if there are no connections planned
            return RealtimeInfo(station, connections)
        }

        val stops = stopInfo.getJSONArray("departureList")
        for (stopIndex in 0 until stops.length()) {
            val stop = stops.getJSONObject(stopIndex)

            val vehicleInfo = stop.getJSONObject("servingLine")
            var vName = vehicleInfo.getString("symbol")
            var vDirection = vehicleInfo.getString("direction")
            if (vDirection.startsWith(vName)) {
                val directionsSplit = vDirection.split(" ".toRegex(), 2)
                if (directionsSplit.size > 1) {
                    vName = directionsSplit[0]
                    vDirection = directionsSplit[1]
                }
            }
            vDirection = vDirection.removePrefix(areaPrefix)
            val vehicle = Vehicle(vName, vName, vDirection)

            val departure = stop.getJSONObject(if (stop.has("realDateTime")) "realDateTime" else "dateTime")
            val departsYear = departure.getInt("year")
            val departsMonth = departure.getInt("month")
            val departsDay = departure.getInt("day")
            val departsHour = departure.getInt("hour")
            val departsMinute = departure.getInt("minute")
            val departsIn = LocalDateTime.of(departsYear, departsMonth, departsDay, departsHour, departsMinute)

            val connection = RealtimeConnection(station, departsIn, vehicle)
            var index = connections.size
            while (index > 0) {
                if (connections[index-1].timeDepart <= departsIn) { break }
                index--
            }
            connections.add(index, connection)
        }

        return RealtimeInfo(station, connections)
    }
}