package io.github.danilodox.warbook.data.remote


import io.github.danilodox.warbook.BuildConfig
import io.github.danilodox.warbook.data.datastore.SecureTokenProvider
import io.github.danilodox.warbook.data.remote.interceptor.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class RetrofitService(private val tokenProvider: SecureTokenProvider) {

    companion object {
        private const val BASE_URL = "https://api.clashofclans.com/v1/"
    }

    private fun provideHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .apply {
                        val logging = HttpLoggingInterceptor()
                        logging.level = if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                        logging.redactHeader("Authorization")
                        addInterceptor(AuthInterceptor(tokenProvider = tokenProvider))
                        addInterceptor(logging)
                    }.build()

    var service: ApiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(provideHttpClient())
            .build()
            .create(ApiService::class.java)


}