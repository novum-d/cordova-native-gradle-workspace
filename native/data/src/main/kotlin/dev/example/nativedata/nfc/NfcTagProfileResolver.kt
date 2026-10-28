package dev.example.nativedata.nfc

import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.jvm.JvmSuppressWildcards

/** 登録済みの判定器から一意のProfileを選ぶ。特定製品のUID条件やタグのキャッシュは持たない。 */
@Singleton
internal class NfcTagProfileResolver @Inject constructor(
    private val identifiers: Set<@JvmSuppressWildcards NfcTagProfileIdentifier>,
) {
    /**
     * 対応技術を持つ判定器だけを呼ぶ。未一致・複数一致・実タグと異なる通信方式は未識別とする。
     * 登録順へ依存せず、例外とキャンセルは呼出元のnfcCallへ伝える。
     */
    suspend fun identify(tag: NfcTagAccess): NfcTagProfile? {
        coroutineContext.ensureActive()
        val technologies = tag.technologies.toSet()
        var selected: NfcTagProfile? = null
        for (identifier in identifiers) {
            coroutineContext.ensureActive()
            val candidate = identifyIfCompatible(identifier, tag, technologies)
            if (candidate != null) {
                if (selected != null) return null
                selected = candidate
            }
        }
        return selected
    }

    /** 実タグが持つ技術と判定器・Profileの両方の要求を照合する。 */
    private suspend fun identifyIfCompatible(
        identifier: NfcTagProfileIdentifier,
        tag: NfcTagAccess,
        technologies: Set<String>,
    ): NfcTagProfile? {
        if (!technologies.containsAll(identifier.requiredTechnologies)) return null
        return identifier.identify(tag)?.takeIf { it.protocol.androidTechnology in technologies }
    }
}
