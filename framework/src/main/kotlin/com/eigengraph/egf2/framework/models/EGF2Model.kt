package com.eigengraph.egf2.framework.models

import com.google.gson.JsonObject
import java.io.Serializable

open class EGF2Model : Serializable {
	companion object {
		const val ME: String = "me"
	}

	open fun getId(): String = ""
	open fun update(): JsonObject = JsonObject()
	open fun create(): JsonObject = JsonObject()
}