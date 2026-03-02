package fr.ping.utils.resources

import com.google.gson.JsonObject
import fr.ping.fr.ping.utils.resources.scheme.FieldScheme
import fr.ping.fr.ping.utils.resources.scheme.ResourceScheme
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.forEach

class Registry<T : Resource> (val type: Class<T>, val registryName: String) : Cleanable {
  private val resourceMap: MutableMap<String, ResourceHandle<T>> = ConcurrentHashMap()
  var lazyRegistry : Boolean = false
  var parentNamespace : Namespace? = null
  var assignedDirectories : MutableList<File> = mutableListOf()
  var resourceScheme : ResourceScheme? = null

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

  fun applyScheme(scheme : ResourceScheme) {
    resourceScheme = scheme
  }

  fun applyScheme(inputStream: InputStream?) {
    if (inputStream == null) return
    val reader = InputStreamReader(inputStream)
    val content = reader.readText()
    println(content)
    val jsonObject : JsonObject = ResourceManager.gson?.fromJson(content, JsonObject::class.java) ?: JsonObject()
    println(jsonObject)
    val scheme = ResourceScheme()
    //scheme.fields = ResourceManager.gson?.fromJson(jsonObject.get("fields"), MutableMap::class.java) as? MutableMap<String, FieldScheme> ?: mutableMapOf()
    jsonObject.asMap().forEach { (key : String, value) ->
      //println("Key: $key, Value: $value")
      //scheme.fields?.put(key, value)
      println("Field is" + ResourceManager.gson?.fromJson(value, FieldScheme::class.java))
      ResourceManager.gson?.fromJson(value, FieldScheme::class.java)?.let { scheme.fields?.put(key, it) }
    }
    applyScheme(scheme)
    reader.close()
    println("§aScheme applied! Using: $scheme")
  }
}