package com.eigengraph.egf2.framework

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

internal abstract class EGF2Api {
	enum class ApiTypes(private var apiClass: Class<out EGF2Api>) {
		AUTH_API(EGF2AuthApi::class.java),
		GRAPH_API(EGF2GraphApi::class.java);

		fun getApiType(): Class<out EGF2Api> = this.apiClass

	}

	init {
		Log.d("EGF2Api", "EGF2Api init")
		if (baseUrl.isEmpty()) {
			Log.d("EGF2Api", "Not specified parameter EGF2GraphApi.baseUrl " + baseUrl)
			RuntimeException("Not specified parameter EGF2GraphApi.baseUrl")
		} else {

			interceptor.level = HttpLoggingInterceptor.Level.BODY

			client = OkHttpClient.Builder()

			adapter = Retrofit.Builder()
					.addConverterFactory(GsonConverterFactory.create(gson))
					.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
			Log.d("EGF2Api", "EGF2Api")
		}
		Log.d("EGF2Api", "EGF2Api init")
	}

	companion object {
		var baseUrl: String = ""
		var prefix: String = ""

		val interceptor = HttpLoggingInterceptor()

		private lateinit var adapter: Retrofit.Builder
		private lateinit var client: OkHttpClient.Builder

		@Suppress("UNCHECKED_CAST")
		internal fun <T> getApi(type: ApiTypes): T = type.getApiType().newInstance() as T

		internal fun getRestAdapterBuilder() = adapter
		internal fun getRestClientBuilder() = client
		var gson: Gson = Gson()

		var headers = HashMap<String, String>()

		fun getHeagers(): HashMap<String, String> = headers

		internal fun addHeader(name: String, value: String) {
			headers.put(name, value)
		}

		internal fun removeHeader(name: String) {
			headers.remove(name)
		}
	}
}