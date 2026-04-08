package fr.ping.fr.ping.utils.resources.scheme

class FieldScheme {
    var type: String? = null
    var required: Boolean = false
    //var default: Any? = null
    var with: MutableList<String>? = null
    var fields: MutableMap<String, FieldScheme>? = null
    var isArray: Boolean = false
    var allowInlined: Boolean = false
    override fun toString(): String {
        return "FieldScheme(type=$type, required=$required, with=$with, fields=$fields, isArray=$isArray)"
    }


}