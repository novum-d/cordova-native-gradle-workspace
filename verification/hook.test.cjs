const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const hook = require('../CordovaComposeApp/hooks/copy-gradle-extras.js');

function fixture(t) {
    const workspace = fs.mkdtempSync(path.join(os.tmpdir(), 'cordova-native-hook-'));
    t.after(() => fs.rmSync(workspace, { recursive: true, force: true }));
    const app = path.join(workspace, 'CordovaComposeApp');
    for (const relative of ['native/gradle/libs.versions.toml', 'native/gradle/settings-extras.gradle',
        'native/gradle/build-extras.gradle', 'native/feature/build.gradle.kts', 'native/data/build.gradle.kts',
        'native/gradle/git-hooks.gradle.kts', 'config/detekt/detekt.yml',
        'config/detekt/formatting.yml', 'config/detekt/autocorrect.yml', 'verification/catalog-verification.gradle', 'CordovaComposeApp/platforms/android/settings.gradle',
        'CordovaComposeApp/platforms/android/app/build-extras.gradle',
        'CordovaComposeApp/node_modules/cordova-android/package.json']) {
        const file = path.join(workspace, relative);
        fs.mkdirSync(path.dirname(file), { recursive: true });
        fs.writeFileSync(file, file.endsWith('package.json') ? '{"version":"14.0.1"}' : '// existing plugin integration\n');
    }
    fs.writeFileSync(path.join(app, 'platforms/android/cdv-gradle-name.gradle'), 'rootProject.name = "ExistingApp"\n');
    return { workspace, app, context: { hook: 'test', opts: { projectRoot: app, platforms: ['android'] } } };
}

test('preserves existing plugin settings and is byte-identical after repeated runs', t => {
    const { app, context } = fixture(t);
    const settings = path.join(app, 'platforms/android/settings.gradle');
    const name = path.join(app, 'platforms/android/cdv-gradle-name.gradle');
    const extras = path.join(app, 'platforms/android/app/build-extras.gradle');
    const originalSettings = fs.readFileSync(settings, 'utf8');
    hook(context);
    const first = [name, extras].map(file => fs.readFileSync(file, 'utf8'));
    hook(context);
    hook(context);
    assert.deepEqual([name, extras].map(file => fs.readFileSync(file, 'utf8')), first);
    assert.equal(fs.readFileSync(settings, 'utf8'), originalSettings);
    assert.match(first[0], /ExistingApp/);
    assert.match(first[1], /existing plugin integration/);
    assert.equal((first[0].match(/BEGIN CORDOVA_NATIVE_GRADLE/g) || []).length, 1);
});

test('fails before modifying generated files when a native module is missing', t => {
    const { workspace, app, context } = fixture(t);
    const name = path.join(app, 'platforms/android/cdv-gradle-name.gradle');
    const before = fs.readFileSync(name, 'utf8');
    fs.unlinkSync(path.join(workspace, 'native/data/build.gradle.kts'));
    assert.throws(() => hook(context), /Required integration file missing/);
    assert.equal(fs.readFileSync(name, 'utf8'), before);
});

test('rejects damaged markers instead of erasing unrelated Gradle content', t => {
    const { app, context } = fixture(t);
    const extras = path.join(app, 'platforms/android/app/build-extras.gradle');
    fs.writeFileSync(extras, '// BEGIN CORDOVA_NATIVE_GRADLE\n// existing custom plugin');
    assert.throws(() => hook(context), /Malformed integration markers/);
    assert.match(fs.readFileSync(extras, 'utf8'), /existing custom plugin/);
});

test('does not touch Android for a non-Android prepare', () => {
    hook({ opts: { projectRoot: '/not-an-android-project', platforms: ['ios'] } });
});
