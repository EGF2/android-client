package com.eigengraph.egf2.framework

import android.util.Log
import com.eigengraph.egf2.framework.util.HttpHeaderInterceptor
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import rx.Observable
import rx.schedulers.Schedulers

data class RegisterModel(@JvmField val first_name: String,
                         @JvmField val last_name: String,
                         @JvmField val email: String,
                         @JvmField val date_of_birth: String,
                         @JvmField val password: String)

data class LoginModel(@JvmField val email: String,
                      @JvmField val password: String)

data class ResetPasswordModel(@JvmField val reset_token: String,
                              @JvmField val new_password: String)

data class ChangePasswordModel(@JvmField val old_password: String,
                               @JvmField val new_password: String)

data class TokenModel(@JvmField val token: String)

internal class EGF2AuthApi : EGF2Api() {
	private var service: EGF2AuthService

	init {
		Log.d("EGF2Api", "EGF2AuthApi init")
		val restAdapter = getRestAdapterBuilder()
		val restClient = getRestClientBuilder()
		restClient.addInterceptor(HttpHeaderInterceptor(baseUrl.replace("https://", "").replace("http://", "").replace("/", ""), { EGF2Api.getHeagers() }))

		if (EGF2.debugMode)
			restClient.addInterceptor(interceptor)

		restAdapter.baseUrl(baseUrl + prefix)
		restAdapter.client(restClient.build())
		service = restAdapter.build().create(EGF2AuthService::class.java)
		Log.d("EGF2Api", "EGF2AuthApi init")
	}

	internal fun register(body: RegisterModel): Observable<String> =
			service.register(body)
					.flatMap {
						if (it.isSuccessful) {
							val token = it.body().token
							addHeader("Authorization", "Bearer " + token)
							Observable.just(token)
						} else {
							onError(it)
						}
					}
					.subscribeOn(Schedulers.io())

	internal fun login(body: LoginModel): Observable<String> =
			service.login(body)
					.flatMap {
						if (it.isSuccessful) {
							val token = it.body().token
							addHeader("Authorization", "Bearer " + token)
							Observable.just(token)
						} else {
							onError(it)
						}
					}
					.subscribeOn(Schedulers.io())

	internal fun verifyEmail(token: String): Observable<Any> =
		service.verifyEmail(token)
				.flatMap {
					if (it.isSuccessful) {
						Observable.just(it)
					} else {
						onError(it)
					}
				}
				.subscribeOn(Schedulers.io())

	internal fun logout(): Observable<Any> =
			service.logout()
					.flatMap {
					if (it.isSuccessful) {
						removeHeader("Authorization")
						Observable.just(it)
					} else {
						onError(it)
					}
					}
				.subscribeOn(Schedulers.io())

	internal fun forgotPassword(email: String): Observable<Any> =
			service.forgotPassword(email)
					.flatMap {
					if (it.isSuccessful) {
						Observable.just(it)
					} else {
						onError(it)
					}
					}
				.subscribeOn(Schedulers.io())

	internal fun resetPassword(body: ResetPasswordModel): Observable<Any> =
			service.resetPassword(body)
					.flatMap {
					if (it.isSuccessful) {
						Observable.just(it)
					} else {
						onError(it)
					}
					}
				.subscribeOn(Schedulers.io())

	internal fun changePassword(body: ChangePasswordModel): Observable<Any> =
			service.changePassword(body)
					.flatMap {
					if (it.isSuccessful) {
						Observable.just(it)
					} else {
						onError(it)
					}
					}
				.subscribeOn(Schedulers.io())

	internal fun resendEmailVerification(): Observable<Any> =
			service.resendEmailVerification()
					.flatMap {
					if (it.isSuccessful) {
						Observable.just(it)
					} else {
						onError(it)
					}
					}
					.subscribeOn(Schedulers.io())

	private fun onError(it: Response<*>): Observable<String>? =
			if (it.errorBody() != null) {
				val error: String = it.errorBody().string()
				val gson: Gson = Gson()
				val je: JsonElement = gson.fromJson(error, JsonElement::class.java)
				val jo: JsonObject = je.asJsonObject
				if (jo.has("code") && jo.has("message")) {
					Observable.error(Throwable(jo.get("code").asString + ": " + jo.get("message").asString))
				} else {
					Observable.error(Throwable(error))
				}
			} else {
				Observable.error(Throwable(it.message()))
			}

}