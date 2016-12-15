@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.eigengraph.egf2.generator.mappers

import com.eigengraph.egf2.generator.Field
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import org.jetbrains.annotations.NotNull
import java.util.*
import javax.lang.model.element.Modifier

class BooleanMapper(targetPackage: String) : Mapper(targetPackage) {

	override fun getField(field: Field, custom_schemas: LinkedHashMap<String, LinkedList<Field>>): FieldSpec {
		val fs: FieldSpec.Builder
		fs = FieldSpec.builder(java.lang.Boolean::class.java, field.name, Modifier.PUBLIC)
		if (field.required) {
			fs.addAnnotation(NotNull::class.java)
			fs.initializer("\$L", field.default)
		} else if (!field.required && field.default != null) {
			fs.initializer("\$L", field.default)
		}
		return fs.build()
	}

	override fun deserialize(field: Field, supername: String, deserialize: MethodSpec.Builder, custom_schemas: LinkedHashMap<String, LinkedList<Field>>) {
		if (field.required) {
			deserialize.addStatement("final Boolean \$L = jsonObject.get(\"\$L\").getAsBoolean()", field.name, field.name)
			deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
		} else {
			deserialize.beginControlFlow("if(jsonObject.has(\"\$L\"))", field.name)
			deserialize.addStatement("final Boolean \$L = jsonObject.get(\"\$L\").getAsBoolean()", field.name, field.name)
			deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
			deserialize.endControlFlow()
		}
		deserialize.addCode("\n")
	}
}