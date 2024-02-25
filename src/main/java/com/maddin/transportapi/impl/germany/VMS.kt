package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.caches.POICacheImpl
import com.maddin.transportapi.caches.SearchPOICacheImpl
import com.maddin.transportapi.components.Connection
import com.maddin.transportapi.components.ConnectionIdentifier
import com.maddin.transportapi.components.ConnectionImpl
import com.maddin.transportapi.utils.InvalidResponseContentException
import com.maddin.transportapi.components.RealtimeConnection
import com.maddin.transportapi.components.StationImpl
import com.maddin.transportapi.components.LocationLatLon
import com.maddin.transportapi.components.DirectionImpl
import com.maddin.transportapi.components.Line
import com.maddin.transportapi.components.LineImpl
import com.maddin.transportapi.components.LineMOT
import com.maddin.transportapi.components.LineMOTImpl
import com.maddin.transportapi.components.LineVariant
import com.maddin.transportapi.components.LineVariantImpl
import com.maddin.transportapi.components.LocationAreaRect
import com.maddin.transportapi.components.LocationLatLonImpl
import com.maddin.transportapi.components.MOTImpl
import com.maddin.transportapi.components.MOTType
import com.maddin.transportapi.components.MOTTypes
import com.maddin.transportapi.components.Platform
import com.maddin.transportapi.components.PlatformImpl
import com.maddin.transportapi.components.RealtimeConnectionImpl
import com.maddin.transportapi.endpoints.RealtimeAPI
import com.maddin.transportapi.endpoints.RealtimeRequest
import com.maddin.transportapi.components.Station
import com.maddin.transportapi.components.StationDirectionImpl
import com.maddin.transportapi.components.Stop
import com.maddin.transportapi.components.StopImpl
import com.maddin.transportapi.components.Street
import com.maddin.transportapi.components.StreetImpl
import com.maddin.transportapi.components.Trip
import com.maddin.transportapi.endpoints.TripSearchAPI
import com.maddin.transportapi.components.TripConnection
import com.maddin.transportapi.components.TripConnectionImpl
import com.maddin.transportapi.components.TripImpl
import com.maddin.transportapi.endpoints.TripSearchRequest
import com.maddin.transportapi.components.TripWalkConnection
import com.maddin.transportapi.components.ModeOfTransport
import com.maddin.transportapi.components.POIIdentifierImpl
import com.maddin.transportapi.components.PublicTransport
import com.maddin.transportapi.components.StationIdentifier
import com.maddin.transportapi.components.Tram
import com.maddin.transportapi.components.Walk
import com.maddin.transportapi.components.WalkInstruction
import com.maddin.transportapi.components.WalkInstructionEnter
import com.maddin.transportapi.components.WalkInstructionGo
import com.maddin.transportapi.components.WalkInstructionImpl
import com.maddin.transportapi.components.WalkInstructionLeave
import com.maddin.transportapi.components.WalkInstructionTurn
import com.maddin.transportapi.components.toLiVaId
import com.maddin.transportapi.components.toLineId
import com.maddin.transportapi.components.toPlatId
import com.maddin.transportapi.endpoints.CachedPOIAPI
import com.maddin.transportapi.endpoints.CachedSearchPOIAPI
import com.maddin.transportapi.endpoints.ConnectionAPI
import com.maddin.transportapi.endpoints.ConnectionRequest
import com.maddin.transportapi.endpoints.ConnectionResponse
import com.maddin.transportapi.endpoints.ConnectionResponseImpl
import com.maddin.transportapi.endpoints.LocatePOIAPI
import com.maddin.transportapi.endpoints.LocatePOIRequest
import com.maddin.transportapi.endpoints.LocatePOIResponse
import com.maddin.transportapi.endpoints.LocatePOIResponseImpl
import com.maddin.transportapi.endpoints.POIRequest
import com.maddin.transportapi.endpoints.POIResponse
import com.maddin.transportapi.endpoints.POIResponseImpl
import com.maddin.transportapi.endpoints.POIUseCase
import com.maddin.transportapi.endpoints.RealtimeResponse
import com.maddin.transportapi.endpoints.RealtimeResponseImpl
import com.maddin.transportapi.endpoints.SearchPOIRequest
import com.maddin.transportapi.endpoints.SearchPOIResponse
import com.maddin.transportapi.endpoints.SearchPOIResponseImpl
import com.maddin.transportapi.endpoints.TripSearchResponse
import com.maddin.transportapi.endpoints.TripSearchResponseImpl
import com.maddin.transportapi.utils.ExceptionHandler
import com.maddin.transportapi.utils.Identifier
import com.maddin.transportapi.utils.InvalidRequestFormatException
import com.maddin.transportapi.utils.InvalidRequestPOIException
import com.maddin.transportapi.utils.Translatable
import com.maddin.transportapi.utils.URLBuilder
import com.maddin.transportapi.utils.optIntNull
import com.maddin.transportapi.utils.optLongNull
import com.maddin.transportapi.utils.optStringNull
import com.maddin.transportapi.utils.wrapExceptions
import org.json.JSONArray
import java.net.URL
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

val FORMATTER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val FORMATTER_DATE_WEIRD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
val FORMATTER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val FORMATTER_TIME_WEIRD: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
val FORMATTER_TIME_SEC: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
val FORMATTER_DATETIME_SEC: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
const val FORMAT_COORDS = "WGS84[DD.DDDDD]"


interface ChemnitzBahn : Tram { override val areaClassification; get() = PublicTransport.AreaClassifications.REGIONAL }

interface VMSStationId {
    val vmsStateless: String
}
class VMSStationIdImpl(override val vmsStateless: String) : StationIdentifier, VMSStationId {
    override val uuid = vmsStateless
}
private fun String.toStaId() = VMSStationIdImpl(this)

@Suppress("unused")
class VMS(private val limitArea: String) : CachedPOIAPI, CachedSearchPOIAPI, LocatePOIAPI,
    RealtimeAPI, TripSearchAPI, ConnectionAPI {
    constructor() : this("")

    private val limitAreaRegex = if (limitArea.isEmpty()) { null } else { Regex("(?:([\\s0-9a-zA-Z\\u00F0-\\u02AF]*)\\s)?\\($limitArea\\)(, )(.*)") }
    //private val prefixStops = if (limitArea.isEmpty()) "" else "$limitArea, "

    companion object {
        private const val hostBase = "https://efa.vvo-online.de"
        private const val pathBase = "VMSSL3"
        private const val paramOutputJson = "outputFormat=JSON"
        private const val paramCoordOutput = "coordOutputFormat=$FORMAT_COORDS"

        private val urlStopGet = URLBuilder<VMSStationId>()
            .setHost(hostBase).addPaths(pathBase, "XSLT_STOPFINDER_REQUEST")
            .addParams(paramOutputJson, paramCoordOutput, "type_sf=any")
            .addParam("nameInfo_sf") { it.vmsStateless }

        private val urlStopLines = URLBuilder<String>()
            .setHost(hostBase).addPaths(pathBase, "XSLT_SELTT_REQUEST")
            .addParams(paramOutputJson, "type_seltt=any")
            .addParam("nameInfo_seltt") { id -> id }

        private class SearchRequestVMS(val request: SearchPOIRequest, val area: String?)
        private val urlStopSearch = URLBuilder<SearchRequestVMS>()
            .setHost(hostBase).addPaths(pathBase, "XSLT_STOPFINDER_REQUEST")
            .addParams(paramOutputJson, paramCoordOutput)
            .addParam("type_sf") { when {
                it.request.useCase == POIUseCase.REALTIME -> "stop"
                else -> "any"
            } }
            .addParam("place_sf") { it.area }
            .addParam("name_sf") { it.request.search }

        private val urlStopLocate = URLBuilder<LocationAreaRect>()
            .setHost(hostBase).addPaths(pathBase, "XSLT_COORD_REQUEST")
            .addParam("boundingBox", "")
            .addParam("boundingBoxLU") { area -> makeCoordinate(area.topLeft) }
            .addParam("boundingBoxRL") { area -> makeCoordinate(area.bottomRight) }
            .addParams("type_1=STOP", paramOutputJson, paramCoordOutput, "inclFilter=1")

        private val urlRealtime = URLBuilder<RealtimeRequest>()
            .setHost(hostBase).addPaths(pathBase, "XSLT_DM_REQUEST")
            .addParams(paramOutputJson, "language=de", "mode=direct", "useAllStops=1", "type_dm=any", "useRealtime=1")
            .addParam("name_dm") { info -> (info.poi.id as? VMSStationId)?.vmsStateless ?: throw InvalidRequestPOIException("Given POI has no valid id") }
            .addParam("itdDateDayMonthYear") { info -> info.time?.format(FORMATTER_DATE) }
            .addParam("itdTime") { info -> info.time?.format(FORMATTER_TIME) }

        private val urlTrip = URLBuilder<TripSearchRequest>()
            .setHost(hostBase).addPaths(pathBase, "XSLT_TRIP_REQUEST2")
            .addParams(paramCoordOutput, paramOutputJson, "type_origin=any", "type_destination=any")
            .addParam("nameInfo_origin") { info -> info.identifiableStartId.uuid }
            .addParam("nameInfo_via") { info -> info.firstIdentifiableViaId?.uuid }
            .addParam("nameInfo_destination") { info -> info.identifiableEndId.uuid }
            .addParam("itdDateDayMonthYear") { info -> info.time?.format(FORMATTER_DATE) }
            .addParam("itdTime") { info -> info.time?.format(FORMATTER_TIME) }
            .addParam("itdTripDateTimeDepArr") { info -> when (info.timeSpec) {
                TripSearchRequest.TRIP_TIME_DEPARTURE -> "dep"
                TripSearchRequest.TRIP_TIME_ARRIVAL -> "arr"
                else -> null
            } }

        private class ConnectionRequestVMS(val request: ConnectionRequest, val id: ConnectionIdentifierVMS)
        private val urlConnection = URLBuilder<ConnectionRequestVMS>()
            .setHost(hostBase).addPaths(pathBase, "XML_STOPSEQCOORD_REQUEST")
            .addParams(paramCoordOutput, paramOutputJson, "tStOTType=all")
            .addParam("line") { req -> req.id.lineId }
            .addParam("tripCode") { req -> req.id.tripId }
            .addParam("stop") { req -> req.id.stopId }
            .addParam("date") { req -> req.id.stopDate }
            .addParam("time") { req -> req.id.stopTime }

        //https://efa.vvo-online.de/VMSSL3/XSLT_TRIP_REQUEST2?nameInfo_origin=36030062&type_origin=any&name_destination=Chemnitz%2C%20Reichsstr&nameInfo_destination=36030021&type_destination=any&itdDateDayMonthYear=29.01.2024&itdTime=10%3A05&itdTripDateTimeDepArr=dep&includedMeans=checkbox&useRealtime=1&inclMOT_0=true&inclMOT_1=true&inclMOT_2=true&inclMOT_3=true&inclMOT_4=true&inclMOT_5=true&inclMOT_6=true&inclMOT_7=true&inclMOT_8=true&inclMOT_9=true&inclMOT_10=true&inclMOT_17=true&inclMOT_19=true&lineRestriction=400&routeType=LEASTTIME&trITMOTvalue100=15&useProxFootSearch=on&changeSpeed=Normal&maxChanges=true&imparedOptionsActive=1&name_via=&nameInfo_via=invalid&type_via=any&dwellTimeMinutes=&itdLPxx_snippet=1&itdLPxx_template=tripresults_pt_trip&computationType=sequence&useUT=1&_=1706519142603&outputFormat=JSON

        private fun makeCoordinate(coords: LocationLatLon) : String {
            return "%.6f:%.6f:$FORMAT_COORDS".format(Locale.ROOT, coords.lon, coords.lat)
        }

        object CHEMNITZ_BAHN : ChemnitzBahn
    }

    override val poiCache = POICacheImpl()
    override val searchPOICache = SearchPOICacheImpl()

    override fun getPOIAPI(request: POIRequest): POIResponse = wrapExceptions { handler ->
        handler.default = { POIResponseImpl(request=request, poi=null, exceptions=handler.combineExceptions()) }

        val requestUrlS = urlStopGet.build((request.poiId as? VMSStationId) ?:
            throw InvalidRequestPOIException("Given POI has no valid id"))
        handler.add("url station: $requestUrlS")

        // get the json response from the server
        val dataS = URL(requestUrlS).readText()
        handler.add("data station: $dataS")
        val stationRaw = JSONObject(dataS)

        // extract all found stations -> select the very first one (throw an exception if no station was found)
        val stationRawF = getStationResponse1(stationRaw, handler)?.optJSONObject(0)
            ?: throw InvalidResponseContentException("No station with the given id ${request.poiId} was found")

        // parse the station object, throws an error if it could not be parsed (i.e. an important field was missing)
        val station = parseStation1(stationRawF, handler)

        val requestUrlL = urlStopLines.build(request.poiId.uuid)
        handler.add("url lines: $requestUrlL")

        val dataL = URL(requestUrlL).readText()
        handler.add("data lines: $dataL")

        val linesRaw = JSONObject(dataL).optJSONArray("modes") ?: JSONArray()
        station?.lines = parseMap(linesRaw, ::parseLine2, handler).map { it.value }

        return POIResponseImpl(request=request, poi=station, exceptions=handler.combineExceptions())
    }

    override fun searchPOIsAPI(request: SearchPOIRequest): SearchPOIResponse = wrapExceptions { handler ->
        handler.default = { SearchPOIResponseImpl(request=request, pois=emptyList(), exceptions=handler.combineExceptions()) }

        val requestUrl = urlStopSearch.build(SearchRequestVMS(request, limitArea))
        handler.add("url: $requestUrl")

        if (request.search.lowercase().endsWith("reichss")) {
            println("MADDIN101: url $requestUrl")
        }

        val data = URL(requestUrl).readText()
        handler.add("data: $data")
        val stationsRaw = JSONObject(data)

        val stationsArray = getStationResponse1(stationsRaw, handler) ?: JSONArray()
        SearchPOIResponseImpl(request=request, pois=parseArray(stationsArray, ::parseStation1, handler, ::verifyStation1))
    }

    override fun getRealtimeInformation(request: RealtimeRequest): RealtimeResponse = wrapExceptions { handler ->
        handler.default = { RealtimeResponseImpl(request, emptyList(), handler.combineExceptions()) }

        val requestUrl = urlRealtime.build(request)
        handler.add("url: $requestUrl")

        val data = URL(requestUrl).readText()
        handler.add("data: $data")

        val obj = JSONObject(data)
        val departuresRaw = if (obj.isNull("departureList")) { JSONArray() } else { obj.optJSONArray("departureList") ?:
            throw InvalidResponseContentException("Realtime response does not have a valid key \"departureList\"")
        }
        val departures = parseArray(departuresRaw, ::parseRealtimeConnection1, handler, ::filterRealtimeConnection1).sortedBy { it.estimateDepartureActual() }

        return RealtimeResponseImpl(request, departures, handler.combineExceptions())
    }

    override fun getRealtimeFeatures() = RealtimeAPI.FEATURE_REALTIME_PAST or RealtimeAPI.FEATURE_REALTIME_FUTURE

    /*override fun locateStations(request: LocateStationsRequest) : LocateStationsResponse {
        val requestURL = urlStopLocate.build(request.location.toRect())
        println("MADDIN101: locate $requestURL")
        val stationsRaw = JSONObject(URL(requestURL).readText())
        return LocateStationsResponseImpl(request=request, stations=extractStations(stationsRaw.getJSONArray("pins")))
    }*/

    override fun locatePOIs(request: LocatePOIRequest) : LocatePOIResponse = wrapExceptions { handler ->
        handler.default = { LocatePOIResponseImpl(request=request, pois=emptyList(), exceptions=handler.combineExceptions()) }

        val requestURL = urlStopLocate.build(request.location.toRect())
        handler.add("url: $requestURL")

        val data = URL(requestURL).readText()
        handler.add("data: $data")

        val stationsRaw = JSONObject(data).optJSONArray("pins") ?:
            throw InvalidResponseContentException("API response object has no key \"pins\"")

        LocatePOIResponseImpl(request=request, pois=parseArray(stationsRaw, ::parseStation3, handler), exceptions=handler.combineExceptions())
    }

    override fun searchTrips(request: TripSearchRequest): TripSearchResponse = wrapExceptions { handler ->
        handler.default = { TripSearchResponseImpl(request=request, trips=emptyList(), exceptions=handler.combineExceptions()) }

        val requestUrl = urlTrip.build(request)
        handler.add("url: $requestUrl")

        val data = URL(requestUrl).readText()
        handler.add("data: $data")

        val tripsRaw = JSONObject(data).optJSONArray("trips") ?:
            handler.raise(InvalidResponseContentException("Trip response has no key \"trips\"")) ?:
            JSONArray()

        val trips = parseArray(tripsRaw, ::parseTrip1, handler)

        return TripSearchResponseImpl(request=request, trips=trips, exceptions=handler.combineExceptions())
    }

    override fun getSearchTripWaypointCount() = TripSearchAPI.TRIP_SEARCH_WAYPOINT_COUNT_FROM_VIA_TO

    override fun getConnection(request: ConnectionRequest): ConnectionResponse = wrapExceptions { handler ->
        handler.default = { ConnectionResponseImpl(request=request, connection=null, exceptions=handler.combineExceptions()) }

        val id = ConnectionIdentifierVMS.of(request.connectionId) ?:
            throw InvalidRequestFormatException("Connection Id is not in the right format (${request.connectionId.uuid}), could not be converted to VMS implementation \"ConnectionIdentifier\"")

        val requestUrl = urlConnection.build(ConnectionRequestVMS(request, id))
        handler.add("url: $requestUrl")

        val data = URL(requestUrl).readText()
        handler.add("data: $data")

        val connectionRaw = JSONObject(data).optJSONObject("stopSeqCoords") ?:
            throw InvalidResponseContentException("Response has no key \"stopSeqCoords\"")

        val connection = parseConnection2(connectionRaw, handler)

        return ConnectionResponseImpl(request=request, connection=connection, exceptions=handler.combineExceptions())
    }


    // internal functions

    private fun parseTurnInst1(arr: JSONArray, curIndex: Int, obj: JSONObject, handler: ExceptionHandler<*>): WalkInstruction? {
        val type = obj.optStringNull("manoeuvre") ?:
            return handler.raise(InvalidResponseContentException("Walk instruction has no key \"manoeuvre\""))

        if (type == "ORIGIN") {
            return null
        }

        val coordFrom = parseCoords1(obj.optStringNull("coords"), handler)
        val coordTo = parseCoords1(arr.optJSONObject(curIndex+1)?.optStringNull("coords"), handler)

        val coords = if (coordFrom != null) { listOfNotNull(coordFrom, coordTo) } else { null }

        return when (type) {
            "KEEP" -> WalkInstructionGo(
                distanceInMeters=obj.optIntNull("dis"),
                durationInSeconds=obj.optLongNull("tTime"),
                where=parseStreet1(obj.optStringNull("name"), handler),
                path=coords
            )
            "CONTINUE" -> WalkInstructionGo(
                distanceInMeters=obj.optIntNull("dis"),
                durationInSeconds=obj.optLongNull("tTime"),
                where=parseStreet1(obj.optStringNull("name"), handler),
                path=coords
            )
            "TURN" -> WalkInstructionTurn(
                direction=when (obj.optStringNull("dir")) {
                    "LEFT" -> WalkInstructionImpl.DIRECTION_LEFT
                    "SLIGHT_LEFT" -> WalkInstructionImpl.DIRECTION_SLIGHT_LEFT
                    "RIGHT" -> WalkInstructionImpl.DIRECTION_RIGHT
                    "SLIGHT_RIGHT" -> WalkInstructionImpl.DIRECTION_SLIGHT_RIGHT
                    else -> null
                },
                onto=parseStreet1(obj.optStringNull("name"), handler),
                path=coords
            )
            "LEAVE" -> {
                val exit = parseEntryOrExit1(obj.optString("name"), handler)
                WalkInstructionLeave(
                    what=exit.what,
                    where=exit.where,
                    path=coords
                )
            }
            "ENTER" -> {
                val entry = parseEntryOrExit1(obj.optString("name"), handler)
                WalkInstructionEnter(
                    what=entry.what,
                    where=entry.where,
                    path=coords
                )
            }
            else -> handler.raise(InvalidResponseContentException("Walk instruction has unknown manoeuvre named \"$type\""))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseStreet1(name: String?, handler: ExceptionHandler<*>): Street? {
        if (name.isNullOrBlank()) { return null }
        return StreetImpl(name=name)
    }

    private class EntryOrExit(val what: Translatable, val where: Translatable?)
    private val regexLeave = Regex("((Ausstieg|Einstieg) )?(?<what>([0-9a-zA-Z\\u00F0-\\u02AF ](?!(rechts|links)))*)( (?<where>rechts|links))?")
    @Suppress("UNUSED_PARAMETER")
    private fun parseEntryOrExit1(name: String, handler: ExceptionHandler<*>): EntryOrExit {
        val matches = regexLeave.matchEntire(name)?.groups

        val what = matches?.get("what")?.value ?: ""
        val where = matches?.get("where")?.value

        return EntryOrExit(
            WalkInstructionImpl.createTranslateFunction(what), // TODO
            where?.let { WalkInstructionImpl.createTranslateFunction(it) }
        )
    }

    private fun verifyStation1(obj: JSONObject): Boolean {
        return obj.optStringNull("anyType") == "stop"
    }

    private fun parseStation1(obj: JSONObject, handler: ExceptionHandler<*>): StationImpl? {
        //if (!isPOIStop1(obj)) { return null } // TODO: verify function
        //if (!isPOIInArea1(obj)) { return null }

        val saveCount = handler.save()

        val id = obj.optStringNull("stateless") ?:
            return handler.raise(InvalidResponseContentException("Station has no key \"stateless\" (id)"), restoreTo=saveCount)
        handler.add("station id: $id")

        val name = cleanStationName(obj.optStringNull("name") ?:
            return handler.raise(InvalidResponseContentException("Station has no key \"name\" (name)"), restoreTo=saveCount)
        , handler)

        val coords = obj.optJSONObject("ref")?.optStringNull("coords")?.let { parseCoords1(it, handler) }

        handler.restoreToCount(saveCount)
        return StationImpl(id=id.toStaId(), name=name, location=coords)
    }

    private fun parseStation2(obj: JSONObject, handler: ExceptionHandler<*>): Station? {
        val saveCount = handler.save()

        val id = obj.optJSONObject("ref")?.optStringNull("id") ?:
            return handler.raise(InvalidResponseContentException("Station has no key \"ref\".\"id\" (id)"), restoreTo=saveCount)
        handler.add("station id: $id")

        val name = cleanStationName(obj.optStringNull("name") ?:
            return handler.raise(InvalidResponseContentException("Station has no key \"name\" (name)"), restoreTo=saveCount)
        , handler)

        val coords = obj.optJSONObject("ref")?.optStringNull("coords")?.let { parseCoords1(it, handler) }

        handler.restoreToCount(saveCount)
        return StationImpl(id=id.toStaId(), name=name, location=coords)
    }

    private fun parseStation3(obj: JSONObject, handler: ExceptionHandler<*>): Station? {
        val saveCount = handler.save()

        val id = obj.optStringNull("id") ?:
            return handler.raise(InvalidResponseContentException("Station has no key \"id\" (id)"), restoreTo=saveCount)
        handler.add("station id: $id")

        val name = obj.optStringNull("desc") ?:
            return handler.raise(InvalidResponseContentException("Station has no key \"desc\" (name)"), restoreTo=saveCount)

        val coords = obj.optStringNull("coords")?.let { parseCoords1(it, handler) }

        handler.restoreToCount(saveCount)
        return StationImpl(id=id.toStaId(), name=name, location=coords)
    }

    private fun parseStation4(obj: JSONObject, handler: ExceptionHandler<*>): Station? {
        val saveCount = handler.save()

        val id = obj.optStringNull("stopID") ?:
            return handler.raise(InvalidResponseContentException("Station has no key \"stopID\" (id)"))

        val name = obj.optStringNull("nameWO") ?:
            obj.optStringNull("stopName")?.let { cleanStationName(it, handler) } ?:
            return handler.raise(InvalidResponseContentException("Station has no key \"nameWO\" or \"stopName\" (name)"), restoreTo=saveCount)

        val coordX = obj.optStringNull("x")
        val coordY = obj.optStringNull("y")
        val coords = parseCoords2(coordX, coordY, handler)

        return StationImpl(id=id.toStaId(), name=name, location=coords)
    }

    private fun parseLine1(obj: JSONObject, existingLines: MutableMap<String, Line>?, handler: ExceptionHandler<*>): Line? {
        val saveCount = handler.save()

        val diva = obj.optJSONObject("diva") ?:
            return handler.raise(InvalidResponseContentException("Line has no valid key \"diva\""), restoreTo=saveCount)

        val lineId = diva.optStringNull("line") ?:
            return handler.raise(InvalidResponseContentException("Line diva has no valid key \"line\" (id)"), restoreTo=saveCount)
        handler.add("line id: $lineId")

        val existing = existingLines?.get(lineId) ?: run {
            val lineName = obj.optStringNull("number") ?:
                return handler.raise(InvalidResponseContentException("Line has no valid key \"number\" (name/number)"), restoreTo=saveCount)
            val lineType = parseVehicleType(obj.optStringNull("product"), lineName, handler)
            val line = LineImpl(id=lineId.toLineId(), name=lineName, variants=mutableListOf(), defaultMOTType=lineType)
            existingLines?.put(lineId, line)
            line
        }

        diva.optStringNull("stateless")?.let { lineVariantId ->
            val lineVariant = LineVariantImpl(id=lineVariantId.toLiVaId())
            (existing.variants as? MutableList)?.add(lineVariant)
        }

        handler.restoreToCount(saveCount)
        return existing
    }

    private fun parseLine2(obj: JSONObject, existingLines: MutableMap<String, Line>?, handler: ExceptionHandler<*>): Line? {
        val line = obj.optJSONObject("mode") ?:
            return handler.raise(InvalidResponseContentException("Lines parent object has no key \"mode\" from which line data could be inferred"))

        return parseLine1(line, existingLines, handler)
    }

    private fun parseLine3(obj: JSONObject, handler: ExceptionHandler<*>): Line? {
        val lineId = obj.optStringNull("stateless")?.toLineId()
        handler.add("line id: $lineId")
        var lineName = obj.optStringNull("symbol")
        if (lineName.isNullOrEmpty()) {
            val trainType = obj.optStringNull("trainType")
            val trainName = obj.optStringNull("trainNum")
            lineName = if (!trainType.isNullOrEmpty() && !trainName.isNullOrEmpty()) { "$trainType$trainName" } else { lineName }
        }

        if (lineName == null && lineId == null) { return handler.raise(InvalidResponseContentException("Line has neither keys \"stateless\" (line id) nor \"symbol\" (line name)")) }

        return LineImpl(id=lineId, name=lineName)
    }

    private fun <T, S> parseMap(arr: JSONArray, parser: (JSONObject, MutableMap<T, S>, ExceptionHandler<*>) -> Unit, handler: ExceptionHandler<*>): Map<T, S> {
        val objects = mutableMapOf<T, S>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i) ?: continue

            val saveCount = handler.save()
            handler.add("index (parse map): $i")

            parser(obj, objects, handler)

            handler.restoreToCount(saveCount)
        }
        return objects
    }

    private fun <T> parseArray(arr: JSONArray, parser: (JSONArray, Int, JSONObject, ExceptionHandler<*>) -> T?, handler: ExceptionHandler<*>, verifier: ((JSONObject) -> Boolean)?=null): List<T> {
        val objects = mutableListOf<T>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue

            val saveCount = handler.save()
            handler.add("index (parse array): $i")

            if (verifier?.let { it(obj) } == false) {
                handler.restoreToCount(saveCount)
                continue
            }

            val objT = parser(arr, i, obj, handler)
            if (objT == null) {
                handler.restoreToCount(saveCount)
                continue
            }
            objects.add(objT)

            handler.restoreToCount(saveCount)
        }
        return objects
    }

    private fun <T> parseArray(arr: JSONArray, parser: (JSONObject, ExceptionHandler<*>) -> T?, handler: ExceptionHandler<*>, verifier: ((JSONObject) -> Boolean)?=null): List<T> {
        return parseArray(arr, { _, _, o, h -> parser(o, h) }, handler, verifier)
    }

    private fun getStationResponse1(obj: JSONObject, handler: ExceptionHandler<*>): JSONArray? {
        val saveCount = handler.save()

        var stopFinder = obj.optJSONObject("stopFinder") ?:
            return handler.raise(InvalidResponseContentException("${javaClass.name}.getStationResponse1() response had no attribute \"stopFinder\""), restoreTo=saveCount)

        if (!stopFinder.has("points")) {
            return handler.raise(InvalidResponseContentException("${javaClass.name}.getStationResponse1() response had no attribute \"stopFinder\".\"points\""), restoreTo=saveCount)
        }

        val stations = stopFinder.optJSONArray("points") ?: run {
            stopFinder = stopFinder.optJSONObject("points") ?:
                return handler.raise(InvalidResponseContentException("${javaClass.name}.getStationResponse1() response contains \"stopFinder\".\"points\" which is neither JSONArray nor JSONObject"), restoreTo=saveCount)

            val onlyStation = stopFinder.optJSONObject("point") ?:
                return handler.raise(InvalidResponseContentException("${javaClass.name}.getStationResponse1() response contains \"stopFinder\".\"points\" which is JSONObject but does not contain \"point\""), restoreTo=saveCount)
            JSONArray().put(onlyStation)
        }

        handler.restoreToCount(saveCount)
        return stations
    }

    private fun parseStop1(obj: JSONObject, handler: ExceptionHandler<*>): Stop? {
        val station = parseStation2(obj, handler) ?: return null
        val platform = parsePlatform1(obj, handler)

        val ref = obj.optJSONObject("ref") ?:
            return handler.raise(InvalidResponseContentException("Stop has no object with key \"ref\""))
        val depPlanned = ref.optStringNull("depDateTimeSec")?.let { parseTime1(it, handler) }
        val arrPlanned = ref.optStringNull("arrDateTimeSec")?.let { parseTime1(it, handler) }

        val depActual = addTimeDelay1(depPlanned, ref.optString("depDelay"), ref.optString("depValid"))
        val arrActual = addTimeDelay1(arrPlanned, ref.optString("arrDelay"), ref.optString("arrValid"))

        var flags = Stop.FLAG_NONE
        if (depActual != null) {
            flags = flags or Stop.FLAG_REALTIME_DEPARTURE
        }
        if (arrActual != null) {
            flags = flags or Stop.FLAG_REALTIME_ARRIVAL
        }

        return StopImpl(
            poi=station, platformPlanned=platform,
            departurePlanned=depPlanned, departureActual=depActual,
            arrivalPlanned=arrPlanned, arrivalActual=arrActual,
            flags=flags
        )
    }

    private fun parseStop2(obj: JSONObject, handler: ExceptionHandler<*>): Stop? {
        val station = parseStation2(obj, handler) ?: return null
        val platform = parsePlatform1(obj, handler)

        val dateTime = obj.optJSONObject("dateTime") ?:
            return handler.raise(InvalidResponseContentException("Stop has no object with key \"dateTime\""))
        val depPlanned = parseTime2(dateTime, handler)
        val depActual = parseTime2(dateTime, handler, realtimeIfPossible=true)

        var flags = Stop.FLAG_NONE
        if (depActual != null) {
            flags = flags or Stop.FLAG_REALTIME_DEPARTURE
        }

        return StopImpl(
            poi=station, platformPlanned=platform,
            departurePlanned=depPlanned, departureActual=depActual,
            flags=flags
        )
    }

    private fun parseStop3(obj: JSONObject, handler: ExceptionHandler<*>): Stop? {
        val station = parseStation4(obj, handler) ?: return null
        val platform = parsePlatform2(obj, handler)

        val depPlanned = obj.optJSONObject("dateTime")?.let { parseTime3(it, handler) }
        val depActual = obj.optJSONObject("realDateTime")?.let { parseTime3(it, handler) }

        var flags = Stop.FLAG_NONE
        if (depActual != null) {
            flags = flags or Stop.FLAG_REALTIME_DEPARTURE
        }

        val tripStatus = obj.optString("realtimeTripStatus").split("|")
        if (tripStatus.contains("MONITORED")) {
            flags = flags or Stop.FLAG_REALTIME
        }

        return StopImpl(
            poi=station, platformPlanned=platform,
            departurePlanned=depPlanned, departureActual=depActual,
            flags=flags
        )
    }

    private fun filterRealtimeConnection1(obj: JSONObject): Boolean {
        // TODO: find a better way to do this
        // weird hack, but sometimes C-Bahnen will be displayed twice, once as C-Bahn and once as R-Bahn
        // since C-Bahnen will have a nicer icon, we simply ignore the "second" R-Bahn
        val operator = obj.optJSONObject("operator")?.optString("name")
        val vehicleType = obj.optJSONObject("servingLine")?.optString("name")
        return !((operator == "City-Bahn Chemnitz") && (vehicleType == "R-Bahn"))
    }

    private fun parseRealtimeConnection1(obj: JSONObject, handler: ExceptionHandler<*>) : RealtimeConnection? {
        val stop = parseStop3(obj, handler) ?: return null

        val tripId = obj.optJSONObject("diva")?.optStringNull("tripCode")

        val mot = parseMOT2(obj.optJSONObject("servingLine") ?:
            return handler.raise(InvalidResponseContentException("Realtime connection has no key \"servingLine\" (vehicle)"))
        , handler) ?: return null

        val id = ConnectionIdentifierVMS.make((mot as? LineMOT)?.line, tripId, stop)

        var flags = Connection.FLAG_NONE
        val tripStatus = obj.optString("realtimeTripStatus").split("|")
        if (tripStatus.contains("TRIP_CANCELLED")) {
            flags = flags or Connection.FLAG_CANCELLED
        }

        return RealtimeConnectionImpl(id=id, stop=stop, modeOfTransport=mot, flags=flags)
    }

    private fun parseTime1(time: String, handler: ExceptionHandler<*>): LocalDateTime? {
        return try {
            LocalDateTime.from(FORMATTER_DATETIME_SEC.parse(time))
        } catch (e: DateTimeParseException) {
            handler.raise(InvalidResponseContentException("Could not parse time \"$time\"", e))
        }
    }

    private fun parseTime2(dateTime: JSONObject, handler: ExceptionHandler<*>, realtimeIfPossible: Boolean=false): LocalDateTime? {
        var timeSec = true
        var time = dateTime.optStringNull("timeSec")
        if (time == null) {
            time = dateTime.optStringNull("time")
            timeSec = false
        }
        var date = dateTime.optStringNull("date")
        if (realtimeIfPossible) {
            var timeSecR = true
            var timeR = dateTime.optStringNull("rtTimeSec")
            if (timeR == null) {
                timeR = dateTime.optStringNull("rtTime")
                timeSecR = false
            }
            if (timeR != null) {
                time = timeR
                timeSec = timeSecR
            }

            date = dateTime.optStringNull("rtDate") ?: date
        }
        time ?: return handler.raise(InvalidResponseContentException("DateTime object has no key \"timeSec\" or \"time\" (or \"rtTimeSec\" or \"rtTime\" when possible)"))
        date ?: return handler.raise(InvalidResponseContentException("DateTime object has no key \"date\" (or \"rtDate\" when possible)"))

        val dateP = try {
            LocalDate.from(FORMATTER_DATE.parse(date))
        } catch (e: DateTimeParseException) {
            return handler.raise(InvalidResponseContentException("Date of datetime object could not be parsed with time \"$time\" and date \"$date\"", e))
        }

        val timeP = try {
            LocalTime.from((if (timeSec) FORMATTER_TIME_SEC else FORMATTER_TIME).parse(time))
        } catch (e: DateTimeParseException) {
            return handler.raise(InvalidResponseContentException("Time of datetime object could not be parsed with time \"$time\" and date \"$date\"", e))
        }

        return LocalDateTime.of(dateP, timeP)
    }

    private fun parseTime3(dateTime: JSONObject, handler: ExceptionHandler<*>): LocalDateTime? {
        val year = dateTime.optStringNull("year")?.toIntOrNull() ?:
            return handler.raise(InvalidResponseContentException("Datetime object has no key \"year\" or the value can not be converted to int"))
        val month = dateTime.optStringNull("month")?.toIntOrNull() ?:
            return handler.raise(InvalidResponseContentException("Datetime object has no key \"month\" or the value can not be converted to int"))
        val day = dateTime.optStringNull("day")?.toIntOrNull() ?:
            return handler.raise(InvalidResponseContentException("Datetime object has no key \"day\" or the value can not be converted to int"))

        val hour = dateTime.optStringNull("hour")?.toIntOrNull() ?:
            return handler.raise(InvalidResponseContentException("Datetime object has no key \"hour\" or the value can not be converted to int"))
        val minute = dateTime.optStringNull("minute")?.toIntOrNull() ?:
            return handler.raise(InvalidResponseContentException("Datetime object has no key \"minute\" or the value can not be converted to int"))

        return LocalDateTime.of(year, month, day, hour, minute)
    }

    private fun addTimeDelay1(planned: LocalDateTime?, delay: String?, valid: String?): LocalDateTime? {
        if (planned == null) { return null }
        if (valid != "1") { return null }
        val delayLong = try { delay?.toLong() } catch (_: NumberFormatException) { null } ?: return null
        if (delayLong == 0L) { return null }
        return planned.plusMinutes(delayLong)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parsePlatform1(obj: JSONObject, handler: ExceptionHandler<*>): Platform? {
        val id = obj.optJSONObject("ref")?.optStringNull("platform")?.toPlatId()
        val name = obj.optStringNull("platformName") ?: return null
        if (name.isBlank()) { return null }
        return PlatformImpl(id=id, name=name)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parsePlatform2(obj: JSONObject, handler: ExceptionHandler<*>): Platform? {
        val id = obj.optStringNull("platform")?.toPlatId()
        val name = obj.optStringNull("platformName") ?: return null
        if (name.isBlank()) { return null }
        return PlatformImpl(id=id, name=name)
    }

    private fun parsePath1(path: String, handler: ExceptionHandler<*>): List<LocationLatLon> {
        return path.split(' ').mapNotNull { parseCoords1(it, handler) }
    }

    private fun parseMOT1(obj: JSONObject, handler: ExceptionHandler<*>): ModeOfTransport? {
        val variant = parseLineVariant1(obj, handler)
        val line = parseLine1(obj, null, handler)
        val type = parseVehicleType(obj.optStringNull("product"), line?.name, handler) ?: line?.defaultMOTType

        if ((variant == null) && (line == null)) {
            if (type == null) { return null }
            return MOTImpl(motType=type)
        }
        return LineMOTImpl(motType=type, variant=variant, line=line)
    }

    private fun parseMOT2(obj: JSONObject, handler: ExceptionHandler<*>): ModeOfTransport? {
        val line = parseLine3(obj, handler)
        val variant = parseLineVariant2(obj, handler)
        val type = parseVehicleType(obj.optStringNull("name"), line?.name, handler)

        if (line == null && variant == null) {
            return handler.raise(InvalidResponseContentException("No line or destination could be parsed"))
        }

        return LineMOTImpl(motType=type, line=line, variant=variant)
    }

    private fun parseTrip1(obj: JSONObject, handler: ExceptionHandler<*>): Trip? {
        val legs = obj.optJSONArray("legs") ?: return handler.raise(InvalidResponseContentException("Trip has no key \"legs\""))
        val connections = parseArray(legs, ::parseConnection1, handler)
        if (connections.isEmpty()) { return handler.raise(InvalidResponseContentException("Trip has no connections")) }
        return TripImpl(connections=connections.flatten())
    }

    private fun parseConnection1(arr: JSONArray, index: Int, obj: JSONObject, handler: ExceptionHandler<*>): List<TripConnection>? {
        val tripId = obj.optJSONObject("mode")?.optJSONObject("diva")?.optStringNull("tripCode")

        val mot = obj.optJSONObject("mode")?.let { parseMOT1(it, handler) }

        var stops = obj.optJSONArray("stopSeq")?.let { parseArray(it, ::parseStop1, handler) }
        stops = stops ?: obj.optJSONArray("points")?.let { parseArray(it, ::parseStop2, handler) }
        if (stops.isNullOrEmpty()) {
            return handler.raise(InvalidResponseContentException("The keys \"stopSeq\" and/or \"points\" are both missing or empty"))
        }

        val id = ConnectionIdentifierVMS.make((mot as? LineMOT)?.line, tripId, stops.firstOrNull())

        val path = obj.optStringNull("path")?.let { parsePath1(it, handler) }

        val thisIsCertainlyWalk = mot?.motType is Walk

        val turnInst = obj.optJSONArray("turnInst")?.let { parseArray(it, ::parseTurnInst1, handler) }

        val walkPathRaw = obj.optJSONObject("interchange")?.optStringNull("path")?.let { parsePath1(it, handler) }
        val walkPath = walkPathRaw ?: path

        val walkStops = if (thisIsCertainlyWalk) { stops } else {
            val nextConn = arr.optJSONObject(index+1)
            val nextStop = nextConn?.optJSONArray("points")?.optJSONObject(0)?.let { parseStop2(it, handler) } ?:
                nextConn?.optJSONArray("stopSeq")?.optJSONObject(0)?.let { parseStop1(it, handler) }
            listOfNotNull(
                stops.lastOrNull(),
                nextStop
            )
        }

        var connV: TripConnection? = null
        var connW: TripConnection? = null

        if (!thisIsCertainlyWalk) {
            connV = TripConnectionImpl(id=id, modeOfTransport=mot, path=path, stops=stops)
        }

        if (thisIsCertainlyWalk) {
            connW = TripWalkConnection(modeOfTransport=mot, path=walkPath, stops=stops, instructions=turnInst)
        } else if (!turnInst.isNullOrEmpty()) {
            connW = TripWalkConnection(modeOfTransport=TripWalkConnection.DEFAULT_VEHICLE_INTERCHANGE, path=walkPath, stops=walkStops, instructions=turnInst)
        } else if (!walkPathRaw.isNullOrEmpty()) {
            connW = TripConnectionImpl(modeOfTransport=TripWalkConnection.DEFAULT_VEHICLE_INTERCHANGE, path=walkPath, stops=walkStops)
        }

        return listOfNotNull(connV, connW)
    }

    private fun parseConnection2(obj: JSONObject, handler: ExceptionHandler<*>): Connection? {
        val params = obj.optJSONObject("params") ?:
            return handler.raise(InvalidResponseContentException("Connection has no key \"params\""))

        val vehicleRaw = params.optJSONObject("mode")
        val idRaw = vehicleRaw?.optJSONObject("diva")?.optStringNull("tripCode")

        val mot = parseMOT1(vehicleRaw, handler)

        val stops = parseArray(params.optJSONArray("stopSeq") ?:
            return handler.raise(InvalidResponseContentException("Connection params has no key \"stopSeq\""))
        , ::parseStop1, handler)

        val id = ConnectionIdentifierVMS.make((mot as? LineMOT)?.line, idRaw, stops.firstOrNull())

        val path = obj.optJSONObject("coords")?.optStringNull("path")?.let { parsePath1(it, handler) }

        return ConnectionImpl(id=id, stops=stops, modeOfTransport=mot, path=path)
    }

    // parse the destination of a vehicle/line -> returns StationDirection if possible, normal Direction otherwise
    private fun parseLineVariant1(obj: JSONObject, handler: ExceptionHandler<*>): LineVariant? {
        val saveCount = handler.save()

        val stationId = obj.optStringNull("destId")
        if (stationId != null) { handler.add("station id (destination): $stationId") }

        var name = obj.optStringNull("destination") ?:
            return handler.raise(InvalidResponseContentException("Destination has no valid name"), restoreTo=saveCount)

        val destination = if (stationId != null) {
            name = cleanStationName(name, handler)
            StationDirectionImpl(StationImpl(id=stationId.toStaId(), name=name))
        } else {
            DirectionImpl(name)
        }

        handler.restoreToCount(saveCount)
        return LineVariantImpl(direction=destination)
    }

    private fun parseLineVariant2(obj: JSONObject, handler: ExceptionHandler<*>): LineVariant? {
        var lineNameS = obj.optStringNull("symbol")
        if (lineNameS.isNullOrBlank()) { lineNameS = null }

        var destinationS = obj.optStringNull("direction")
        if (lineNameS != null && destinationS?.startsWith(lineNameS) == true) {
            val destinationSplit = destinationS.split(" ", limit=2)
            if (destinationSplit.size > 1) {
                lineNameS = destinationSplit[0]
                destinationS = destinationSplit[1]
            }
        }

        val destinationId = obj.optStringNull("destID")
        val destination = when {
            destinationS == null -> null
            destinationId != null -> StationDirectionImpl(StationImpl(id=destinationId.toStaId(), name=cleanStationName(destinationS, handler)))
            else -> DirectionImpl(destinationS)
        }

        if (destination == null && lineNameS.isNullOrEmpty()) { return null }

        return LineVariantImpl(name=lineNameS, direction=destination)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class ConnectionIdentifierVMS private constructor(val lineId: String, val tripId: String, val stopId: String, val stopTime: String, val stopDate: String, val connectionId: String) : ConnectionIdentifier {
        override val uuid: String = connectionId
        override val uid: String = concat(lineId, tripId)
        companion object {
            fun parse(connectionId: String): ConnectionIdentifierVMS? {
                val parts = connectionId.split(Identifier.SAFE_CONCAT)
                if (parts.size != 5) { return null }
                val lineId = parts[0]
                val tripId = parts[1]
                val stopId = parts[2]
                val stopTime = parts[3]
                val stopDate = parts[4]
                return ConnectionIdentifierVMS(lineId, tripId, stopId, stopTime, stopDate, connectionId)
            }
            fun of(connectionId: Identifier): ConnectionIdentifierVMS? {
                if (connectionId is ConnectionIdentifierVMS) { return connectionId }
                return parse(connectionId.uuid)
            }
            fun make(line: Line?, tripId: String?, stop: Stop?): ConnectionIdentifierVMS? {
                val lineId = line?.id?.uuid ?: return null
                tripId ?: return null
                val stopId = stop?.poi?.id?.uuid ?: return null
                val stopTime = stop.departurePlanned?.format(FORMATTER_TIME_WEIRD) ?: return null
                val stopDate = stop.departurePlanned?.format(FORMATTER_DATE_WEIRD) ?: return null
                val connId = make(lineId, tripId, stopId, stopTime, stopDate)
                return ConnectionIdentifierVMS(lineId, tripId, stopId, stopTime, stopDate, connId)
            }

            private fun make(vararg components: String) = components.joinToString(Identifier.SAFE_CONCAT)
        }
    }

    // parse a coords string to a location
    private fun parseCoords1(coordsS: String?, handler: ExceptionHandler<*>): LocationLatLon? {
        if (coordsS == null) { return null }

        val saveCount = handler.save()
        handler.add("coords (string): $coordsS")

        val coordsL = coordsS.split(",")
        if (coordsL.size != 2) {
            return handler.raise(InvalidResponseContentException("Coords were not split into 2 parts"), restoreTo=saveCount)
        }

        handler.restoreToCount(saveCount)
        return parseCoords2(coordsL[0], coordsL[1], handler)
    }

    private fun parseCoords2(coordX: String?, coordY: String?, handler: ExceptionHandler<*>): LocationLatLon? {
        if ((coordX.isNullOrEmpty()) || (coordY.isNullOrEmpty())) { return null }

        val saveCount = handler.save()
        handler.add("coord x (string): $coordX")
        handler.add("coord y (string): $coordY")

        val lon = coordX.toDoubleOrNull() ?:
            return handler.raise(InvalidResponseContentException("Coordinate X (lon) could not be converted to double: $coordX"), restoreTo=saveCount)
        val lat = coordY.toDoubleOrNull() ?:
            return handler.raise(InvalidResponseContentException("Coordinate Y (lat) could not be converted to double: $coordY"), restoreTo=saveCount)

        handler.restoreToCount(saveCount)
        return LocationLatLonImpl(lat, lon)
    }

    // map the transport modes to vehicle types
    private fun parseVehicleType(product: String?, lineName: String?, handler: ExceptionHandler<*>): MOTType? {
        return when (product) {
            "Bus" -> if (lineName?.startsWith("N") == true) MOTTypes.BUS_NIGHT else MOTTypes.BUS
            "PlusBus" -> MOTTypes.BUS_REGIONAL
            "Schülerlinie" -> MOTTypes.BUS_SCHOOL
            "Ersatzverkehr" -> MOTTypes.BUS_REPLACES_TRAIN
            "Tram" -> MOTTypes.TRAM
            "Chemnitz Bahn" -> CHEMNITZ_BAHN
            "Zug" -> when {
                lineName?.startsWith("RB") == true -> VehicleTypes.RB
                lineName?.startsWith("RE") == true -> VehicleTypes.RE
                lineName?.startsWith("IC") == true -> VehicleTypes.IC
                else -> MOTTypes.TRAIN
            }
            "R-Bahn" -> when {
                lineName?.startsWith("RB") == true -> VehicleTypes.RB
                lineName?.startsWith("RE") == true -> VehicleTypes.RE
                else -> MOTTypes.TRAIN
            }
            "Fussweg" -> MOTTypes.WALK
            else -> handler.raise(InvalidResponseContentException("Found unknown vehicle type \"$product\""))
        }
    }

    // remove some stuff from the station name (i.e. the area: "Chemnitz, Reichsstr" -> "Reichsstr")
    @Suppress("UNUSED_PARAMETER")
    private fun cleanStationName(stationName: String, handler: ExceptionHandler<*>) : String {
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

    private fun isPOIStop1(obj: JSONObject) : Boolean {
        return obj.optString("anyType") == "stop"
    }

    private fun isPOIInArea1(obj: JSONObject) : Boolean {
        return limitArea.isEmpty() || obj.optString("mainLoc") == limitArea
    }

    private fun isStationStop(station: JSONObject) : Boolean {
        return station.optString("anyType") == "stop" || station.optString("type") == "STOP"
    }

    private fun isStationInArea(station: JSONObject) : Boolean {
        return limitArea.isEmpty() || station.optString("mainLoc", "") == limitArea || station.has("locality")
    }
}