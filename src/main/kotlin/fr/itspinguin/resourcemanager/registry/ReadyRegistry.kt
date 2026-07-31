package fr.itspinguin.resourcemanager.registry

import com.google.gson.JsonObject
import fr.itspinguin.resourcemanager.Resource
import fr.itspinguin.resourcemanager.ResourceManager
import java.io.File

class ReadyRegistry<T : Resource>(
  type : Class<T>,
  indexes : MutableMap<String, (T) -> String> = mutableMapOf()
) : Registry<T>(
  type,
  indexes
) {
  constructor(type: Class<T>, registryName: String, indexes : MutableMap<String, (T) -> String> = mutableMapOf()) : this(type, indexes) {
    ResourceManager[registryName] = this
  }

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