package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.components.LineMOT
import com.maddin.transportapi.components.LineMOTImpl
import com.maddin.transportapi.components.MOTImpl
import com.maddin.transportapi.components.MOTTypes
import com.maddin.transportapi.components.POI
import com.maddin.transportapi.components.POIIdentifier
import com.maddin.transportapi.components.POIIdentifierImpl
import com.maddin.transportapi.components.Platform
import com.maddin.transportapi.components.RealtimeConnection
import com.maddin.transportapi.components.RealtimeConnectionImpl
import com.maddin.transportapi.components.Station
import com.maddin.transportapi.components.StationIdentifier
import com.maddin.transportapi.components.StationImpl
import com.maddin.transportapi.components.Stop
import com.maddin.transportapi.components.StopImpl
import com.maddin.transportapi.endpoints.POIUseCase
import com.maddin.transportapi.endpoints.RealtimeAPI
import com.maddin.transportapi.endpoints.RealtimeRequest
import com.maddin.transportapi.endpoints.RealtimeRequestImpl
import com.maddin.transportapi.endpoints.RealtimeResponse
import com.maddin.transportapi.endpoints.RealtimeResponseImpl
import com.maddin.transportapi.endpoints.SearchPOIAPI
import com.maddin.transportapi.endpoints.SearchPOIRequest
import com.maddin.transportapi.endpoints.SearchPOIRequestImpl
import com.maddin.transportapi.endpoints.SearchPOIResponse
import com.maddin.transportapi.endpoints.SearchPOIResponseImpl
import com.maddin.transportapi.endpoints.TripSearchAPI
import com.maddin.transportapi.utils.APIResponse
import com.maddin.transportapi.utils.ExceptionHandler
import com.maddin.transportapi.utils.FusedAPI
import com.maddin.transportapi.utils.IThoughtOfThePossibilityException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

class VMSandCVAGPOIIdImpl(vmsId: VMSStationId, cvagId: CVAGStationId) : StationIdentifier, CVAGStationId by cvagId, VMSStationId by vmsId {
    override val uuid = vmsId.vmsStateless
}


class VMSandCVAG : FusedAPI, SearchPOIAPI, RealtimeAPI, TripSearchAPI by vms {
    companion object {
        private val vms = VMS("Chemnitz")
        private val cvag = CVAG()

        private val strToStrasse = "((?<=[a-z\\u00F0-\\u02AF])s|(?<=[ -])S)tr(\\.|a(ss|\\u00df)e)?".toRegex()
        private val plToPlatz = "((?<![a-z\\u00F0-\\u02AF])p|(?<=[ -])P)l(\\.|atz)?".toRegex()
        private val hbfToHauptbahnhof = "Hbf(?=( |$))".toRegex()
        private val hbfRemoveComma = "(?<=[hH]auptbahnhof),(?= )".toRegex()
        private val ignorePrefixes = arrayOf("chemnitz")
        private fun String.cleanStationNameForComparison() =
            this.replace(strToStrasse, "strasse")
                .replace(plToPlatz, "platz")
                .replace(hbfToHauptbahnhof, "hauptbahnhof")
                .replace(hbfRemoveComma, "")
                .split(", ".toRegex()).mapIndexedNotNull { index, s ->
                    if (index > 0) { return@mapIndexedNotNull s }
                    if (s.lowercase() in ignorePrefixes) { return@mapIndexedNotNull null }
                    s
                } .joinToString(", ")
                .lowercase()
    }

    private fun searchPOIsVMS(request: SearchPOIRequest): SearchPOIResponse {
        // prepare request for delegation
        val vmsRequest = SearchPOIRequestImpl(request.search)

        // get response
        return vms.searchPOIs(vmsRequest)
    }

    private fun searchPOIsCVAG(request: SearchPOIRequest): SearchPOIResponse? {
        // check if request is eligible for delegation
        if (request.useCase != null && request.useCase != POIUseCase.REALTIME) { return null }
        if (cvag.getMissingSearchPOIFeatures(request) != 0) { return null }

        // prepare request for delegation
        val cvagRequest = SearchPOIRequestImpl(request.search.replace(".", ""))

        // get response
        return cvag.searchPOIs(cvagRequest)
    }

    private fun Station.copy(id: StationIdentifier?=null, name: String?=null) : Station {
        return StationImpl(
            id = id ?: this.id,
            name = name ?: this.name,
            location = location,
            lines = lines,
            platforms = platforms
        )
    }

    override fun searchPOIs(request: SearchPOIRequest): SearchPOIResponse {
        val vmsResponse = searchPOIsVMS(request)
        val cvagResponse = searchPOIsCVAG(request) ?: return vmsResponse

        val pois = vmsResponse.pois.map { vmsS ->
            if (vmsS !is Station) { return@map vmsS }

            // find the CVAG station that (best) matches this station
            val cleanNameV = vmsS.name.cleanStationNameForComparison()

            val cvagS = cvagResponse.pois.find {
                val cleanNameC = it.name.cleanStationNameForComparison()
                cleanNameC == cleanNameV
            } ?: return@map vmsS

            vmsS.copy(
                id=VMSandCVAGPOIIdImpl(vmsS.id as VMSStationId, cvagS.id as CVAGStationId),
                name=cvagS.name
            )
        }

        val exceptions = mergeExceptions(vmsResponse, cvagResponse)
        return SearchPOIResponseImpl(request=request, pois=pois, exceptions=exceptions)
    }

    override val searchPOIFeatures = vms.searchPOIFeatures

    private fun getRealtimeInformationVMS(request: RealtimeRequest) : RealtimeResponse {
        // prepare request for delegation
        val vmsRequest = RealtimeRequestImpl(request.poi, time=request.time)

        // get response
        return vms.getRealtimeInformation(vmsRequest)
    }

    private fun getRealtimeInformationCVAG(request: RealtimeRequest) : RealtimeResponse? {
        if (request.poi.id !is CVAGStationId) { return null }
        val missingFeatures = cvag.getMissingRealtimeFeatures(request)
        if (missingFeatures and (RealtimeAPI.FEATURE_REALTIME_FUTURE or RealtimeAPI.FEATURE_REALTIME_PAST) != 0) {
            val delay = request.time?.let { ChronoUnit.SECONDS.between(LocalDateTime.now(), it) } ?: 0
            if (delay.absoluteValue > 15) { return null } // if we are asking more than 15 seconds into the future or past, we dont engage
        } else if (missingFeatures != 0) {
            return null
        }

        // prepare request for delegation
        val cvagRequest = RealtimeRequestImpl(request.poi)

        // get response
        return cvag.getRealtimeInformation(cvagRequest)
    }

    private fun Stop.copy(departureActual: LocalDateTime? = null, platformActual: Platform? = null, flags: Int = 0) = StopImpl(
        poi=poi, platformPlanned=platformPlanned, platformActual=platformActual ?: this.platformActual,
        departurePlanned=departurePlanned, departureActual=departureActual ?: this.departureActual,
        arrivalPlanned=arrivalPlanned, arrivalActual=arrivalActual,
        flags=this.flags or flags
    )

    private fun RealtimeConnection.copy(departureActual: LocalDateTime? = null, platformActual: Platform? = null, flags: Int = 0) : RealtimeConnection {
        val stop = stop.copy(departureActual, platformActual, flags)
        val stops = stops.map { if (it == this.stop) { stop } else { it } }
        return RealtimeConnectionImpl(
            id=id, stops=stops, stop=stop,
            modeOfTransport=modeOfTransport, path=path, flags=this.flags
        )
    }

    private fun String.isProbablyTheSameLineNumberAs(other: String): Boolean {
        val s1 = this.trim().lowercase()
        val s2 = other.trim().lowercase()

        if (s1 == s2) { return true }
        if (s1.startsWith("ev") && s2.endsWith("ev")) {
            if (s1.removePrefix("ev") == s2.removeSuffix("ev")) { return true } // EV1 == 1EV
        }
        return false
    }

    private fun String.isProbablyTheSameDirectionAs(other: String): Boolean {
        val s1 = this.trim().lowercase()
        val s2 = other.trim().lowercase()

        for ((a, b) in s1.zip(s2)) {
            if (a == ' ') { break } // Flemmingstr == Flemmingstr ü. Klinkimum
            if (b == ' ') { break }
            if (a == '.') { break } // Flemmingstr. == Flemmingstr
            if (b == '.') { break }
            if (a != b) { return false }
        }
        return true
    }

    override fun getRealtimeInformation(request: RealtimeRequest): RealtimeResponse {
        val vmsResponse = getRealtimeInformationVMS(request)
        val cvagResponse = getRealtimeInformationCVAG(request) ?: return vmsResponse

        val connections = vmsResponse.connections.map { vmsC ->
            val vmsDep = vmsC.estimateDepartureActual() ?: return@map vmsC
            val vmsLS = (vmsC.modeOfTransport as? LineMOT)?.symbol ?: return@map vmsC
            val vmsLD = (vmsC.modeOfTransport as? LineMOT)?.direction ?: return@map vmsC

            val cvagC = cvagResponse.connections.find {
                val lineMOT = (it.modeOfTransport as? LineMOT) ?: return@find false

                if (lineMOT.symbol?.isProbablyTheSameLineNumberAs(vmsLS) != true) { return@find false } // check if same line symbol (i.e. 31, 21, C11)
                if (lineMOT.direction?.isProbablyTheSameDirectionAs(vmsLD) != true) { return@find false } // estimate if same direction (i.e. Zentralhaltestelle)

                val timeDiscrepancy = it.departsOrArrivesIn(vmsDep) ?: return@find false
                timeDiscrepancy.seconds in -120..240 // if this connection is within a -2m to 4m timeframe, it is probably the same, and even if not, the time will be skewed by 4 minutes at most
            } ?: return@map vmsC

            vmsC.copy(cvagC.stop.departureActual, cvagC.stop.platformActual, cvagC.stop.flags)
        }.sortedBy { it.estimateDepartureActual() }


        val exceptions = mergeExceptions(vmsResponse, cvagResponse)

        return RealtimeResponseImpl(request=request, connections=connections, exceptions=exceptions)
    }

    override fun getRealtimeFeatures() = vms.getRealtimeFeatures()
}