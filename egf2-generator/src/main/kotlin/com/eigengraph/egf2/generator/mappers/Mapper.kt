package com.eigengraph.egf2.generator.mappers

import com.eigengraph.egf2.generator.Field
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import java.util.*

abstract class Mapper(val targetPackage: String) {
	abstract fun deserialize(field: Field, supername: String, deserialize: MethodSpec.Builder, custom_schemas: LinkedHashMap<String, LinkedList<Field>>)
	abstract fun getField(field: Field, custom_schemas: LinkedHashMap<String, LinkedList<Field>>): FieldSpec?
}