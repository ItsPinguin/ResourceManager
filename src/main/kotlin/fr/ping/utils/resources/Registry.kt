package fr.ping.utils.resources

import java.io.File
import java.util.concurrent.ConcurrentHashMap

class Registry<T : Resource> (val type: Class<T>, val registryName: String) : Cleanable {
  private val resourceMap: MutableMap<String, ResourceHandle<T>> = ConcurrentHashMap()
  var lazyRegistry : Boolean = false
  var parentNamespace : Namespace? = null

  fun registerResource(resourceName: String, resource: T?) : Registry<T> {
    if (resourceMap.containsKey(resourceName)) {
      resourceMap[resourceName]?.resource = resource
    } else {
      resourceMap[resourceName] = ResourceHandle(resource, this, resourceName)
    }
    return this
  }

  fun hasResource(resourceName: String) : Boolean = resourceMap.containsKey(resourceName)

  fun listResources() : List<String> = resourceMap.keys.toList()

  @Deprecated("Use getHandle() instead")
  fun listHandles() : List<ResourceHandle<T>> = resourceMap.values.toList()

  @Deprecated("Use getHandle() instead")
  fun getResource(resourceName: String): T? {
    return resourceMap[resourceName]?.resource
  }

  @Deprecated("Use getHandle() instead")
  operator fun get(resourceName: String): T? {
    return resourceMap[resourceName]?.resource
  }

  fun getHandle(resourceName: String) : ResourceHandle<T>? {
    return resourceMap[resourceName]?.acquire()
  }

  fun notifyUnused(handle: ResourceHandle<T>) {
    if (!lazyRegistry) return
    resourceMap.remove(handle.resourceName)
  }

  override fun toString(): String {
    return "Registry(registryName='$registryName', resourceMap=$resourceMap)"
  }

  override fun clean() {
    resourceMap.values.forEach {
      it.resource?.clean()
      it.parentRegistry = null
    }
    parentNamespace = null
    resourceMap.clear()
  }

  fun saveToFile(resource: String, folder: File) {
    @Suppress("DEPRECATION")
    ResourceIO.saveToFile(folder, resourceMap[resource])
  }

  fun loadFromFile(resource: String, folder: File) {
    ResourceIO.loadToRegistry(folder, File(folder, "$resource.json"), this)
  }
}