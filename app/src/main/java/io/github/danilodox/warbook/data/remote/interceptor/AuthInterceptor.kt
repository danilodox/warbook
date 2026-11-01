package io.github.danilodox.warbook.data.remote.interceptor

import android.util.Log
import io.github.danilodox.warbook.BuildConfig
import io.github.danilodox.warbook.data.datastore.SecureTokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: SecureTokenProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()

        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrEmpty()) {
                addHeader("Authorization", "Bearer $token")
                if (BuildConfig.DEBUG) {
                    Log.d("AuthInterceptor", "Token in use: $token")
                }
            }

        }.build()
        return chain.proceed(request)
    }
}