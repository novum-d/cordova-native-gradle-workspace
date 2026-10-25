/** Cordovaの生成物に、共有Gradle設定を読み込む管理ブロックを復元するhook。 */
"use strict";
const fs = require("node:fs");
const path = require("node:path");

const marker = "CORDOVA_NATIVE_GRADLE";
/** 管理マーカー内だけを差し替え、内容を検証して更新有無を返す。重複・破損したマーカーは拒否する。 */
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
    // 内容が同じなら書き込まず、prepareの繰り返しで差分や更新時刻を増やさない。
    const block = `${start}\n${statement}\n${end}`;
    const updated = beginAt < 0
        ? `${original}${original.endsWith("\n") || original.length === 0 ? "" : "\n"}${block}\n`
        : original.slice(0, beginAt) + block + original.slice(endAt + end.length);
    if (updated !== original) fs.writeFileSync(file, updated);
    // 完了ログを出す前に実ファイルを読み戻し、参照が登録されたことを確かめる。
    if (fs.readFileSync(file, "utf8") !== updated) {
        throw new Error(`Integration file content did not match after writing: ${file}`);
    }
    return updated !== original;
}

/** Android対象のprepare／compileで、必要ファイルと対応版を確認してから参照を登録する。 */
module.exports = function (context) {
    const selected = context.opts.platforms || context.opts.cordova?.platforms || [];
    if (selected.length && !selected.includes("android")) return;
    const appDir = path.resolve(context.opts.projectRoot);
    // 実行したアプリと読み込んだhookを表示し、別checkout・別アプリへの出力を判別できるようにする。
    console.log(`[native-gradle] project root: ${appDir}`);
    console.log(`[native-gradle] hook script: ${__filename}`);
    const androidDir = path.join(appDir, "platforms/android");
    if (!fs.existsSync(androidDir)) throw new Error("Run cordova platform add android@14.0.1 first");
    const workspace = path.resolve(appDir, "..");
    // 書き換え前に管理元を検査し、ファイル不足の状態で生成側だけを進めない。
    for (const relative of ["native/gradle/libs.versions.toml", "native/gradle/settings-extras.gradle",
        "native/gradle/build-extras.gradle", "native/feature/build.gradle.kts", "native/data/build.gradle.kts",
        "native/gradle/git-hooks.gradle.kts", "native/gradle/config/detekt/detekt.yml",
        "native/gradle/config/detekt/formatting.yml", "native/gradle/config/detekt/autocorrect.yml"]) {
        if (!fs.existsSync(path.join(workspace, relative))) throw new Error(`Required integration file missing: ${relative}`);
    }
    const packageVersion = require(path.join(appDir, "node_modules/cordova-android/package.json")).version;
    if (!packageVersion.startsWith("14.")) throw new Error(`This integration targets Cordova Android 14, found ${packageVersion}`);
    // CatalogはSettings評価時、app依存はProject評価時に必要なので、入口を分けて登録する。
    const targets = [
        ["cdv-gradle-name.gradle", 'apply from: new File(settingsDir, "../../../native/gradle/settings-extras.gradle")'],
        ["app/build-extras.gradle", 'apply from: new File(rootDir, "../../../native/gradle/build-extras.gradle")'],
    ];
    for (const [relative, statement] of targets) {
        const file = path.join(androidDir, relative);
        const changed = upsert(file, statement);
        // シンボリックリンク先も含む実際の出力パスを、更新不要だった場合にも表示する。
        console.log(`[native-gradle] ${changed ? "updated" : "unchanged"}: ${fs.realpathSync(file)}`);
    }
    console.log(`[native-gradle] integration restored (${context.hook || "manual"})`);
};
