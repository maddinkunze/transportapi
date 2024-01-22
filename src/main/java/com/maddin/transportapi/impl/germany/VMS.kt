package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.DefaultSearchStationCache
import com.maddin.transportapi.InvalidResponseContentException
import com.maddin.transportapi.RealtimeConnection
import com.maddin.transportapi.RealtimeInfo
import com.maddin.transportapi.Station
import com.maddin.transportapi.CachedSearchStationAPI
import com.maddin.transportapi.CachedStationAPI
import com.maddin.transportapi.Connection
import com.maddin.transportapi.DefaultStationCache
import com.maddin.transportapi.LocationLatLon
import com.maddin.transportapi.Direction
import com.maddin.transportapi.FutureRealtimeAPI
import com.maddin.transportapi.Line
import com.maddin.transportapi.LineVariant
import com.maddin.transportapi.LocatableStation
import com.maddin.transportapi.LocationArea
import com.maddin.transportapi.LocationStationAPI
import com.maddin.transportapi.Serving
import com.maddin.transportapi.StationDirection
import com.maddin.transportapi.Stop
import com.maddin.transportapi.Vehicle
import com.maddin.transportapi.VehicleType
import com.maddin.transportapi.VehicleTypes
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

fun String.utf8(): String {
    return URLEncoder.encode(this, "UTF-8")
}

@Suppress("NewApi")
fun JSONObject.getTime(key: String): LocalDateTime {
    val time = getJSONObject(key)
    val departsYear = time.getInt("year")
    val departsMonth = time.getInt("month")
    val departsDay = time.getInt("day")
    val departsHour = time.getInt("hour")
    val departsMinute = time.getInt("minute")
    return LocalDateTime.of(departsYear, departsMonth, departsDay, departsHour, departsMinute)
}

@Suppress("unused")
class VMS(private val limitArea: String) : CachedStationAPI, CachedSearchStationAPI, LocationStationAPI, FutureRealtimeAPI {
    constructor() : this("")

    private val limitAreaRegex = if (limitArea.isEmpty()) { null } else {Regex("(?:([\\s0-9a-zA-Z\\u00F0-\\u02AF]*)\\s)?\\($limitArea\\)(, )(.*)") }
    private val prefixStops = if (limitArea.isEmpty()) "" else "$limitArea, "

    companion object {
        private const val urlBase = "https://efa.vvo-online.de/VMSSL3/"

        private const val pathStopLines = "XSLT_SELTT_REQUEST"
        private const val queryStopLines = "?type_seltt=any&outputFormat=JSON"

        private const val pathStops = "XSLT_STOPFINDER_REQUEST"
        private val queryStops = "?coordOutputFormat=${FORMAT_COORDS.utf8()}&outputFormat=JSON&type_sf=any"

        private const val pathRealtime = "XSLT_DM_REQUEST"
        private val queryRealtime = "?language=de&includeCompleteStopSeq=1&mode=direct&useAllStops=1&outputFormat=JSON&type_dm=any&useRealtime=1"

        private const val pathLocate = "XSLT_COORD_REQUEST"
        private val queryLocate = "?coordOutputFormat=${FORMAT_COORDS.utf8()}&type_1=STOP&outputFormat=JSON&inclFilter=1&boundingBox="

        val VT_CHEMNITZ_BAHN = object : VehicleType {
            override val supertypes = listOf(VehicleTypes.TRAM)
        }
    }

    override val stationCache = DefaultStationCache()
    override val searchStationCache = DefaultSearchStationCache()

    private fun getStationURL(id: String): String {
        return "$urlBase$pathStops$queryStops&name_sf=$id"
    }
    private fun getStationServingLinesURL(id: String): String {
        return "$urlBase$pathStopLines$queryStopLines&nameInfo_seltt=$id"
    }
    override fun getStationAPI(id: String): Station {
        val requestUrl = getStationURL(id)
        val stationRaw = JSONObject(URL(requestUrl).readText()).getJSONObject("stopFinder").getJSONObject("points").getJSONObject("point")

        val requestUrlLines = getStationServingLinesURL(id)
        val linesRaw = JSONObject(URL(requestUrlLines).readText()).getJSONArray("modes")

        val stationName = stationRaw.getString("object")
        val stationLoc = getStationCoords(stationRaw)

        val lines = mutableMapOf<String, Line>()
        for (i in 0 until linesRaw.length()) {
            val desc = linesRaw.getJSONObject(i).getJSONObject("mode")
            val diva = desc.getJSONObject("diva")

            val lineId = diva.getString("line")
            if (lineId !in lines) {
                val lineName = desc.getString("number")
                val lineType = when (desc.getString("product")) {
                    "Bus" -> if (lineName.startsWith("N")) VehicleTypes.BUS_NIGHT else VehicleTypes.BUS
                    "PlusBus" -> VehicleTypes.BUS_REGIONAL
                    "Schülerlinie" -> VehicleTypes.BUS_SCHOOL
                    "Tram" -> VehicleTypes.TRAM
                    "Chemnitz Bahn" -> VT_CHEMNITZ_BAHN
                    "Zug" -> VehicleTypes.TRAIN
                    else -> null
                }
                lines[lineId] = Line(lineId, lineName, mutableListOf(), lineType)
            }

            val lineVId = diva.getString("stateless")

            val line = lines[lineId]!!
            (line.variants as MutableList).add(LineVariant(lineVId))
        }

        return object : LocatableStation(id, stationName, stationLoc), Serving {
            override val lines = lines.map { it.value }

        }
    }

    private fun getSearchStationURL(search: String): String {
        return "$urlBase$pathStops$queryStops&name_sf=${search.utf8()}"
    }

    override fun searchStationsAPI(search: String): List<Station> {
        val requestUrl = getSearchStationURL(prefixStops+search)
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
    private fun getRealtimeURL(station: Station, from: LocalDateTime): String {
        val stationId = station.id.utf8()
        val date = from.format(FORMATTER_DATE).utf8()
        val time = from.format(FORMATTER_TIME).utf8()
        return "$urlBase$pathRealtime$queryRealtime&name_dm=${stationId}&itdDateDayMonthYear=${date}&itdTime=${time}"
    }

    @Suppress("NewApi")
    override fun getRealtimeInformation(station: Station, from: LocalDateTime): RealtimeInfo {
        val requestUrl = getRealtimeURL(station, from)
        val stopInfo = JSONObject(URL(requestUrl).readText())

        val connections = mutableListOf<RealtimeConnection>()

        if (!stopInfo.has("departureList")) {
            throw InvalidResponseContentException("VMS.getRealtimeInformation() response had no attribute \"departureList\" after requesting $requestUrl")
        }
        if (stopInfo.isNull("departureList")) { // departureList will be null (and not an empty array) if there are no connections planned
            return RealtimeInfo(connections)
        }

        val departures = stopInfo.getJSONArray("departureList")
        for (connIndex in 0 until departures.length()) {
            val conn = departures.getJSONObject(connIndex)

            val vehicleInfo = conn.getJSONObject("servingLine")
            var vName = vehicleInfo.getString("symbol")
            var vDirection = vehicleInfo.getString("direction")
            if (vDirection.startsWith(vName)) {
                val directionsSplit = vDirection.split(" ", limit=2)
                if (directionsSplit.size > 1) {
                    vName = directionsSplit[0]
                    vDirection = directionsSplit[1]
                }
            }
            vDirection = vDirection.removePrefix(prefixStops)
            val vehicle = Vehicle(null, Line(vName, vName), Direction(vDirection))
            val cid = vehicleInfo.getString("stateless")

            var flagsStop = Stop.FLAG_NONE
            var flagsConnection = Connection.FLAG_NONE

            val tripStatus = conn.optString("realtimeTripStatus", "").split("|")
            @Suppress("KotlinConstantConditions")
            if (tripStatus.contains("TRIP_CANCELLED")) {
                flagsConnection = flagsConnection or Connection.FLAG_CANCELLED
            }
            if (tripStatus.contains("MONITORED")) {
                flagsStop = flagsStop or Stop.FLAG_REALTIME
            }

            val departurePlanned = conn.getTime("dateTime")
            var departureActual = departurePlanned
            if (conn.has("realDateTime")) {
                departureActual = conn.getTime("realDateTime")
                flagsStop = flagsStop or Stop.FLAG_REALTIME
            }

            val stop = Stop(station, departurePlanned=departurePlanned, departureActual=departureActual, flags=flagsStop)
            val connection = RealtimeConnection(cid, stop, vehicle=vehicle, flags=flagsConnection)
            var index = connections.size
            while (index > 0) {
                if (connections[index-1].stop.departureActual <= departureActual) { break }
                index--
            }
            connections.add(index, connection)
        }

        return RealtimeInfo(from, connections)
    }

    private fun makeCoordinate(coords: LocationLatLon) : String {
        return "%.6f:%.6f:$FORMAT_COORDS".format(Locale.ROOT, coords.lon, coords.lat)
    }

    /*private fun getLocateStationsURL(location: LocationArea): String {
        val area = location.toRect()
        val topLeft = URLEncoder.encode(makeCoordinate(area.topLeft), "UTF-8")
        val botRight = URLEncoder.encode(makeCoordinate(area.bottomRight), "UTF-8")
        return "$urlBase$pathLocate$queryRealtime&boundingBoxLU=${topLeft}&boundingBoxRL=${botRight}"
    }*/

    private fun getLocateStationsURL(location: LocationArea): String {
        val area = location.toRect()
        val topLeft = URLEncoder.encode(makeCoordinate(area.topLeft), "UTF-8")
        val botRight = URLEncoder.encode(makeCoordinate(area.bottomRight), "UTF-8")
        // TODO: this has to be easier -> create a custom URLBuilder maybe? (see the commented getLocateStationsURL method above)
        return "https://efa.vvo-online.de/VMSSL3/XSLT_COORD_REQUEST?boundingBox=&boundingBoxLU=${topLeft}&boundingBoxRL=${botRight}&coordOutputFormat=${URLEncoder.encode(FORMAT_COORDS, "UTF-8")}&type_1=STOP&outputFormat=json&inclFilter=1"
    }

    override fun locateStations(location: LocationArea) : List<Station> {
        val requestURL = getLocateStationsURL(location)
        val stationsRaw = JSONObject(URL(requestURL).readText())
        return extractStations(stationsRaw.getJSONArray("pins"))
    }

    private fun isStationStop(station: JSONObject) : Boolean {
        return station.optString("anyType") == "stop" || station.optString("type") == "STOP"
    }

    private fun isStationInArea(station: JSONObject) : Boolean {
        return limitArea.isEmpty() || station.optString("mainLoc", "") == limitArea || station.has("locality")
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

            val stationLoc = getStationCoords(station)

            stations.add(LocatableStation(stationId, stationName, stationLoc))
        }

        return stations
    }

    private fun getStationCoords(station: JSONObject): LocationLatLon {
        var stationCoordsString = "0,0"
        if (station.has("coords")) { stationCoordsString = station.getString("coords") }
        else if (station.has("ref")) { stationCoordsString = station.getJSONObject("ref").getString("coords") }
        val stationCoords = stationCoordsString.split(",").map { it.toDouble() }
        return LocationLatLon(stationCoords[1], stationCoords[0])
    }
}