package fr.ping.fr.ping.utils.resources

import com.google.gson.JsonObject
import fr.ping.utils.resources.Resource
import fr.ping.utils.resources.ResourceHandle
import java.io.File

abstract class Registry<T : Resource> (
  val type: Class<T>
) {
  val resourceMap = mutableMapOf<String, ResourceHandle<T>>()
  val indexMap : MutableMap<String, MutableMap<Any, MutableList<String>>> = mutableMapOf()

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
      resource.javaClass.declaredFields.forEach { field ->
        field.isAccessible = true
        field.annotations.firstOrNull { annotation ->
          annotation is RegistryIndex
        }?.let {
          indexMap.getOrPut((it as RegistryIndex).name) { mutableMapOf() }
            .getOrPut(field.get(resource)) { mutableListOf() }.add(resource.id)
        }
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

  fun listIdsByIndex(index: String, value: Any) : List<String> = indexMap[index]?.get(value) ?: listOf()

  fun listIds() : List<String> = resourceMap.keys.toList()

  fun listHandles() : List<ResourceHandle<T>> = resourceMap.values.toList()

  fun listResources() : List<T> = resourceMap.values.mapNotNull { it.resource }

  fun reloadResources() {
    resourceMap.values.forEach { handle ->
      handle.resource
    }
  }
}