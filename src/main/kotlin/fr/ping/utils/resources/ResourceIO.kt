package fr.ping.utils.resources

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File

object ResourceIO {
  fun <T : Resource> loadToRegistry(registryFolder: File, file: File, registry: Registry<T>) {
    file.parentFile.mkdirs()
    val jsonElement = ResourceManager.gson?.fromJson(file.reader(), JsonObject::class.java) ?: JsonObject()
    registry.resourceScheme?.let {
      println("Checking scheme for file ${file.path}")
      val passed = try {
        it.isSchemeValid(jsonElement)
      } catch (e: Exception) {
        println("Invalid scheme loading from file ${file.path}")
        false
      }
      println("Scheme passed: $passed")
      if (!passed) return
    }
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

  fun loadResource(file : File) {
    val jsonElement = ResourceManager.gson?.fromJson(file.reader(), JsonElement::class.java) ?: JsonObject()
    val type = jsonElement.asJsonObject.get("type").asString
  }
}