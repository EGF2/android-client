package com.eigengraph.egf2.generator.mappers

import com.eigengraph.egf2.generator.EGF2Generator
import com.eigengraph.egf2.generator.Field
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import org.jetbrains.annotations.NotNull
import java.util.*
import javax.lang.model.element.Modifier

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class StructMapper(targetPackage: String) : Mapper(targetPackage) {

	override fun getField(field: Field, custom_schemas: LinkedHashMap<String, LinkedList<Field>>): FieldSpec? {
		val fs: FieldSpec.Builder
		if (custom_schemas.containsKey(field.schema)) {
			val schema = EGF2Generator.getName(field.schema, false)
			val struct = ClassName.get(targetPackage + EGF2Generator.packageCommons, schema)
			fs = FieldSpec.builder(struct, field.name, Modifier.PUBLIC)
			if (field.default != null) fs.initializer("\$L", field.default)
			if (field.required) fs.addAnnotation(NotNull::class.java)
			return fs.build()
		} else if (field.schema == "any") {
			fs = FieldSpec.builder(Object::class.java, field.name, Modifier.PUBLIC)
			if (field.default != null) fs.initializer("\$L", field.default)
			if (field.required) fs.addAnnotation(NotNull::class.java)
			return fs.build()
		}
		return null
	}

	override fun deserialize(field: Field, supername: String, deserialize: MethodSpec.Builder, custom_schemas: LinkedHashMap<String, LinkedList<Field>>) {
		if (custom_schemas.containsKey(field.schema)) {
			val schema = EGF2Generator.getName(field.schema, false)
			val struct = ClassName.get(targetPackage + EGF2Generator.packageCommons, schema)
			if (field.required) {
				deserialize.addStatement("final \$T \$L = context.deserialize(jsonObject.get(\"\$L\"), \$T.class)", struct, field.name, field.name, struct)
				deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
			} else {
				deserialize.beginControlFlow("if(jsonObject.has(\"\$L\"))", field.name)
				deserialize.addStatement("final \$T \$L = context.deserialize(jsonObject.get(\"\$L\"), \$T.class)", struct, field.name, field.name, struct)
				deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
				deserialize.endControlFlow()
			}
		} else if (field.schema == "any") {
			if (field.required) {
				deserialize.addStatement("final Object \$L = context.deserialize(jsonObject.get(\"\$L\"), Object.class)", field.name, field.name)
				deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
			} else {
				deserialize.beginControlFlow("if(jsonObject.has(\"\$L\"))", field.name)
				deserialize.addStatement("final Object \$L = context.deserialize(jsonObject.get(\"\$L\"), Object.class)", field.name, field.name)
				deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
				deserialize.endControlFlow()
			}
		}
		deserialize.addCode("\n")
	}
}