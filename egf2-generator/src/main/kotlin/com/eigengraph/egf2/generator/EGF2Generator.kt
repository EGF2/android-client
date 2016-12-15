package com.eigengraph.egf2.generator

import com.eigengraph.egf2.generator.mappers.*
import com.google.gson.GsonBuilder
import com.squareup.javapoet.*
import org.jetbrains.annotations.NotNull
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.lang.model.element.Modifier

object EGF2Generator {

	internal val packageCommons = ".models.commons"
	internal val packageModels = ".models"
	internal val packageDeserializers = ".models.deserializers"
	internal val packageFramework = "com.eigengraph.egf2.framework"

	private var prefixForModels: String = "EGF2"
	private var file: File? = null
	private var modelForFile: String? = null
	private var kinds: Array<String>? = null
	private var mapDeserizers = emptyMap<String, String>()
	private val DESERIALIZER_MAPPERS = ConcurrentHashMap<String, Mapper>()

	var excludeModels: Array<String> = emptyArray()
	var back_end_only = ArrayList<String>()


	fun generate(config: EGF2Config) {

		if (!config.getSource().exists()) {
			throw Exception("Could not get source file, please, set \"source\" in section Graph ")
		}

		if (config.getTargetDirectory().exists() || config.getTargetDirectory().mkdirs()) {
			try {
				DESERIALIZER_MAPPERS.put("string", StringMapper(config.getTargetPackage()))
				DESERIALIZER_MAPPERS.put("date", StringMapper(config.getTargetPackage()))
				DESERIALIZER_MAPPERS.put("boolean", BooleanMapper(config.getTargetPackage()))
				DESERIALIZER_MAPPERS.put("number", FloatMapper(config.getTargetPackage()))
				DESERIALIZER_MAPPERS.put("integer", IntegerMapper(config.getTargetPackage()))
				DESERIALIZER_MAPPERS.put("struct", StructMapper(config.getTargetPackage()))
				DESERIALIZER_MAPPERS.put("object_id", ObjectMapper(config.getTargetPackage()))
				DESERIALIZER_MAPPERS.put("array", ArrayMapper(config.getTargetPackage()))

				modelForFile = config.getModelForFile()
				kinds = config.getKinds()
				excludeModels = config.getExcludeModels()

				val br = BufferedReader(FileReader(config.getSource()))
				val gson = GsonBuilder()
						.registerTypeAdapter(Graph::class.java, GraphDeserializer(excludeModels))
						.registerTypeAdapter(Obj::class.java, ObjDeserializer())
						.setPrettyPrinting()
						.create()

				val graph: Graph = gson.fromJson(br, Graph::class.java)

				val prefix = config.getPrefixForModels()
				prefix?.let { prefixForModels = it }

				file = File(config.getTargetDirectory().absolutePath)

				createCustomSchemas(graph.custom_schemas, config)

				val baseClass = createBaseClass(graph, config)

				graph.obj.forEach {
					createClass(baseClass, config, it, graph.custom_schemas)
				}

				createEGF2ModelDeserializer(config)
				createEGF2GsonFactory(config)
				createEGF2Config(config, graph)
				createEGF2MapClasses(config)

			} catch (e: Exception) {
				throw Exception("EGF2Generator:" + e.message)
			}
		} else {
			throw Exception("Could not create or access target directory " + config.getTargetDirectory().absolutePath)
		}
	}

	private fun createEGF2ModelDeserializer(config: EGF2Config) {
		val jsonDeserializer = ClassName.get("com.google.gson", "JsonDeserializer")
		val jsonParseException = ClassName.get("com.google.gson", "JsonParseException")
		val jsonElement = ClassName.get("com.google.gson", "JsonElement")
		val jsonDeserializationContext = ClassName.get("com.google.gson", "JsonDeserializationContext")
		val jsonObject = ClassName.get("com.google.gson", "JsonObject")
		val nameClass = ClassName.get(packageFramework + packageModels, "EGF2Model")

		val deserialize = MethodSpec.methodBuilder("deserialize").addException(jsonParseException)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override::class.java)
				.addParameter(jsonElement, "json")
				.addParameter(Type::class.java, "typeOfT")
				.addParameter(jsonDeserializationContext, "context")
				.returns(nameClass)
				.addStatement("final \$T jsonObject = json.getAsJsonObject()", jsonObject)
				.beginControlFlow("if(jsonObject.has(\"object_type\"))")
				.beginControlFlow("switch(jsonObject.get(\"object_type\").getAsString())")

		mapDeserizers.forEach {
			deserialize.addCode("case \$L.OBJECT_TYPE:\n",
					ClassName.get(config.getTargetPackage() + EGF2Generator.packageModels, it.key))
			deserialize.addStatement("\treturn new \$L().deserialize(json, typeOfT, context)",
					ClassName.get(config.getTargetPackage() + EGF2Generator.packageDeserializers, it.value))
		}

		deserialize.endControlFlow()
		deserialize.endControlFlow()
		deserialize.addStatement("return new EGF2Model()")

		val clazz = TypeSpec.classBuilder("EGF2ModelDeserializer")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addSuperinterface(ParameterizedTypeName.get(jsonDeserializer, nameClass))
				.addMethod(deserialize.build())
				.build()

		val javaFile = JavaFile.builder(config.getTargetPackage() + packageDeserializers, clazz).build()
		javaFile.writeTo(file)
	}

	private fun createEGF2Config(config: EGF2Config, graph: Graph) {
		val url = MethodSpec.methodBuilder("url")
				.addAnnotation(Override::class.java)
				.addModifiers(Modifier.PUBLIC)
				.returns(String::class.java)
				.addStatement("return \$S", config.getUrl())

		val urlPrefix = MethodSpec.methodBuilder("urlPrefix")
				.addAnnotation(Override::class.java)
				.addModifiers(Modifier.PUBLIC)
				.returns(String::class.java)
				.addStatement("return \$S", config.getUrlPrefix())

		val defaultCount = MethodSpec.methodBuilder("defaultCount")
				.addAnnotation(Override::class.java)
				.addModifiers(Modifier.PUBLIC)
				.returns(Int::class.java)
				.addStatement("return \$L", graph.pagination.default_count)

		val maxCount = MethodSpec.methodBuilder("maxCount")
				.addAnnotation(Override::class.java)
				.addModifiers(Modifier.PUBLIC)
				.returns(Int::class.java)
				.addStatement("return \$L", graph.pagination.max_count)

		val paginationMode = MethodSpec.methodBuilder("paginationMode")
				.addAnnotation(Override::class.java)
				.addModifiers(Modifier.PUBLIC)
				.returns(String::class.java)
				.addStatement("return \$S", graph.pagination.pagination_mode)

		val EGF2Config = TypeSpec.classBuilder(getName("Config"))
				.addSuperinterface(ClassName.get(packageFramework, "IEGF2Config"))
				.addModifiers(Modifier.PUBLIC)
				.addMethod(url.build())
				.addMethod(urlPrefix.build())
				.addMethod(defaultCount.build())
				.addMethod(maxCount.build())
				.addMethod(paginationMode.build())
				.build()

		val javaFile = JavaFile.builder(config.getTargetPackage(), EGF2Config).build()
		javaFile.writeTo(file)
	}

	private fun createEGF2MapClasses(config: EGF2Config) {
		val p = ParameterizedTypeName.get(HashMap::class.java, java.lang.String::class.java, Type::class.java)
		val typeToken = ClassName.get("com.google.gson.reflect", "TypeToken")

		val func = MethodSpec.methodBuilder("create")
				.addAnnotation(Override::class.java)
				.addModifiers(Modifier.PUBLIC)
				.returns(p)

		func.addStatement("\$T map = new \$T()", p, p)

		mapDeserizers.forEach {
			val edge = ClassName.get(packageFramework + packageModels, "EGF2Edge")
			val p2 = ParameterizedTypeName.get(edge, ClassName.get(config.getTargetPackage() + EGF2Generator.packageModels, it.key))
			func.addStatement("map.put(\$T.class.getSimpleName(), new \$T(){}.getType())",
					ClassName.get(config.getTargetPackage() + EGF2Generator.packageModels, it.key),
					ParameterizedTypeName.get(typeToken, p2))
		}
		func.addStatement("return map")

		val EGF2MapTypesFactory = TypeSpec.classBuilder(getName("MapTypesFactory"))
				.addSuperinterface(ClassName.get(packageFramework, "IEGF2MapTypesFactory"))
				.addModifiers(Modifier.PUBLIC)
				.addMethod(func.build())
				.build()

		val javaFile = JavaFile.builder(config.getTargetPackage(), EGF2MapTypesFactory).build()
		javaFile.writeTo(file)
	}

	private fun createEGF2GsonFactory(config: EGF2Config) {
		val gson = ClassName.get("com.google.gson", "Gson")
		val gsonBuilder = ClassName.get("com.google.gson", "GsonBuilder")

		val func = MethodSpec.methodBuilder("create")
				.addAnnotation(Override::class.java)
				.addModifiers(Modifier.PUBLIC)
				.returns(gson)
				.addCode("return new \$L()\n", gsonBuilder)

		func.addCode(".registerTypeAdapter(\$L.class, new \$L())\n",
				ClassName.get(packageFramework + packageModels, "EGF2Model"),
				ClassName.get(config.getTargetPackage() + EGF2Generator.packageDeserializers, "EGF2ModelDeserializer"))

		mapDeserizers.forEach {
			func.addCode(".registerTypeAdapter(\$L.class, new \$L())\n",
					ClassName.get(config.getTargetPackage() + EGF2Generator.packageModels, it.key),
					ClassName.get(config.getTargetPackage() + EGF2Generator.packageDeserializers, it.value))
		}

		func.addStatement(".create()")

		val EGF2GsonFactory = TypeSpec.classBuilder(getName("GsonFactory"))
				.addSuperinterface(ClassName.get(packageFramework, "IEGF2GsonFactory"))
				.addModifiers(Modifier.PUBLIC)
				.addMethod(func.build())
				.build()

		val javaFile = JavaFile.builder(config.getTargetPackage(), EGF2GsonFactory).build()
		javaFile.writeTo(file)
	}

	private fun createCustomSchemas(custom_schemas: LinkedHashMap<String, LinkedList<Field>>, config: EGF2Config) {
		custom_schemas.forEach {
			val fields = ArrayList<FieldSpec>()
			val enums = ArrayList<TypeSpec>()

			it.value.forEach {
				fillEnum(enums, it)
				val split = it.type.split(":")
				DESERIALIZER_MAPPERS[split[0]]?.getField(it, custom_schemas)?.let { fields.add(it) }
			}

			val serializable = ClassName.get("java.io", "Serializable")

			val clazz = TypeSpec.classBuilder(getName(it.key, false))
					.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
					.addSuperinterface(serializable)
					.addFields(fields)
					.addTypes(enums)
					.build()

			val javaFile = JavaFile.builder(config.getTargetPackage() + packageCommons, clazz).build()
			javaFile.writeTo(file)
		}
	}

	private fun fillEnum(enums: ArrayList<TypeSpec>, field: Field) {
		field.enum?.let {
			e ->
			val enum = TypeSpec.enumBuilder("ENUM_" + field.name.toUpperCase()).addModifiers(Modifier.PUBLIC)
			e.forEach {
				enum.addEnumConstant(it.toUpperCase())
			}
			enums.add(enum.build())
		}
	}

	private fun createClass(baseClass: TypeSpec?, config: EGF2Config, it: Obj, custom_schemas: LinkedHashMap<String, LinkedList<Field>>) {
		val fields = ArrayList<FieldSpec>()
		val enums = ArrayList<TypeSpec>()

		it.edges.forEach {
			val jd = java.lang.String.join(", ", it.contains)
			val e = FieldSpec.builder(String::class.java, "EDGE_" + it.name.toUpperCase(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
					.initializer("\$S", it.name)
					.addJavadoc("Objects: \$L", jd)
					.build()
			fields.add(e)
		}

		it.fields.forEach {
			fillEnum(enums, it)
			val split = it.type.split(":")
			DESERIALIZER_MAPPERS[split[0]]?.getField(it, custom_schemas)?.let { fields.add(it) }
		}

		val code = FieldSpec.builder(String::class.java, "CODE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				.initializer("\$S", it.code)
				.build()

		val object_type = FieldSpec.builder(String::class.java, "OBJECT_TYPE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				.initializer("\$S", it.name)
				.build()

		val clazz = TypeSpec.classBuilder(getName(it.name))
				.superclass(TypeVariableName.get(baseClass?.name))
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addField(code)
				.addField(object_type)
				.addFields(fields)
				.addTypes(enums)

		if (it.name == modelForFile) {
			val EGF2File = ClassName.get(packageFramework + packageModels, "IEGF2File")
			clazz.addSuperinterface(EGF2File)

			val getUploadUrl = MethodSpec.methodBuilder("getUploadUrl")
					.addModifiers(Modifier.PUBLIC)
					.addAnnotation(Override::class.java)
					.addAnnotation(NotNull::class.java)
					.returns(String::class.java)
					.addStatement("return upload_url")
			val getId = MethodSpec.methodBuilder("getId")
					.addModifiers(Modifier.PUBLIC)
					.addAnnotation(Override::class.java)
					.addAnnotation(NotNull::class.java)
					.returns(String::class.java)
					.addStatement("return id")

			clazz.addMethod(getUploadUrl.build())
			clazz.addMethod(getId.build())

			kinds?.let {
				val enum = TypeSpec.enumBuilder("ENUM_KINDS").addModifiers(Modifier.PUBLIC)
				it.forEach {
					enum.addEnumConstant(it.toUpperCase())
				}
				clazz.addType(enum.build())
			}
		}

		val jsonObject = ClassName.get("com.google.gson", "JsonObject")
		val gson = ClassName.get("com.google.gson", "Gson")

		val create = MethodSpec.methodBuilder("create")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override::class.java)
				.returns(jsonObject)
				.addStatement("\$T jo = super.create()", jsonObject)


		it.fields.forEach {
			field ->
			field.edit_mode?.let {
				if (field.edit_mode == "E" || field.edit_mode == "NE") {
					when (field.type) {
						"string",
						"date",
						"boolean",
						"number",
						"integer",
						"object_id" -> {
							if (field.required) {
								create.addStatement("jo.addProperty(\"\$L\", \$L)", field.name, field.name)
							} else {
								create.beginControlFlow("if(\$L != null)", field.name)
								create.addStatement("jo.addProperty(\"\$L\", \$L)", field.name, field.name)
								create.endControlFlow()
							}
						}
						"struct" -> {
							if (field.required) {
								create.addStatement("jo.add(\"\$L\", new \$T().toJsonTree(\$L))", field.name, gson, field.name)
							} else {
								create.beginControlFlow("if(\$L != null)", field.name)
								create.addStatement("jo.add(\"\$L\", new \$T().toJsonTree(\$L))", field.name, gson, field.name)
								create.endControlFlow()
							}
						}
						else -> {
						}
					}
				}
			}
		}
		create.addStatement("return jo")

		val update = MethodSpec.methodBuilder("update")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override::class.java)
				.returns(jsonObject)
				.addStatement("\$T jo = super.update()", jsonObject)


		it.fields.forEach {
			field ->
			field.edit_mode?.let {
				if (field.edit_mode == "E" || field.edit_mode == "NC") {
					when (field.type) {
						"string",
						"date",
						"boolean",
						"number",
						"integer",
						"object_id" -> {
							if (field.required) {
								update.addStatement("jo.addProperty(\"\$L\", \$L)", field.name, field.name)
							} else {
								update.beginControlFlow("if(\$L != null)", field.name)
								update.addStatement("jo.addProperty(\"\$L\", \$L)", field.name, field.name)
								update.endControlFlow()
							}
						}
						"struct" -> {
							if (field.required) {
								update.addStatement("jo.add(\"\$L\", new \$T().toJsonTree(\$L))", field.name, gson, field.name)
							} else {
								update.beginControlFlow("if(\$L != null)", field.name)
								update.addStatement("jo.add(\"\$L\", new \$T().toJsonTree(\$L))", field.name, gson, field.name)
								update.endControlFlow()
							}
						}
						else -> {
						}
					}
				}
			}
		}

		update.addStatement("return jo")

		clazz.addMethod(create.build())
		clazz.addMethod(update.build())

		val cl: TypeSpec = clazz.build()

		val javaFile = JavaFile.builder(config.getTargetPackage() + packageModels, cl).build()
		javaFile.writeTo(file)

		createDeserializer(cl, it, config, custom_schemas)
	}

	private fun createDeserializer(clazz: TypeSpec, obj: Obj, config: EGF2Config, custom_schemas: LinkedHashMap<String, LinkedList<Field>>) {
		val jsonDeserializer = ClassName.get("com.google.gson", "JsonDeserializer")
		val jsonParseException = ClassName.get("com.google.gson", "JsonParseException")
		val jsonElement = ClassName.get("com.google.gson", "JsonElement")
		val jsonDeserializationContext = ClassName.get("com.google.gson", "JsonDeserializationContext")
		val jsonObject = ClassName.get("com.google.gson", "JsonObject")
		val nameClass = getName(obj.name)

		val deserialize = MethodSpec.methodBuilder("deserialize").addException(jsonParseException)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override::class.java)
				.addParameter(jsonElement, "json")
				.addParameter(Type::class.java, "typeOfT")
				.addParameter(jsonDeserializationContext, "context")
				.returns(TypeVariableName.get(clazz.name))
				.addStatement("final \$T jsonObject = json.getAsJsonObject()", jsonObject)
				.addStatement("final \$L obj = new \$L()", nameClass, nameClass)
				.addStatement("obj.deserialize(jsonObject)")
				.addCode("\n")

		obj.fields.forEach {
			val split = it.type.split(":")
			DESERIALIZER_MAPPERS[split[0]]?.deserialize(it, "obj", deserialize, custom_schemas)
		}
		val cache = ClassName.get(EGF2Generator.packageFramework, "EGF2Cache")
		obj.edges.forEach {
			if (it.contains.isNotEmpty()) {
				deserialize.beginControlFlow("if(jsonObject.has(\"\$L\"))", it.name)
				if (it.contains.size == 1) {
					if (!back_end_only.contains(it.contains[0]) && !excludeModels.contains(it.contains[0])) {
						val obj = ClassName.get(config.getTargetPackage() + EGF2Generator.packageModels, getName(it.contains[0]))
						val edge = ClassName.get(packageFramework + packageModels, "EGF2Edge")
						val p = ParameterizedTypeName.get(edge, obj)
						val typeToken = ClassName.get("com.google.gson.reflect", "TypeToken")
						val p2 = ParameterizedTypeName.get(typeToken, p)
						deserialize.addStatement("final \$T type = new \$T(){}.getType()", Type::class.java, p2)
						deserialize.addStatement("final \$T \$LEdge = context.deserialize(jsonObject.get(\"\$L\"), type)", p, it.contains[0], it.name)
						deserialize.addStatement("\$L.INSTANCE.addEdge(obj.id, \$S, \$LEdge, null)", cache, it.name, it.contains[0])
					}
				} else {
					val obj = ClassName.get(config.getTargetPackage() + EGF2Generator.packageModels, getName("BaseModel"))
					val edge = ClassName.get(packageFramework + packageModels, "EGF2Edge")
					val p = ParameterizedTypeName.get(edge, obj)

					val list = ClassName.get("java.util", "List")
					val arrayList = ClassName.get("java.util", "ArrayList")
					val p2 = ParameterizedTypeName.get(list, obj)
					val p3 = ParameterizedTypeName.get(arrayList, obj)
					val jsonObject = ClassName.get("com.google.gson", "JsonObject")
					val jsonArray = ClassName.get("com.google.gson", "JsonArray")
					val jsonElement = ClassName.get("com.google.gson", "JsonElement")

					deserialize.addStatement("final \$L result = new \$L()", p2, p3)
					deserialize.addStatement("final \$L edge = new \$L()", p, p)
					deserialize.addStatement("final \$T ot = jsonObject.get(\"object_type\").getAsString()", String::class.java)
					deserialize.addStatement("final \$T jo = jsonObject.getAsJsonObject(\"\$L\")", jsonObject, it.name)
					deserialize.addStatement("final \$T arr = jo.getAsJsonArray(\"results\")", jsonArray)

					deserialize.beginControlFlow("for (\$T element : arr)", jsonElement)
					deserialize.beginControlFlow("switch (ot)")
					it.contains.forEach {
						if (!back_end_only.contains(it) && !excludeModels.contains(it)) {
							deserialize.beginControlFlow("case \"\$L\":", it)
							val clazz = ClassName.get(config.getTargetPackage() + EGF2Generator.packageModels, EGF2Generator.getName(it))
							deserialize.addStatement("final \$T \$LEdge = context.deserialize(element, \$T.class)", clazz, it, clazz)
							deserialize.addStatement("result.add(\$LEdge)", it)
							deserialize.addStatement("break")
							deserialize.endControlFlow()
						}
					}
					deserialize.endControlFlow()
					deserialize.endControlFlow()
					deserialize.addStatement("edge.setCount(jo.get(\"count\").getAsInt())")
					deserialize.beginControlFlow("if(jo.has(\"first\"))")
					deserialize.addStatement("edge.setFirst(jo.get(\"first\").getAsString())")
					deserialize.endControlFlow()
					deserialize.beginControlFlow("if(jo.has(\"last\"))")
					deserialize.addStatement("edge.setLast(jo.get(\"last\").getAsString())")
					deserialize.endControlFlow()
					deserialize.addStatement("edge.setResults(result)")
					deserialize.addStatement("\$L.INSTANCE.addEdge(obj.id, \$S, edge, null)", cache, it.name)

				}
				deserialize.endControlFlow()
			}
		}

		deserialize.addStatement("return obj")

		val d = deserialize.build()

		val clazzName = ClassName.get(config.getTargetPackage() + packageModels, clazz.name)

		mapDeserizers = mapDeserizers.plus(Pair(clazz.name, clazz.name + "Deserializer"))

		val clazz = TypeSpec.classBuilder(clazz.name + "Deserializer")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addSuperinterface(ParameterizedTypeName.get(jsonDeserializer, clazzName))
				.addMethod(d)
				.build()

		val javaFile = JavaFile.builder(config.getTargetPackage() + packageDeserializers, clazz).build()
		javaFile.writeTo(file)
	}

	private fun createBaseClass(graph: Graph, config: EGF2Config): TypeSpec? {
		val jsonObject = ClassName.get("com.google.gson", "JsonObject")
		val jsonParseException = ClassName.get("com.google.gson", "JsonParseException")

		val deserialize = MethodSpec.methodBuilder("deserialize")
				.addException(jsonParseException)
				.addModifiers(Modifier.PUBLIC)
				.addParameter(jsonObject, "jsonObject")
				.returns(Void.TYPE)

		val getId = MethodSpec.methodBuilder("getId")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(NotNull::class.java)
				.addAnnotation(Override::class.java)
				.returns(String::class.java)
				.addStatement("return id")

		val baseFields = ArrayList<FieldSpec>()

		graph.common_fields.forEach {
			val split = it.type.split(":")
			DESERIALIZER_MAPPERS[split[0]]?.getField(it, graph.custom_schemas)?.let { baseFields.add(it) }
			DESERIALIZER_MAPPERS[split[0]]?.deserialize(it, "this", deserialize, graph.custom_schemas)
		}

		val gson = ClassName.get("com.google.gson", "Gson")

		val create = MethodSpec.methodBuilder("create")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override::class.java)
				.returns(jsonObject)
				.addStatement("\$T jo = new \$T()", jsonObject, jsonObject)

		graph.common_fields.forEach {
			field ->
			field.edit_mode?.let {
				if (field.edit_mode == "E" || field.edit_mode == "NE") {
					when (field.type) {
						"string",
						"date",
						"boolean",
						"number",
						"integer",
						"object_id" -> {
							if (field.required) {
								create.addStatement("jo.addProperty(\"\$L\", \$L)", field.name, field.name)
							} else {
								create.beginControlFlow("if(\$L != null)", field.name)
								create.addStatement("jo.addProperty(\"\$L\", \$L)", field.name, field.name)
								create.endControlFlow()
							}
						}
						"struct" -> {
							if (field.required) {
								create.addStatement("jo.add(\"\$L\", new \$T().toJsonTree(\$L))", field.name, gson, field.name)
							} else {
								create.beginControlFlow("if(\$L != null)", field.name)
								create.addStatement("jo.add(\"\$L\", new \$T().toJsonTree(\$L))", field.name, gson, field.name)
								create.endControlFlow()
							}
						}
						else -> {
						}
					}
				}
			}
		}

		create.addStatement("return jo")

		val update = MethodSpec.methodBuilder("update")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override::class.java)
				.returns(jsonObject)
				.addStatement("\$T jo = new \$T()", jsonObject, jsonObject)

		graph.common_fields.forEach {
			field ->
			field.edit_mode?.let {
				if (field.edit_mode == "E" || field.edit_mode == "NC") {
					when (field.type) {
						"string",
						"date",
						"boolean",
						"number",
						"integer",
						"object_id" -> {
							if (field.required) {
								update.addStatement("jo.addProperty(\"\$L\", \$L)", field.name, field.name)
							} else {
								update.beginControlFlow("if(\$L != null)", field.name)
								update.addStatement("jo.addProperty(\"\$L\", \$L)", field.name, field.name)
								update.endControlFlow()
							}
						}
						"struct" -> {
							if (field.required) {
								update.addStatement("jo.add(\"\$L\", new \$T().toJsonTree(\$L))", field.name, gson, field.name)
							} else {
								update.beginControlFlow("if(\$L != null)", field.name)
								update.addStatement("jo.add(\"\$L\", new \$T().toJsonTree(\$L))", field.name, gson, field.name)
								update.endControlFlow()
							}
						}
						else -> {
						}
					}
				}
			}
		}

		update.addStatement("return jo")

		val baseClass = TypeSpec.classBuilder(getName("BaseModel"))
				.superclass(ClassName.get(packageFramework + packageModels, "EGF2Model"))
				.addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
				.addFields(baseFields)
				.addMethod(getId.build())
				.addMethod(deserialize.build())
				.addMethod(create.build())
				.addMethod(update.build())
				.build()

		val javaFile = JavaFile.builder(config.getTargetPackage() + packageModels, baseClass).build()
		javaFile.writeTo(file)
		return baseClass
	}

	internal fun getName(n: String, usePrefix: Boolean = true): String {
		var name = n
		var index = name.indexOf("_")
		while (index >= 0) {
			val char = Character.toUpperCase(name[index + 1])
			name = name.replaceRange(index, index + 2, char.toString())
			index = name.indexOf("_")
		}
		if (usePrefix)
			return prefixForModels + name.capitalize()
		else
			return name.capitalize()
	}
}