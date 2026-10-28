package dev.example.nativedata.nfc.testing

import dev.example.nativedata.nfc.card.NfcCardReaderFactory
import dev.example.nativedata.nfc.memory.NfcMemoryDataSource
import dev.example.nativedata.nfc.profile.NfcTagProfileResolver
import dev.example.nativedata.nfc.profile.icodesli.IcodeSliTagProfileIdentifier

/** 本番のSLI判定器だけを登録し、各JVMテストへ独立したResolverを渡す。 */
internal fun testNfcProfileResolver(): NfcTagProfileResolver =
    NfcTagProfileResolver(setOf(IcodeSliTagProfileIdentifier()))

/** DataSourceとReaderへ同じ識別器集合を注入し、本番の生成経路を再現する。 */
internal fun testNfcCardReaderFactory(): NfcCardReaderFactory {
    val resolver = testNfcProfileResolver()
    return NfcCardReaderFactory(NfcMemoryDataSource(resolver), resolver)
}
