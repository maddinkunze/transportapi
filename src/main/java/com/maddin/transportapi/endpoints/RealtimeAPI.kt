package com.maddin.transportapi.endpoints

import com.maddin.transportapi.components.POI
import com.maddin.transportapi.components.RealtimeConnection
import com.maddin.transportapi.utils.APIRequest
import com.maddin.transportapi.utils.APIResponse
import com.maddin.transportapi.utils.IThoughtOfThePossibilityException
import java.time.LocalDateTime

interface RealtimeRequest : APIRequest {
    val poi: POI
    val time: LocalDateTime?; get() = null

    fun getRequiredFeatures(): Int {
        var features = RealtimeAPI.FEATURE_REALTIME_NONE
        val now = LocalDateTime.now()
        if (time?.let { it < now } == true) { features = features or RealtimeAPI.FEATURE_REALTIME_PAST
        }
        if (time?.let { it > now } == true) { features = features or RealtimeAPI.FEATURE_REALTIME_FUTURE
        }
        return features
    }
}
class RealtimeRequestImpl(
    override var poi: POI,
    override var time: LocalDateTime?=null
) : RealtimeRequest

interface RealtimeResponse : APIResponse {
    override val request: RealtimeRequest
    val connections: List<RealtimeConnection>
}
class RealtimeResponseImpl(
    override val request: RealtimeRequest,
    override val connections: List<RealtimeConnection>,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : RealtimeResponse

interface RealtimeAPI {
    companion object {
        const val FEATURE_REALTIME_NONE = 0
        const val FEATURE_REALTIME_PAST = 1  // allow to check departures from a station starting in the past
        const val FEATURE_REALTIME_FUTURE = 2  // allow to check departures from a station starting not from now but somewhere in the future
    }

    fun getRealtimeInformation(request: RealtimeRequest): RealtimeResponse
    fun getRealtimeFeatures(): Int = FEATURE_REALTIME_NONE
    fun supportsRealtimeFeature(feature: Int): Boolean = (getRealtimeFeatures() and feature) != 0
    fun getMissingRealtimeFeatures(request: RealtimeRequest): Int = request.getRequiredFeatures() and getRealtimeFeatures().inv()
}