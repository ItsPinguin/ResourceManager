package fr.ping.fr.ping.utils.resources.scheme

import com.google.gson.JsonObject
import fr.ping.fr.ping.utils.resources.Registry
import java.io.File

object SchemeRegistry : Registry<ResourceScheme>(ResourceScheme::class.java) {
  override fun loadResource(string: String): ResourceScheme? {
    //TODO("Not yet implemented")
    return null
  }

  override fun loadResource(file: File): ResourceScheme? {
    //TODO("Not yet implemented")
    return null
  }

  override fun loadResource(map: Map<String, Any>): ResourceScheme? {
  return null
  //TODO("Not yet implemented")
  }

  override fun loadResource(json: JsonObject): ResourceScheme? {
  return null
  //TODO("Not yet implemented")
  }
}