package fr.ping.utils.resources

import java.io.File

abstract class Resource(
  var id : String = "none",
  var type : String = "none",
  @Transient
  var file : File? = null
) : Cleanable {

  override fun clean() {

  }
}