package com.eigengraph.egf2.framework

import java.lang.reflect.Type
import java.util.*

interface IEGF2MapTypesFactory {
	fun create(): HashMap<String, Type>
}