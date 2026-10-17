"""Exercises the real Kotlin DSL hook in disposable repositories. No Git/Detekt CLI or local tool JAR is used."""
from pathlib import Path
import hashlib
import json
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
}
logs = workspace / 'docs/git-hook-tests'
logs.mkdir(parents=True, exist_ok=True)
results = []
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
        task = 'detektFormatCheck' if scenario == 'check-does-not-format' else 'preCommit'
        command = [str(workspace / 'scripts/with-env.sh'), str(workspace / 'native/gradlew'),
                   '-p', str(native), f'-Pscenario={scenario}', task, '--console=plain', '--no-watch-fs']
        result = subprocess.run(command, cwd=workspace, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=240)
        (logs / f'{scenario}.log').write_text(result.stdout)
        if (fixture / 'native/data/src/main/kotlin/fixture/Answer.kt').exists():
            (logs / f'{scenario}-after.json').write_text(json.dumps({'working_tree_source': (fixture / 'native/data/src/main/kotlin/fixture/Answer.kt').read_text()}, indent=2) + '\n')
        index_hash = hashlib.sha256((fixture / '.git/index').read_bytes()).hexdigest()
        unchanged = index_hash == (fixture / 'index-before.sha256').read_text()
        passed = (result.returncode == 0) == should_pass and expected in result.stdout and unchanged
        target = fixture / 'native/data/src/main/kotlin/fixture/Answer.kt'
        if scenario in ['no-staged-kotlin', 'check-does-not-format']:
            passed = passed and 'value + 42    ' in target.read_text()
        if scenario == 'existing-unstaged':
            passed = passed and 'answer(value: Int): Int = value + 42' in target.read_text()
        results.append({'scenario': scenario, 'passed': passed, 'exit_code': result.returncode,
                        'expected_success': should_pass, 'git_index_unchanged': unchanged})
        print(f'{"PASS" if passed else "FAIL"}: {scenario} (exit {result.returncode}, index unchanged={unchanged})', flush=True)
        if not passed:
            print(result.stdout[-8000:], flush=True)
summary = {'passed': sum(r['passed'] for r in results), 'total': len(results), 'results': results}
(logs / 'results.json').write_text(json.dumps(summary, indent=2) + '\n')
if not all(r['passed'] for r in results):
    raise SystemExit('Gradle Git hook functional verification failed')
