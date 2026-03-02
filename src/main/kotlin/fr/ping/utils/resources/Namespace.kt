package fr.ping.utils.resources

import java.util.concurrent.ConcurrentHashMap

class Namespace(val namespaceName: String) : Cleanable {
  private val registryMap: MutableMap<String, Registry<*>> = ConcurrentHashMap()

  inline fun <reified T : Resource> useRegistry(registryName: String) : Registry<T> = useRegistry(registryName, T::class.java)

  fun <T : Resource> useRegistry(registryName: String, type: Class<T>) : Registry<T> {
    return getRegistry<T>(registryName) ?: createRegistry<T>(type, registryName)
  }

  fun dropRegistry(registryName: String) {
    registryMap.remove(registryName)?.clean()
  }

  fun hasRegistry(registryName: String) : Boolean = registryMap.containsKey(registryName)

  fun listRegistries() : List<String> = registryMap.keys.toList()

  inline fun <reified T : Resource> createRegistry(registryName: String): Registry<T> = createRegistry(T::class.java, registryName)

  fun <T : Resource> createRegistry(type: Class<T>, registryName: String): Registry<T> {
    if (registryMap.containsKey(registryName)) {
      throw IllegalArgumentException("Registry '$registryName' already exists!")
    }
    registryMap[registryName] = Registry<T>(type, registryName)
    registryMap[registryName]!!.parentNamespace = this
    @Suppress("UNCHECKED_CAST")
    return registryMap[registryName]!! as Registry<T>
  }

  fun <T : Resource> getRegistry(registryName: String): Registry<T>? {
    @Suppress("UNCHECKED_CAST")
    return registryMap[registryName] as Registry<T>?
  }

  override fun toString(): String {
    return "Namespace(namespaceName='$namespaceName', registryMap=$registryMap)"
  }

  override fun clean() {
    registryMap.forEach { it.value.clean() }
    registryMap.clear()
  }
}