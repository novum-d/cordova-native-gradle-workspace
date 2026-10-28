package dev.example.nativedata.nfc

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 対象の有効期間・読取排他・Profile選択を、業務カードによらず共通管理する。 */
internal class DefaultNfcCardRepository<E : NfcReadFailure>(
    private val target: DetectedNfcTagHandle,
    private val failures: NfcReadFailureFactory<E>,
    private val reader: NfcMemoryReader,
    private val profileResolver: NfcTagProfileResolver,
) : NfcCardRepository<E> {
    /** 同じタグを指す別Repositoryとも直列化し、終了した対象への新しい接続を拒否する。 */
    override suspend fun read(
        expectedModel: NfcTagModel,
        range: NfcMemoryRange,
    ): Result<ByteArray, E> = withContext(Dispatchers.IO) {
        target.readMutex.withLock {
            if (target.isActive) {
                nfcCall(failures) {
                    profileResolver.identify(target.access)
                }.andThen { profile ->
                    if (profile == null || profile.model != expectedModel) {
                        Err(failures.unsupportedTag())
                    } else {
                        reader.read(tag = target.access, profile = profile, range = range, failures = failures)
                    }
                }
            } else {
                Err(failures.targetExpired())
            }
        }
    }
}
