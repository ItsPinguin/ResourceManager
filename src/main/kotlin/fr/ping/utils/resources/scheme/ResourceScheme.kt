package fr.ping.fr.ping.utils.resources.scheme

import com.google.gson.JsonObject
import fr.ping.utils.resources.ResourceManager
import java.io.File

data class ResourceScheme(var fields: MutableMap<String, FieldScheme>? = mutableMapOf()) {
  fun isSchemeValid(jsonElement: JsonObject, fields : Map<String, FieldScheme>? = this.fields) : Boolean {
    if (fields.isNullOrEmpty()) return true
    fields.forEach { field ->
      if (!jsonElement.has(field.key)) return false
      val element = jsonElement.get(field.key)
      if (field.value.required && element.isJsonNull) throw MissingFieldException(this, field.value)
      if (field.value.isArray) {
        if (!element.isJsonArray) throw InvalidTypeFieldException(this, field.value, element)
      }
      when (field.value.type) {
        "object" -> {
          if (!element.isJsonObject) return false
          return isSchemeValid(element.asJsonObject, field.value.fields)
        }
        "array" -> {
          if (!element.isJsonArray) return false
          //TODO CHECK EACH ELEMENT
        }
        "string" -> {
          if (field.value.type == "string"
            && element.isJsonPrimitive
            && element.asString.isNotBlank()) return true
        }
        "int", "number" -> {
          try {
            element.asLong
            return true
          } catch (e: Exception) {
            return false
          }
        }
        else -> return true //CHECK TYPE
      }
    }
    return true
  }

  override fun toString(): String {
    return "ResourceScheme(fields=$fields)"
  }

  companion object {
    fun fromFile(file: File) : ResourceScheme {
      if (!file.exists()) return ResourceScheme()
      val jsonElement = ResourceManager.gson?.fromJson(file.reader(), ResourceScheme::class.java) ?: ResourceScheme()
      return jsonElement
    }
  }


}