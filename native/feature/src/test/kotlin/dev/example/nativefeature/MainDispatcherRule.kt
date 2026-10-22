package dev.example.nativefeature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** JVMテスト中のMainをテスト用Dispatcherへ差し替え、失敗時も終了処理で元へ戻すJUnit Rule。 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    /** テスト開始時にMainを置き換え、runTestと同じ仮想時間で処理できるようにする。 */
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    /** 後続のテストへ差し替えを持ち越さないよう、Mainの設定を解除する。 */
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
