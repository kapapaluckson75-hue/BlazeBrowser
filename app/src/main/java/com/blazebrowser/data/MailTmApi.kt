package com.blazebrowser.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// Mail.tm API interface
interface MailTmApi {
    @GET("domains")
    suspend fun getDomains(): MailTmDomainResponse

    @POST("accounts")
    suspend fun createAccount(@Body request: CreateAccountRequest): MailTmAccountResponse

    @POST("token")
    suspend fun login(@Body request: LoginRequest): MailTmTokenResponse

    @GET("messages")
    suspend fun getMessages(@Header("Authorization") auth: String): MailTmMessageListResponse

    @GET("messages/{id}")
    suspend fun getMessage(
        @Header("Authorization") auth: String,
        @Path("id") messageId: String
    ): MailTmMessageDetailResponse

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(
        @Header("Authorization") auth: String,
        @Path("id") accountId: String
    )
}

data class MailTmDomainResponse(
    @SerializedName("hydra:member") val domains: List<MailTmDomain> = emptyList()
)

data class MailTmDomain(
    val id: String,
    val domain: String,
    @SerializedName("isActive") val isActive: Boolean
)

data class CreateAccountRequest(
    val address: String,
    val password: String
)

data class MailTmAccountResponse(
    val id: String,
    val address: String,
    val password: String? = null
)

data class LoginRequest(
    val address: String,
    val password: String
)

data class MailTmTokenResponse(
    val token: String,
    val id: String
)

data class MailTmMessageListResponse(
    @SerializedName("hydra:member") val messages: List<MailTmMessage> = emptyList(),
    @SerializedName("hydra:totalItems") val totalItems: Int = 0
)

data class MailTmMessage(
    val id: String,
    val from: MailTmAddress,
    val to: List<MailTmAddress>,
    val subject: String,
    val intro: String,
    @SerializedName("seen") val seen: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class MailTmMessageDetailResponse(
    val id: String,
    val from: MailTmAddress,
    val to: List<MailTmAddress>,
    val subject: String,
    val intro: String,
    @SerializedName("seen") val seen: Boolean,
    @SerializedName("html") val html: List<String>,
    @SerializedName("text") val text: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class MailTmAddress(
    val address: String,
    val name: String
)

object MailTmClient {
    private const val BASE_URL = "https://api.mail.tm/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val api: MailTmApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MailTmApi::class.java)
}
