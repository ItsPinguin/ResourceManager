package fr.ping.fr.ping.utils.resources.scheme

import com.google.gson.JsonElement

open class SchemeException(scheme: ResourceScheme) : Exception()

class MissingFieldException(scheme: ResourceScheme, field: FieldScheme) : SchemeException(scheme)

class InvalidTypeFieldException(scheme: ResourceScheme, field: FieldScheme, actual: JsonElement) : SchemeException(scheme)