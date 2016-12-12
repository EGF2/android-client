package com.eigengraph.egf2.generator

import java.util.*

class Graph {
	var common_fields = LinkedList<Field>()
	var custom_schemas = LinkedHashMap<String, LinkedList<Field>>()
	var obj = LinkedList<Obj>()
	var pagination = Pagination()
}

class Pagination {
	var default_count = 0
	var max_count = 0
	var pagination_mode = ""
}

class Field {
	var name: String = ""
	var type: String = ""
	var schema: String = ""
	var edit_mode: String? = null
	var default: String? = null
	var required: Boolean = false
	var enum: ArrayList<String>? = null
	var integer: Boolean = false
	var object_types: ArrayList<String>? = null
}

class Edge {
	var name: String = ""
	var contains: List<String> = emptyList()
}

class Obj {
	var name: String = ""
	var code: String = ""
	var fields = LinkedList<Field>()
	var edges = LinkedList<Edge>()
}
