package com.eigengraph.egf2.framework

import android.content.Context
import com.eigengraph.egf2.framework.models.EGF2Edge
import com.eigengraph.egf2.framework.models.EGF2Model
import com.eigengraph.egf2.framework.realm.cacheRealm
import com.eigengraph.egf2.framework.realm.edgeCountRealm
import com.eigengraph.egf2.framework.realm.edgeRealm
import com.eigengraph.egf2.framework.realm.objectRealm
import com.eigengraph.egf2.framework.util.convertFromBytes
import com.eigengraph.egf2.framework.util.convertToBytes
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.Sort
import rx.Observable
import java.util.*

@Suppress("UNCHECKED_CAST")
object EGF2Cache {

	private var configDB: RealmConfiguration? = null

	fun init(appContext: Context, name: String? = null, version: Long = 0, key: ByteArray? = null) {
		Realm.init(appContext)
		val config = RealmConfiguration.Builder()
		name?.let { config.name(name) }
		key?.let { config.encryptionKey(key) }
		config.schemaVersion(version)

		config.deleteRealmIfMigrationNeeded()

		configDB = config.build()
	}

	// object_id | normalize(expand)
	// object_id/edge | normalize(expand) | count | after

	fun <T : EGF2Model> getObjectById(id: String, normalizeExpand: String?): Observable<T?> {
		return Observable.create<T?> {
			val realm = Realm.getInstance(configDB)
			try {
				if (normalizeExpand == null) {
					val obj = realm.where(objectRealm::class.java).equalTo("id", id).findFirst()
					if (obj != null && obj.body != null) {
						val t = convertFromBytes(obj.body as ByteArray) as T
						it.onNext(t)
					} else {
						//it.onError(Throwable("Object Not Found"))
						it.onNext(null)
					}
				} else {
					//TODO fix get cache with filter by normalizeExpand
					val cache = realm.where(cacheRealm::class.java).equalTo("id", id + "/" + normalizeExpand).findFirst()
					if (cache == null) {
						it.onNext(null)
					} else {
						val obj = realm.where(objectRealm::class.java).equalTo("id", id).findFirst()
						if (obj != null && obj.body != null) {
							val t = convertFromBytes(obj.body as ByteArray) as T
							it.onNext(t)
						} else {
							//it.onError(Throwable("Object Not Found"))
							it.onNext(null)
						}
					}
				}
			} catch (e: Exception) {
				it.onError(e)
			} finally {
				realm.close()
				it.onCompleted()
			}
		}
	}

	fun <T : EGF2Model> getEdgeObjects(id: String, edge: String, after: EGF2Model?, count: Int, normalizeExpand: String?): Observable<EGF2Edge<T>?> {
		return Observable.create {

			val realm = Realm.getInstance(configDB)
			try {
				//TODO fix get cache with filter by normalizeExpand
				var i = id + "/" + edge
				normalizeExpand?.let { i += "/" + it }
				val cache = realm.where(cacheRealm::class.java).equalTo("id", i).findFirst()

				if (cache == null) {
					it.onNext(null)
				} else {

					val listId = ArrayList<String>()
					var flag = false
					var idx = 0
					val ed = realm.where(edgeRealm::class.java).equalTo("id_src", id).equalTo("edge", edge).findAllSorted("index", Sort.ASCENDING)
					ed.forEach {
						when (EGF2.paginationMode) {
							EGF2.PAGINATION_MODE.INDEX,
							EGF2.PAGINATION_MODE.OBJECT -> {
								if (after == null) flag = true
								if (flag && idx < count) {
									listId.add(it.id_dst)
									idx++
								}
								if (after != null && it.id_dst == after.getId()) flag = true
							}
						}
					}

					val edgeCount = realm.where(edgeCountRealm::class.java).equalTo("id", id + "/" + edge).findFirst().count

					if (listId.size != count && ed.size < edgeCount) {
						it.onNext(null)
					} else {

						val listObj = ArrayList<T>()
						listId.forEach {
							val r = realm.where(objectRealm::class.java).equalTo("id", it).findFirst()
							val obj = convertFromBytes(r.body as ByteArray) as T
							listObj.add(obj)
						}

						val edt = EGF2Edge<T>()
						edt.results = listObj
						edt.count = edgeCount

						when (EGF2.paginationMode) {
							EGF2.PAGINATION_MODE.INDEX -> {
								edt.last = (edt.results.size - 1).toString()
							}
							EGF2.PAGINATION_MODE.OBJECT -> {
								edt.last = edt.results.last().getId()
							}
						}

						it.onNext(edt)
					}
				}
			} catch (e: Exception) {
				it.onError(e)
			} finally {
				realm.close()
				it.onCompleted()
			}
		}
	}

	//TODO return true/false
	fun <T : EGF2Model> getEdgeObject(idSrc: String, edge: String, idDst: String, normalizeExpand: String?): Observable<T?> {
		return Observable.create {

			val realm = Realm.getInstance(configDB)
			try {
				//TODO fix get cache with filter by normalizeExpand
				val i = idSrc + "/" + edge + normalizeExpand?.let { "/" + it }
				val cache = realm.where(cacheRealm::class.java).equalTo("id", i).findFirst()

				if (cache == null) {
					it.onNext(null)
				} else {
					val ed = realm.where(edgeRealm::class.java)
							.equalTo("id_src", idSrc).equalTo("edge", edge).equalTo("id_dst", idDst)
							.findFirst()
					if (ed == null) {
						it.onNext(null)
					} else {
						val r = realm.where(objectRealm::class.java).equalTo("id", idDst).findFirst()
						if (r != null) {
							val obj = convertFromBytes(r.body as ByteArray) as T
							it.onNext(obj)
						} else {
							it.onNext(null)
						}
					}
				}
			} catch (e: Exception) {
				it.onError(e)
			} finally {
				realm.close()
				it.onCompleted()
			}
		}
	}

	fun addObject(id: String, obj: EGF2Model, normalizeExpand: String? = "") {
		val objectRealm = objectRealm()
		objectRealm.id = id
		objectRealm.body = convertToBytes(obj)

		val cache = cacheRealm()
		cache.id = id
		normalizeExpand?.let { cache.id = cache.id + "/" + it }

		Realm.getInstance(configDB).use {
			it.executeTransactionAsync {
				it.copyToRealmOrUpdate(objectRealm)
				it.copyToRealmOrUpdate(cache)
			}
			it.close()
		}
	}

	fun addEdge(id: String, edgeName: String, edge: EGF2Edge<out EGF2Model>, normalizeExpand: String? = null) {

		val edgeCount = edgeCountRealm()
		edgeCount.id = id + "/" + edgeName
		edgeCount.count = edge.count

		val cache = cacheRealm()
		cache.id = id + "/" + edgeName
		cache.after = ""
		cache.count = edge.results.size
		normalizeExpand?.let { cache.id = cache.id + "/" + it }

		val listObj = ArrayList<objectRealm>()
		val listEdge = ArrayList<edgeRealm>()

		edge.results.forEachIndexed { i, egfModel ->
			val obj = objectRealm()
			val e = edgeRealm()

			e.edge = edgeName
			e.index = i
			e.id_src = id
			e.id_dst = egfModel.getId()

			obj.id = egfModel.getId()
			obj.body = convertToBytes(egfModel)

			listEdge.add(e)
			listObj.add(obj)
		}

		Realm.getInstance(configDB).use {
			it.executeTransactionAsync {
				if (EGF2.isFirstPage(edge)) {
					it.where(edgeRealm::class.java).equalTo("id_src", id).equalTo("edge", edgeName).findAll().deleteAllFromRealm()
				}

				it.insertOrUpdate(listObj)
				it.insert(listEdge)
				it.insertOrUpdate(cache)
				it.insertOrUpdate(edgeCount)
			}
			it.close()
		}
	}

	fun deleteObject(id: String) {
		Realm.getInstance(configDB).use {
			it.executeTransactionAsync {
				it.where(objectRealm::class.java)
						.equalTo("id", id)
						.findAll().deleteAllFromRealm()
				it.where(cacheRealm::class.java)
						.beginsWith("id", id)
						.findAll().deleteAllFromRealm()
			}
			it.close()
		}
	}

	fun addObjectOnEdge(idSrc: String, edge: String, obj: EGF2Model) {
		Realm.getInstance(configDB).use {
			val objectRealm = objectRealm()
			objectRealm.id = obj.getId()
			objectRealm.body = convertToBytes(obj)

			val cache2 = cacheRealm()
			cache2.id = obj.getId()

			val cacheOld = it.where(cacheRealm::class.java).equalTo("id", idSrc + "/" + edge).findFirst()
			val cache = cacheRealm()
			cache.id = idSrc + "/" + edge
			cache.after = ""
			cache.count = cacheOld.count + 1

			val e = edgeRealm()
			e.edge = edge
			e.index = 0
			e.id_src = idSrc
			e.id_dst = obj.getId()

			val ec = it.where(edgeCountRealm::class.java).equalTo("id", idSrc + "/" + edge).findFirst()
			if (ec == null) {
				val edgeCount = edgeCountRealm()
				edgeCount.id = idSrc + "/" + edge
				edgeCount.count = 1

				it.executeTransaction {
					it.copyToRealmOrUpdate(edgeCount)
					it.copyToRealmOrUpdate(objectRealm)
					it.copyToRealmOrUpdate(cache2)
					it.copyToRealm(e)
					it.copyToRealmOrUpdate(cache)
				}
			} else {

				val edgeCount = edgeCountRealm()
				edgeCount.id = idSrc + "/" + edge
				edgeCount.count = ec.count + 1

				it.executeTransaction {
					it.copyToRealmOrUpdate(edgeCount)
					it.copyToRealmOrUpdate(ec)
					it.where(cacheRealm::class.java)
							.beginsWith("id", obj.getId())
							.findAll().deleteAllFromRealm()
					it.copyToRealmOrUpdate(objectRealm)
					it.copyToRealmOrUpdate(cache2)
					it.copyToRealm(e)
					it.copyToRealmOrUpdate(cache)

					it.where(edgeRealm::class.java)
							.equalTo("id_src", idSrc).equalTo("edge", edge)
							.findAll().forEach {
						it.index++
					}
				}
			}
		}
	}

	fun deleteObjectOnEdge(idSrc: String, edge: String, idDst: String) {
		Realm.getInstance(configDB).use {
			val ec = it.where(edgeCountRealm::class.java).equalTo("id", idSrc + "/" + edge).findFirst()
			it.executeTransaction {
				it.where(objectRealm::class.java)
						.equalTo("id", idDst)
						.findAll().deleteAllFromRealm()
				it.where(cacheRealm::class.java)
						.beginsWith("id", idDst)
						.findAll().deleteAllFromRealm()

				ec.count--

				var flag = false
				it.where(edgeRealm::class.java)
						.equalTo("id_src", idSrc).equalTo("edge", edge)
						.findAll().forEach {
					if (flag) it.index--
					if (it.id_dst == idDst) flag = true
				}

				it.where(edgeRealm::class.java)
						.equalTo("id_src", idSrc).equalTo("edge", edge)
						.findAll().deleteAllFromRealm()
			}
			it.close()
		}
	}

	fun getIndex(idSrc: String, edge: String, idDst: String): Int? {
		val realm = Realm.getInstance(configDB)
		val obj = realm.where(edgeRealm::class.java).equalTo("id_src", idSrc).equalTo("edge", edge).equalTo("id_dst", idDst).findFirst()
		var i: Int? = null
		obj?.let {
			i = it.index
		}
		realm.close()
		return i
	}

	fun clear() {
		Realm.getInstance(configDB).use {
			it.executeTransactionAsync(Realm::deleteAll)
			it.close()
		}
	}

	fun compact() {
		try {
			Realm.compactRealm(configDB)
		} catch (e: Exception) {
			//FIXME
		}
	}
}