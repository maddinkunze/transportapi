package com.maddin.transportapi.endpoints

import com.maddin.transportapi.components.Connection
import com.maddin.transportapi.components.ConnectionIdentifier
import com.maddin.transportapi.utils.APIRequest
import com.maddin.transportapi.utils.APIResponse
import com.maddin.transportapi.utils.IThoughtOfThePossibilityException

interface ConnectionRequest : APIRequest {
    val connectionId : ConnectionIdentifier
    fun getRequiredFeatures() = ConnectionAPI.CONNECTION_FEATURE_NONE
}
class ConnectionRequestImpl(override val connectionId: ConnectionIdentifier) : ConnectionRequest {
    constructor(connection: Connection) : this(connectionId=connection.id!!)
    companion object {
        fun make(connection: Connection) : ConnectionRequest? {
            return connection.id?.let { ConnectionRequestImpl(it) }
        }
    }
}

interface ConnectionResponse : APIResponse {
    override val request: ConnectionRequest
    val connection: Connection?
}

class ConnectionResponseImpl(
    override val request: ConnectionRequest,
    override val connection: Connection?,
    override val exceptions: List<IThoughtOfThePossibilityException>? = null
) : ConnectionResponse

interface ConnectionAPI {
    companion object {
        const val CONNECTION_FEATURE_NONE = 0
    }
    fun getConnection(request: ConnectionRequest) : ConnectionResponse
    fun getConnectionFeatures(): Int = CONNECTION_FEATURE_NONE
    fun supportsConnectionFeature(feature: Int): Boolean = (getConnectionFeatures() and feature) != 0
    fun getMissingConnectionFeatures(request: ConnectionRequest): Int = request.getRequiredFeatures() and getConnectionFeatures().inv()
}