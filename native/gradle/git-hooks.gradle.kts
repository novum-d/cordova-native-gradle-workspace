import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.eclipse.jgit.api.Git
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
val kotlinSources = files(listOf("feature", "data").map { module ->
    fileTree(workspaceDir.resolve("native/$module/src")) {
        include("**/*.kt")
        exclude("**/build/**", "**/generated/**")
    }
})

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
var beforeFormatting = emptyMap<String, String>()
val hookReport = layout.buildDirectory.file("reports/git-hooks/preCommit.txt")

val preparePreCommit = tasks.register("preparePreCommit") {
    group = "verification"
    description = "Reads the Git index with JGit and snapshots native Kotlin working-tree bytes."
    notCompatibleWithConfigurationCache("Reads the live Git index and passes execution-time state to hook tasks")
    doLast {
        val builder = FileRepositoryBuilder().findGitDir(workspaceDir)
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

val preCommitFormatCheck = tasks.register<Detekt>("detektPreCommitFormatCheck") {
    group = "verification"
    description = "Rejects remaining formatting violations after preCommit auto-correction."
    dependsOn(preCommitFormat)
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

// コミット前、自動整形・静的解析のタスクを設定
tasks.register("preCommit") {
    group = "verification"
    description = "TvApp hook behavior as Gradle tasks: staged gate, format, analyze, reject newly formatted changes."
    dependsOn(preCommitAnalysis)
    notCompatibleWithConfigurationCache("Checks the working tree after the ordered hook pipeline")
    doLast {
        if (stagedKotlin.isEmpty()) return@doLast
        val afterFormatting = sourceDigests()
        val changedFiles = (beforeFormatting.keys + afterFormatting.keys).filter {
            beforeFormatting[it] != afterFormatting[it]
        }.sorted()
        hookReport.get().asFile.appendText(
            "Formatting changed files: ${changedFiles.size}\n" + changedFiles.joinToString("\n") + "\n",
        )
        check(changedFiles.isEmpty()) {
            "Formatting changed files. Review and stage the formatted changes before committing:\n" +
                changedFiles.joinToString("\n")
        }
        logger.lifecycle("preCommit passed. Existing unstaged changes are preserved; the Git index was not updated.")
    }
}
