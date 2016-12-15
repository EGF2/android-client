package com.eigengraph.egf2.framework.models

class EGF2Edge<T : EGF2Model> {
	var count = 0
	var first: String? = null
	var last: String? = null
	var results: List<T> = emptyList()
}