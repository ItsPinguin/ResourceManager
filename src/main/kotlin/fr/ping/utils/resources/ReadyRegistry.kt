package fr.ping.fr.ping.utils.resources

import com.google.gson.JsonObject
import fr.ping.utils.resources.Resource
import fr.ping.utils.resources.ResourceManager.gson
import java.io.File

class ReadyRegistry<T : Resource>(
  val clazz : Class<T>
) : Registry<T>(
  clazz
) {
  override fun loadResource(string: String): T? {
    return gson?.fromJson(string, type)
  }

  override fun loadResource(file: File): T {
    return loadResource(file.readText()) ?: throw Exception("Failed to load resource from file")
  }

  override fun loadResource(map: Map<String, Any>): T? {
    return null
  }

  override fun loadResource(json: JsonObject): T? {
    return gson?.fromJson(json, type)
  }


}