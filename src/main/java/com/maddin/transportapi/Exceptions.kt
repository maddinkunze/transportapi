package com.maddin.transportapi

// I thought of the possibility, but I don't know in which specific cases this could occur.
/* For example the API response is not JSON, this error could happen because of a wrong input
    format or because of an API change or the API maybe even went down entirely.
In those cases you can throw an IThoughOfThePossibility (subclass) exception. */
abstract class IThoughtOfThePossibilityException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

// Somehow the data the api returned is not as expected
open class InvalidResponseException(message: String? = null, cause: Throwable? = null) : IThoughtOfThePossibilityException(message, cause)

// The data the api returned is in the wrong format (eg not json)
class InvalidResponseFormatException(message: String? = null, cause: Throwable? = null) : InvalidResponseException(message, cause)

// The data the api returned contains wrong information (eg the seconds field contains a string instead of a number)
class InvalidResponseContentException(message: String? = null, cause: Throwable? = null) : InvalidResponseException(message, cause)