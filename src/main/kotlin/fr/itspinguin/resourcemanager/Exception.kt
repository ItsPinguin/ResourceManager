package fr.itspinguin.resourcemanager

import java.io.File

data class LoadingException(
  val type: LoadingExceptionType,
  var file: File? = null,
  val schemeExceptions: List<SchemeException>? = null) : Exception()
enum class LoadingExceptionType {
  NULL_RESOURCE,
  NULL_TYPE,
  NO_REGISTRY,
  INVALID_SCHEME,
}


data class SchemeException(
  val type: SchemeExceptionType,
  val details: Details = Details()
) : Exception()
enum class SchemeExceptionType {
  NULL_RESOURCE,
  NULL_TYPE,
  NO_REGISTRY,
  INVALID_SCHEME,
  REQUIRED_FIELD,
  ARRAY_EXPECTED,
  OBJECT_EXPECTED,
  INVALID_TYPE,
}
data class Details(val field: String = "", val expected: String? = null)