package com.eigengraph.egf2.framework

import android.util.Log
import com.eigengraph.egf2.framework.util.HttpHeaderInterceptor
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject

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
		restClient.addInterceptor(interceptor)
		restAdapter.baseUrl(baseUrl + preffix)
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

	internal fun verifyEmail(token: String): Observable<Any> {
		val subject = BehaviorSubject.create<Any>()

		service.verifyEmail(token)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						subject.onNext(it)
					} else {
						onError(it)
					}
				}, {
					subject.onError(it)
				})

		return subject
	}

	internal fun logout(): Observable<Any> {
		val subject = BehaviorSubject.create<Any>()

		service.logout()
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						removeHeader("Authorization")
						subject.onNext(it)
					} else {
						onError(it)
					}
				}, {
					subject.onError(it)
				})

		return subject
	}

	internal fun forgotPassword(email: String): Observable<Any> {
		val subject = BehaviorSubject.create<Any>()

		service.forgotPassword(email)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						subject.onNext(it)
					} else {
						onError(it)
					}
				}, {
					subject.onError(it)
				})

		return subject
	}

	internal fun resetPassword(body: ResetPasswordModel): Observable<Any> {
		val subject = BehaviorSubject.create<Any>()

		service.resetPassword(body)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						subject.onNext(it)
					} else {
						onError(it)
					}
				}, {
					subject.onError(it)
				})

		return subject
	}

	internal fun changePassword(body: ChangePasswordModel): Observable<Any> {
		val subject = BehaviorSubject.create<Any>()

		service.changePassword(body)
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						subject.onNext(it)
					} else {
						onError(it)
					}
				}, {
					subject.onError(it)
				})

		return subject
	}

	internal fun resendEmailVerification(): Observable<Any> {
		val subject = BehaviorSubject.create<Any>()

		service.resendEmailVerification()
				.subscribeOn(Schedulers.io())
				.subscribe({
					if (it.isSuccessful) {
						subject.onNext(it)
					} else {
						onError(it)
					}
				}, {
					subject.onError(it)
				})

		return subject
	}

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