package dev.example.nativedata.nfc.reader

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import dev.example.nativedata.nfc.detection.DetectedNfcTagHandle
import dev.example.nativedata.nfc.failure.NfcReadFailure
import dev.example.nativedata.nfc.failure.NfcReadFailureFactory
import dev.example.nativedata.nfc.failure.nfcCall
import dev.example.nativedata.nfc.memory.NfcMemoryDataSource
import dev.example.nativedata.nfc.memory.NfcMemoryRange
import dev.example.nativedata.nfc.profile.NfcTagModel
import dev.example.nativedata.nfc.profile.NfcTagProfileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 対象の有効期間・読取排他・Profile選択を、業務カードによらず共通管理する。 */
internal class DefaultNfcCardReader<E : NfcReadFailure>(
    private val target: DetectedNfcTagHandle,
    private val failures: NfcReadFailureFactory<E>,
    private val memoryDataSource: NfcMemoryDataSource,
    private val profileResolver: NfcTagProfileResolver,
) : NfcCardReader<E> {
    /** 同じタグを指す別Readerとも直列化し、終了した対象への新しい接続を拒否する。 */
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
                        memoryDataSource.read(
                            tag = target.access,
                            profile = profile,
                            range = range,
                            failures = failures,
                        )
                    }
                }
            } else {
                Err(failures.targetExpired())
            }
        }
    }
}
