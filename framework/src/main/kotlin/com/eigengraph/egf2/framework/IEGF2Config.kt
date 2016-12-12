package com.eigengraph.egf2.framework

interface IEGF2Config {
	fun url(): String
	fun urlPreffix(): String
	fun defaultCount(): Int
	fun maxCount(): Int
	fun paginationMode(): String
}