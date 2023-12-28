package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.DefaultStationCache
import com.maddin.transportapi.InvalidResponseContentException
import com.maddin.transportapi.RealtimeConnection
import com.maddin.transportapi.RealtimeInfo
import com.maddin.transportapi.Station
import com.maddin.transportapi.CachedStationAPI
import com.maddin.transportapi.Coordinate
import com.maddin.transportapi.DefaultLocationLatLon
import com.maddin.transportapi.DefaultStation
import com.maddin.transportapi.Direction
import com.maddin.transportapi.FutureRealtimeAPI
import com.maddin.transportapi.Line
import com.maddin.transportapi.LocationArea
import com.maddin.transportapi.LocationStationAPI
import com.maddin.transportapi.RealtimeStop
import com.maddin.transportapi.Vehicle
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Suppress("NewApi")
val FORMATTER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
@Suppress("NewApi")
val FORMATTER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
const val FORMAT_COORDS = "WGS84[DD.DDDDD]"

@Suppress("unused")
class VMS(private val limitArea: String) : CachedStationAPI, LocationStationAPI, FutureRealtimeAPI {
    private val limitAreaRegex = if (limitArea.isEmpty()) { null } else {Regex("(?:([\\s0-9a-zA-Z\\u00F0-\\u02AF]*)\\s)?\\($limitArea\\)(, )(.*)") }

    constructor() : this("")

    override val stationCache = DefaultStationCache()

    override fun searchStationsAPI(search: String): List<Station> {
        val prefix = if (limitArea.isEmpty()) "" else "$limitArea, "
        val requestUrl = "https://efa.vvo-online.de/VMSSL3/XSLT_STOPFINDER_REQUEST?coordOutputFormat=${URLEncoder.encode(FORMAT_COORDS, "UTF-8")}&outputFormat=JSON&type_sf=any&name_sf=${URLEncoder.encode(prefix+search, "UTF-8")}"
        // TODO: throw InvalidResponseException when the URL fails to load and throw InvalidResponseFormatException when the JSONObject loader fails
        var stationsRaw = JSONObject(URL(requestUrl).readText())
        if (!stationsRaw.has("stopFinder")) {
            throw InvalidResponseContentException("${javaClass.name}.searchStationsAPI() response had no attribute \"stopFinder\" after requesting $requestUrl")
        }
        stationsRaw = stationsRaw.getJSONObject("stopFinder")
        if (!stationsRaw.has("points")) {
            throw InvalidResponseContentException("${javaClass.name}.searchStationsAPI() response had no attribute \"stopFinder\".\"points\" after requesting $requestUrl")
        }

        var stationsArray = stationsRaw.optJSONArray("points")
        if (stationsArray == null) {
            stationsRaw = stationsRaw.optJSONObject("points") ?: throw InvalidResponseContentException("${javaClass.name}.searchStationsAPI() response contains \"stopFinder\".\"points\" which is neither JSONArray nor JSONObject after requesting $requestUrl")
            val onlyStation = stationsRaw.optJSONObject("point") ?: throw InvalidResponseContentException("${javaClass.name}.searchStationsAPI() response contains \"stopFinder\".\"points\" which is JSONObject but does not contain \"point\" after requesting $requestUrl")
            stationsArray = JSONArray().put(onlyStation)
        }

        return extractStations(stationsArray)
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

    @Suppress("NewApi")
    override fun getRealtimeInformation(station: Station, from: LocalDateTime): RealtimeInfo {
        val stationId = URLEncoder.encode(station.id, "UTF-8")
        val date = URLEncoder.encode(from.format(FORMATTER_DATE), "UTF-8")
        val time = URLEncoder.encode(from.format(FORMATTER_TIME), "UTF-8")
        val requestUrl = "https://efa.vvo-online.de/VMSSL3/XSLT_DM_REQUEST?language=deincludeCompleteStopSeq=1&mode=direct&useAllStops=1&outputFormat=JSON&name_dm=${stationId}&type_dm=any&itdDateDayMonthYear=${date}&itdTime=${time}&useRealtime=1"
        val stopInfo = JSONObject(URL(requestUrl).readText())

        val areaPrefix = "$limitArea, "
        val connections = mutableListOf<RealtimeConnection>()

        if (!stopInfo.has("departureList")) {
            throw InvalidResponseContentException("VMS.getRealtimeInformation() response had no attribute \"departureList\" after requesting $requestUrl")
        }
        if (stopInfo.isNull("departureList")) { // departureList will be null (and not an empty array) if there are no connections planned
            return RealtimeInfo(connections)
        }

        val stops = stopInfo.getJSONArray("departureList")
        for (stopIndex in 0 until stops.length()) {
            val stop = stops.getJSONObject(stopIndex)

            val vehicleInfo = stop.getJSONObject("servingLine")
            var vName = vehicleInfo.getString("symbol")
            var vDirection = vehicleInfo.getString("direction")
            if (vDirection.startsWith(vName)) {
                val directionsSplit = vDirection.split(" ", limit=2)
                if (directionsSplit.size > 1) {
                    vName = directionsSplit[0]
                    vDirection = directionsSplit[1]
                }
            }
            vDirection = vDirection.removePrefix(areaPrefix)
            val vehicle = Vehicle(null, Line(vName, vName), Direction(vDirection))

            val departurePlanned = getJSONTime(stop.getJSONObject("dateTime"))
            val departureActual = getJSONTime(stop.getJSONObject(if (stop.has("realDateTime")) "realDateTime" else "dateTime"))

            val connection = RealtimeConnection(vehicle, RealtimeStop(station, departurePlanned, departureActual))
            var index = connections.size
            while (index > 0) {
                if ((connections[index-1].stop as RealtimeStop).departureActual <= departureActual) { break }
                index--
            }
            connections.add(index, connection)
        }

        return RealtimeInfo(from, connections)
    }

    @Suppress("SimpleDateFormat", "NewApi")
    override fun getRealtimeInformation(station: Station): RealtimeInfo {
        return getRealtimeInformation(station, LocalDateTime.now())
    }

    @Suppress("NewApi")
    private fun getJSONTime(time: JSONObject) : LocalDateTime {
        val departsYear = time.getInt("year")
        val departsMonth = time.getInt("month")
        val departsDay = time.getInt("day")
        val departsHour = time.getInt("hour")
        val departsMinute = time.getInt("minute")
        return LocalDateTime.of(departsYear, departsMonth, departsDay, departsHour, departsMinute)
    }

    private fun makeCoordinate(coords: Coordinate) : String {
        return "%.6f:%.6f:$FORMAT_COORDS".format(Locale.ROOT, coords.lon, coords.lat)
    }

    override fun locateStations(location: LocationArea) : List<Station> {
        val area = location.toRect()
        val topLeft = URLEncoder.encode(makeCoordinate(area.topLeft), "UTF-8")
        val botRight = URLEncoder.encode(makeCoordinate(area.bottomRight), "UTF-8")
        val requestURL = "https://efa.vvo-online.de/VMSSL3/XSLT_COORD_REQUEST?boundingBox=&boundingBoxLU=${topLeft}&boundingBoxRL=${botRight}&coordOutputFormat=${URLEncoder.encode(FORMAT_COORDS, "UTF-8")}&type_1=STOP&outputFormat=json&inclFilter=1"
        val stationsRaw = JSONObject(URL(requestURL).readText())
        return extractStations(stationsRaw.getJSONArray("pins"))
    }

    private fun isStationStop(station: JSONObject) : Boolean {
        return station.optString("anyType") == "stop" || station.optString("type") == "STOP"
    }

    private fun isStationInArea(station: JSONObject) : Boolean {
        return limitArea.isEmpty() || station.optString("mainLoc", "") == limitArea || station.optString("locality") == limitArea
    }
    private fun extractStations(stationsArray: JSONArray) : List<Station> {
        val stations = mutableListOf<Station>()

        for (index in 0 until stationsArray.length()) {
            val station = stationsArray.optJSONObject(index) ?: continue
            if (!isStationStop(station)) { continue }
            if (!isStationInArea(station)) { continue }

            var stationName = ""
            if (station.has("name")) { stationName = cleanStationName(station.getString("name")) }
            else if (station.has("desc")) { stationName = station.getString("desc") }

            val stationId = station.getString("stateless")

            var stationCoordsString = "0,0"
            if (station.has("coords")) { stationCoordsString = station.getString("coords") }
            else if (station.has("ref")) { stationCoordsString = station.getJSONObject("ref").getString("coords") }
            val stationCoords = stationCoordsString.split(",").map { it.toDouble() }
            val stationLoc = DefaultLocationLatLon(stationCoords[1], stationCoords[0])

            stations.add(DefaultStation(stationId, stationName, stationLoc))
        }

        return stations
    }
}