package fr.ping.utils.resources

import java.io.File

object ResourceIO {
  fun <T : Resource> loadToRegistry(registryFolder: File, file: File, registry: Registry<T>) {
    file.parentFile.mkdirs()
    ResourceManager.gson?.fromJson(file.reader(), registry.type).let {
      val id = file.path.toString().replace(registryFolder.path + "/", "").replace(".json", "")
      val resource = (it as T)
      resource.setId(id)
      registry.registerResource(id, resource)
    }
  }

  fun loadAllToRegistry(registryFolder: File, registry: Registry<*>, effectiveFolder: File = registryFolder) {
    registryFolder.mkdirs()
    effectiveFolder.listFiles()?.forEach {
      if (it.isDirectory) {
        loadAllToRegistry(registryFolder, registry, it)
      } else {
        loadToRegistry(registryFolder, it, registry)
      }
    }
  }

  fun saveToFile(registryFolder: File, handle: ResourceHandle<*>?) {
    if (handle?.resource == null) return
    registryFolder.mkdirs()
    val file = File(registryFolder, handle?.resourceName.toString() + ".json")
    if (!file.exists()) {
      file.parentFile.mkdirs()
      file.createNewFile()
    }
    file.writeText(ResourceManager.gson?.toJson(handle?.resource) ?: "{}")
  }

  fun saveAllToFile(registryFolder: File, registry: Registry<*>) {
    registryFolder.mkdirs()
    @Suppress("DEPRECATION")
    registry.listHandles().forEach {
      saveToFile(registryFolder, it)
    }
  }
}