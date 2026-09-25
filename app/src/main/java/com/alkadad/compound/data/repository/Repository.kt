package com.alkadad.compound.data.repository

import android.content.Context
import com.alkadad.compound.data.api.ApiClient
import com.alkadad.compound.data.api.TokenManager
import com.alkadad.compound.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AuthRepository(private val context: Context) {
    private val api = ApiClient.createApi(context)
    private val tokenManager = TokenManager(context)

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val authResponse = response.body()!!
                tokenManager.saveToken(authResponse.token)
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Invalid credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMe(): Result<User> {
        return try {
            val response = api.getMe()
            if (response.isSuccessful) {
                Result.success(response.body()!!.user)
            } else {
                Result.failure(Exception("Failed to get user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearToken()
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
}

class TableRepository(private val context: Context) {
    private val api = ApiClient.createApi(context)

    suspend fun getTables(): Result<List<Table>> {
        return try {
            val response = api.getTables()
            if (response.isSuccessful) {
                Result.success(response.body()!!.tables)
            } else {
                Result.failure(Exception("Failed to get tables"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTable(id: String): Result<Pair<Table, List<Row>>> {
        return try {
            val response = api.getTable(id)
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(Pair(body.table, body.rows))
            } else {
                Result.failure(Exception("Failed to get table"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTable(request: CreateTableRequest): Result<Table> {
        return try {
            val response = api.createTable(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create table"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTable(id: String, request: CreateTableRequest): Result<Table> {
        return try {
            val response = api.updateTable(id, request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update table"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTable(id: String): Result<Unit> {
        return try {
            val response = api.deleteTable(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete table"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadTableImage(id: String, imageFile: File): Result<String> {
        return try {
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("houseCard", imageFile.name, requestFile)
            val response = api.uploadTableImage(id, imagePart)
            if (response.isSuccessful) {
                val body = response.body()
                val url = body?.imageUrl
                if (!url.isNullOrBlank()) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("No image URL in response"))
                }
            } else {
                Result.failure(Exception("Failed to upload image: HTTP ${response.code()} ${response.errorBody()?.string().orEmpty().take(500)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RowRepository(private val context: Context) {
    private val api = ApiClient.createApi(context)

    suspend fun getRows(tableId: String, search: String? = null): Result<List<Row>> {
        return try {
            val response = api.getRows(tableId, search)
            if (response.isSuccessful) {
                Result.success(response.body()!!.rows)
            } else {
                Result.failure(Exception("Failed to get rows"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRow(tableId: String, rowId: String): Result<Row> {
        return try {
            val response = api.getRow(tableId, rowId)
            if (response.isSuccessful) {
                Result.success(response.body()!!.row)
            } else {
                Result.failure(Exception("Failed to get row: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRow(tableId: String, name: String, data: Map<String, String>): Result<Row> {
        return try {
            val response = api.createRow(tableId, CreateRowRequest(name, data))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create row"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRow(tableId: String, rowId: String, name: String, data: Map<String, String>): Result<Row> {
        return try {
            val response = api.updateRow(tableId, rowId, CreateRowRequest(name, data))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update row"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRow(tableId: String, rowId: String): Result<Unit> {
        return try {
            val response = api.deleteRow(tableId, rowId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete row"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadRowImages(tableId: String, rowId: String, imageFiles: List<File>): Result<List<Image>> {
        return try {
            val parts = imageFiles.map { file ->
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images", file.name, requestFile)
            }
            val response = api.uploadRowImages(tableId, rowId, parts)
            if (response.isSuccessful) {
                Result.success(response.body()!!.images)
            } else {
                Result.failure(Exception("Failed to upload images: HTTP ${response.code()} ${response.errorBody()?.string().orEmpty().take(500)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRowImage(tableId: String, rowId: String, imageIndex: Int): Result<Unit> {
        return try {
            val response = api.deleteRowImage(tableId, rowId, imageIndex)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
