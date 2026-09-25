package com.alkadad.compound.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id") val id: String,
    val email: String,
    val name: String,
    val role: String
)

data class AuthResponse(
    val success: Boolean,
    val token: String,
    val user: User
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class Table(
    @SerializedName("_id") val id: String,
    val name: String,
    val description: String?,
    @SerializedName("houseCardImage") val houseCardImage: String?,
    val columns: List<Column>?,
    @SerializedName("createdBy") val createdBy: User?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class Column(
    val key: String,
    val label: String,
    val type: String = "text",
    val options: List<String>? = null
)

data class TableResponse(
    val success: Boolean,
    val count: Int,
    val tables: List<Table>
)

data class TableDetailResponse(
    val success: Boolean,
    val table: Table,
    val rows: List<Row>
)

data class CreateTableRequest(
    val name: String,
    val description: String? = null,
    val columns: List<Column>? = null
)

data class Row(
    @SerializedName("_id") val id: String,
    @SerializedName("tableId") val tableId: String,
    val name: String = "",
    val data: Map<String, String>?,
    val images: List<Image>?,
    @SerializedName("createdBy") val createdBy: User?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class Image(
    val url: String,
    val caption: String?,
    @SerializedName("uploadedAt") val uploadedAt: String
)

data class RowResponse(
    val success: Boolean,
    val count: Int,
    val rows: List<Row>
)

data class RowDetailResponse(
    val success: Boolean,
    val row: Row
)

data class CreateRowRequest(
    val name: String,
    val data: Map<String, String>
)

data class ImageUploadResponse(
    val success: Boolean,
    val images: List<Image>,
    // Backend returns this row unpopulated (createdBy is an id string), so keep it untyped.
    val row: JsonElement?
)

data class HouseCardUploadResponse(
    val success: Boolean,
    @SerializedName("imageUrl") val imageUrl: String?,
    // Backend returns this table unpopulated (createdBy is an id string), so keep it untyped.
    val table: JsonElement?
)

data class ErrorResponse(
    val success: Boolean,
    val message: String
)
