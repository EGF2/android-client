package com.eigengraph.egf2.framework

import com.google.gson.Gson

interface IEGF2GsonFactory {
	fun create(): Gson
}