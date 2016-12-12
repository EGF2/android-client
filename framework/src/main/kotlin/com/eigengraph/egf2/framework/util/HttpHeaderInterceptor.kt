package com.eigengraph.egf2.framework.util

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.*

class HttpHeaderInterceptor(val host: String, val function: () -> HashMap<String, String>) : Interceptor {
	override fun intercept(chain: Interceptor.Chain?): Response? {
		val request: Request = chain!!.request()
		if (request.url().host().equals(host, true)) {
			val headers = function.invoke()
			val newRequest: Request.Builder = request.newBuilder()
			for ((key, value) in headers) {
				newRequest.header(key, value)
			}
			return chain.proceed(newRequest.build())
		}
		return chain.proceed(request)
	}
}