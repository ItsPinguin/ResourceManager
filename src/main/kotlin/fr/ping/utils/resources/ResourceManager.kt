package fr.ping.utils.resources

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.concurrent.ConcurrentHashMap

object ResourceManager : Cleanable {
  private val namespaceMap: MutableMap<String, Namespace> = ConcurrentHashMap()
  val resourcePathRegex = Regex("(^[a-z0-9]+):([a-z0-9]+)/([a-z0-9|/]+[a-zA-Z0-9]+)$")

  val gson: Gson? by lazy {
    GsonBuilder()
      .registerTypeAdapterFactory(WrappedResource.WrappedResourceAdapterFactory())
      .disableHtmlEscaping()
      .setPrettyPrinting()
      .create()
  }

  fun useNamespace(name: String) : Namespace {
    return namespaceMap[name] ?: namespaceMap.getOrPut(name) { Namespace(name) }
  }

  fun hasNamespace(name: String) : Boolean {
    return namespaceMap.containsKey(name)
  }

  fun listNamespaces() : List<String> {
    return namespaceMap.keys.toList()
  }

  @Deprecated("Use getHandle() instead")
  operator fun <T : Resource> get(namespace: String, registryName: String, resourceName: String): T? {
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    return this.useNamespace(namespace).getRegistry<T>(registryName)?.get(resourceName)
  }

  @Deprecated("Use getHandle() instead")
  operator fun <T> get(resourcePath: String) : T? {
    return try {
      val path = parseResourcePath(resourcePath)
      @Suppress("UNCHECKED_CAST", "DEPRECATION")
      this[path[0], path[1], path[2]] as? T
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()
      null
    }
  }

  fun <T : Resource> getHandle(namespace: String, registryName: String, resourceName: String, type: Class<T>) : ResourceHandle<T>? {
    @Suppress("UNCHECKED_CAST")
    return this.useNamespace(namespace).getRegistry<T>(registryName)?.getHandle(resourceName)
  }

  fun <T : Resource> getHandle(resourcePath: String, type: Class<T>) : ResourceHandle<T>? {
    return try {
      val path = parseResourcePath(resourcePath)
      @Suppress("UNCHECKED_CAST")
      this.getHandle(path[0], path[1], path[2], type)
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()
      null
    }
  }

  inline fun <reified T : Resource> getHandle(resourcePath: String) : ResourceHandle<T>? {
    return getHandle(resourcePath, T::class.java)
  }

  inline fun <reified T : Resource> getHandle(namespace: String, registryName: String, resourceName: String) : ResourceHandle<T>? {
    return getHandle(namespace, registryName, resourceName, T::class.java)
  }

  override fun toString(): String {
    return "ResourceManager(namespaceMap=$namespaceMap)"
  }

  /**
   * Will attempt to parse:
   * - `"namespace:registry/resource"`
   * Into:
   * - `["namespace", "registry", "resource"]`
   * @throws IllegalArgumentException
   */
  fun parseResourcePath(path: String) : Array<String> {
    val result = resourcePathRegex.find(path)
    if (result == null) throw IllegalArgumentException("Path must be of format: 'namespace:registry/resource', was '$path'")
    val namespace = result.groupValues[1]
    val registry = result.groupValues[2]
    val resource = result.groupValues[3]

    return arrayOf(
      namespace,
      registry,
      resource
    )
  }

  override fun clean() {
    namespaceMap.forEach { it.value.clean() }
    namespaceMap.clear()
    System.gc()
  }
}