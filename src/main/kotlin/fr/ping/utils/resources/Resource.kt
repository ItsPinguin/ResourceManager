package fr.ping.utils.resources

interface Resource : Cleanable {
  fun getId(): String
  fun setId(id: String)
}