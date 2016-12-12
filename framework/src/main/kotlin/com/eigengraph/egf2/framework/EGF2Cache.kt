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

	fun init(appContext: Context, name: String? = null, version: Long = 0, key: ByteArray? = null) {
		Realm.init(appContext)
		val config = RealmConfiguration.Builder()
		name?.let { config.name(name) }
		key?.let { config.encryptionKey(key) }
		config.schemaVersion(version)

		config.deleteRealmIfMigrationNeeded()
		Realm.setDefaultConfiguration(config.build())
	}

	// object_id | normalize(expand)
	// object_id/edge | normalize(expand) | count | after

	fun <T : EGF2Model> getObjectById(id: String, normalizeExpand: String?): Observable<T?> {
		return Observable.create<T?> {
			try {
				val realm = Realm.getDefaultInstance()

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
				it.onCompleted()
			}
		}
	}

	fun <T : EGF2Model> getEdgeObjects(id: String, edge: String, after: EGF2Model?, count: Int, normalizeExpand: String?): Observable<EGF2Edge<T>?> {
		return Observable.create {
			try {
				val realm = Realm.getDefaultInstance()

				//TODO fix get cache with filter by normalizeExpand
				val cache = realm.where(cacheRealm::class.java).equalTo("id", id + "/" + edge + normalizeExpand?.let { "/" + it }).findFirst()

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
						val r = realm.where(objectRealm::class.java)
						listId.forEach {
							r.equalTo("id", it).or()
						}
						val listObj = ArrayList<EGF2Model>()
						r.findAll().forEach {
							val obj = convertFromBytes(it.body as ByteArray) as T
							listObj.add(obj)
						}

						val edt = EGF2Edge<T>()
						edt.result = listObj as List<T>
						edt.count = edgeCount

						when (EGF2.paginationMode) {
							EGF2.PAGINATION_MODE.INDEX -> {
								edt.last = edt.result.size.toString()
							}
							EGF2.PAGINATION_MODE.OBJECT -> {
								edt.last = edt.result.last().getId()
							}
						}

						it.onNext(edt)
					}
				}
			} catch (e: Exception) {
				it.onError(e)
			} finally {
				it.onCompleted()
			}
		}
	}

	fun <T : EGF2Model> getEdgeObject(idSrc: String, edge: String, idDst: String, normalizeExpand: String?): Observable<T?> {
		return Observable.create {
			try {
				val realm = Realm.getDefaultInstance()

				//TODO fix get cache with filter by normalizeExpand
				val cache = realm.where(cacheRealm::class.java).equalTo("id", idSrc + "/" + edge + normalizeExpand?.let { "/" + it }).findFirst()

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
		normalizeExpand?.let { cache.id + "/" + it }

		Realm.getDefaultInstance().use {
			it.executeTransaction {
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
		cache.count = edge.result.size
		normalizeExpand?.let { cache.id + "/" + it }

		val listObj = ArrayList<objectRealm>()
		val listEdge = ArrayList<edgeRealm>()

		edge.result.forEachIndexed { i, egfModel ->
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

		Realm.getDefaultInstance().use {
			it.executeTransaction {
				if (edge.first == null) {
					it.where(edgeRealm::class.java).equalTo("id_src", id).equalTo("edge", edgeName).findAll().deleteAllFromRealm()
				}
				it.copyToRealmOrUpdate(listObj)
				it.copyToRealmOrUpdate(listEdge)
				it.copyToRealmOrUpdate(cache)
				it.copyToRealmOrUpdate(edgeCount)
			}
			it.close()
		}
	}

	fun deleteObject(id: String) {
		Realm.getDefaultInstance().use {
			it.executeTransaction {
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
		Realm.getDefaultInstance().use {
			val objectRealm = objectRealm()
			objectRealm.id = obj.getId()
			objectRealm.body = convertToBytes(obj)

			val cache2 = cacheRealm()
			cache2.id = obj.getId()

			val cache = cacheRealm()
			cache.id = idSrc + "/" + edge
			cache.after = ""
			cache.count = 1

			val e = edgeRealm()
			e.edge = edge
			e.index = 0
			e.id_src = idSrc
			e.id_dst = obj.getId()

			val ec = it.where(edgeCountRealm::class.java).equalTo("id", obj.getId() + "/" + edge).findFirst()
			if (ec == null) {
				val edgeCount = edgeCountRealm()
				edgeCount.id = idSrc + "/" + edge
				edgeCount.count = 1

				it.executeTransaction {
					it.copyToRealmOrUpdate(ec)
					it.copyToRealmOrUpdate(objectRealm)
					it.copyToRealmOrUpdate(cache2)
					it.copyToRealmOrUpdate(e)
					it.copyToRealmOrUpdate(cache)
				}
			} else {
				ec.count++

				it.executeTransaction {
					it.copyToRealmOrUpdate(ec)
					it.where(cacheRealm::class.java)
							.beginsWith("id", obj.getId())
							.findAll().deleteAllFromRealm()
					it.copyToRealmOrUpdate(objectRealm)
					it.copyToRealmOrUpdate(cache2)
					it.copyToRealmOrUpdate(e)
					it.copyToRealmOrUpdate(cache)

					it.where(edgeRealm::class.java)
							.equalTo("id_src", idSrc).equalTo("edge", edge).equalTo("id_dst", obj.getId())
							.findAll().forEach {
						it.index++
					}
				}
			}
		}
	}

	fun deleteObjectOnEdge(idSrc: String, edge: String, idDst: String) {
		Realm.getDefaultInstance().use {
			val ec = it.where(edgeCountRealm::class.java).equalTo("id", idDst + "/" + edge).findFirst()
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
						.equalTo("id_src", idSrc).equalTo("edge", edge).equalTo("id_dst", idDst)
						.findAll().forEach {
					if (flag) it.index--
					if (it.id_dst == idDst) flag = true
				}

				it.where(edgeRealm::class.java)
						.equalTo("id_src", idSrc).equalTo("edge", edge).equalTo("id_dst", idDst)
						.findAll().deleteAllFromRealm()
			}
			it.close()
		}
	}

	fun getIndex(idSrc: String, edge: String, idDst: String): Int? {
		val realm = Realm.getDefaultInstance()
		val obj = realm.where(edgeRealm::class.java).equalTo("id_src", idSrc).equalTo("edge", edge).equalTo("id_dst", idDst).findFirst()
		obj?.let {
			return it.index
		}
		return null
	}

	fun clear() {
		Realm.getDefaultInstance().use(Realm::deleteAll)
		//Realm.getDefaultInstance().use(Realm::close)
		//Realm.deleteRealm(Realm.getDefaultInstance().configuration)
	}

	fun compact() {
		try {
			Realm.compactRealm(Realm.getDefaultInstance().configuration)
		} catch (e: Exception) {
			//FIXME
		}
	}
}