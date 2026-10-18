import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.nio.file.Files
import java.security.MessageDigest

// 静的解析・フォーマットで利用する依存関係を定義
buildscript {
    val catalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    repositories { mavenCentral() }
    dependencies {
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${catalog.findVersion("detekt").get().requiredVersion}")
        classpath("org.eclipse.jgit:org.eclipse.jgit:${catalog.findVersion("jgit").get().requiredVersion}")
    }
}

// 適用先確認がルートプロジェクトであることを確認
check(project == rootProject) { "Apply git-hooks.gradle.kts to the Gradle root project only" }

// git-hooksの適用済みを確認できるプロパティを定義
extra["nativeGitHooksConfigured"] = true

apply<DetektPlugin>()

// configファイルの存在確認
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val workspaceDir = if (rootDir.name == "android") rootDir.resolve("../../..").canonicalFile else rootDir.parentFile
val detektConfig = workspaceDir.resolve("config/detekt/detekt.yml")
val formattingConfig = workspaceDir.resolve("config/detekt/formatting.yml")
val autoCorrectConfig = workspaceDir.resolve("config/detekt/autocorrect.yml")
check(detektConfig.isFile && formattingConfig.isFile && autoCorrectConfig.isFile) { "Missing config/detekt YAML in $workspaceDir" }

// 各モジュールsrcのファイルツリーを作成
val nativeModules = listOf("feature", "data")
fun nativeKotlinSources(baseDir: File) = files(nativeModules.map { module ->
    fileTree(baseDir.resolve("native/$module/src")) {
        include("**/*.kt")
        exclude("**/build/**", "**/generated/**")
    }
})
val kotlinSources = nativeKotlinSources(workspaceDir)
val stagedSourcesDir = layout.buildDirectory.dir("tmp/git-hooks/staged")
val stagedSources = nativeKotlinSources(stagedSourcesDir.get().asFile)

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
    // リントチェック前にフォーマットを実行する
    dependsOn(detektFormatCheck)
}


val rootCheck = tasks.findByName("check")?.let { tasks.named("check") } ?: tasks.register("check")
rootCheck.configure { dependsOn(ktlintCheck, tasks.named("detekt")) }

fun sourceDigests(): Map<String, String> = kotlinSources.files.sortedBy { it.path }.associate { source ->
    check(source.canonicalFile.toPath().startsWith(workspaceDir.canonicalFile.toPath())) {
        "Refusing to format a source outside the workspace: $source"
    }
    check(!Files.isSymbolicLink(source.toPath())) { "Refusing to format a symbolic link: $source" }

    // ファイルパス to ファイルパスで計算されたハッシュ
    source.relativeTo(workspaceDir).invariantSeparatorsPath to
        MessageDigest.getInstance("SHA-256").digest(source.readBytes()).joinToString("") { "%02x".format(it) }
}

var stagedKotlin = emptyList<String>()
var stagedNativeKotlin = emptyList<String>()
var beforeFormatting = emptyMap<String, String>()
val hookReport = layout.buildDirectory.file("reports/git-hooks/preCommit.txt")

val preparePreCommit = tasks.register("preparePreCommit") {
    group = "verification"
    description = "Snapshots staged native Kotlin with JGit and records working-tree bytes before formatting."
    notCompatibleWithConfigurationCache("Reads the live Git index and passes execution-time state to hook tasks")
    doLast {
        val snapshotDir = stagedSourcesDir.get().asFile
        delete(snapshotDir)
        val builder = FileRepositoryBuilder().findGitDir(workspaceDir)
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
            stagedNativeKotlin = stagedKotlin.filter { path ->
                nativeModules.any { path.startsWith("native/$it/src/") } &&
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
        if (stagedKotlin.isEmpty()) logger.lifecycle("No staged Kotlin files; skipping pre-commit detekt.")
        else logger.lifecycle("Staged Kotlin: ${stagedKotlin.joinToString()}; checking all native Kotlin sources.")
    }
}

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
        check(changedFiles.isEmpty()) {
            "Formatting changed files. 以下のファイルを自動整形したため、コミットを中止しました。\n" +
                changedFiles.joinToString("\n") +
                "\n差分を確認し、コミットに含める内容を再ステージしてください。\n" +
                "  git diff -- <file>\n  git add -- <file>"
        }
    }
}

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
tasks.register("preCommit") {
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
