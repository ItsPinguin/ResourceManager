package fr.ping.fr.ping.utils.resources.registry

import fr.ping.utils.resources.Resource

class RegistryIndex<T : Resource>(
  val index : String,
  val consumer: (T) -> String
) {
}