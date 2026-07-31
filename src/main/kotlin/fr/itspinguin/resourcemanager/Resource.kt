package fr.itspinguin.resourcemanager

import java.io.File

abstract class Resource(
  var id : String = "none",
  var type : String = "none",
  @Transient
  var file : File? = null
) : fr.itspinguin.resourcemanager.Cleanable {

  override fun clean() {

  }
}