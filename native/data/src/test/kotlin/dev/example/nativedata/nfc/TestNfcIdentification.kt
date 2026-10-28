package dev.example.nativedata.nfc

/** 本番のSLI判定器だけを登録し、各JVMテストへ独立したResolverを渡す。 */
internal fun testNfcProfileResolver(): NfcTagProfileResolver =
    NfcTagProfileResolver(setOf(IcodeSliTagProfileIdentifier()))

/** ReaderとRepositoryへ同じ識別器集合を注入し、本番の生成経路を再現する。 */
internal fun testNfcCardRepositoryFactory(): NfcCardRepositoryFactory {
    val resolver = testNfcProfileResolver()
    return NfcCardRepositoryFactory(NfcMemoryReader(resolver), resolver)
}
