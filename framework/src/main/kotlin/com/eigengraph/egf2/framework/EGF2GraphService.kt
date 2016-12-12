package com.eigengraph.egf2.framework

import com.google.gson.JsonObject
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import rx.Observable
import java.util.*

internal interface EGF2GraphService {

	@GET("graph/{object_id}")
	fun getObject(@Path("object_id") id: String,
	              @QueryMap params: HashMap<String, Any>): Observable<Response<JsonObject>>

	@GET("graph/{object_id}/{edge_name}")
	fun getEdge(@Path("object_id") id: String,
	            @Path("edge_name") edge: String,
	            @QueryMap params: HashMap<String, Any>): Observable<Response<JsonObject>>

	@GET("graph/{id_src}/{edge}/{id_dst}")
	fun getEdgeObject(@Path("id_src") idSrc: String,
	                  @Path("edge") edge: String,
	                  @Path("id_dst") idDst: String,
	                  @QueryMap params: HashMap<String, Any>): Observable<Response<JsonObject>>

	@POST("graph")
	fun postObject(@Body body: Any): Observable<Response<JsonObject>>

	@POST("graph/{id_src}/{edge_name}")
	fun postEdge(@Path("id_src") idSrc: String,
	             @Path("edge_name") edge: String,
	             @Body body: Any): Observable<Response<JsonObject>>

	@POST("graph/{id_src}/{edge_name}/{id_dst}")
	fun postEdge(@Path("id_src") idSrc: String,
	             @Path("edge_name") edge: String,
	             @Path("id_dst") idDst: String): Observable<Response<JsonObject>>

	@PUT("graph/{object_id}")
	fun putObject(@Path("object_id") id: String,
	              @Body body: Any): Observable<Response<JsonObject>>

	@DELETE("graph/{object_id}")
	fun deleteObject(@Path("object_id") id: String): Observable<Response<JsonObject>>

	@DELETE("graph/{id_src}/{edge}/{id_dst}")
	fun deleteEdgeObject(@Path("id_src") idSrc: String,
	                     @Path("edge") edge: String,
	                     @Path("id_dst") idDst: String): Observable<Response<JsonObject>>

	@GET("search")
	fun search(@QueryMap params: HashMap<String, Any>): Observable<Response<JsonObject>>

	@GET("new_image")
	fun newImage(@QueryMap params: HashMap<String, Any>): Observable<Response<JsonObject>>

	@GET("new_file")
	fun newFile(@QueryMap params: HashMap<String, Any>): Observable<Response<JsonObject>>

	@PUT
	fun uploadFile(@Url fullUrl: String,
	               @Header("Content-Type") contentType: String,
	               @Body file: RequestBody): Observable<Response<Void>>
}