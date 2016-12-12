package com.eigengraph.egf2.generator

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class ObjDeserializer : JsonDeserializer<Obj> {
	override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Obj {
		val result = Obj()
		val jsonObject = json?.asJsonObject as JsonObject
		result.code = jsonObject.get("code").asString

		if (jsonObject.has("fields")) {
			for ((key, value) in jsonObject.get("fields").asJsonObject.entrySet()) {
				val f: Field? = context?.deserialize(value, Field::class.java)
				f?.let {
					it.name = key
					result.fields.add(it)
				}
			}
		}

		if (jsonObject.has("edges")) {
			for ((key, value) in jsonObject.get("edges").asJsonObject.entrySet()) {
				val e: Edge? = context?.deserialize(value, Edge::class.java)
				e?.let {
					it.name = key
					result.edges.add(it)
				}
			}
		}

		return result
	}
}