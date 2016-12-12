@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.eigengraph.egf2.generator.mappers

import com.eigengraph.egf2.generator.EGF2Generator
import com.eigengraph.egf2.generator.Field
import com.eigengraph.egf2.generator.Mapper
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import org.jetbrains.annotations.NotNull
import java.lang.reflect.Type
import java.util.*
import javax.lang.model.element.Modifier

class ArrayMapper(targetPackage: String) : Mapper(targetPackage) {

	override fun getField(field: Field, custom_schemas: LinkedHashMap<String, LinkedList<Field>>): FieldSpec? {
		val fs: FieldSpec.Builder
		val split = field.type.split(":")
		val arrayList = ClassName.get("java.util", "ArrayList")
		when (split[1]) {
			"object_id" -> {
				fs = FieldSpec.builder(ParameterizedTypeName.get(ArrayList::class.java, String::class.java), field.name, Modifier.PUBLIC).addJavadoc("\$L", "List " + EGF2Generator.getName(split[1]) + ".id")
				if (field.default != null) fs.initializer("\$L", field.default)
				if (field.required) fs.addAnnotation(NotNull::class.java)
				return fs.build()
			}
			"struct" -> {
				if (custom_schemas.containsKey(field.schema)) {
					val schema = EGF2Generator.getName(field.schema, false)
					val struct = ClassName.get(targetPackage + EGF2Generator.packageCommons, schema)
					val arrayList = ClassName.get("java.util", "ArrayList")
					fs = FieldSpec.builder(ParameterizedTypeName.get(arrayList, struct), field.name, Modifier.PUBLIC)
					if (field.default != null) fs.initializer("\$L", field.default)
					if (field.required) fs.addAnnotation(NotNull::class.java)
					return fs.build()
				} else return null
			}
			"string", "date" -> {
				fs = FieldSpec.builder(ParameterizedTypeName.get(ArrayList::class.java, java.lang.String::class.java), field.name, Modifier.PUBLIC)
				if (field.default != null) fs.initializer("\$L", field.default)
				if (field.required) fs.addAnnotation(NotNull::class.java)
				return fs.build()
			}
			"boolean" -> {
				fs = FieldSpec.builder(ParameterizedTypeName.get(ArrayList::class.java, java.lang.Boolean::class.java), field.name, Modifier.PUBLIC)
				if (field.default != null) fs.initializer("\$L", field.default)
				if (field.required) fs.addAnnotation(NotNull::class.java)
				return fs.build()
			}
			"number" -> {
				fs = FieldSpec.builder(ParameterizedTypeName.get(arrayList, ClassName.get("java.lang", "Float")), field.name, Modifier.PUBLIC)
				if (field.default != null) fs.initializer("\$L", field.default)
				if (field.required) fs.addAnnotation(NotNull::class.java)
				return fs.build()
			}
			"integer" -> {
				fs = FieldSpec.builder(ParameterizedTypeName.get(ArrayList::class.java, java.lang.Integer::class.java), field.name, Modifier.PUBLIC)
				if (field.default != null) fs.initializer("\$L", field.default)
				if (field.required) fs.addAnnotation(NotNull::class.java)
				return fs.build()
			}
			else -> {
				return null
			}
		}
	}

	override fun deserialize(field: Field, supername: String, deserialize: MethodSpec.Builder, custom_schemas: LinkedHashMap<String, LinkedList<Field>>) {
		val split = field.type.split(":")
		when (split[1]) {
			"string", "date" -> {
				val arrayList = ClassName.get("java.util", "ArrayList")
				val p = ParameterizedTypeName.get(arrayList, ClassName.get(java.lang.String::class.java))
				deserializeType(deserialize, field, p, supername)
			}
			"boolean" -> {
				val arrayList = ClassName.get("java.util", "ArrayList")
				val b = ClassName.get("java.lang", "Boolean")
				val p = ParameterizedTypeName.get(arrayList, b)
				deserializeType(deserialize, field, p, supername)
			}
			"number" -> {
				val arrayList = ClassName.get("java.util", "ArrayList")
				val f = ClassName.get("java.lang", "Float")
				val p = ParameterizedTypeName.get(arrayList, f)
				deserializeType(deserialize, field, p, supername)
			}
			"integer" -> {
				val arrayList = ClassName.get("java.util", "ArrayList")
				val p = ParameterizedTypeName.get(arrayList, ClassName.get(java.lang.Integer::class.java))
				deserializeType(deserialize, field, p, supername)
			}
			"object_id" -> {
				val arrayList = ClassName.get("java.util", "ArrayList")
				val cache = ClassName.get(EGF2Generator.packageFramework, "EGF2Cache")

				if (field.object_types?.size == 1) {
					if (!EGF2Generator.back_end_only.contains(field.object_types?.get(0))
							&& !EGF2Generator.excludeModels.contains(field.object_types?.get(0))) {
						val clazz = ClassName.get(targetPackage + EGF2Generator.packageModels, EGF2Generator.getName(field.object_types?.get(0) as String))

						val p = ParameterizedTypeName.get(arrayList, clazz)
						val typeToken = ClassName.get("com.google.gson.reflect", "TypeToken")
						val p2 = ParameterizedTypeName.get(typeToken, p)

						val p3 = ParameterizedTypeName.get(arrayList, ClassName.get("java.lang", "String"))

						deserialize.addStatement("final \$T \$LArray = new \$T()", p3, field.name, p3)

						deserialize.addStatement("final \$T \$LType = new \$T(){}.getType()", Type::class.java, field.name, p2)
						deserialize.addStatement("final \$T \$LObject = context.deserialize(jsonObject.get(\"\$L\"), \$LType)", p, field.name, field.name, field.name)

						deserialize.beginControlFlow("for(\$T o : \$LObject)", clazz, field.name)
						deserialize.addStatement("\$LArray.add(o.id)", field.name)
						deserialize.addStatement("\$L.INSTANCE.addObject(o.id, o, null)", cache)
						deserialize.endControlFlow()
						deserialize.addStatement("\$L.\$L = \$LArray", supername, field.name, field.name)
					}
				} else {
					val jsonObject = ClassName.get("com.google.gson", "JsonObject")
					val jsonElement = ClassName.get("com.google.gson", "JsonElement")

					val p3 = ParameterizedTypeName.get(arrayList, ClassName.get("java.lang", "String"))
					deserialize.addStatement("final \$T \$LArray = new \$T()", p3, field.name, p3)

					deserialize.beginControlFlow("for(\$T je : jsonObject.get(\"\$L\").getAsJsonArray())", jsonElement, field.name)
					deserialize.beginControlFlow("if(je.isJsonObject())")
					deserialize.addStatement("\$T jo = je.getAsJsonObject()", jsonObject)
					deserialize.beginControlFlow("if(jo.has(\"object_type\"))")
					deserialize.beginControlFlow("switch(jo.get(\"object_type\").getAsString())")
					field.object_types?.forEach {
						if (!EGF2Generator.back_end_only.contains(it) && !EGF2Generator.excludeModels.contains(it)) {
							deserialize.beginControlFlow("case \"\$L\":", it)

							val clazz = ClassName.get(targetPackage + EGF2Generator.packageModels, EGF2Generator.getName(it))
							deserialize.addStatement("final \$T \$LObject = context.deserialize(jsonObject.get(\"\$L\"), \$T.class)", clazz, field.name, field.name, clazz)
							deserialize.addStatement("\$LArray.add(\$LObject.id)", field.name, field.name)
							deserialize.addStatement("\$L.INSTANCE.addObject(\$LObject.id, \$LObject, null)", cache, field.name, field.name)
							deserialize.addStatement("break")
							deserialize.endControlFlow()
						}
					}
					deserialize.endControlFlow()
					deserialize.endControlFlow()
					deserialize.endControlFlow()
					deserialize.endControlFlow()
					deserialize.addStatement("\$L.\$L = \$LArray", supername, field.name, field.name)
				}
				deserialize.addCode("\n")
			}
			"struct" -> {
				if (custom_schemas.containsKey(field.schema)) {
					val schema = EGF2Generator.getName(field.schema, false)
					val struct = ClassName.get(targetPackage + EGF2Generator.packageCommons, schema)
					val arrayList = ClassName.get("java.util", "ArrayList")
					val p = ParameterizedTypeName.get(arrayList, struct)
					deserializeType(deserialize, field, p, supername)
				}
			}
		}
	}

	private fun deserializeType(deserialize: MethodSpec.Builder, field: Field, p: ParameterizedTypeName?, supername: String) {
		val typeToken = ClassName.get("com.google.gson.reflect", "TypeToken")
		val p2 = ParameterizedTypeName.get(typeToken, p)
		if (field.required) {
			deserialize.addStatement("final \$T type = new \$T(){}.getType()", Type::class.java, p2)
			deserialize.addStatement("final \$T \$L = context.deserialize(jsonObject.get(\"\$L\"), type)", p, field.name, field.name)
			deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
			deserialize.addCode("\n")
		} else {
			deserialize.beginControlFlow("if(jsonObject.has(\"\$L\"))", field.name)
			deserialize.addStatement("final \$T type = new \$T(){}.getType()", Type::class.java, p2)
			deserialize.addStatement("final \$T \$L = context.deserialize(jsonObject.get(\"\$L\"), type)", p, field.name, field.name)
			deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
			deserialize.endControlFlow()
			deserialize.addCode("\n")
		}
	}
}