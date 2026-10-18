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
                    "staged-formatting-only", "staged-formatting-without-working-file", "staged-formatting-spaces-and-unicode" -> {
                        val stagedTarget = if (scenario == "staged-formatting-spaces-and-unicode") {
                            "native/data/src/main/kotlin/fixture/日本語 Probe.kt"
                        } else target
                        put(stagedTarget, good.replace("42\n", "42    \n"))
                        git.add().addFilepattern(stagedTarget).call()
                        if (scenario == "staged-formatting-without-working-file") {
                            check(fixtureDir.resolve(stagedTarget).delete())
                        } else {
                            put(stagedTarget, good)
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
                    "deletion-only" -> {
                        put(target, good)
                        git.add().addFilepattern(target).call()
                        git.commit().setMessage("Fixture baseline").setAuthor("Hook Test", "hook@example.invalid")
                            .setCommitter("Hook Test", "hook@example.invalid").setSign(false).call()
                        git.rm().addFilepattern(target).call()
                    }
                    "spaces-and-unicode" -> {
                        val special = "native/data/src/main/kotlin/fixture/日本語 Probe.kt"
                        put(special, good)
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
