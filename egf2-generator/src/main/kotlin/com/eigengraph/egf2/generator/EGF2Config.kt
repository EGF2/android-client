package com.eigengraph.egf2.generator

import java.io.File

interface EGF2Config {
	fun getUrl(): String
	fun getUrlPrefix(): String
	fun getSource(): File
	fun getTargetDirectory(): File
	fun getTargetPackage(): String
	fun getPrefixForModels(): String?
	fun getModelForFile(): String?
	fun getKinds(): Array<String>?
	fun getExcludeModels(): Array<String>
}