package test.rekord

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.Streams
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.goodforgod.gson.configuration.GsonConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.nio.file.Path

class Recorder {
}

const val TESTS_REKORDS_SNAPSHOT = "tests.rekords.snapshot"

const val TESTS_REKORDS_UPDATE_SNAPSHOT = "tests.rekords.updateSnapshot"
val mapper = GsonConfiguration().builder()
    .registerTypeAdapterFactory(GenericTypeAdapterFactory<Any>())
    .setDateFormat("yyyy-MM-dd HH:mm:ss")
    .setPrettyPrinting()
    .serializeNulls()
    .create()
//val mapper = XStream(object : XppDriver() {
//    override fun createWriter(out: Writer): HierarchicalStreamWriter {
//        return CompactWriter(out, nameCoder)
//    }
//}).apply { addPermission(AnyTypePermission.ANY) }
//val mapper = jacksonObjectMapper()
//    .registerModule(JavaTimeModule())
//    .enable(SerializationFeature.INDENT_OUTPUT)
//    .enable(SerializationFeature.WRAP_ROOT_VALUE)
//    .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
////        .allowIfBaseType()
//        .denyForExactBaseType(Collection::class.java)
//        .build(), ObjectMapper.DefaultTyping.EVERYTHING)
////    .activateDefaultTyping(LaissezFaireSubTypeValidator(),ObjectMapper.DefaultTyping.EVERYTHING)
//    .setDateFormat(SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));



class Rekord : BeforeEachCallback, ParameterResolver {
    private lateinit var snapshot: Snapshot

    override fun beforeEach(ctx: ExtensionContext) {
        val isUpdate = isUpdate(ctx.requiredTestMethod)
        val fileName = parseFileName(ctx.requiredTestMethod)
        val filePath = Path.of("src", "test", "resources", "__generated__", fileName)
        snapshot = Snapshot(filePath.toFile(), isUpdate)
    }

    override fun supportsParameter(parameterCtx: ParameterContext, p1: ExtensionContext): Boolean {
        return parameterCtx.parameter.type === Snapshot::class.java
    }

    override fun resolveParameter(parameterCtx: ParameterContext, ctx: ExtensionContext): Any {
        return snapshot
    }

}

inline fun <reified T> T.toMatch(snapshot: Snapshot) {
    snapshot.match(this)
}

data class Snapshot(val file: File, val isUpdate: Boolean) {

    inline fun <reified T> match(any: T) {
        val actual = mapper.toJson(any)
        if (isUpdate) {
            write(actual)
        }
        val expected = file.readText()
        assertEquals(expected, actual)
    }

    fun write(actual: String) {
        file.parentFile.mkdirs()
        file.createNewFile()
        file.writeText(actual)
    }
}

//fun <T> ObjectMapper.toJson(any: T): String = writeValueAsString(any)
//fun <T> XStream.toJson(any: T): String = toXML(any)


//fun isUpdate(method: Method): Boolean {
//    val hasAnnotation = method.annotations
//        .any { it.annotationClass == UpdateSnaphot::class }
//    println("hasAnnotation = ${hasAnnotation}")
//
//    val prop = TESTS_REKORD_UPDATE_SNAPSHOT
//    val hasProp = System.getProperties().containsKey(prop)
//    println("hasProp $prop = ${hasProp}")
//
//    val prop2 = TESTS_REKORD_SNAPSHOT
//    val hasProp2 = System.getProperties().containsKey(prop2)
//    println("hasProp2 $prop = ${hasProp2}")
//    return hasAnnotation || hasProp || hasProp2
//}

//@Throws(IOException::class)
//private fun initGoldenFile() {
//    val file: File = goldenFilePath.toFile()
//    if (file.exists()) {
//        val backup: Path = RecordExtensions.backup(file)
//        println("MVC - Back up file  = $backup")
//    }
//    file.parentFile.mkdirs()
//    file.createNewFile()
//    println("MVC - File created = " + file.toPath())
//}

private const val TYPE = "@type"

class GenericTypeAdapterFactory<T> : TypeAdapterFactory {

    override fun <R> create(gson: Gson, typeToken: TypeToken<R>): TypeAdapter<R>? {
        val rawType = typeToken.rawType
        if (!Any::class.java.isAssignableFrom(rawType)) {
            return null
        }

        val typeAdapterCache = mutableMapOf<Class<*>, TypeAdapter<*>>()

        return object : TypeAdapter<R>() {
            override fun write(out: JsonWriter, value: R) {
                if (value == null) {
                    out.nullValue()
                    return
                }

                val srcType = value.javaClass
                val delegate = typeAdapterCache.computeIfAbsent(srcType) {
                    gson.getDelegateAdapter(this@GenericTypeAdapterFactory, TypeToken.get(it))
                } as TypeAdapter<Any>
                val jsonElement = delegate.toJsonTree(value)

                val clone = JsonObject()
                if (srcType.isArray) {
                    clone.addProperty(TYPE, srcType.componentType.name + "[]")
                    clone.add("value", jsonElement)
                } else {
                    clone.addProperty(TYPE, srcType.name)
                    if (jsonElement.isJsonObject) {
                        val jsonObject = jsonElement.asJsonObject
                        jsonObject.entrySet().forEach { (key, jsonElement) -> clone.add(key, jsonElement) }
                    } else {
                        clone.add("value", jsonElement)
                    }
                }

                Streams.write(clone, out)
            }

            override fun read(reader: JsonReader): R {
                val jsonElement = Streams.parse(reader).asJsonObject
                val type = jsonElement.remove(TYPE).asString
                val subtype = if (type.endsWith("[]")) {
                    Class.forName("[L" + type.removeSuffix("[]") + ";") as Class<out R>
                } else {
                    Class.forName(type) as Class<out R>
                }
                val delegate = typeAdapterCache.computeIfAbsent(subtype) {
                    gson.getDelegateAdapter(this@GenericTypeAdapterFactory, TypeToken.get(it))
                } as TypeAdapter<out R>

                return if (jsonElement.has("value")) {
                    delegate.fromJsonTree(jsonElement.get("value"))
                } else {
                    delegate.fromJsonTree(jsonElement)
                }
            }
        }.nullSafe()
    }
}
