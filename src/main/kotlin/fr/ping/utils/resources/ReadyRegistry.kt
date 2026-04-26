package fr.ping.fr.ping.utils.resources

import com.google.gson.JsonObject
import fr.ping.utils.resources.Resource
import fr.ping.utils.resources.ResourceManager
import java.io.File

class ReadyRegistry<T : Resource>(
  val clazz : Class<T>
) : Registry<T>(
  clazz
) {
  override fun loadResource(string: String): T? {
    return ResourceManager.getGson().fromJson(string, type)
  }

  override fun loadResource(file: File): T {
    return loadResource(file.readText()) ?: throw Exception("Failed to load resource from file")
  }

  override fun loadResource(map: Map<String, Any>): T? {
    return null
  }

  override fun loadResource(json: JsonObject): T? {
    return ResourceManager.getGson().fromJson(json, type)
  }


}