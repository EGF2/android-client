package com.eigengraph.egf2.generator

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type
import java.util.*

class GraphDeserializer(val excludeModels: Array<String>) : JsonDeserializer<Graph> {
	override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Graph {
		val result = Graph()
		val jsonObject = json?.asJsonObject as JsonObject
		if (jsonObject.has("graph")) {
			for ((key, value) in jsonObject.get("graph").asJsonObject.entrySet()) {
				if (key == "custom_schemas") {
					for ((k, v) in value.asJsonObject.entrySet()) {
						val list = LinkedList<Field>()
						for ((k2, v2) in v.asJsonObject.entrySet()) {
							val cf: Field? = context?.deserialize(v2, Field::class.java)
							cf?.let {
								it.name = k2
								list.add(it)
							}
						}
						result.custom_schemas.put(k, list)
					}
				} else if (key == "common_fields") {
					for ((k, v) in value.asJsonObject.entrySet()) {
						val cf: Field? = context?.deserialize(v, Field::class.java)
						cf?.let {
							it.name = k
							result.common_fields.add(it)
						}
					}
				} else if (key == "pagination") {
					val p: Pagination? = context?.deserialize(value, Pagination::class.java)
					result.pagination = p as Pagination
				} else if (value.asJsonObject.has("code")) {
					if (!excludeModels.contains(key)) {
						var back_end_only = false
						if (value.asJsonObject.has("back_end_only"))
							back_end_only = value.asJsonObject.get("back_end_only").asBoolean
						if (!back_end_only) {
							val obj: Obj? = context?.deserialize(value, Obj::class.java)
							obj?.let {
								it.name = key
								result.obj.add(it)
							}
						} else {
							EGF2Generator.back_end_only.add(key)
						}
					}
				}
			}
		}
		return result
	}
}