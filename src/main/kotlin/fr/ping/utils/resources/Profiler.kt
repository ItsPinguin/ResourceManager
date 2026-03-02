package fr.ping.utils.resources

object Profiler {
  fun requestStatus(request: StatusRequest): StatusReport {
    when (request.type) {
      StatusRequestType.NAMESPACES -> {
        return StatusReport(
          request.type,
          ResourceManager.listNamespaces(),
          )
      }
      StatusRequestType.REGISTRIES -> {
        return StatusReport(
          request.type,
          registries = listRegistries(request.namespaces, request.registries)
        )
      }
      StatusRequestType.HANDLES -> {
        val namespaces = request.namespaces ?: ResourceManager.listNamespaces()
        val registries = listRegistries(namespaces, request.registries)
        return StatusReport(
          request.type,
          namespaces,
          registries,
          listHandles(namespaces, registries, request.pinned)
        )
      }
    }
  }

  private fun listRegistries(namespaces: List<String>?, registries: List<String>?): List<String> {
    val foundRegistries = mutableListOf<String>()
    var namespaces = namespaces ?: ResourceManager.listNamespaces()
    namespaces.forEach { namespace ->
      registries?.forEach { registry ->
        ResourceManager.useNamespace(namespace).hasRegistry(registry).let {
          if (it) foundRegistries.add("$namespace:$registry")
        }
      } ?: ResourceManager.useNamespace(namespace).listRegistries().forEach { registry ->
        foundRegistries.add("$namespace:$registry")
      }
    }
    return foundRegistries
  }

  private fun listHandles(namespaces: List<String>?, registries: List<String>?, pinned: Boolean?): List<HandleReport> {
    val foundHandles = mutableListOf<HandleReport>()
    (namespaces ?: ResourceManager.listNamespaces()).forEach { namespace ->
      (registries ?: ResourceManager.useNamespace(namespace).listRegistries()).forEach { registry ->
        val registry = registry.replace("$namespace:", "")
        ResourceManager.useNamespace(namespace).useRegistry<Resource>(registry).listResources().forEach {
          val resource = ResourceManager.getHandle<Resource>(namespace, registry, it)
          resource?.release()
          foundHandles.add(HandleReport(
            "$namespace:$registry/$it",
            resource?.usageCount?.get()))
        }
      }
      ResourceManager.useNamespace(namespace)
    }
    return foundHandles
  }
}

data class StatusRequest(
  val type: StatusRequestType,
  val namespaces: List<String>? = null,
  val registries: List<String>? = null,
  val pinned: Boolean? = null,
  val usageCountRange: IntRange? = 0..Int.MAX_VALUE,
)

enum class StatusRequestType {
  NAMESPACES, REGISTRIES, HANDLES
}

data class StatusReport(
  val type: StatusRequestType,
  val namespaces: List<String>? = null,
  val registries: List<String>? = null,
  val handles: List<HandleReport>? = null
)

data class HandleReport(
  val handleName: String = "",
  val usageCount: Int? = 0
)