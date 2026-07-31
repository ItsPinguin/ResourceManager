package fr.itspinguin.resourcemanager

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType

class WrappedResource<T : Resource>(
  var path : String? = null,
  var value : T? = null,
  private var handle: ResourceHandle<T>? = null
) {
  fun get() : T? {
    return handle?.resource ?: value
  }

  override fun toString(): String {
    return get().toString()
  }

  fun acquireHandle() {
    handle?.acquire()
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

  class WrappedResourceAdapter<T : Resource>(
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
          wrapped.path = path
          wrapped.handle = ResourceManager.getHandle(path, resourceType)
          return wrapped
        }
        JsonToken.BEGIN_OBJECT -> {
          val inlineResource = gson.fromJson<T>(reader, resourceType)
          WrappedResource<T>().let { it.value = inlineResource; return it }
        }
        JsonToken.NUMBER -> {
          @Suppress("UNCHECKED_CAST")
          return WrappedResource(value = TypeWrapper(reader.nextDouble())) as WrappedResource<T>
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