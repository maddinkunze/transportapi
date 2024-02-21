package com.maddin.transportapi.utils

import org.json.JSONException
import java.io.PrintStream
import java.io.PrintWriter
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.print.attribute.standard.Severity

// I thought of the possibility, but I don't know in which specific cases this could occur.
/* For example the API response is not JSON, this error could happen because of a wrong input
    format or because of an API change or the API maybe even went down entirely.
In those cases you can throw an IThoughOfThePossibility (subclass) exception. */
// infos can contain stuff like the URL requested that returned invalid information
abstract class IThoughtOfThePossibilityException(
    message: String? = null,
    cause: Throwable? = null,
    val severity: Int = 0,
    val infos: MutableList<String> = mutableListOf()
) : Exception(message, cause) {
    override fun printStackTrace(s: PrintStream?) {
        super.printStackTrace(s)
        s?.println("infos: ${infos.joinToString("; ")}")
    }

    override fun printStackTrace(s: PrintWriter?) {
        super.printStackTrace(s)
        s?.println("infos: ${infos.joinToString("; ")}")
    }
}

// exception was caught (and maybe considered) but not handled in any way; should always contain a cause
class UnhandledException(cause: Throwable) : IThoughtOfThePossibilityException(cause=cause)

// There was a problem with the connection
open class ConnectionException(message: String? = null, cause: Throwable? = null) : IThoughtOfThePossibilityException(message, cause)

// no internet
open class NoInternetException(message: String? = null, cause: Throwable? = null) : ConnectionException(message, cause)

// server not reachable or returned invalid status code
open class NotReachableException(message: String? = null, cause: Throwable? = null) : ConnectionException(message, cause)

open class InvalidRequestException(message: String? = null, cause: Throwable? = null) : IThoughtOfThePossibilityException(message, cause)

class InvalidRequestFormatException(message: String? = null, cause: Throwable? = null) : InvalidRequestException(message, cause)

open class InvalidRequestContentException(message: String? = null, cause: Throwable? = null) : InvalidRequestException(message, cause)

// The endpoint requires a certain type of poi (i.e. only Stations) but a different type was provided
class InvalidRequestPOIException(message: String? = null, cause: Throwable? = null) : InvalidRequestException(message, cause)

// Somehow the data the api returned is not as expected
open class InvalidResponseException(message: String? = null, cause: Throwable? = null) : IThoughtOfThePossibilityException(message, cause)

// The data the api returned is in the wrong format (eg not json)
class InvalidResponseFormatException(message: String? = null, cause: Throwable? = null) : InvalidResponseException(message, cause)

// The data the api returned contains wrong information (eg the seconds field contains a string instead of a number)
class InvalidResponseContentException(message: String? = null, cause: Throwable? = null) : InvalidResponseException(message, cause)

/*class ExceptionHandler<T>(vararg infos: String) {
    private val infos = mutableListOf(*infos)
    private val callbacks = mutableListOf<() -> String>()
    var default: ((List<IThoughtOfThePossibilityException>) -> T)? = null

    fun add(item: String) = infos.add(item)
    fun add(callback: () -> String) = callbacks.add(callback)
    fun get() = infos.apply { addAll(callbacks.map { it() }) }
    fun raise(e: Throwable) : Nothing? {
        val exc = when (e) {
            is IThoughtOfThePossibilityException -> e
            is SocketTimeoutException -> NotReachableException(cause=e)
            is UnknownHostException -> NoInternetException("The device seems to have no internet connection", e)
            is JSONException -> InvalidResponseFormatException("The servers response does not seem to be the correct format (expected json)", e)
            else -> UnhandledException(cause=e)
        }
    }
}
inline fun <T> wrapExceptions(vararg infos: String, noinline onerror: ((IThoughtOfThePossibilityException) -> T)?, lambda: (ExceptionInfoHandler) -> T) : T {
    val handler = ExceptionInfoHandler(*infos)
    return try {
        lambda(handler)
    } catch (e: Throwable) {
        val exc = when (e) {
            is IThoughtOfThePossibilityException -> e
            is SocketTimeoutException -> NotReachableException(cause=e)
            is UnknownHostException -> NoInternetException("The device seems to have no internet connection", e)
            is JSONException -> InvalidResponseFormatException("The servers response does not seem to be the correct format (expected json)", e)
            else -> UnhandledException(cause=e)
        }

        onerror?.let {
            it(exc)
        } ?: throw exc
    }
}*/

class ExceptionHandler<T>(vararg infos: String, var throwAtSeverity: Int?=null) {
    private val infos = mutableListOf(*infos)
    private val callbacks = mutableListOf<() -> String?>()
    private val exceptions = mutableListOf<IThoughtOfThePossibilityException>()
    private val states = mutableListOf<SaveState>()
    var default: (() -> T?)? = null

    private class SaveState(val infoLength: Int, val callbacksLength: Int, val severityReducedBy: Int)

    fun add(info: String) = infos.add(info)

    fun save(reduceSeverityBy: Int=0): Int {
        val count = states.size
        throwAtSeverity = throwAtSeverity?.let { it + reduceSeverityBy }
        states.add(SaveState(infos.size, callbacks.size, reduceSeverityBy))
        return count
    }
    fun restore() {
        restoreToCount(states.size-1)
    }
    fun restoreToCount(count: Int) {
        if (count < 0) { return }
        val state = states.getOrNull(count) ?: return
        restoreListToCount(states, count)
        restoreListToCount(infos, state.infoLength)
        restoreListToCount(callbacks, state.callbacksLength)
        throwAtSeverity = throwAtSeverity?.let { it - state.severityReducedBy }
    }

    private fun restoreListToCount(list: MutableList<*>, count: Int) {
        if (count < 0) { return }
        while (list.size > count) {
            list.removeLastOrNull()
        }
    }

    fun combineInfos(): List<String> {
        return infos + callbacks.mapNotNull { it() }
    }

    fun combineExceptions(): List<IThoughtOfThePossibilityException> {
        return exceptions
    }

    private fun classify(e: Throwable): IThoughtOfThePossibilityException {
        return when (e) {
            is IThoughtOfThePossibilityException -> e
            is SocketTimeoutException -> NotReachableException(cause=e)
            is UnknownHostException -> NoInternetException("The device seems to have no internet connection", e)
            is JSONException -> InvalidResponseFormatException("The servers response does not seem to be the correct format (expected json)", e)
            else -> UnhandledException(cause=e)
        }
    }

    fun raise(e: Throwable, restoreTo: Int?=null) : Nothing? {
        val exc = classify(e)

        exc.infos.addAll(combineInfos())

        if (restoreTo != null) {
            if (restoreTo < 0) { restore() }
            else { restoreToCount(restoreTo) }
        }

        exceptions.add(exc)
        if (shouldThrow(exc)) {
            throw exc
        }
        return null
    }

    private fun shouldThrow(e: IThoughtOfThePossibilityException): Boolean {
        throwAtSeverity?.let { return e.severity >= it }
        return false
    }

    fun makeDefault(e: Throwable?=null): T =
        default?.let { e?.let { exceptions.add(classify(it)) }; it() } ?:
        e?.let { throw it } ?:
        throw IllegalStateException("Tried to receive default value but the default field was not filled and no valid exception was passed")
}

inline fun <T> wrapExceptions(vararg infos: String, throwAtSeverity: Int?=null, lambda: (ExceptionHandler<T>) -> T): T {
    val handler = ExceptionHandler<T>(*infos, throwAtSeverity=throwAtSeverity)
    return try {
        lambda(handler)
    } catch (e: Throwable) {
        handler.makeDefault(e)
    }
}