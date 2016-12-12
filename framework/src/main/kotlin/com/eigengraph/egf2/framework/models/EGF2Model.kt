package com.eigengraph.egf2.framework.models

import com.google.gson.JsonObject
import java.io.Serializable

abstract class EGF2Model : Serializable {
	companion object {
		const val ME: String = "me"
	}

	abstract fun getId(): String

	abstract fun update(): JsonObject

	abstract fun create(): JsonObject
}