package app.tinks.tink.di

import app.tinks.tink.BuildConfig
import app.tinks.tink.book.BookApi
import app.tinks.tink.book.GoogleBooksApi
import app.tinks.tink.haircut.HaircutApi
import app.tinks.tink.merriam.network.MerriamApi
import app.tinks.tink.settings.ApiEnvironment
import app.tinks.tink.settings.AppEnvironmentRepository
import app.tinks.tink.story.StoryApi
import app.tinks.tink.time.TimeApi
import app.tinks.tink.weight.WeightApi
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
import javax.inject.Named
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
        loggingInterceptor: HttpLoggingInterceptor,
        appEnvironmentRepository: AppEnvironmentRepository,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url
                val shouldUseDevApi =
                    appEnvironmentRepository.currentEnvironment() == ApiEnvironment.Dev &&
                        url.host == "api.tinks.app" &&
                        BuildConfig.TINK_API_BASE_URL.contains("api.tinks.app") &&
                        !url.encodedPath.startsWith("/dev/")

                val updatedRequest = if (shouldUseDevApi) {
                    request.newBuilder()
                        .url(
                            url.newBuilder()
                                .encodedPath("/dev${url.encodedPath}")
                                .build()
                        )
                        .build()
                } else {
                    request
                }
                chain.proceed(updatedRequest)
            }
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
            .baseUrl(BuildConfig.TINK_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()

    @Provides
    @Singleton
    @Named("googleBooksRetrofit")
    fun provideGoogleBooksRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()

    @Provides
    @Singleton
    fun provideBookApi(retrofit: Retrofit): BookApi =
        retrofit.create(BookApi::class.java)

    @Provides
    @Singleton
    fun provideGoogleBooksApi(
        @Named("googleBooksRetrofit") retrofit: Retrofit
    ): GoogleBooksApi =
        retrofit.create(GoogleBooksApi::class.java)

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
    fun provideHaircutApi(retrofit: Retrofit): HaircutApi =
        retrofit.create(HaircutApi::class.java)

    @Provides
    @Singleton
    fun provideTimeApi(retrofit: Retrofit): TimeApi =
        retrofit.create(TimeApi::class.java)

    @Provides
    @Singleton
    fun provideWeightApi(retrofit: Retrofit): WeightApi =
        retrofit.create(WeightApi::class.java)
}
