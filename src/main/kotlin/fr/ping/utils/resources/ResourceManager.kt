package fr.ping.utils.resources

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.ping.fr.ping.utils.resources.LoadingException
import fr.ping.fr.ping.utils.resources.LoadingExceptionType
import fr.ping.fr.ping.utils.resources.Registry
import fr.ping.fr.ping.utils.resources.SchemeException
import fr.ping.fr.ping.utils.resources.SchemeExceptionType
import fr.ping.fr.ping.utils.resources.scheme.ResourceScheme
import fr.ping.fr.ping.utils.resources.scheme.SchemeRegistry
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object ResourceManager : Cleanable {
  private val registryMap: MutableMap<String, Registry<*>> = ConcurrentHashMap<String, Registry<*>>().apply { put("scheme", SchemeRegistry) }
  val typeToRegistryMap: MutableMap<Class<out Resource>, String> = mutableMapOf()
  //val resourcePathRegex = Regex("(^[a-z0-9]+):([a-z0-9]+)/([a-z0-9|/]+[a-zA-Z0-9]+)$")

  val latestErrors = mutableListOf<LoadingException>()
  private val resourcePaths = mutableSetOf<String>()
  private val resourceSchemePaths = mutableSetOf<File>()


  operator fun set(registryName: String, registry: Registry<*>) {
    registryMap[registryName] = registry
    typeToRegistryMap[registry.type] = registryName
  }

  fun getRegistry(registryName: String) : Registry<*>? = registryMap[registryName]

  fun getRegistry(type: Class<out Resource>) : Registry<*>? = registryMap[typeToRegistryMap[type]]

  fun getRegistry(registryName: String, type: Class<out Resource>) : Registry<*>? =
    getRegistry(registryName)?.let { if (it.type == type) it else null }

  fun addResourcePath(path: String) {
    resourcePaths.removeIf { it.startsWith(path) }
    if (resourcePaths.none { path.startsWith(it) })
      resourcePaths.add(path)
  }

  fun addAllResourcePaths(paths: Collection<String>) = paths.forEach { addResourcePath(it) }

  fun getResourcePaths() : List<String> = resourcePaths.toList()

  private var gson: Gson = GsonBuilder()
    .registerTypeAdapterFactory(WrappedResource.WrappedResourceAdapterFactory())
    .disableHtmlEscaping()
    .setPrettyPrinting()
    .create()

  fun getGson() : Gson = gson

  fun registerTypeAdapter(type: Class<*>, adapter: Any) {
    gson = gson.newBuilder().registerTypeAdapter(type, adapter).create()
  }

  fun findSchemeResources(loadDirectly: Boolean = true) : Set<File> {
    resourcePaths.forEach { resourcePath ->
      File(resourcePath).walkTopDown().forEach { resourceFile ->
        if (!resourceFile.isFile || resourceFile.extension != "json") return@forEach
        resourceFile.readText().let {
          if (!(it.contains("\"type\": \"scheme\"")
                || it.contains("\"type\":\"scheme\""))) return@let
          if (loadDirectly)
            loadSchemeResource(resourceFile)
          resourceSchemePaths.add(resourceFile)
        }
      }
    }
    return resourceSchemePaths
  }

  fun loadSchemeResource(file: File) {
    try {
      val scheme = ResourceScheme.fromFile(file)
      SchemeRegistry.registerResource(scheme.id, scheme)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun loadResource(text: String?, validate: Boolean = true, file : File? = null) : LoadingException? {
    if (text == null) throw LoadingException(LoadingExceptionType.NULL_RESOURCE)
    val jsonObject = gson?.fromJson(text, JsonObject::class.java) ?: throw LoadingException(LoadingExceptionType.NULL_RESOURCE)
    val type = jsonObject.get("type")?.asString ?: throw LoadingException(LoadingExceptionType.NULL_TYPE)
    if (type == "scheme") return null
    val registry = registryMap[type] ?: let {
      println("No registry for type $type, available registries: ${registryMap.keys}")
      return LoadingException(LoadingExceptionType.NO_REGISTRY)
    }
    if (validate) {
      val errors = validateResource(text)
      if (errors.isNotEmpty()) return LoadingException(LoadingExceptionType.INVALID_SCHEME, null, errors)
    }
    val id = jsonObject.get("id")?.asString ?: "undefined"
    val resource = registry.loadResource(jsonObject)
    resource?.id = id
    resource?.file = file
    registry.registerResource(id, resource)
    return null
  }

  fun validateResource(text: String?): List<SchemeException> {
    val jsonObject = gson?.fromJson(text, JsonElement::class.java)?.asJsonObject
      ?: return listOf(SchemeException(SchemeExceptionType.NULL_RESOURCE))
    if (jsonObject.get("type")?.asString == null) return listOf(SchemeException(SchemeExceptionType.NULL_TYPE))
    if (registryMap[jsonObject.get("type")?.asString] == null) return listOf(SchemeException(SchemeExceptionType.NO_REGISTRY))
    val scheme = SchemeRegistry.getResource(jsonObject.get("type")?.asString) ?: return listOf()
    return scheme.getSchemeErrors(jsonObject)
  }

  fun loadAllResources(validate: Boolean = true, verbose: Boolean = true, crushPreviousErrors: Boolean = true) : List<LoadingException> {
    if (crushPreviousErrors) latestErrors.clear()
    var resourceCount = 0
    val errors = latestErrors.size
    resourcePaths.forEach { resourcePath ->
      File(resourcePath).walkTopDown().forEach { resourceFile ->
        if (!resourceFile.isFile || resourceFile.extension != "json") return@forEach
        if (resourceSchemePaths.contains(resourceFile)) return@forEach
        try {
          resourceCount++
          loadResource(resourceFile.readText(), validate, resourceFile)?.let {
            latestErrors.add(it.apply { file = resourceFile })
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
        println("Loaded ${resourceFile.name}")
      }
    }
    if (verbose) {
      println("Loaded $resourceCount resources with ${latestErrors.size - errors} problematic resources.")
      latestErrors.forEach { error ->
        println("Error on file '${error.file ?: "unknown"}': ${error.type}")
        error.schemeExceptions?.forEach { schemeException ->
          println("| ${schemeException.type}${schemeException.details.expected
            ?.let { " : expected type '${schemeException.details.expected}' on field '${schemeException.details.field}'" } ?: ""}")
        }
      }
    }
    return latestErrors
  }

  fun <T : Resource> getResource(id: String, type: Class<T>) : Class<out T>? {
    return registryMap[typeToRegistryMap[type]]?.getResource(id) as Class<out T>?
  }

  fun <T : Resource> getHandle(id: String, type: Class<T>) : ResourceHandle<T>? {
    val registryName = typeToRegistryMap[type]
    val registry = registryMap[registryName]
    return registry?.getResourceHandle(id) as ResourceHandle<T>?
  }

  operator fun <T : Resource> get(id: String, type: Class<T>) : ResourceHandle<T>? = getHandle(id, type)

  override fun clean() {
    System.gc()
  }
}