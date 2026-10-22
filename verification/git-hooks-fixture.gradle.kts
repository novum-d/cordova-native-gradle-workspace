// 機能テスト専用の一時Gitリポジトリに、各シナリオの作業ツリー・indexを構築する。
import org.eclipse.jgit.api.Git
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.security.MessageDigest

// シェルのGitコマンドに頼らず、実際のhookと同じ版のJGitでステージ状態を作る。
buildscript {
    val catalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    repositories { mavenCentral() }
    dependencies {
        classpath("org.eclipse.jgit:org.eclipse.jgit:${catalog.findVersion("jgit").get().requiredVersion}")
    }
}

// マーカーファイルのある一時ディレクトリだけを許可し、本番workspaceへの誤適用を防ぐ。
val fixtureDir = rootDir.parentFile
check(fixtureDir.resolve(".git-hook-functional-fixture").isFile) { "Fixture setup may only run in its temporary test repository" }
val scenario = providers.gradleProperty("scenario").get()
val reuseFixture = providers.gradleProperty("reuseFixture").isPresent
val restageFixture = providers.gradleProperty("restageFixture").isPresent
val target = "native/data/src/main/kotlin/fixture/Answer.kt"
val good = "package fixture\n\ninternal fun answer(value: Int): Int = value + 42\n"
/** 一時リポジトリ配下に親ディレクトリごと検証用ファイルを書き出す。 */
fun put(relative: String, content: String) {
    fixtureDir.resolve(relative).apply { parentFile.mkdirs(); writeText(content) }
}

// 通常は新規のindexを作る。再試行ケースだけは同じindexを再利用し、明示された場合のみ再ステージする。
val setup = tasks.register("setupHookFixture") {
    doLast {
        if (reuseFixture) {
            if (restageFixture) {
                Git.open(fixtureDir).use { git -> git.add().addFilepattern(target).call() }
            }
        } else {
            Git.init().setDirectory(fixtureDir).call().use { git ->
                // 整形・解析違反、部分ステージ、削除、特殊パスを、それぞれ独立した状態として作る。
                when (scenario) {
                    "no-staged-kotlin" -> {
                        put(target, "package fixture\n\ninternal fun answer(value: Int): Int = value + 42    \n")
                        put("README.txt", "Only a non-Kotlin file is staged.\n")
                        git.add().addFilepattern("README.txt").call()
                    }
                    "clean-staged" -> {
                        put(target, good)
                        git.add().addFilepattern(target).call()
                    }
                    "formatting-changed" -> {
                        put(target, "package fixture\n\ninternal fun answer(value: Int): Int = value + 42    \n")
                        git.add().addFilepattern(target).call()
                    }
                    "staged-formatting-only", "staged-formatting-without-working-file", "staged-formatting-spaces-and-unicode", "staged-formatting-html-name" -> {
                        val stagedTarget = when (scenario) {
                            "staged-formatting-spaces-and-unicode" -> "native/data/src/main/kotlin/fixture/日本語 Probe.kt"
                            "staged-formatting-html-name" -> "native/data/src/main/kotlin/fixture/Probe <unsafe>&.kt"
                            else -> target
                        }
                        // ファイル名の命名ルールを抑え、ここでは空白・日本語・HTML特殊文字のパス処理だけを検証する。
                        val pathGood = if (stagedTarget == target) good else "@file:Suppress(\"Filename\")\n\n$good"
                        put(stagedTarget, pathGood.replace("42\n", "42    \n"))
                        git.add().addFilepattern(stagedTarget).call()
                        if (scenario == "staged-formatting-without-working-file") {
                            check(fixtureDir.resolve(stagedTarget).delete())
                        } else {
                            put(stagedTarget, pathGood)
                        }
                    }
                    // 通常indexを整形済みに、代替indexを未整形にし、GIT_INDEX_FILEが尊重されるか調べる。
                    "alternate-index-formatting-only" -> {
                        put(target, good.replace("42\n", "42    \n"))
                        git.add().addFilepattern(target).call()
                        val unformattedIndex = git.repository.indexFile.readBytes()
                        put(target, good)
                        git.add().addFilepattern(target).call()
                        val formattedIndex = git.repository.indexFile.readBytes()
                        fixtureDir.resolve(".git/index").writeBytes(formattedIndex)
                        fixtureDir.resolve("alternate-index").writeBytes(unformattedIndex)
                    }
                    // HEAD・index・作業ツリーの値をすべて変え、未ステージ変更が残るか確認する。
                    "existing-unstaged" -> {
                        put(target, good.replace("42", "40"))
                        git.add().addFilepattern(target).call()
                        git.commit().setMessage("Fixture baseline").setAuthor("Hook Test", "hook@example.invalid")
                            .setCommitter("Hook Test", "hook@example.invalid").setSign(false).call()
                        put(target, good.replace("42", "41"))
                        git.add().addFilepattern(target).call()
                        put(target, good)
                    }
                    "analysis-failed" -> {
                        put(target, "package fixture\n\ninternal fun first(): Nothing {\n    throw Exception(\"first\")\n}\n\ninternal fun second(): Nothing {\n    throw Exception(\"second\")\n}\n")
                        git.add().addFilepattern(target).call()
                    }
                    // 正しいソースに実行・設定のエラーだけを組み合わせ、ルール違反との区別を確認する。
                    "analysis-execution-failed", "invalid-detekt-config" -> {
                        put(target, good)
                        if (scenario == "invalid-detekt-config") {
                            put("native/gradle/config/detekt/detekt.yml", "build: [invalid yaml\n")
                        }
                        git.add().addFilepattern(target).call()
                    }
                    "remaining-formatting-violation" -> {
                        put(target, "package fixture\n\nimport kotlin.collections.*\n\ninternal fun answer(): List<Int> = emptyList()\n")
                        git.add().addFilepattern(target).call()
                    }
                    "data-class-mutable", "data-class-immutable" -> {
                        val declaration = if (scenario == "data-class-mutable") "var" else "val"
                        put(target, "package fixture\n\ninternal data class Answer($declaration count: Int)\n")
                        git.add().addFilepattern(target).call()
                    }
                    "compose-list", "compose-set", "compose-map", "compose-mutable-list", "compose-immutable-collections" -> {
                        val collection = when (scenario) {
                            "compose-set" -> "Set<String>"
                            "compose-map" -> "Map<String, String>"
                            "compose-mutable-list" -> "MutableList<String>"
                            "compose-immutable-collections" -> "ImmutableList<String>"
                            else -> "List<String>"
                        }
                        val source = if (scenario == "compose-immutable-collections") {
                            "package fixture\n\nimport androidx.compose.runtime.Composable\n" +
                                "import kotlinx.collections.immutable.ImmutableList\n" +
                                "import kotlinx.collections.immutable.ImmutableMap\n" +
                                "import kotlinx.collections.immutable.ImmutableSet\n\n" +
                                "@Composable\ninternal fun Items(\n    items: ImmutableList<String>,\n" +
                                "    keys: ImmutableSet<String>,\n    counts: ImmutableMap<String, Int>,\n) {\n" +
                                "    check(items.isNotEmpty() && keys.isNotEmpty() && counts.isNotEmpty())\n}\n"
                        } else {
                            "package fixture\n\nimport androidx.compose.runtime.Composable\n\n" +
                                "@Composable\ninternal fun Items(items: $collection) {\n    check(items.isNotEmpty())\n}\n"
                        }
                        put(target, source)
                        git.add().addFilepattern(target).call()
                    }
                    "deletion-only" -> {
                        put(target, good)
                        git.add().addFilepattern(target).call()
                        git.commit().setMessage("Fixture baseline").setAuthor("Hook Test", "hook@example.invalid")
                            .setCommitter("Hook Test", "hook@example.invalid").setSign(false).call()
                        git.rm().addFilepattern(target).call()
                    }
                    "spaces-and-unicode" -> {
                        val special = "native/data/src/main/kotlin/fixture/日本語 Probe.kt"
                        put(special, "@file:Suppress(\"Filename\")\n\n$good")
                        git.add().addFilepattern(special).call()
                    }
                    "unstaged-formatting-changed" -> {
                        put(target, good)
                        git.add().addFilepattern(target).call()
                        put("native/feature/src/main/kotlin/fixture/Other.kt", "package fixture\n\ninternal fun other(value: Int): Int = value + 1    \n")
                    }
                    "check-does-not-format" -> {
                        put(target, "package fixture\n\ninternal fun answer(value: Int): Int = value + 42    \n")
                        git.add().addFilepattern(target).call()
                    }
                    else -> error("Unknown scenario $scenario")
                }
            }
        }
        // hook実行前のindexを記録し、検証後にPython側でバイト単位の不変性を照合する。
        val indexFile = if (scenario == "alternate-index-formatting-only") "alternate-index" else ".git/index"
        val index = fixtureDir.resolve(indexFile).readBytes()
        fixtureDir.resolve("index-before.sha256").writeText(
            MessageDigest.getInstance("SHA-256").digest(index).joinToString("") { "%02x".format(it) },
        )
        if (scenario == "alternate-index-formatting-only") {
            fixtureDir.resolve("default-index-before.sha256").writeText(
                MessageDigest.getInstance("SHA-256").digest(fixtureDir.resolve(".git/index").readBytes())
                    .joinToString("") { "%02x".format(it) },
            )
        }
    }
}

// 実際のhookを起動する直前にfixtureを構築する。読み取り専用checkも同じ準備を使う。
tasks.named("preparePreCommit") { dependsOn(setup) }
tasks.named("detektFormatCheck") { dependsOn(setup) }
if (scenario == "analysis-execution-failed") {
    tasks.named("detektPreCommit") {
        doFirst {
            // HTMLが存在しても、解析の実行エラーでそれを開かないことを確認する。
            put("native/build/reports/detekt/detektPreCommit.html", "STALE REPORT")
            error("Fixture detekt execution failure")
        }
    }
}
