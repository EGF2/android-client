package com.eigengraph.egf2.framework.util

import android.util.Log
import com.eigengraph.egf2.framework.EGF2

object Logger {
	fun d(message: String) {
		if (EGF2.debugMode) Log.d("EGF2", message)
	}
}