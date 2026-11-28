package app.tinks.tink.zi.game

import app.tinks.tink.network.ApiResult
import app.tinks.tink.zi.Zi
import app.tinks.tink.zi.ZiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ZiGameCharacterDataSource {
    fun getAllZis(): Flow<ApiResult<List<Zi>>>
    fun downgradeMissedToReview(zis: String, proficiency: Int): Flow<ApiResult<Unit>>
}

@Singleton
class ZiRepositoryGameDataSource @Inject constructor(
    private val repository: ZiRepository,
) : ZiGameCharacterDataSource {
    override fun getAllZis(): Flow<ApiResult<List<Zi>>> = repository.getAllZis()

    override fun downgradeMissedToReview(zis: String, proficiency: Int): Flow<ApiResult<Unit>> =
        repository.putZi(zis, proficiency)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ZiGameModule {
    @Binds
    @Singleton
    abstract fun bindZiGameCharacterDataSource(impl: ZiRepositoryGameDataSource): ZiGameCharacterDataSource

    @Binds
    @Singleton
    abstract fun bindZiVoiceModelProvider(impl: ZiVoiceModelManager): ZiVoiceModelProvider

    @Binds
    @Singleton
    abstract fun bindZiSpeechEngine(impl: SherpaOnnxSpeechEngine): ZiSpeechEngine

    @Binds
    @Singleton
    abstract fun bindZiGameProgressRepository(impl: ZiGameProgressStore): ZiGameProgressRepository
}
