package fr.itspinguin.resourcemanager

class TypeWrapper<T> (
  var value: T
) : Resource() {
  override fun clean() {}

  override fun toString(): String {
    return "$value"
  }
}