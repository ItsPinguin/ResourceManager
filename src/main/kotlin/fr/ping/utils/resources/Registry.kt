package fr.ping.fr.ping.utils.resources

import com.google.gson.JsonObject
import fr.ping.utils.resources.Resource
import fr.ping.utils.resources.ResourceHandle
import java.io.File

abstract class Registry<T : Resource> (
  val type: Class<T>
) {
  val resourceMap = mutableMapOf<String, ResourceHandle<T>>()

  abstract fun loadResource(string: String) : T?
  abstract fun loadResource(file: File) : T?
  abstract fun loadResource(map: Map<String, Any>) : T?
  abstract fun loadResource(json: JsonObject) : T?

  fun registerResource(id: String, any: Any?) = any?.let { registerResource(id, any as T) } ?: Unit

  fun registerResource(id: String, resource: T) {
    try {
      resourceMap[id] = ResourceHandle(this, id).apply { this.resource = resource }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun getResource(id: String?) : T? = resourceMap[id]?.resource

  fun getResourceHandle(id: String?) : ResourceHandle<T>? = resourceMap[id]

  fun listIds() : List<String> = resourceMap.keys.toList()

  fun listHandles() : List<ResourceHandle<T>> = resourceMap.values.toList()

  fun listResources() : List<T> = resourceMap.values.mapNotNull { it.resource }

  fun reloadResources() {
    resourceMap.values.forEach { handle ->
      handle.resource
    }
  }
}