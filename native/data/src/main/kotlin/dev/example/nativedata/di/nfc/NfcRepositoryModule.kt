package dev.example.nativedata.di.nfc

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.example.nativedata.nfc.detection.DefaultNfcTagDetectionRepository
import dev.example.nativedata.nfc.detection.NfcTagDetectionRepository
import dev.example.nativedata.repository.card.CardRepositoryFactory
import dev.example.nativedata.repository.card.DefaultCardRepositoryFactory

/** 汎用タグ検出と業務カード生成の契約を実装へ関連付ける。Tag単位の独自DIスコープは作らない。 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NfcRepositoryModule {
    @Binds
    abstract fun bindCardRepositoryFactory(implementation: DefaultCardRepositoryFactory): CardRepositoryFactory

    @Binds
    abstract fun bindTagDetectionRepository(implementation: DefaultNfcTagDetectionRepository): NfcTagDetectionRepository
}
