package dev.example.nativedata.nfc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.example.nativedata.nfc.profile.NfcTagProfileIdentifier
import dev.example.nativedata.nfc.profile.icodesli.IcodeSliTagProfileIdentifier

/** 型番の判定器をSetへ登録する。新しい判定器は共通Resolverを変更せず、登録を追加する。 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NfcTagIdentificationModule {
    @Binds
    @IntoSet
    abstract fun bindIcodeSliIdentifier(implementation: IcodeSliTagProfileIdentifier): NfcTagProfileIdentifier
}
