package com.eigengraph.egf2.framework.realm

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.RealmClass

@RealmClass
open class edgeRealm : RealmObject() {
	@Index
	var id_src: String = ""
	@Index
	var edge: String = ""
	@Index
	var id_dst: String = ""
	var index: Int = 0
}