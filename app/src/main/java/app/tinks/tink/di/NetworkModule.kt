package app.tinks.tink.di

import app.tinks.tink.merriam.network.MerriamApi
import app.tinks.tink.story.StoryApi
import app.tinks.tink.time.TimeApi
import app.tinks.tink.zi.ZiApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.tinks.app/")
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()

    @Provides
    @Singleton
    fun provideMerriamApi(retrofit: Retrofit): MerriamApi =
        retrofit.create(MerriamApi::class.java)

    @Provides
    @Singleton
    fun provideZiApi(retrofit: Retrofit): ZiApi =
        retrofit.create(ZiApi::class.java)

    @Provides
    @Singleton
    fun provideStoryApi(retrofit: Retrofit): StoryApi =
        retrofit.create(StoryApi::class.java)

    @Provides
    @Singleton
    fun provideTimeApi(retrofit: Retrofit): TimeApi =
        retrofit.create(TimeApi::class.java)
}
