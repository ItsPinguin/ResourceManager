package fr.ping.utils.resources

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType

class WrappedResource<T : Resource>(
  private var value : T? = null,
  @Transient
  private var handle: ResourceHandle<T>? = null ) {
  @Transient
  private var namespace: String = ""
  @Transient
  private var registry: String = ""
  @Transient
  private var resource: String = ""

  private var path : String? = null

  fun get() : T? {
    return handle?.resource ?: value
  }

  /**
   * Using this will not change the handle value but release it and set the actual value.
   * If you wish to modify the handle, use setPath() instead.
   * Or if you want to change the handle value, get its resource location and use it.
   */
  fun set(value: T?) {
    handle?.release()
    handle = null
    this.value = value
  }

  fun setPath(namespace: String, registry: String, resource: String) : WrappedResource<T> {
    this.namespace = namespace
    this.registry = registry
    this.resource = resource
    this.path = "$namespace:$registry/$resource"
    handle?.release()
    //@Suppress("UNCHECKED_CAST")
    //handle = ResourceManager.getHandle<T>(namespace, registry, resource, Class.forName(value?.javaClass?.name) as Class<T>)
    return this
  }

  override fun toString(): String {
    return get().toString()
  }

  fun releaseHandle() {
    handle?.release()
  }

  class WrappedResourceAdapterFactory : TypeAdapterFactory {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
      // We only care about WrappedResource<...> types
      if (!WrappedResource::class.java.isAssignableFrom(type.rawType)) {
        return null
      }

      // Get the generic type argument T (e.g., Item, Armor, etc.)
      val genericType = type.type as? ParameterizedType
        ?: throw IllegalStateException("WrappedResource must be parameterized (e.g., WrappedResource<Item>)")

      val resourceType = resolveTypeToClass(genericType.actualTypeArguments[0])
        ?: throw IllegalStateException("Could not determine generic type T for WrappedResource")

      // Cast the adapter to the expected type and return it
      @Suppress("UNCHECKED_CAST")
      return WrappedResourceAdapter(
        gson,
        resourceType as Class<Resource>
      ) as TypeAdapter<T>
    }

    private fun resolveTypeToClass(type: java.lang.reflect.Type): Class<*>? {
      return when (type) {
        is Class<*> -> type
        is ParameterizedType -> type.rawType as? Class<*>
        is java.lang.reflect.WildcardType -> type.upperBounds.firstOrNull() as? Class<*>
        else -> null
      }
    }
  }

  class WrappedResourceAdapter<T : Resource>
    (
    private val gson: Gson,
    private val resourceType: Class<T> // The type T (e.g., Item.class)
  ) : TypeAdapter<WrappedResource<T>>() {

    override fun read(reader: JsonReader): WrappedResource<T> {
      val wrapped = WrappedResource<T>()

      when (reader.peek()) {
        JsonToken.STRING -> {
          val path = reader.nextString()


          if (path.isEmpty()) {
            return wrapped
          }

          //if (!ResourceManager.resourcePathRegex.matches(path)) {
          //  @Suppress("UNCHECKED_CAST")
          //  return WrappedResource<TypeWrapper<String>>(TypeWrapper(path)) as WrappedResource<T>
          //}
//
          //val handle = ResourceManager.getHandle(path, resourceType)
          //handle?.acquire()
//
          //wrapped.handle = handle
          //wrapped.path = path
          //return wrapped
        }
        JsonToken.BEGIN_OBJECT -> {
          val inlineResource = gson.fromJson<T>(reader, resourceType)
          WrappedResource<T>().let { it.set(inlineResource); return it }
        }
        JsonToken.NUMBER -> {
          @Suppress("UNCHECKED_CAST")
          return WrappedResource<TypeWrapper<Double>>(TypeWrapper(reader.nextDouble())) as WrappedResource<T>
        }
        JsonToken.NULL -> {
          reader.nextNull()
        }
        else -> {
          reader.skipValue()
        }
      }
      return wrapped
    }

    override fun write(writer: JsonWriter, value: WrappedResource<T>?) {
      if (value?.handle != null) {
        writer.jsonValue(gson.toJson(value.path))
      } else if (value?.value != null) {
        if (value.value is TypeWrapper<*>) {
          writer.jsonValue(gson.toJson((value.value as TypeWrapper<*>).value))
        } else {
          writer.jsonValue(gson.toJson(value.value))
        }
      } else {
        writer.nullValue()
      }
    }
  }
}