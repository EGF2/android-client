package com.eigengraph.egf2.framework

interface IEGF2Config {
	fun url(): String
	fun urlPrefix(): String
	fun defaultCount(): Int
	fun maxCount(): Int
	fun paginationMode(): String
}