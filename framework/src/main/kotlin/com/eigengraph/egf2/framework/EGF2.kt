package com.eigengraph.egf2.framework

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.eigengraph.egf2.framework.EGF2Api.ApiTypes.AUTH_API
import com.eigengraph.egf2.framework.EGF2Api.ApiTypes.GRAPH_API
import com.eigengraph.egf2.framework.models.EGF2Edge
import com.eigengraph.egf2.framework.models.EGF2Model
import com.eigengraph.egf2.framework.models.EGF2Search
import com.eigengraph.egf2.framework.models.IEGF2File
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.RequestBody
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.util.*

object EGF2 {

	enum class PAGINATION_MODE {
		INDEX,
		OBJECT
	}

	private lateinit var auth: EGF2AuthApi
	private lateinit var graph: EGF2GraphApi

	@JvmField
	var DEF_COUNT = 25
	@JvmField
	var MAX_COUNT = 50

	var paginationMode: PAGINATION_MODE = PAGINATION_MODE.OBJECT

	fun register(body: RegisterModel): Observable<String> = auth.register(body)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())

	fun login(body: LoginModel): Observable<String> = auth.login(body)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())

	fun verifyEmail(token: String) = auth.verifyEmail(token)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())

	fun forgotPassword(email: String) = auth.forgotPassword(email)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())

	fun resetPassword(body: ResetPasswordModel) = auth.resetPassword(body)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())

	fun changePassword(body: ChangePasswordModel) = auth.changePassword(body)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())

	fun resendEmailVerification() = auth.resendEmailVerification()
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())

	fun logout() = auth.logout()
			.doOnCompleted { clearCache() }
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())

	fun <T : EGF2Model> getSelfUser(normalizeExpand: String? = null, useCache: Boolean = true, clazz: Class<T>) = getObjectByID(EGF2Model.ME, normalizeExpand, useCache, clazz)

	fun <T : EGF2Model> getObjectByID(id: String, normalizeExpand: String? = null, useCache: Boolean = true, clazz: Class<T>): Observable<T> {

		val param = HashMap<String, Any>()
		normalizeExpand?.let { param.put("expand", it) }

		if (useCache) {
			return EGF2Cache.getObjectById<T>(id, normalizeExpand)
					.flatMap {
						if (it == null) {
							graph.getObjectById(id, param, clazz)
						} else {
							Observable.just(it)
						}
					}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
		} else {
			return graph.getObjectById(id, param, clazz)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
		}
	}

	fun <T : EGF2Model> getEdgeObjects(id: String, edge: String, after: EGF2Model?, count: Int, normalizeExpand: String? = null, useCache: Boolean = true): Observable<EGF2Edge<T>> {
		val param = HashMap<String, Any>()
		after?.let {
			when (paginationMode) {
				PAGINATION_MODE.OBJECT -> param.put("after", after.getId())
				PAGINATION_MODE.INDEX -> {
					val index = EGF2Cache.getIndex(id, edge, after.getId())
					index?.let {
						param.put("after", index)
					}
				}
			}
		}
		if (count != DEF_COUNT) param.put("count", count)
		normalizeExpand?.let { param.put("expand", normalizeExpand) }

		if (useCache) {
			return EGF2Cache.getEdgeObjects<T>(id, edge, after, count, normalizeExpand)
					.flatMap {
						if (it == null) {
							graph.getEdgeObjects<T>(id, edge, param)
						} else {
							Observable.just(it)
						}
					}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
		} else {
			return graph.getEdgeObjects<T>(id, edge, param)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
		}
	}

	fun <T : EGF2Model> getEdgeObject(idSrc: String, edge: String, idDst: String, normalizeExpand: String? = null, useCache: Boolean = true, clazz: Class<T>): Observable<T> {
		val param = HashMap<String, Any>()
		normalizeExpand?.let { param.put("expand", it) }

		if (useCache) {
			return EGF2Cache.getEdgeObject<T>(idSrc, edge, idDst, normalizeExpand)
					.flatMap {
						if (it == null) {
							graph.getEdgeObject(idSrc, edge, idDst, param, clazz)
						} else {
							Observable.just(it)
						}
					}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
		} else {
			return graph.getEdgeObject(idSrc, edge, idDst, param, clazz)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
		}
	}

	fun <T : EGF2Model> createObject(body: Any, clazz: Class<T>): Observable<T> =
			graph.createObject(body, clazz)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())

	fun <T : EGF2Model> createObjectOnEdge(id: String, edge: String, body: Any, clazz: Class<T>): Observable<T> =
			graph.createObjectOnEdge(id, edge, body, clazz)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())


	fun createEdge(idSrc: String, edge: String, obj: EGF2Model): Observable<JsonObject> =
			graph.createEdge(idSrc, edge, obj)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())


	fun <T : EGF2Model> updateObject(id: String, body: Any, clazz: Class<T>): Observable<T> =
			graph.updateObject(id, body, clazz)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())


	fun deleteObject(id: String): Observable<JsonObject> =
			graph.deleteObject(id)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())


	fun deleteObjectFromEdge(idSrc: String, edge: String, obj: EGF2Model): Observable<JsonObject> =
			graph.deleteObjectFromEdge(idSrc, edge, obj)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())


	fun <T : EGF2Model> search(q: String, `object`: String, fields: String, filters: String, sort: String, range: String, normalizeExpand: String): Observable<EGF2Search<T>> {
		val param = HashMap<String, Any>()
		if (q.isNotEmpty()) param.put("q", q)
		if (`object`.isNotEmpty()) param.put("object", `object`)
		if (fields.isNotEmpty()) param.put("fields", fields)
		if (filters.isNotEmpty()) param.put("filters", filters)
		if (sort.isNotEmpty()) param.put("sort", sort)
		if (range.isNotEmpty()) param.put("range", range)
		if (normalizeExpand.isNotEmpty()) param.put("expand", normalizeExpand)

		return graph.search<T>(param)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
	}

	@Suppress("IMPLICIT_CAST_TO_ANY")
	fun <T : EGF2Model> uploadFile(file: String, mime: String, title: String, clazz: Class<T>): Observable<T> {
		val param = HashMap<String, Any>()
		param.put("mime_type", mime)
		param.put("title", title)
		return graph.newFile(param)
				.flatMap {
					val f = File(file)
					val requestBody: RequestBody = RequestBody.create(MediaType.parse(mime), f)

					val t: T = Gson().fromJson(it, clazz)

					Observable.combineLatest(
							Observable.just(t),
							graph.uploadFile((t as IEGF2File).getUploadUrl(), mime, requestBody),
							{ file, uploaded ->
								if (uploaded) {
									val jo = JsonObject()
									jo.addProperty("uploaded", true)
									graph.updateObject((file as IEGF2File).getId(), jo, clazz)
								} else {
									//Throwable("File Not Uploaded")
									Observable.error<Any>(Throwable("File Not Uploaded"))
								}
							}).map { t }
				}
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
	}

	@Suppress("IMPLICIT_CAST_TO_ANY")
	fun <T : EGF2Model> uploadImage(file: String, mime: String, title: String, kind: String, clazz: Class<T>): Observable<T> {
		val param = HashMap<String, Any>()
		param.put("mime_type", mime)
		param.put("title", title)
		param.put("kind", kind)

		return graph.newImage(param)
				.flatMap {
					val f = File(file)
					val requestBody: RequestBody = RequestBody.create(MediaType.parse(mime), f)

					val t: T = Gson().fromJson(it, clazz)

					Observable.combineLatest(
							Observable.just(t),
							graph.uploadFile((t as IEGF2File).getUploadUrl(), mime, requestBody),
							{ file, uploaded ->
								if (uploaded) {
									val jo = JsonObject()
									jo.addProperty("uploaded", true)
									graph.updateObject((file as IEGF2File).getId(), jo, clazz)
								} else {
									//Throwable("Image Not Uploaded")
									Observable.error<Any>(Throwable("Image Not Uploaded"))
								}
							}).map { t }
				}
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
	}

	//TODO normalizer
	fun normalizeExpand(vararg expand: String): String {
		return TextUtils.join(",", expand.sorted())
	}

	fun builder(applicationContext: Context): Builder {
		return Builder(applicationContext)
	}

	class Builder(val applicationContext: Context) {

		private var baseUrl: String = ""
		private var preffix: String = ""
		private var gson = Gson()
		private var mode = PAGINATION_MODE.OBJECT
		private var dbName: String? = null
		private var dbKey: ByteArray? = null
		private var version: Long = 0
		private var def_count = MAX_COUNT
		private var max_count = DEF_COUNT
		private var token: String? = null

		fun build() {
			EGF2Api.baseUrl = baseUrl
			EGF2Api.preffix = preffix
			EGF2Api.gson = gson

			paginationMode = mode

			MAX_COUNT = max_count
			DEF_COUNT = def_count

			auth = EGF2Api.getApi<EGF2AuthApi>(AUTH_API)
			graph = EGF2Api.getApi<EGF2GraphApi>(GRAPH_API)

			if (!token.isNullOrEmpty())
				EGF2Api.addHeader("Authorization", "Bearer " + token)

			EGF2Cache.init(applicationContext, dbName, version, dbKey)
		}

		fun url(url: String): Builder {
			baseUrl = url
			return this
		}

		fun usePreffix(preffix: String): Builder {
			this.preffix = preffix
			return this
		}

		fun gson(gson: IEGF2GsonFactory): Builder {
			this.gson = gson.create()
			return this
		}

		fun paginationMode(mode: PAGINATION_MODE): Builder {
			this.mode = mode
			return this
		}

		fun dbName(name: String): Builder {
			if (name.isNotEmpty())
				dbName = name
			return this
		}

		fun dbKey(key: ByteArray): Builder {
			dbKey = key
			return this
		}

		fun version(version: Long): Builder {
			this.version = version
			return this
		}

		fun config(config: IEGF2Config): Builder {
			baseUrl = config.url()
			preffix = config.urlPreffix()
			Log.d("EGF2", config.paginationMode().toUpperCase())
			Log.d("EGF2", PAGINATION_MODE.values().toString())
			mode = PAGINATION_MODE.valueOf(config.paginationMode().toUpperCase())

			def_count = config.defaultCount()
			max_count = config.maxCount()

			return this
		}

		fun token(token: String?): Builder {
			this.token = token
			return this
		}
	}

	fun clearCache() {
		EGF2Cache.clear()
	}

	fun compactCache() {
		EGF2Cache.compact()
	}

	fun isLoggedIn() = EGF2Api.headers.containsKey("Authorization") && EGF2Api.headers["Authorization"]?.isNotEmpty() as Boolean

}
