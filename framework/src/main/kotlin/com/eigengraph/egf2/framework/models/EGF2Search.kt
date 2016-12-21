package com.eigengraph.egf2.framework.models

class EGF2Search<T> {
	var count = 0
	var first: String? = null
	var last: String? = null
	var results: List<T> = emptyList()
}