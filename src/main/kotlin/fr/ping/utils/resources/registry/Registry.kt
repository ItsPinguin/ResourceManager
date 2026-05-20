package fr.ping.fr.ping.utils.resources.registry

import com.google.gson.JsonObject
import fr.ping.utils.resources.Resource
import fr.ping.utils.resources.ResourceHandle
import java.io.File
import kotlin.collections.get

abstract class Registry<T : Resource> (
  val type: Class<T>,
  private val indexes : MutableMap<String, (T) -> String> = mutableMapOf()
) {
  val resourceMap = mutableMapOf<String, ResourceHandle<T>>()
  val indexMap : MutableMap<String, MutableMap<Any, MutableSet<String>>> = mutableMapOf()

  abstract fun loadResource(string: String) : T?
  abstract fun loadResource(file: File) : T?
  abstract fun loadResource(map: Map<String, Any>) : T?
  abstract fun loadResource(json: JsonObject) : T?

  fun registerResource(id: String, any: Any?) = any?.let { registerResource(id, any as T) } ?: Unit

  fun registerResource(id: String, resource: T) {
    try {
      resource.id = id
      val handle = resourceMap[id] ?: ResourceHandle(this, id)
      handle.resource = resource
      resourceMap[id] = handle
      indexes.forEach { index ->
        indexMap.getOrPut(index.key) { mutableMapOf() }
          .getOrPut(index.value.invoke(resource)) { mutableSetOf() }.add(resource.id)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun unregisterResource(id : String) {
    resourceMap.remove(id)
  }

  fun getResource(id: String?) : T? = resourceMap[id]?.resource

  fun getResourceHandle(id: String?) : ResourceHandle<T>? {
    if (id == null) return null
    if (resourceMap[id] == null) {
      resourceMap[id] = ResourceHandle(this, id)
    }
    return resourceMap[id]
  }

  fun listIdsByIndex(index: String, value: Any) : Set<String> = indexMap[index]?.get(value.toString())?.toSet() ?: setOf()

  fun listIds() : List<String> = resourceMap.keys.toList()

  fun listHandles() : List<ResourceHandle<T>> = resourceMap.values.toList()

  fun listResources() : List<T> = resourceMap.values.mapNotNull { it.resource }

  fun reloadResources() {
    resourceMap.values.forEach { handle ->
      handle.resource
    }
  }

  fun registerIndex(index: String, indexMapper: (T) -> String) = indexes.put(index, indexMapper)
}