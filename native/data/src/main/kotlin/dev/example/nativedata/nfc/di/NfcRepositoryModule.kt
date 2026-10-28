package dev.example.nativedata.nfc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.example.nativedata.card.CardRepositoryFactory
import dev.example.nativedata.card.DefaultCardRepositoryFactory
import dev.example.nativedata.nfc.detection.DefaultNfcTagDetectionRepository
import dev.example.nativedata.nfc.detection.NfcTagDetectionRepository

/** 汎用タグ検出と業務カード生成の契約を実装へ関連付ける。Tag単位の独自DIスコープは作らない。 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NfcRepositoryModule {
    @Binds
    abstract fun bindCardRepositoryFactory(implementation: DefaultCardRepositoryFactory): CardRepositoryFactory

    @Binds
    abstract fun bindTagDetectionRepository(implementation: DefaultNfcTagDetectionRepository): NfcTagDetectionRepository
}
