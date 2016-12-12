package com.eigengraph.egf2.framework

import com.eigengraph.egf2.framework.models.EGF2Edge
import com.eigengraph.egf2.framework.models.EGF2Model
import rx.Subscription
import rx.functions.Action1
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject

object EGF2Bus {

	enum class EVENT {
		OBJECT_CREATED,
		OBJECT_UPDATED,
		OBJECT_DELETED,
		OBJECT_LOADED,
		EDGE_ADDED,
		EDGE_REMOVED,
		EDGE_REFRESHED,
		EDGE_PAGE_LOADED
	}

	class ObjectEvent(@JvmField val event: EVENT, @JvmField val id: String?, @JvmField val obj: EGF2Model?)
	class EdgeEvent(@JvmField val event: EVENT, @JvmField val id: String, @JvmField val edgeName: String, @JvmField val edge: EGF2Edge<out EGF2Model>? = null, @JvmField val obj: EGF2Model? = null)

	private val mBusSubjectO = SerializedSubject(PublishSubject.create<ObjectEvent>())
	private val mBusSubjectE = SerializedSubject(PublishSubject.create<EdgeEvent>())

	fun subscribeForObject(event: EVENT, id: String?, onNext: Action1<ObjectEvent>): Subscription {
		return mBusSubjectO
				.filter { it.event == event && (id == null || it.id == id) }
				.subscribe(onNext)
	}

	fun subscribeForEdge(event: EVENT, id: String, edge: String, onNext: Action1<EdgeEvent>): Subscription {
		return mBusSubjectE
				.filter { it.event == event && it.id == id && it.edgeName == edge }
				.subscribe(onNext)
	}

	fun post(event: EVENT, id: String?, obj: EGF2Model?) {
		mBusSubjectO.onNext(ObjectEvent(event, id, obj))
	}

	fun post(event: EVENT, id: String, edgeName: String, edge: EGF2Edge<out EGF2Model>?) {
		mBusSubjectE.onNext(EdgeEvent(event, id, edgeName, edge = edge))
	}

	fun post(event: EVENT, id: String, edge: String, obj: EGF2Model) {
		mBusSubjectE.onNext(EdgeEvent(event, id, edge, obj = obj))
	}
}