"use strict";

const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");
const workspace = path.resolve(__dirname, "..");
const read = relative => fs.readFileSync(path.join(workspace, relative), "utf8");
const json = relative => JSON.parse(read(relative));
const digest = bytes => crypto.createHash("sha256").update(bytes).digest("hex");
const noticeFiles = ["cordova-template-NOTICE.txt", "cordova-android-NOTICE.txt",
    "kotlin-NOTICE.txt", "coroutines-NOTICE.txt"];
const licenseFiles = ["Apache-2.0.txt", "cordova-android-LICENSE.txt", "webkit-LICENSE.txt", "kotlin-COPYRIGHT.txt", "attributions.txt",
    "kotlin-boost-LICENSE.txt", "kotlin-gwt-LICENSE.txt", "kotlin-guava-LICENSE.txt"];

function render() {
    const manifest = json("native/third-party-licenses/runtime.json");
    const notices = noticeFiles.map(file => `${file}\n${read(`native/third-party-licenses/${file}`).trimEnd()}\n`).join("\n");
    const components = manifest.components.map(c => `${c.coordinate} — ${c.license}`
        + (c.supplementalLicenses ? `; includes ${c.supplementalLicenses.join(", ")}` : "")
        + `\nLicense evidence: ${c.licensePom}\n`).join("\n");
    const text = "Third-party software notices\n\n"
        + "Apache Cordova Android 14.0.1 / Cordova Hello World template 7.0.0: Apache-2.0\n\n"
        + components + "\n" + notices + "\n"
        + licenseFiles.map(file => `${file}\n${read(`native/third-party-licenses/${file}`).trimEnd()}\n`).join("\n");
    return { "NOTICE": notices, "CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt": text };
}

function checkRuntime(actual, manifest = json("native/third-party-licenses/runtime.json")) {
    const reviewed = new Map(manifest.components.map(c => [c.coordinate, c]));
    for (const variant of manifest.variants) {
        const artifacts = actual[variant];
        if (!Array.isArray(artifacts)) throw new Error(`Missing runtime inventory: ${variant}`);
        if (!artifacts.length) throw new Error(`Empty runtime inventory: ${variant}`);
        for (const artifact of artifacts) {
            const entry = reviewed.get(artifact.coordinate);
            if (!entry || entry.sha256 !== artifact.sha256 || entry.artifact !== path.basename(artifact.path)) {
                throw new Error(`License review required: ${variant}: ${artifact.coordinate}`);
            }
        }
        const coordinates = new Set(artifacts.map(a => a.coordinate));
        if (coordinates.size !== reviewed.size) throw new Error(`License inventory is stale: ${variant}`);
    }
}

function checkSources() {
    const app = json("CordovaComposeApp/package.json");
    const lock = json("CordovaComposeApp/package-lock.json");
    if (Object.keys(app.dependencies || {}).some(name => name !== "cordova-android")
        || Object.keys(app.cordova?.plugins || {}).length) {
        throw new Error("License review required for added application dependencies or Cordova plugins");
    }
    const pluginFile = path.join(workspace, "CordovaComposeApp/plugins/fetch.json");
    if (fs.existsSync(pluginFile) && Object.keys(JSON.parse(fs.readFileSync(pluginFile))).length) {
        throw new Error("License review required for installed Cordova plugins");
    }
    for (const [name, source] of Object.entries(json("native/third-party-licenses/cordova-sources.json"))) {
        if (json(`${source.path}/package.json`).version !== source.version) {
            throw new Error(`License review required: ${name} version changed`);
        }
        if (name === "cordova-android" && lock.packages["node_modules/cordova-android"].version !== source.version) {
            throw new Error("License review required: cordova-android lock version changed");
        }
        for (const [file, hash] of Object.entries(source.files)) {
            if (digest(fs.readFileSync(path.join(workspace, source.path, file))) !== hash) {
                throw new Error(`License review required: ${name}/${file} changed`);
            }
            const copy = source.copies[file];
            if (digest(fs.readFileSync(path.join(workspace, copy))) !== hash) {
                throw new Error(`Bundled upstream license differs: ${copy}`);
            }
        }
    }
}

function checkPreparedAssets(directory) {
    const assets = { "index.html": read("CordovaComposeApp/www/index.html") };
    for (const [relative, contents] of Object.entries(render())) {
        const prefix = "CordovaComposeApp/www/";
        if (relative.startsWith(prefix)) assets[relative.slice(prefix.length)] = contents;
    }
    for (const [relative, contents] of Object.entries(assets)) {
        const file = path.join(directory, relative);
        if (!fs.existsSync(file) || fs.readFileSync(file, "utf8") !== contents) {
            throw new Error(`Stale prepared license asset: ${relative}; run npm run cordova:prepare`);
        }
    }
}

function main(args) {
    const [mode, runtimeFile, assetsDirectory] = args;
    if (!["generate", "check"].includes(mode)) throw new Error("Usage: node scripts/licenses.js generate|check [runtime-artifacts.json] [prepared-www]");
    const outputs = render();
    if (mode === "generate") {
        for (const [relative, contents] of Object.entries(outputs)) {
            const file = path.join(workspace, relative);
            fs.mkdirSync(path.dirname(file), { recursive: true });
            fs.writeFileSync(file, contents);
        }
        console.log("Generated NOTICE and bundled license text");
    } else {
        checkSources();
        for (const [relative, contents] of Object.entries(outputs)) {
            if (read(relative) !== contents) throw new Error(`Stale license output: ${relative}; run npm run licenses:generate`);
        }
        if (runtimeFile) checkRuntime(JSON.parse(fs.readFileSync(runtimeFile, "utf8")));
        if (assetsDirectory) checkPreparedAssets(assetsDirectory);
        console.log(`License checks passed${runtimeFile ? " for resolved Android runtime artifacts" : " (run Gradle verifyThirdPartyLicenses to check runtime dependencies)"}`);
    }
}

module.exports = { checkRuntime, checkPreparedAssets, render };
if (require.main === module) {
    try { main(process.argv.slice(2)); }
    catch (error) { console.error(error.message); process.exitCode = 1; }
}
