package com.eigengraph.egf2.framework.realm

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class cacheRealm : RealmObject() {
	@PrimaryKey
	@Index
	var id: String = ""
	@Index
	var after: String = ""
	var count: Int = 0
}