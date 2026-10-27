package dev.example.nativedata.nfc

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.example.nativedata.golf.DefaultGolfCardRepositoryFactory
import dev.example.nativedata.golf.GolfCardRepositoryFactory

/** 汎用タグ検出の契約と実装を関連付ける。Tag単位の独自DIスコープは作らない。 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NfcRepositoryModule {
    @Binds
    abstract fun bindGolfCardRepositoryFactory(
        implementation: DefaultGolfCardRepositoryFactory,
    ): GolfCardRepositoryFactory

    @Binds
    abstract fun bindTagDetectionRepository(implementation: DefaultNfcTagDetectionRepository): NfcTagDetectionRepository
}
