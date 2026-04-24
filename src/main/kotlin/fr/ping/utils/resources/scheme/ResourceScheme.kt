package fr.ping.fr.ping.utils.resources.scheme

import com.google.gson.JsonObject
import fr.ping.fr.ping.utils.resources.Details
import fr.ping.fr.ping.utils.resources.LoadingExceptionType
import fr.ping.fr.ping.utils.resources.SchemeException
import fr.ping.fr.ping.utils.resources.SchemeExceptionType
import fr.ping.utils.resources.Resource
import fr.ping.utils.resources.ResourceManager
import java.io.File

data class ResourceScheme(var fields: MutableMap<String, FieldScheme>? = mutableMapOf()) : Resource() {
  fun getSchemeErrors(jsonElement: JsonObject, fields : Map<String, FieldScheme>? = this.fields) : List<SchemeException> {
    val errors = mutableListOf<SchemeException>()
    if (fields.isNullOrEmpty()) return errors
    fields.forEach { field ->
      //TODO check inlined definition
      val element = jsonElement.get(field.key)
      if (field.value.required && (element == null || element.isJsonNull)) {
        errors.add(SchemeException(SchemeExceptionType.REQUIRED_FIELD, Details(field.key, field.value.type)))
        println("hi ${field.value.type} ${field.key} $element $jsonElement")
        return@forEach
      }
      if (field.value.isArray && !element.isJsonArray)
        errors.add(SchemeException(SchemeExceptionType.ARRAY_EXPECTED, Details(field.key, field.value.type)))
      when (field.value.type) {
        "object" -> {
          if (!element.isJsonObject)
            errors.add(SchemeException(SchemeExceptionType.OBJECT_EXPECTED, Details(field.key, field.value.type)))
          return getSchemeErrors(element.asJsonObject, field.value.fields)
          TODO("FIX PATHS THING")
        }
        "array" -> {
          if (!element.isJsonArray)
            errors.add(SchemeException(SchemeExceptionType.ARRAY_EXPECTED, Details(field.key, field.value.type)))
          //TODO CHECK EACH ELEMENT
        }
        "string" -> {
          if (element.isJsonPrimitive && !element.asString.isNotBlank())
            errors.add(SchemeException(SchemeExceptionType.INVALID_TYPE, Details(field.key, field.value.type)))
        }
        "int", "number" -> {
          try {
            element.asLong
          } catch (e: Exception) {
            errors.add(SchemeException(SchemeExceptionType.INVALID_TYPE, Details(field.key, field.value.type)))
          }
        }
        else -> {} //CHECK TYPE
      }
    }
    return errors
  }

  override fun toString(): String {
    return "ResourceScheme(fields=$fields)"
  }

  override fun clean() {
  }

  companion object {
    fun fromFile(file: File) : ResourceScheme {
      if (!file.exists()) return ResourceScheme()
      val jsonElement = ResourceManager.gson?.fromJson(file.reader(), ResourceScheme::class.java) ?: ResourceScheme()
      return jsonElement
    }
  }


}