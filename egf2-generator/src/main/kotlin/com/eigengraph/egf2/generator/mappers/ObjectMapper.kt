package com.eigengraph.egf2.generator.mappers

import com.eigengraph.egf2.generator.EGF2Generator
import com.eigengraph.egf2.generator.Field
import com.eigengraph.egf2.generator.Mapper
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import org.jetbrains.annotations.NotNull
import java.util.*
import javax.lang.model.element.Modifier

class ObjectMapper(targetPackage: String) : Mapper(targetPackage) {
	override fun getField(field: Field, custom_schemas: LinkedHashMap<String, LinkedList<Field>>): FieldSpec {
		val fs: FieldSpec.Builder
		val jd = java.lang.String.join(", ", field.object_types)
		fs = FieldSpec.builder(String::class.java, field.name, Modifier.PUBLIC).addJavadoc("Objects: \$L", jd)
		if (field.default != null) fs.initializer("\$L", field.default)
		if (field.required) fs.addAnnotation(NotNull::class.java)
		return fs.build()
	}

	override fun deserialize(field: Field, supername: String, deserialize: MethodSpec.Builder, custom_schemas: LinkedHashMap<String, LinkedList<Field>>) {
		deserialize.addStatement("String \$L = null", field.name)
		deserialize.beginControlFlow("if(jsonObject.has(\"\$L\"))", field.name)
		deserialize.addStatement("JsonElement je = jsonObject.get(\"\$L\")", field.name)
		deserialize.beginControlFlow("if (je.isJsonPrimitive())")
		deserialize.addStatement("\$L = jsonObject.get(\"\$L\").getAsString()", field.name, field.name)
		deserialize.nextControlFlow("else if (je.isJsonObject())")

		val cache = ClassName.get(EGF2Generator.packageFramework, "EGF2Cache")

		if (field.object_types?.size == 1) {
			if (!EGF2Generator.back_end_only.contains(field.object_types?.get(0)) && !EGF2Generator.excludeModels.contains(field.object_types?.get(0))) {
				val clazz = ClassName.get(targetPackage + EGF2Generator.packageModels, EGF2Generator.getName(field.object_types?.get(0) as String))
				deserialize.addStatement("final \$T \$L = context.deserialize(jsonObject.get(\"\$L\"), \$T.class)", clazz, field.name + "Object", field.name, clazz)
				deserialize.addStatement("\$L = \$LObject.id", field.name, field.name)
				deserialize.addStatement("\$L.INSTANCE.addObject(\$L, \$LObject, null)", cache, field.name, field.name)
			}
		} else {
			val jsonObject = ClassName.get("com.google.gson", "JsonObject")
			deserialize.addStatement("final \$T \$LJO = jsonObject.get(\"\$L\").getAsJsonObject()", jsonObject, field.name, field.name)
			deserialize.addStatement("final \$T ot = \$LJO.get(\"object_type\").getAsString()", String::class.java, field.name)
			deserialize.beginControlFlow("switch (ot)")
			field.object_types?.forEach {
				if (!EGF2Generator.back_end_only.contains(it) && !EGF2Generator.excludeModels.contains(it)) {
					deserialize.beginControlFlow("case \"\$L\":", it)

					val clazz = ClassName.get(targetPackage + EGF2Generator.packageModels, EGF2Generator.getName(it))
					deserialize.addStatement("final \$T \$L = context.deserialize(jsonObject.get(\"\$L\"), \$T.class)", clazz, field.name + "Object", field.name, clazz)
					deserialize.addStatement("\$L = \$LObject.id", field.name, field.name)
					deserialize.addStatement("\$L.INSTANCE.addObject(\$L, \$LObject, null)", cache, field.name, field.name)
					deserialize.addStatement("break")
					deserialize.endControlFlow()
				}
			}
			deserialize.endControlFlow()
		}
		deserialize.endControlFlow()
		deserialize.endControlFlow()
		deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
		deserialize.addCode("\n")
	}
}