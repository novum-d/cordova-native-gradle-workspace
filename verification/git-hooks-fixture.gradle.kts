import org.eclipse.jgit.api.Git
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.security.MessageDigest

buildscript {
    val catalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    repositories { mavenCentral() }
    dependencies {
        classpath("org.eclipse.jgit:org.eclipse.jgit:${catalog.findVersion("jgit").get().requiredVersion}")
    }
}

val fixtureDir = rootDir.parentFile
check(fixtureDir.resolve(".git-hook-functional-fixture").isFile) { "Fixture setup may only run in its temporary test repository" }
val scenario = providers.gradleProperty("scenario").get()
val reuseFixture = providers.gradleProperty("reuseFixture").isPresent
val restageFixture = providers.gradleProperty("restageFixture").isPresent
val target = "native/data/src/main/kotlin/fixture/Answer.kt"
val good = "package fixture\n\ninternal fun answer(value: Int): Int = value + 42\n"
fun put(relative: String, content: String) {
    fixtureDir.resolve(relative).apply { parentFile.mkdirs(); writeText(content) }
}

val setup = tasks.register("setupHookFixture") {
    doLast {
        if (reuseFixture) {
            if (restageFixture) {
                Git.open(fixtureDir).use { git -> git.add().addFilepattern(target).call() }
            }
        } else {
            Git.init().setDirectory(fixtureDir).call().use { git ->
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
                        // These cases exercise path handling, independently of the Filename naming rule.
                        val pathGood = if (stagedTarget == target) good else "@file:Suppress(\"Filename\")\n\n$good"
                        put(stagedTarget, pathGood.replace("42\n", "42    \n"))
                        git.add().addFilepattern(stagedTarget).call()
                        if (scenario == "staged-formatting-without-working-file") {
                            check(fixtureDir.resolve(stagedTarget).delete())
                        } else {
                            put(stagedTarget, pathGood)
                        }
                    }
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

tasks.named("preparePreCommit") { dependsOn(setup) }
tasks.named("detektFormatCheck") { dependsOn(setup) }
if (scenario == "analysis-execution-failed") {
    tasks.named("detektPreCommit") {
        doFirst {
            // Even an existing HTML file must not be opened for execution errors.
            put("native/build/reports/detekt/detektPreCommit.html", "STALE REPORT")
            error("Fixture detekt execution failure")
        }
    }
}
