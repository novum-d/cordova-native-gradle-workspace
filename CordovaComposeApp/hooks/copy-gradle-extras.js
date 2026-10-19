"use strict";
const fs = require("node:fs");
const path = require("node:path");

const marker = "CORDOVA_NATIVE_GRADLE";
function upsert(file, statement) {
    const start = `// BEGIN ${marker}`;
    const end = `// END ${marker}`;
    const original = fs.existsSync(file) ? fs.readFileSync(file, "utf8") : "";
    const beginAt = original.indexOf(start);
    const endAt = original.indexOf(end);
    if (original.indexOf(start, beginAt + start.length) >= 0 || original.indexOf(end, endAt + end.length) >= 0) {
        throw new Error(`Duplicate integration markers in ${file}`);
    }
    if ((beginAt < 0) !== (endAt < 0) || (beginAt >= 0 && endAt < beginAt)) {
        throw new Error(`Malformed integration markers in ${file}`);
    }
    const block = `${start}\n${statement}\n${end}`;
    const updated = beginAt < 0
        ? `${original}${original.endsWith("\n") || original.length === 0 ? "" : "\n"}${block}\n`
        : original.slice(0, beginAt) + block + original.slice(endAt + end.length);
    if (updated !== original) fs.writeFileSync(file, updated);
}

module.exports = function (context) {
    const selected = context.opts.platforms || context.opts.cordova?.platforms || [];
    if (selected.length && !selected.includes("android")) return;
    const appDir = context.opts.projectRoot;
    const androidDir = path.join(appDir, "platforms/android");
    if (!fs.existsSync(androidDir)) throw new Error("Run cordova platform add android@14.0.1 first");
    const workspace = path.resolve(appDir, "..");
    for (const relative of ["native/gradle/libs.versions.toml", "native/gradle/settings-extras.gradle",
        "native/gradle/build-extras.gradle", "native/feature/build.gradle.kts", "native/data/build.gradle.kts",
        "native/gradle/git-hooks.gradle.kts", "native/gradle/licenses.gradle", "scripts/licenses.sh", "native/gradle/config/detekt/detekt.yml",
        "native/gradle/config/detekt/formatting.yml", "native/gradle/config/detekt/autocorrect.yml", "verification/catalog-verification.gradle"]) {
        if (!fs.existsSync(path.join(workspace, relative))) throw new Error(`Required integration file missing: ${relative}`);
    }
    const packageVersion = require(path.join(appDir, "node_modules/cordova-android/package.json")).version;
    if (!packageVersion.startsWith("14.")) throw new Error(`This integration targets Cordova Android 14, found ${packageVersion}`);
    upsert(path.join(androidDir, "cdv-gradle-name.gradle"),
        'apply from: new File(settingsDir, "../../../native/gradle/settings-extras.gradle")');
    upsert(path.join(androidDir, "app/build-extras.gradle"),
        'apply from: new File(rootDir, "../../../native/gradle/build-extras.gradle")');
    console.log(`[native-gradle] integration restored (${context.hook || "manual"})`);
};
