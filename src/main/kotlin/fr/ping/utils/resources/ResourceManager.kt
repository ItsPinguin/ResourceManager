package fr.ping.utils.resources

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.ToNumberPolicy
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import fr.ping.fr.ping.utils.resources.LoadingException
import fr.ping.fr.ping.utils.resources.LoadingExceptionType
import fr.ping.fr.ping.utils.resources.registry.Registry
import fr.ping.fr.ping.utils.resources.SchemeException
import fr.ping.fr.ping.utils.resources.SchemeExceptionType
import fr.ping.fr.ping.utils.resources.scheme.ResourceScheme
import fr.ping.fr.ping.utils.resources.scheme.SchemeRegistry
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object ResourceManager : Cleanable {
  val logger = java.util.logging.Logger.getLogger("ResourceManager")
  private val registryMap: MutableMap<String, Registry<*>> = ConcurrentHashMap<String, Registry<*>>().apply { put("scheme", SchemeRegistry) }
  val typeToRegistryMap: MutableMap<Class<out Resource>, String> = mutableMapOf()

  /**
   * This is the list of all loading exceptions that occurred during calls to `loadAllResources`.
   * It might get cleared if you call `loadAllResources` with `crushPreviousErrors` set to `true`.
   */
  val latestErrors = mutableListOf<LoadingException>()
  private val resourcePaths = mutableSetOf<String>()
  private val resourceSchemePaths = mutableSetOf<File>()


  /**
   * This method registers a registry in the ResourceManager.
   *
   * @param registryName the name of the registry
   * @param registry the registry to register
   */
  operator fun set(registryName: String, registry: Registry<*>) {
    registryMap[registryName] = registry
    typeToRegistryMap[registry.type] = registryName
  }

  /**
   * This method returns a registry from the ResourceManager.
   * @param registryName the name of the registry
   */
  fun getRegistry(registryName: String) : Registry<*>? = registryMap[registryName]

  /**
   * This method returns a registry from the ResourceManager. Given a type, it finds the corresponding registry name and uses `getRegistry(String)`.
   * @param type the type of the registry.
   */
  fun getRegistry(type: Class<out Resource>) : Registry<*>? = registryMap[typeToRegistryMap[type]]

  /**
   * This method returns a registry from the ResourceManager. Given a registry name and a type, it finds the corresponding registry and checks if it is of the specified type.
   * @param registryName the name of the registry.
   * @param type the type of the registry.
   * @return `null` if the registry is not found or if it is not of the specified type.
   */
  fun getRegistry(registryName: String, type: Class<out Resource>) : Registry<*>? =
    getRegistry(registryName)?.let { if (it.type == type) it else null }

  /**
   * This method adds a resource path to the ResourceManager.
   * Adding a path will remove all of its subpaths from the list, to avoid duplicate loading.
   * @param path the path to add.
   * @see resourcePaths
   */
  fun addResourcePath(path: String) {
    resourcePaths.removeIf { it.startsWith(path) }
    if (resourcePaths.none { path.startsWith(it) })
      resourcePaths.add(path)
  }

  /**
   * This method adds multiple resource paths to the ResourceManager.
   * @param paths the paths to add.
   * @see addResourcePath
   */
  fun addAllResourcePaths(paths: Collection<String>) = paths.forEach { addResourcePath(it) }

  /**
   * This method returns a list of all resource paths added to the ResourceManager.
   * @see resourcePaths
   */
  fun getResourcePaths() : List<String> = resourcePaths.toList()

  /***
   * The Gson instance used by the ResourceManager.
   * You may add type adapters using `registerTypeAdapter(Class, Any)`.
   */
  private var gson: Gson = GsonBuilder()
    .registerTypeAdapterFactory(WrappedResource.WrappedResourceAdapterFactory())
    .disableHtmlEscaping()
    .setPrettyPrinting()
    .setObjectToNumberStrategy(ToNumberPolicy.DOUBLE)
    .create()

  /**
   * @return the Gson instance used by the ResourceManager.
   * @see gson
   */
  fun getGson() : Gson = gson

  /**
   * This method registers a type adapter for a specific type without messing with the existing Gson instance.
   * @param type the type to register the adapter for.
   */
  fun registerTypeAdapter(type: Class<*>, adapter: Any) {
    gson = gson.newBuilder().registerTypeHierarchyAdapter(type, adapter).create()
  }

  fun registerTypeAdapterFactory(factory: TypeAdapterFactory) {
    gson = gson.newBuilder().registerTypeAdapterFactory(factory).create()
  }

  /**
   * This method finds all resource schemes in the resource paths and loads them if `loadDirectly` is set to true.
   * Scheme loading has to be separate from resource loading, because resource loading might require schemes to be checked, thus loaded.
   * @param loadDirectly whether to load the schemes directly or not.
   * @return a set of all resource scheme files found.
   */
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

  /**
   * This method loads a resource scheme from a file.
   * @param file the file to load the scheme from.
   */
  fun loadSchemeResource(file: File) {
    try {
      val scheme = ResourceScheme.fromFile(file)
      SchemeRegistry.registerResource(scheme.id, scheme)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * This method loads a resource from a string.
   * @param text the string to load the resource from.
   * @param validate whether to validate the resource using a scheme or not.
   * @param file the file the resource was loaded from, if any. This is used for error reporting purposes.
   * @return a LoadingException if the resource could not be loaded, or null if the resource was loaded successfully.
   */
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

  /**
   * This method validates a resource using a scheme. The scheme is determined by the `type` field in the JSON.
   * @param text the JSON string to validate.
   */
  fun validateResource(text: String?): List<SchemeException> {
    val jsonObject = gson.fromJson(text, JsonElement::class.java)?.asJsonObject
      ?: return listOf(SchemeException(SchemeExceptionType.NULL_RESOURCE))
    if (jsonObject.get("type")?.asString == null) return listOf(SchemeException(SchemeExceptionType.NULL_TYPE))
    if (registryMap[jsonObject.get("type")?.asString] == null) return listOf(SchemeException(SchemeExceptionType.NO_REGISTRY))
    val scheme = SchemeRegistry.getResource(jsonObject.get("type")?.asString) ?: return listOf()
    return scheme.getSchemeErrors(jsonObject)
  }

  /**
   * This method loads all resources from all resource paths.
   * @param validate whether to validate the resources using a scheme or not.
   * @param verbose whether to print the loading progress to the console or not.
   * @param crushPreviousErrors whether to clear the previous errors or not. They could come from previous calls to this method. Unless you have a good reason, keep set to true.
   * @return a list of all loading exceptions that occurred during the loading process.
   */
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

  /**
   * This method returns a resource from the ResourceManager.
   * Only use it when the resource has to be used directly and doesn't require updating.
   * If you have multiple registries of the same type, make sure to use `getResource(String, Class, String)`
   * @param id the id of the resource.
   * @param type the type of the resource.
   */
  fun <T : Resource> getResource(id: String, type: Class<T>) : Class<out T>? {
    return registryMap[typeToRegistryMap[type]]?.getResource(id) as Class<out T>?
  }

  /**
   * This method returns a resource from the ResourceManager.
   * Only use it when the resource has to be used directly and doesn't require updating.
   * @param id the id of the resource.
   * @param type the type of the resource.
   * @param registryName the name of the registry to use.
   */
  fun <T : Resource> getResource(id: String, type: Class<T>, registryName: String) : Class<out T>? {
    return registryMap[registryName]?.getResource(id) as Class<out T>?
  }

  /**
   * This method returns a resource handle from the ResourceManager.
   * It is the most recommended way to get a resource from the ResourceManager.
   * If you have multiple registries of the same type, make sure to use `getHandle(String, Class, String)`
   * @param id the id of the resource.
   * @param type the type of the resource.
   */
  fun <T : Resource> getHandle(id: String, type: Class<T>) : ResourceHandle<T>? {
    val registryName = typeToRegistryMap[type]
    val registry = registryMap[registryName ?: throw Exception("No registry for type ${type.simpleName}")]
    return registry?.getResourceHandle(id) as ResourceHandle<T>?
  }

  /**
   * This method returns a resource handle from the ResourceManager.
   * It is the most recommended way to get a resource from the ResourceManager.
   */
  fun <T : Resource> getHandle(id: String, type: Class<T>, registryName: String) : ResourceHandle<T>? {
    val registry = registryMap[registryName]
    if (registry?.type != type) throw Exception("Requested type ${type.simpleName} does not match registry type ${registry?.type?.simpleName} for registry $registryName")
    return registry.getResourceHandle(id) as ResourceHandle<T>?
  }

  /**
   * This method returns a resource handle from the ResourceManager.
   * @see getHandle
   */
  operator fun <T : Resource> get(id: String, type: Class<T>) : ResourceHandle<T>? = getHandle(id, type)

  /**
   * This method returns a resource handle from the ResourceManager.
   * @see getHandle
   */
  operator fun <T : Resource> get(id: String, type: Class<T>, registryName: String) : ResourceHandle<T>? = getHandle(id, type, registryName)

  override fun clean() {
    System.gc()
  }

  inline fun <reified T> parseAny(any: Any?) : T? {
    return getGson().fromJson(
      when (any) {
        is JsonElement -> any
        else -> getGson().toJsonTree(any)
    }, object : TypeToken<T>() {}.type )
  }

  inline fun <reified T> parseJson(element: JsonElement?): T? {
    if (element == null || element.isJsonNull) return null

    return getGson().fromJson<T>(
      element,
      object : TypeToken<T>() {}.type
    )
  }

  fun getRegistryMap() : Map<String, Registry<*>> = registryMap.toMap()
}