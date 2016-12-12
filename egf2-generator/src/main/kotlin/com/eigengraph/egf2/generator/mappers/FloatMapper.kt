package com.eigengraph.egf2.generator.mappers

import com.eigengraph.egf2.generator.Field
import com.eigengraph.egf2.generator.Mapper
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import java.util.*
import javax.lang.model.element.Modifier

class FloatMapper(targetPackage: String) : Mapper(targetPackage) {
	override fun getField(field: Field, custom_schemas: LinkedHashMap<String, LinkedList<Field>>): FieldSpec {
		val fs: FieldSpec.Builder
		fs = FieldSpec.builder(Float::class.java, field.name, Modifier.PUBLIC)
		//if(it.required) fs.addAnnotation(NotNull::class.java)
		return fs.build()
	}

	override fun deserialize(field: Field, supername: String, deserialize: MethodSpec.Builder, custom_schemas: LinkedHashMap<String, LinkedList<Field>>) {
		if (field.required) {
			deserialize.addStatement("final float \$L = jsonObject.get(\"\$L\").getAsFloat()", field.name, field.name)
			deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
			deserialize.addCode("\n")
		} else {
			deserialize.beginControlFlow("if(jsonObject.has(\"\$L\"))", field.name)
			deserialize.addStatement("final float \$L = jsonObject.get(\"\$L\").getAsFloat()", field.name, field.name)
			deserialize.addStatement("\$L.\$L = \$L", supername, field.name, field.name)
			deserialize.endControlFlow()
			deserialize.addCode("\n")
		}
	}
}