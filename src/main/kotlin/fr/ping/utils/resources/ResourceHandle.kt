package fr.ping.utils.resources

import jdk.vm.ci.meta.TriState
import java.util.concurrent.atomic.AtomicInteger

class ResourceHandle<T : Resource> (
  var parentRegistry: Registry<T>? = null,
  val resourceName: String? = null
) {
  var resource: T? = null
  var usageCount = AtomicInteger(0)
  var isPinned = false
  var isSchemeValid = TriState.UNKNOWN

  constructor(resource: T? = null, parentRegistry: Registry<T>? = null, resourceName: String) : this(parentRegistry, resourceName) {
    this.resource = resource
  }

  fun acquire() : ResourceHandle<T> {
    usageCount.incrementAndGet()
    return this
  }

  fun release() {
    usageCount.decrementAndGet()
    if (usageCount.get() == 0 && !isPinned) {
      parentRegistry?.notifyUnused(this)
    }
  }

  fun getResourcePath() : String {
    return "${parentRegistry?.parentNamespace?.namespaceName}:${parentRegistry?.registryName}/$resourceName"
  }

  override fun toString(): String {
    return "ResourceHandle(resourceName=$resourceName, resource=$resource, usageCount=$usageCount, isPinned=$isPinned)"
  }
}