const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const installer = path.join(__dirname, '../scripts/setup-git-hooks.sh');

function run(command, args, cwd, overrides = {}) {
    return spawnSync(command, args, {
        cwd,
        encoding: 'utf8',
        env: {
            ...process.env,
            GIT_CONFIG_NOSYSTEM: '1',
            GIT_CONFIG_GLOBAL: '/dev/null',
            ...overrides,
        },
    });
}

function git(workspace, ...args) {
    const result = run('git', args, workspace);
    assert.equal(result.status, 0, result.stderr);
    return result.stdout.trim();
}

function fixture(t) {
    const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'native-git-hooks-'));
    t.after(() => fs.rmSync(temporary, { recursive: true, force: true }));
    const workspace = path.join(temporary, '日本語 workspace');
    fs.mkdirSync(path.join(workspace, 'scripts'), { recursive: true });
    fs.copyFileSync(installer, path.join(workspace, 'scripts/setup-git-hooks.sh'));
    git(workspace, 'init', '--quiet');
    git(workspace, 'config', 'user.name', 'Hook Test');
    git(workspace, 'config', 'user.email', 'hook@example.invalid');
    git(workspace, 'config', 'commit.gpgSign', 'false');
    const hook = path.join(workspace, '.git/hooks/pre-commit');
    const setup = () => run('sh', [path.join(workspace, 'scripts/setup-git-hooks.sh')], temporary);
    return { workspace, temporary, hook, setup };
}

test('installs an executable hook and can be repeated without replacing it', t => {
    const { hook, setup } = fixture(t);
    assert.equal(setup().status, 0);
    fs.accessSync(hook, fs.constants.X_OK);
    const before = fs.readFileSync(hook, 'utf8');
    const inode = fs.statSync(hook).ino;
    fs.chmodSync(hook, 0o644);
    assert.equal(setup().status, 0);
    assert.equal(fs.readFileSync(hook, 'utf8'), before);
    assert.equal(fs.statSync(hook).ino, inode);
    fs.accessSync(hook, fs.constants.X_OK);
});

test('Git invokes Gradle through with-env and aborts or continues the commit using its exit status', t => {
    const { workspace, temporary, setup } = fixture(t);
    assert.equal(setup().status, 0);
    fs.writeFileSync(path.join(workspace, 'scripts/with-env.sh'),
        '#!/bin/sh\nprintf "%s\\n" "$PWD" "$@" > "$HOOK_CALL_LOG"\nexit "${HOOK_EXIT_CODE:-0}"\n',
        { mode: 0o755 });
    const log = path.join(temporary, 'hook-call.txt');
    const commit = exitCode => run('git', ['commit', '--allow-empty', '-m', 'Hook check'],
        path.join(workspace, 'scripts'), { HOOK_CALL_LOG: log, HOOK_EXIT_CODE: exitCode });
    assert.notEqual(commit('42').status, 0);
    assert.notEqual(run('git', ['rev-parse', '--verify', 'HEAD'], workspace).status, 0);
    assert.deepEqual(fs.readFileSync(log, 'utf8').trimEnd().split('\n'), [
        fs.realpathSync(workspace), './native/gradlew', '-p', 'native', ':preCommit',
    ]);
    assert.equal(commit('0').status, 0);
    assert.equal(git(workspace, 'log', '-1', '--format=%s'), 'Hook check');
});

test('refuses to overwrite an existing custom hook', t => {
    const { hook, setup } = fixture(t);
    const existing = '#!/bin/sh\necho existing hook\n';
    fs.writeFileSync(hook, existing, { mode: 0o644 });
    const result = setup();
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /not overwritten/);
    assert.equal(fs.readFileSync(hook, 'utf8'), existing);
    assert.equal(fs.statSync(hook).mode & 0o777, 0o644);
});

test('refuses to replace a symbolic link', t => {
    const { temporary, hook, setup } = fixture(t);
    const target = path.join(temporary, 'missing-hook');
    fs.symlinkSync(target, hook);
    assert.notEqual(setup().status, 0);
    assert.equal(fs.readlinkSync(hook), target);
    assert.equal(fs.existsSync(target), false);
});

test('preserves core.hooksPath and does not install into an inactive directory', t => {
    const { workspace, hook, setup } = fixture(t);
    git(workspace, 'config', 'core.hooksPath', '.custom-hooks');
    const result = setup();
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /core.hooksPath/);
    assert.equal(git(workspace, 'config', '--get', 'core.hooksPath'), '.custom-hooks');
    assert.equal(fs.existsSync(hook), false);
    assert.equal(fs.existsSync(path.join(workspace, '.custom-hooks')), false);
});
