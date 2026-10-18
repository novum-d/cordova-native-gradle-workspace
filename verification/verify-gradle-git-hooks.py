"""Exercises the real Kotlin DSL hook in disposable repositories. No Git/Detekt CLI or local tool JAR is used."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import tempfile

workspace = Path(__file__).resolve().parent.parent
cases = {
    'no-staged-kotlin': (True, 'No staged Kotlin files'),
    'clean-staged': (True, 'preCommit passed'),
    'formatting-changed': (False, 'Formatting changed files'),
    'existing-unstaged': (True, 'preCommit passed'),
    'analysis-failed': (False, 'TooGenericExceptionThrown'),
    'deletion-only': (True, 'No staged Kotlin files'),
    'spaces-and-unicode': (True, 'preCommit passed'),
    'unstaged-formatting-changed': (False, 'Formatting changed files'),
    'check-does-not-format': (False, 'detektFormatCheck FAILED'),
    'staged-formatting-only': (False, 'detektPreCommitStagedFormatCheck FAILED'),
    'staged-formatting-without-working-file': (False, 'detektPreCommitStagedFormatCheck FAILED'),
    'staged-formatting-spaces-and-unicode': (False, 'detektPreCommitStagedFormatCheck FAILED'),
    'alternate-index-formatting-only': (False, 'detektPreCommitStagedFormatCheck FAILED'),
}
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output-dir', type=Path, default=workspace / 'docs/git-hook-tests')
logs = parser.parse_args().output_dir.resolve()
logs.mkdir(parents=True, exist_ok=True)
results = []


def run_case(fixture, scenario, should_pass, expected, label=None, properties=()):
    label = label or scenario
    native = fixture / 'native'
    task = 'detektFormatCheck' if scenario == 'check-does-not-format' else 'preCommit'
    command = [str(workspace / 'scripts/with-env.sh'), str(workspace / 'native/gradlew'),
               '-p', str(native), f'-Pscenario={scenario}', *properties, task,
               '--console=plain', '--no-watch-fs', '--build-cache']
    environment = dict(os.environ)
    if scenario == 'alternate-index-formatting-only':
        environment['GIT_INDEX_FILE'] = str(fixture / 'alternate-index')
    result = subprocess.run(command, cwd=workspace, env=environment, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, timeout=240)
    (logs / f'{label}.log').write_text(result.stdout)
    relative = 'native/data/src/main/kotlin/fixture/' + (
        '日本語 Probe.kt' if scenario == 'staged-formatting-spaces-and-unicode' else 'Answer.kt')
    target = fixture / relative
    snapshot = native / 'build/tmp/git-hooks/staged' / relative
    sources = {}
    if target.is_file():
        sources['working_tree_source'] = target.read_text()
    if snapshot.is_file():
        sources['staged_source_snapshot'] = snapshot.read_text()
    (logs / f'{label}-after.json').write_text(json.dumps(sources, indent=2) + '\n')
    index = fixture / ('alternate-index' if scenario == 'alternate-index-formatting-only' else '.git/index')
    index_before = fixture / 'index-before.sha256'
    unchanged = index.is_file() and index_before.is_file() and (
        hashlib.sha256(index.read_bytes()).hexdigest() == index_before.read_text())
    passed = (result.returncode == 0) == should_pass and expected in result.stdout and unchanged
    if scenario in ['no-staged-kotlin', 'check-does-not-format']:
        passed = passed and 'value + 42    ' in sources.get('working_tree_source', '')
    if scenario == 'existing-unstaged':
        passed = passed and 'value + 42\n' in sources.get('working_tree_source', '')
        passed = passed and 'value + 41\n' in sources.get('staged_source_snapshot', '')
    if scenario.startswith('staged-formatting-') or scenario == 'alternate-index-formatting-only' or label == 'retry-without-restaging':
        passed = passed and 'value + 42    ' in sources.get('staged_source_snapshot', '')
        if scenario == 'staged-formatting-without-working-file':
            passed = passed and not target.exists()
        else:
            passed = passed and 'value + 42\n' in sources.get('working_tree_source', '')
    if scenario == 'alternate-index-formatting-only':
        passed = passed and hashlib.sha256((fixture / '.git/index').read_bytes()).hexdigest() == (
            fixture / 'default-index-before.sha256').read_text()
    if label == 'retry-after-restaging':
        passed = passed and sources.get('working_tree_source') == sources.get('staged_source_snapshot')
    results.append({'scenario': label, 'passed': passed, 'exit_code': result.returncode,
                    'expected_success': should_pass, 'git_index_unchanged': unchanged})
    print(f'{"PASS" if passed else "FAIL"}: {label} (exit {result.returncode}, index unchanged={unchanged})', flush=True)
    if not passed:
        print(result.stdout[-8000:], flush=True)


with tempfile.TemporaryDirectory(prefix='gradle-git-hook-tests-') as temporary:
    for scenario, (should_pass, expected) in cases.items():
        fixture = Path(temporary) / scenario
        native = fixture / 'native'
        native.mkdir(parents=True)
        (fixture / '.git-hook-functional-fixture').touch()
        config = fixture / 'config/detekt'
        config.mkdir(parents=True)
        for name in ['detekt.yml', 'formatting.yml', 'autocorrect.yml']:
            (config / name).write_text((workspace / 'config/detekt' / name).read_text())
        (native / 'settings.gradle.kts').write_text('''
            dependencyResolutionManagement {
                repositories { mavenCentral() }
                versionCatalogs { create("libs") { from(files(%s)) } }
            }
            rootProject.name = "GitHookFunctionalFixture"
        ''' % json.dumps(str(workspace / 'native/gradle/libs.versions.toml')))
        (native / 'build.gradle.kts').write_text(
            'apply(from = %s)\napply(from = %s)\n' % (
                json.dumps(str(workspace / 'native/gradle/git-hooks.gradle.kts')),
                json.dumps(str(workspace / 'verification/git-hooks-fixture.gradle.kts')),
            )
        )
        run_case(fixture, scenario, should_pass, expected)
        if scenario == 'formatting-changed':
            run_case(fixture, scenario, False, 'detektPreCommitStagedFormatCheck FAILED',
                     label='retry-without-restaging', properties=('-PreuseFixture=true',))
            run_case(fixture, scenario, True, 'preCommit passed', label='retry-after-restaging',
                     properties=('-PreuseFixture=true', '-PrestageFixture=true'))
summary = {'passed': sum(r['passed'] for r in results), 'total': len(results), 'results': results}
(logs / 'results.json').write_text(json.dumps(summary, indent=2) + '\n')
if not all(r['passed'] for r in results):
    raise SystemExit('Gradle Git hook functional verification failed')
