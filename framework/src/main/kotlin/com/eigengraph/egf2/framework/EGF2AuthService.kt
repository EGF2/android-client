package com.eigengraph.egf2.framework

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import rx.Observable

internal interface EGF2AuthService {
	@POST("register")
	fun register(@Body body: Any): Observable<Response<TokenModel>>

	@POST("login")
	fun login(@Body body: Any): Observable<Response<TokenModel>>

	@GET("verify_email")
	fun verifyEmail(@Query("token") token: String): Observable<Response<Any>>

	@GET("logout")
	fun logout(): Observable<Response<Void>>

	@GET("forgot_password")
	fun forgotPassword(@Query("email") email: String): Observable<Response<Any>>

	@POST("reset_password")
	fun resetPassword(@Body body: Any): Observable<Response<Any>>

	@POST("change_password")
	fun changePassword(@Body body: Any): Observable<Response<Any>>

	@POST("resend_email_verification")
	fun resendEmailVerification(): Observable<Response<Any>>
}