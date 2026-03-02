package fr.ping.utils.resources

class TypeWrapper<T> (
  var value: T
) : Resource {
  override fun clean() {}

  override fun toString(): String {
    return "$value"
  }

  override fun getId(): String {
    return ""
  }

  override fun setId(id: String) {
  }
}