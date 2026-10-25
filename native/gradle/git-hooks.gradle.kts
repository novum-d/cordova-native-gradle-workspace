// native・Cordova共通の品質タスク。通常checkは読み取り専用、preCommitだけが作業ツリーを整形する。
// 整形したら確認・再ステージを促して停止し、再試行時もindexの内容を別途検査する。
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.FileSystemOperations
import org.gradle.process.ExecOperations
import java.nio.file.Files
import java.security.MessageDigest
import javax.inject.Inject

/** タスクから使うファイル操作・外部プロセス実行を、Gradleのサービスとして受け取る。 */
interface GitHookServices {
    @get:Inject val files: FileSystemOperations
    @get:Inject val processes: ExecOperations
}

// 静的解析・フォーマットで利用する依存関係を定義
buildscript {
    val catalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    repositories { mavenCentral() }
    dependencies {
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${catalog.findVersion("detekt").get().requiredVersion}")
        classpath("org.eclipse.jgit:org.eclipse.jgit:${catalog.findVersion("jgit").get().requiredVersion}")
    }
}

// タスク名・対象ソース・Gitの状態を一元管理するため、Gradleルートへの適用だけを許可する。
check(project == rootProject) { "Apply git-hooks.gradle.kts to the Gradle root project only" }

// Cordovaの共有extrasが重ねて適用しないよう、ルートに適用済みの印を残す。
extra["nativeGitHooksConfigured"] = true

apply<DetektPlugin>()

// 呼び出し元がnativeかCordovaのplatformかを判別し、同じworkspaceの設定を参照する。
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val workspaceDir = if (rootDir.name == "android") rootDir.resolve("../../..").canonicalFile else rootDir.parentFile
val detektConfig = workspaceDir.resolve("native/gradle/config/detekt/detekt.yml")
val formattingConfig = workspaceDir.resolve("native/gradle/config/detekt/formatting.yml")
val autoCorrectConfig = workspaceDir.resolve("native/gradle/config/detekt/autocorrect.yml")
check(detektConfig.isFile && formattingConfig.isFile && autoCorrectConfig.isFile) { "Missing native/gradle/config/detekt YAML in $workspaceDir" }

// Library・開発ホスト・Cordovaプラグインの管理元へ同じ検査を適用し、生成先は重複検査しない。
val kotlinSourceRoots = listOf("native/feature/src", "native/data/src", "native/dev-app/src", "kotlin-plugin/src")
// 本体・テストを含むsrc全体を対象とし、生成ディレクトリは解析から外す。
fun nativeKotlinSources(baseDir: File) = files(kotlinSourceRoots.map { relative ->
    fileTree(baseDir.resolve(relative)) {
        include("**/*.kt")
        exclude("**/build/**", "**/generated/**")
    }
})
val kotlinSources = nativeKotlinSources(workspaceDir)
// 作業ツリーとindexの内容を混同しないよう、ステージ済みソースはbuild/tmpへ別に取り出す。
val stagedSourcesDir = layout.buildDirectory.dir("tmp/git-hooks/staged")
val stagedSources = nativeKotlinSources(stagedSourcesDir.get().asFile)
val hookServices = objects.newInstance<GitHookServices>()
val detektReportsDir = layout.buildDirectory.dir("reports/detekt")
val preCommitDetektNames = listOf(
    "detektPreCommitFormat", "detektPreCommitFormatCheck", "detektPreCommit", "detektPreCommitStagedFormatCheck",
)
// 表示設定は呼び出しごとの環境から取得する。CIや画面のないLinuxではブラウザーを起動しない。
val openReport = providers.gradleProperty("openPreCommitReport").map(String::toBoolean).orElse(true)
val onCi = providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) || it == "1" }.orElse(false)
val osName = System.getProperty("os.name").lowercase()
val display = providers.environmentVariable("DISPLAY")
val waylandDisplay = providers.environmentVariable("WAYLAND_DISPLAY")
val executablePath = providers.environmentVariable("PATH")

// detektのプラグインを追加（フォーマッター・Composeのルール）
dependencies {
    add("detektPlugins", catalog.findLibrary("detekt-formatting").get())
    add("detektPlugins", catalog.findLibrary("detekt-twitter-compose").get())
}

configure<DetektExtension> {
    toolVersion = catalog.findVersion("detekt").get().requiredVersion
    // デフォルトルールの適用
    buildUponDefaultConfig = true
    // 独自のルールを重ねる
    config.setFrom(detektConfig)
    // 解析・フォーマットの対象を設定
    source.setFrom(kotlinSources)
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = catalog.findVersion("jvm-target").get().requiredVersion
    setSource(kotlinSources)
    // ルール違反時にビルドを失敗させる
    ignoreFailures = false
    // htmlで出力
    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/$name.html"))
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

// 手動整形用。入力をその場で書き換えるため、以前の実行結果を使って省略しない。
val detektFormat = tasks.register<Detekt>("detektFormat") {
    description = "Auto-corrects native Kotlin formatting using the TvApp rule configuration."
    group = "verification"
    config.setFrom(formattingConfig, autoCorrectConfig)
    buildUponDefaultConfig = false
    autoCorrect = true
    // 以前の出力を再利用して実行を省略する判定を無効にする
    outputs.upToDateWhen { false }
    // タスク結果のビルドキャッシュを無効にする
    outputs.cacheIf { false }
}

// 通常check用。修正はせず、整形ルール違反が残っていれば失敗させる。
val detektFormatCheck = tasks.register<Detekt>("detektFormatCheck") {
    description = "Checks native Kotlin formatting without changing files."
    group = "verification"
    config.setFrom(formattingConfig)
    buildUponDefaultConfig = false
    autoCorrect = false
}
val ktlintCheck = tasks.register("ktlintCheck") {
    description = "Compatibility alias for detektFormatCheck, matching TvApp."
    group = "verification"
    // 既存コマンドとの互換名。依存先は自動整形ではなく、読み取り専用の整形チェック。
    dependsOn(detektFormatCheck)
}


// 両ホストでルートのcheckを用意し、整形チェックと通常の静的解析を結び付ける。
val rootCheck = tasks.findByName("check")?.let { tasks.named("check") } ?: tasks.register("check")
rootCheck.configure { dependsOn(ktlintCheck, tasks.named("detekt")) }

// パスごとの実ファイル内容のハッシュを取り、フォーマット前後の変更・追加・削除を判定する。
fun sourceDigests(): Map<String, String> = kotlinSources.files.sortedBy { it.path }.associate { source ->
    check(source.canonicalFile.toPath().startsWith(workspaceDir.canonicalFile.toPath())) {
        "Refusing to format a source outside the workspace: $source"
    }
    check(!Files.isSymbolicLink(source.toPath())) { "Refusing to format a symbolic link: $source" }

    // workspace相対パスをキー、ファイルの内容から計算したSHA-256を値にする。
    source.relativeTo(workspaceDir).invariantSeparatorsPath to
        MessageDigest.getInstance("SHA-256").digest(source.readBytes()).joinToString("") { "%02x".format(it) }
}

// 以下は1回のpreCommit中だけ共有する実行時状態。生きたindexを読むため構成キャッシュの対象外。
var stagedKotlin = emptyList<String>()
var stagedNativeKotlin = emptyList<String>()
var beforeFormatting = emptyMap<String, String>()
var preCommitPrepared = false
var formattingChanged = false
val hookReport = layout.buildDirectory.file("reports/git-hooks/preCommit.txt")

// indexの内容と整形前の作業ツリーを保存する。ここから始まる処理でgit addは実行しない。
val preparePreCommit = tasks.register("preparePreCommit") {
    group = "verification"
    description = "Snapshots staged native Kotlin with JGit and records working-tree bytes before formatting."
    notCompatibleWithConfigurationCache("Reads the live Git index and passes execution-time state to hook tasks")
    doLast {
        val snapshotDir = stagedSourcesDir.get().asFile
        preCommitPrepared = false
        formattingChanged = false
        // 古いレポートと以前の独自一覧を削除し、今回の結果だけを表示対象にする。
        hookServices.files.delete {
            delete(
                listOf(snapshotDir, layout.buildDirectory.file("reports/git-hooks/preCommit.html").get().asFile) +
                    preCommitDetektNames.map { detektReportsDir.get().file("$it.html").asFile },
            )
        }
        val builder = FileRepositoryBuilder().findGitDir(workspaceDir)
        // Gitが代替indexを指定したコミットでも、既定indexを読んで検査をすり抜けないようにする。
        System.getenv("GIT_INDEX_FILE")?.takeIf { it.isNotEmpty() }?.let { indexPath ->
            builder.setIndexFile(workspaceDir.resolve(indexPath))
        }

        check(builder.gitDir != null) { "No Git repository found for $workspaceDir" }

        builder.build().use { repository ->
            check(repository.workTree.canonicalFile == workspaceDir.canonicalFile) {
                "Git work tree must be the workspace root: $workspaceDir"
            }
            Git(repository).use { git ->
                val status = git.status().call()
                check(status.conflicting.isEmpty()) { "Resolve Git conflicts before running preCommit" }
                stagedKotlin = (status.added + status.changed).filter { it.endsWith(".kt") }.sorted()
            }
            // Kotlinのステージがあれば全対象を検査し、index内容はLibraryとプラグインの管理元へ絞る。
            stagedNativeKotlin = stagedKotlin.filter { path ->
                kotlinSourceRoots.any { path.startsWith("$it/") } &&
                    path.split('/').none { it == "build" || it == "generated" }
            }
            // 作業ファイルではなく、indexが指す内容を読み取り専用で取り出す
            val index = repository.readDirCache()
            stagedNativeKotlin.forEach { path ->
                val entry = checkNotNull(index.getEntry(path)) { "Staged file disappeared from the Git index: $path" }
                check(entry.fileMode == FileMode.REGULAR_FILE || entry.fileMode == FileMode.EXECUTABLE_FILE) {
                    "Staged Kotlin must be a regular file: $path"
                }
                val destination = snapshotDir.resolve(path).canonicalFile
                check(destination.toPath().startsWith(snapshotDir.canonicalFile.toPath())) {
                    "Staged file must stay inside the snapshot directory: $path"
                }
                destination.parentFile.mkdirs()
                repository.open(entry.objectId).openStream().use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
        beforeFormatting = if (stagedKotlin.isEmpty()) emptyMap() else sourceDigests()
        hookReport.get().asFile.apply {
            parentFile.mkdirs()
            writeText("Staged Kotlin: ${stagedKotlin.size}\n" + stagedKotlin.joinToString("\n") + "\n")
        }
        preCommitPrepared = true
        if (stagedKotlin.isEmpty()) logger.lifecycle("No staged Kotlin files; skipping pre-commit detekt.")
        else logger.lifecycle("Staged Kotlin: ${stagedKotlin.joinToString()}; checking all native Kotlin sources.")
    }
}

// indexを保持したまま作業ツリーを自動整形し、次のタスクで変更の有無を判定する。
val preCommitFormat = tasks.register<Detekt>("detektPreCommitFormat") {
    group = "verification"
    description = "Formatting phase of preCommit."
    dependsOn(preparePreCommit)
    onlyIf("staged Kotlin files exist") { stagedKotlin.isNotEmpty() }
    config.setFrom(formattingConfig, autoCorrectConfig)
    buildUponDefaultConfig = false
    autoCorrect = true
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    notCompatibleWithConfigurationCache("Uses the live pre-commit snapshot")
}

// 1ファイルでも内容が変わればコミットを止める。自動ステージせず、利用者に差分確認を任せる。
val checkPreCommitFormattingChanges = tasks.register("checkPreCommitFormattingChanges") {
    group = "verification"
    description = "Stops after auto-formatting so the user can review and restage the changes."
    dependsOn(preCommitFormat)
    onlyIf("staged Kotlin files exist") { stagedKotlin.isNotEmpty() }
    notCompatibleWithConfigurationCache("Compares the live working tree with the pre-formatting snapshot")
    doLast {
        val afterFormatting = sourceDigests()
        val changedFiles = (beforeFormatting.keys + afterFormatting.keys).filter {
            beforeFormatting[it] != afterFormatting[it]
        }.sorted()
        hookReport.get().asFile.appendText(
            "Formatting changed files: ${changedFiles.size}\n" + changedFiles.joinToString("\n") + "\n",
        )
        formattingChanged = changedFiles.isNotEmpty()
        check(changedFiles.isEmpty()) {
            "Formatting changed files. 以下のファイルを自動整形したため、コミットを中止しました。\n" +
                changedFiles.joinToString("\n") +
                "\n差分を確認し、コミットに含める内容を再ステージしてください。\n" +
                "  git diff -- <file>\n  git add -- <file>"
        }
    }
}

// 自動修正できない整形違反も残さないよう、作業ツリーを読み取り専用で再検査する。
val preCommitFormatCheck = tasks.register<Detekt>("detektPreCommitFormatCheck") {
    group = "verification"
    description = "Rejects remaining formatting violations after preCommit auto-correction."
    dependsOn(checkPreCommitFormattingChanges)
    onlyIf("staged Kotlin files exist") { stagedKotlin.isNotEmpty() }
    config.setFrom(formattingConfig)
    buildUponDefaultConfig = false
    autoCorrect = false
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    notCompatibleWithConfigurationCache("Uses the live pre-commit snapshot")
}

// 整形の確認が終わった後に、Compose・不変性などを含む通常の解析ルールを実行する。
val preCommitAnalysis = tasks.register<Detekt>("detektPreCommit") {
    group = "verification"
    description = "Static analysis phase of preCommit, after formatting."
    dependsOn(preCommitFormatCheck)
    onlyIf("staged Kotlin files exist") { stagedKotlin.isNotEmpty() }
    config.setFrom(detektConfig)
    buildUponDefaultConfig = true
    autoCorrect = false
    outputs.upToDateWhen { false }
    notCompatibleWithConfigurationCache("Uses the live pre-commit snapshot")
}

// 作業ツリーが整形済みでもindexが古い場合を拒否する。再ステージしない再試行でも必ず検査する。
val preCommitStagedFormatCheck = tasks.register<Detekt>("detektPreCommitStagedFormatCheck") {
    group = "verification"
    description = "Checks the staged native Kotlin contents without changing the Git index or working files."
    dependsOn(preCommitAnalysis)
    onlyIf("staged native Kotlin files exist") { stagedNativeKotlin.isNotEmpty() }
    setSource(stagedSources)
    config.setFrom(formattingConfig)
    buildUponDefaultConfig = false
    autoCorrect = false
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    notCompatibleWithConfigurationCache("Checks the live Git index snapshot")
    doFirst {
        logger.lifecycle(
            "ステージ済みのKotlinを検査します。整形違反がある場合は、対象ファイルの差分を確認し、" +
                "コミットに含める内容を再ステージしてください（git diff -- <file> / git add -- <file>）。",
        )
    }
}

// コミット前、自動整形・静的解析のタスクを設定
val preCommit = tasks.register("preCommit") {
    group = "verification"
    description = "Formats and analyzes native Kotlin, and rejects unformatted staged contents on every attempt."
    dependsOn(preCommitStagedFormatCheck)
    notCompatibleWithConfigurationCache("Uses the live Git index and working-tree state")
    doLast {
        if (stagedKotlin.isEmpty()) return@doLast
        hookReport.get().asFile.appendText("Staged native Kotlin formatting passed: ${stagedNativeKotlin.size}\n")
        logger.lifecycle("preCommit passed. Existing unstaged changes are preserved; the Git index was not updated.")
    }
}

val preCommitPhases = listOf(
    preparePreCommit, preCommitFormat, checkPreCommitFormattingChanges,
    preCommitFormatCheck, preCommitAnalysis, preCommitStagedFormatCheck, preCommit,
)
val preCommitDetektTasks = listOf(preCommitFormat, preCommitFormatCheck, preCommitAnalysis, preCommitStagedFormatCheck)

// detekt 1.23.8の隔離CLIで発生した原因例外を、型名で照合してルール違反と識別する。
// 同じ型を直接参照するためだけに、detekt-toolingをbuildscriptのclasspathへ追加しない。
fun Detekt.failedWithRuleViolations(): Boolean = generateSequence(state.failure) { it.cause }.any {
    it.javaClass.name == "io.github.detekt.tooling.api.MaxIssuesReached"
}

// ルール違反に対応するdetekt標準HTMLだけを開く。設定破損や実行エラーでは古いHTMLを開かない。
val showPreCommitReport = tasks.register("showPreCommitReport") {
    group = "verification"
    description = "Opens detekt's own HTML report only when preCommit finds rule violations."
    onlyIf("preCommit found formatting or analysis rule violations") {
        preCommitPrepared && stagedKotlin.isNotEmpty() &&
            (formattingChanged || preCommitDetektTasks.any { it.get().failedWithRuleViolations() })
    }
    // prepareに登録したfinalizerが、後続フェーズを追い越して表示しないよう最後まで待たせる。
    mustRunAfter(preCommitPhases)
    notCompatibleWithConfigurationCache("Reads live pre-commit failures and opens a desktop report")
    doLast {
        val failedAnalysis = preCommitDetektTasks.map { it.get() }.firstOrNull { it.failedWithRuleViolations() }
        val reportTask = failedAnalysis ?: preCommitFormat.get()
        val report = reportTask.reports.html.outputLocation.get().asFile
        if (!report.isFile) {
            logger.warn("detekt HTML report is unavailable: $report")
            return@doLast
        }
        logger.lifecycle("detekt HTML report: ${report.toURI().toASCIIString()}")
        if (!openReport.get() || onCi.get()) return@doLast
        val command = when {
            osName.startsWith("mac") -> listOf("open", report.absolutePath)
            osName.startsWith("windows") -> listOf("rundll32.exe", "url.dll,FileProtocolHandler", report.toURI().toASCIIString())
            osName.startsWith("linux") && (display.orNull?.isNotBlank() == true || waylandDisplay.orNull?.isNotBlank() == true) ->
                listOf("xdg-open", report.absolutePath)
            else -> {
                logger.lifecycle("Desktop browser is unavailable; open the HTML report using the link above.")
                return@doLast
            }
        }
        try {
            // 長時間動作するGradle Daemonの古いPATHを使わず、今回の呼び出しのPATHから探す。
            val executable = executablePath.orNull.orEmpty().split(File.pathSeparator)
                .map { File(it, command.first()) }.firstOrNull { it.isFile && it.canExecute() }
            if (executable == null) {
                logger.warn("Could not find ${command.first()}; use the HTML report link above.")
                return@doLast
            }
            var result = hookServices.processes.exec {
                commandLine(listOf(executable.absolutePath) + command.drop(1))
                isIgnoreExitValue = true
            }
            // macOSの既定アプリで開けない場合だけSafariを試し、表示失敗で解析結果を上書きしない。
            if (result.exitValue != 0 && osName.startsWith("mac")) {
                logger.lifecycle("Default HTML application could not open the report; trying Safari.")
                result = hookServices.processes.exec {
                    commandLine(executable.absolutePath, "-a", "Safari", report.absolutePath)
                    isIgnoreExitValue = true
                }
            }
            if (result.exitValue == 0) logger.lifecycle("Opened detekt HTML report.")
            else logger.warn("Could not open detekt HTML report (exit ${result.exitValue}); use the link above.")
        } catch (error: Exception) {
            logger.warn("Could not open detekt HTML report; use the link above. ${error.message}")
        }
    }
}
// 後続フェーズが失敗しても表示判定へ進めるよう、最初の準備タスクのfinalizerに登録する。
preparePreCommit.configure { finalizedBy(showPreCommitReport) }
