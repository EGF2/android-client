package com.eigengraph.egf2.framework

import com.eigengraph.egf2.framework.models.EGF2Edge
import com.eigengraph.egf2.framework.models.EGF2Model
import com.eigengraph.egf2.framework.models.EGF2Search
import com.eigengraph.egf2.framework.util.HttpHeaderInterceptor
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.RequestBody
import retrofit2.Response
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import java.lang.reflect.Type
import java.util.*

/*
* create, update object - return object
* create src/edge/dst - return created_at
* create src/edge object - return object
* delete - return deteted_at
* */

internal class EGF2GraphApi : EGF2Api() {
	private var service: EGF2GraphService

	init {
		val restAdapter = getRestAdapterBuilder()
		val restClient = getRestClientBuilder()
		restClient.addInterceptor(HttpHeaderInterceptor(baseUrl.replace("https://", "").replace("http://", "").replace("/", ""), { EGF2Api.getHeagers() }))

		if (EGF2.debugMode)
			restClient.addInterceptor(interceptor)

		restAdapter.baseUrl(baseUrl + prefix)
		restAdapter.client(restClient.build())
		service = restAdapter.build().create(EGF2GraphService::class.java)
	}

	internal fun <T : EGF2Model> getObjectById(id: String, param: HashMap<String, Any>, clazz: Class<T>): Observable<T> {
		val requestSubject = BehaviorSubject.create<T>()

		service.getObject(id, param)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						val json = it.body()?.toString()
						val obj = EGF2Api.gson.fromJson(json, clazz)
						requestSubject.onNext(obj)
						EGF2Cache.addObject(id, obj, param["expand"] as String?)
						EGF2Bus.post(EGF2Bus.EVENT.OBJECT_UPDATED, id, obj)
					} else {
						requestSubject.onError(onError(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun <T : EGF2Model> getEdgeObjects(id: String, edge: String, param: HashMap<String, Any>, clazz: Class<T>): Observable<EGF2Edge<T>> {
		val requestSubject = BehaviorSubject.create<EGF2Edge<T>>()

		service.getEdge(id, edge, param)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						val type: Type? = EGF2.mapClassTypes[clazz.simpleName]
						if (type != null) {
							val json = it.body()?.toString()
							val e: EGF2Edge<T> = EGF2Api.gson.fromJson(json, type)
							requestSubject.onNext(e)
							if (e.results.isNotEmpty()) {
								EGF2Cache.addEdge(id, edge, e, param["expand"] as String?)
							}
							val after = param["after"]
							if (after == null) {
								EGF2Bus.post(EGF2Bus.EVENT.EDGE_PAGE_LOADED, id, edge, e)
							} else {
								EGF2Bus.post(EGF2Bus.EVENT.EDGE_REFRESHED, id, edge, e)
							}
						} else {
							//TODO
							requestSubject.onError(Throwable(clazz.simpleName + " Type Not Found"))
						}
					} else {
						requestSubject.onError(onErrorEdge(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun <T : EGF2Model> getEdgeObject(idSrc: String, edge: String, idDst: String, param: HashMap<String, Any>, clazz: Class<T>): Observable<T> {
		val requestSubject = BehaviorSubject.create<T>()

		service.getEdgeObject(idSrc, edge, idDst, param)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						val json = it.body()?.toString()
						val obj = EGF2Api.gson.fromJson(json, clazz)
						requestSubject.onNext(obj)
						EGF2Cache.addObject(obj.getId(), obj, null)
						EGF2Bus.post(EGF2Bus.EVENT.OBJECT_UPDATED, idDst, obj)
					} else {
						requestSubject.onError(onError(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun <T : EGF2Model> createObject(`object`: Any, clazz: Class<T>): Observable<T> {
		val requestSubject = BehaviorSubject.create<T>()

		service.postObject(`object`)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						val json = it.body()?.toString()
						val obj = EGF2Api.gson.fromJson(json, clazz)
						requestSubject.onNext(obj)
						EGF2Cache.addObject(obj.getId(), obj, null)
						EGF2Bus.post(EGF2Bus.EVENT.OBJECT_CREATED, obj.getId(), obj)
					} else {
						requestSubject.onError(onError(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun createEdge(idSrc: String, edge: String, obj: EGF2Model): Observable<JsonObject> {
		val requestSubject = BehaviorSubject.create<JsonObject>()

		service.postEdge(idSrc, edge, obj.getId())
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						requestSubject.onNext(it.body())
						EGF2Cache.addObjectOnEdge(idSrc, edge, obj)
						EGF2Bus.post(EGF2Bus.EVENT.EDGE_ADDED, idSrc, edge, obj)
					} else {
						requestSubject.onError(onError2(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun <T : EGF2Model> updateObject(id: String, `object`: Any, clazz: Class<T>): Observable<T> {
		val requestSubject = BehaviorSubject.create<T>()

		service.putObject(id, `object`)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						val json = it.body()?.toString()
						val obj = EGF2Api.gson.fromJson(json, clazz)
						requestSubject.onNext(obj)
						EGF2Cache.addObject(obj.getId(), obj, null)
						EGF2Bus.post(EGF2Bus.EVENT.OBJECT_UPDATED, obj.getId(), obj)
					} else {
						requestSubject.onError(onError(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun <T : EGF2Model> createObjectOnEdge(id: String, edge: String, `object`: Any, clazz: Class<T>): Observable<T> {
		val requestSubject = BehaviorSubject.create<T>()

		service.postEdge(id, edge, `object`)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						val json = it.body()?.toString()
						val obj = EGF2Api.gson.fromJson(json, clazz)
						requestSubject.onNext(obj)
						EGF2Cache.addObjectOnEdge(id, edge, obj)
						EGF2Bus.post(EGF2Bus.EVENT.EDGE_ADDED, id, edge, obj)
					} else {
						requestSubject.onError(onError(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun deleteObject(id: String): Observable<JsonObject> {
		val requestSubject = BehaviorSubject.create<JsonObject>()

		service.deleteObject(id)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						requestSubject.onNext(it.body())
						EGF2Cache.deleteObject(id)
						EGF2Bus.post(EGF2Bus.EVENT.OBJECT_DELETED, id, null)
					} else {
						requestSubject.onError(onError2(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun deleteObjectFromEdge(idSrc: String, edge: String, obj: EGF2Model): Observable<JsonObject> {
		val requestSubject = BehaviorSubject.create<JsonObject>()

		service.deleteEdgeObject(idSrc, edge, obj.getId())
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						requestSubject.onNext(it.body())
						EGF2Cache.deleteObjectOnEdge(idSrc, edge, obj.getId())
						EGF2Bus.post(EGF2Bus.EVENT.EDGE_REMOVED, idSrc, edge, obj)
					} else {
						requestSubject.onError(onError2(it))
					}
				}, {
					requestSubject.onError(it)
				})

		return requestSubject.asObservable()
	}

	internal fun <T : EGF2Model> search(param: HashMap<String, Any>): Observable<EGF2Search<T>> =
			service.search(param)
					.flatMap {
						if (it.isSuccessful) {
							val type: Type = object : TypeToken<EGF2Search<T>>() {}.type
							val json = it.body()?.toString()
							val e: EGF2Search<T> = EGF2Api.gson.fromJson(json, type)
							Observable.just(e)
						} else {
							Observable.error(onError2(it))
						}
					}
					.subscribeOn(Schedulers.io())

	internal fun newFile(param: HashMap<String, Any>): Observable<JsonObject> =
			service.newFile(param)
					.flatMap {
						if (it.isSuccessful) {
							Observable.just(it.body())
						} else {
							Observable.error(onError2(it))
						}
					}
					.subscribeOn(Schedulers.io())

	internal fun newImage(param: HashMap<String, Any>): Observable<JsonObject> =
			service.newImage(param)
					.flatMap {
						if (it.isSuccessful) {
							Observable.just(it.body())
						} else {
							Observable.error(onError2(it))
						}
					}
					.subscribeOn(Schedulers.io())

	internal fun uploadFile(url: String, contentType: String, file: RequestBody): Observable<Boolean> =
			service.uploadFile(url, contentType, file)
					.flatMap {
						Observable.just(it.isSuccessful)
					}
					.subscribeOn(Schedulers.io())

	private fun onError2(it: Response<JsonObject>): Throwable =
			if (it.errorBody() != null) {
				val error: String = it.errorBody().string()
				try {
					val je: JsonElement = EGF2Api.gson.fromJson(error, JsonElement::class.java)
					val jo: JsonObject = je.asJsonObject
					if (jo.has("code") && jo.has("message")) {
						Throwable(jo.get("code").asString + ": " + jo.get("message").asString)
					} else {
						Throwable(error)
					}
				} catch (e: Exception) {
					Throwable(error)
				}
			} else {
				Throwable(it.message())
			}

	private fun onError(it: Response<JsonObject>): Throwable? {
		if (it.errorBody() != null) {
			val error: String = it.errorBody().string()
			val je: JsonElement = EGF2Api.gson.fromJson(error, JsonElement::class.java)
			val jo: JsonObject = je.asJsonObject
			if (jo.has("code") && jo.has("message")) {
				return Throwable(jo.get("code").asString + ": " + jo.get("message").asString)
			} else {
				return Throwable(error)
			}
		} else {
			return Throwable(it.message())
		}
	}

	private fun onErrorEdge(it: Response<JsonObject>): Throwable? =
			if (it.errorBody() != null) {
				val error: String = it.errorBody().string()
				val je: JsonElement = EGF2Api.gson.fromJson(error, JsonElement::class.java)
				val jo: JsonObject = je.asJsonObject
				if (jo.has("code") && jo.has("message")) {
					Throwable(jo.get("code").asString + ": " + jo.get("message").asString)
				} else {
					Throwable(error)
				}
			} else {
				Throwable(it.message())
			}
}