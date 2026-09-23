package com.alkadad.compound.data.api

import com.alkadad.compound.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<AuthResponse>

    @GET("tables")
    suspend fun getTables(): Response<TableResponse>

    @GET("tables/{id}")
    suspend fun getTable(@Path("id") id: String): Response<TableDetailResponse>

    @POST("tables")
    suspend fun createTable(@Body request: CreateTableRequest): Response<Table>

    @PUT("tables/{id}")
    suspend fun updateTable(
        @Path("id") id: String,
        @Body request: CreateTableRequest
    ): Response<Table>

    @DELETE("tables/{id}")
    suspend fun deleteTable(@Path("id") id: String): Response<Unit>

    @Multipart
    @POST("tables/{id}/image")
    suspend fun uploadTableImage(
        @Path("id") id: String,
        @Part image: MultipartBody.Part
    ): Response<HouseCardUploadResponse>

    @GET("tables/{tableId}/rows")
    suspend fun getRows(
        @Path("tableId") tableId: String,
        @Query("search") search: String? = null
    ): Response<RowResponse>

    @POST("tables/{tableId}/rows")
    suspend fun createRow(
        @Path("tableId") tableId: String,
        @Body request: CreateRowRequest
    ): Response<Row>

    @PUT("tables/{tableId}/rows/{rowId}")
    suspend fun updateRow(
        @Path("tableId") tableId: String,
        @Path("rowId") rowId: String,
        @Body request: CreateRowRequest
    ): Response<Row>

    @DELETE("tables/{tableId}/rows/{rowId}")
    suspend fun deleteRow(
        @Path("tableId") tableId: String,
        @Path("rowId") rowId: String
    ): Response<Unit>

    @Multipart
    @POST("tables/{tableId}/rows/{rowId}/images")
    suspend fun uploadRowImages(
        @Path("tableId") tableId: String,
        @Path("rowId") rowId: String,
        @Part images: List<MultipartBody.Part>
    ): Response<ImageUploadResponse>

    @DELETE("tables/{tableId}/rows/{rowId}/images/{imageIndex}")
    suspend fun deleteRowImage(
        @Path("tableId") tableId: String,
        @Path("rowId") rowId: String,
        @Path("imageIndex") imageIndex: Int
    ): Response<Unit>
}
