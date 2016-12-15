package com.eigengraph.egf2.generator.mappers

import com.eigengraph.egf2.generator.Field
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import org.jetbrains.annotations.NotNull
import java.util.*
import javax.lang.model.element.Modifier

class StringMapper(targetPackage: String) : Mapper(targetPackage) {
	override fun getField(field: Field, custom_schemas: LinkedHashMap<String, LinkedList<Field>>): FieldSpec {
		val fs: FieldSpec.Builder
		fs = FieldSpec.builder(String::class.java, field.name, Modifier.PUBLIC)
		var default = field.default
		if (field.enum != null) {
			if (default != null) {
				default = "ENUM_" + field.name.toUpperCase() + "." + field.default?.toUpperCase() + ".name().toLowerCase()"
				fs.initializer("\$L", default)
			}
		} else {
			if (field.default != null) fs.initializer("\$S", field.default)
		}
		if (field.required) fs.addAnnotation(NotNull::class.java)
		return fs.build()
	}

	override fun deserialize(field: Field, supername: String, deserialize: MethodSpec.Builder, custom_schemas: LinkedHashMap<String, LinkedList<Field>>) {
		if (field.required) {
			deserialize.addStatement("final String \$L = jsonObject.get(\"\$L\").getAsString()", field.name, field.name)
			deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
		} else {
			deserialize.beginControlFlow("if(jsonObject.has(\"\$L\"))", field.name)
			deserialize.addStatement("final String \$L = jsonObject.get(\"\$L\").getAsString()", field.name, field.name)
			deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
			deserialize.endControlFlow()
		}
		deserialize.addCode("\n")
	}
}