"""Exercises the real Kotlin DSL hook in disposable repositories. No Git/Detekt CLI or local tool JAR is used."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile

workspace = Path(__file__).resolve().parent.parent
cases = {
    'no-staged-kotlin': (True, 'No staged Kotlin files'),
    'clean-staged': (True, 'preCommit passed'),
    'formatting-changed': (False, 'Formatting changed files'),
    'existing-unstaged': (True, 'preCommit passed'),
    'analysis-failed': (False, 'TooGenericExceptionThrown'),
    'analysis-execution-failed': (False, 'Fixture detekt execution failure'),
    'invalid-detekt-config': (False, 'detektPreCommit FAILED'),
    'remaining-formatting-violation': (False, 'NoWildcardImports'),
    'deletion-only': (True, 'No staged Kotlin files'),
    'spaces-and-unicode': (True, 'preCommit passed'),
    'unstaged-formatting-changed': (False, 'Formatting changed files'),
    'check-does-not-format': (False, 'detektFormatCheck FAILED'),
    'staged-formatting-only': (False, 'detektPreCommitStagedFormatCheck FAILED'),
    'staged-formatting-without-working-file': (False, 'detektPreCommitStagedFormatCheck FAILED'),
    'staged-formatting-spaces-and-unicode': (False, 'detektPreCommitStagedFormatCheck FAILED'),
    'alternate-index-formatting-only': (False, 'detektPreCommitStagedFormatCheck FAILED'),
    'staged-formatting-html-name': (False, 'detektPreCommitStagedFormatCheck FAILED'),
    'data-class-mutable': (False, 'DataClassShouldBeImmutable'),
    'data-class-immutable': (True, 'preCommit passed'),
    'compose-list': (False, 'UnstableCollections'),
    'compose-set': (False, 'UnstableCollections'),
    'compose-map': (False, 'UnstableCollections'),
    'compose-mutable-list': (False, 'MutableParams'),
    'compose-immutable-collections': (True, 'preCommit passed'),
}
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output-dir', type=Path, default=workspace / 'docs/git-hook-tests')
parser.add_argument('--scenario', action='append', choices=cases)
options = parser.parse_args()
logs = options.output_dir.resolve()
logs.mkdir(parents=True, exist_ok=True)
results = []


def run_case(fixture, scenario, should_pass, expected, label=None, properties=(), ci=False, browser_exit=0, safari_fallback=False):
    label = label or scenario
    native = fixture / 'native'
    task = 'detektFormatCheck' if scenario == 'check-does-not-format' else 'preCommit'
    command = [str(workspace / 'scripts/with-env.sh'), str(workspace / 'native/gradlew'),
               '-p', str(native), f'-Pscenario={scenario}', *properties, task,
               '--console=plain', '--no-watch-fs', '--build-cache']
    environment = dict(os.environ)
    environment['PATH'] = str(fixture / 'browser-bin') + os.pathsep + environment.get('PATH', '')
    environment['CI'] = 'true' if ci else 'false'
    environment['DISPLAY'] = ':fixture-browser'
    environment['PRECOMMIT_BROWSER_LOG'] = str(fixture / 'browser-open.log')
    environment['PRECOMMIT_BROWSER_EXIT'] = str(browser_exit)
    environment['PRECOMMIT_BROWSER_FALLBACK_EXIT'] = str(0 if safari_fallback else browser_exit)
    browser_log = fixture / 'browser-open.log'
    browser_log.unlink(missing_ok=True)
    report = native / 'build/reports/git-hooks/preCommit.html'
    report.parent.mkdir(parents=True, exist_ok=True)
    if task == 'preCommit':
        report.write_text('STALE REPORT')
        old_reports = native / 'build/reports/detekt'
        old_reports.mkdir(parents=True, exist_ok=True)
        for name in ['detektPreCommitFormat', 'detektPreCommitFormatCheck', 'detektPreCommit', 'detektPreCommitStagedFormatCheck']:
            (old_reports / f'{name}.html').write_text('STALE REPORT')
    if scenario == 'alternate-index-formatting-only':
        environment['GIT_INDEX_FILE'] = str(fixture / 'alternate-index')
    result = subprocess.run(command, cwd=workspace, env=environment, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, timeout=240)
    (logs / f'{label}.log').write_text(result.stdout)
    filename = {'staged-formatting-spaces-and-unicode': '日本語 Probe.kt',
                'staged-formatting-html-name': 'Probe <unsafe>&.kt'}.get(scenario, 'Answer.kt')
    relative = 'native/data/src/main/kotlin/fixture/' + filename
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
    expected_report = None
    if not should_pass and scenario not in ['check-does-not-format', 'analysis-execution-failed', 'invalid-detekt-config']:
        if label == 'retry-without-restaging' or scenario.startswith('staged-formatting-') or scenario == 'alternate-index-formatting-only':
            expected_report = 'detektPreCommitStagedFormatCheck'
        elif scenario in ['formatting-changed', 'unstaged-formatting-changed']:
            expected_report = 'detektPreCommitFormat'
        elif scenario == 'remaining-formatting-violation':
            expected_report = 'detektPreCommitFormat'
        else:
            expected_report = 'detektPreCommit'
    expects_report = expected_report is not None
    expects_browser = expects_report and not ci and '-PopenPreCommitReport=false' not in properties
    calls = browser_log.read_text().splitlines() if browser_log.is_file() else []
    passed = passed and not report.exists() and bool(calls) == expects_browser
    passed = passed and ('detekt HTML report:' in result.stdout) == expects_report
    if expects_report:
        detekt_report = native / f'build/reports/detekt/{expected_report}.html'
        contents = detekt_report.read_text() if detekt_report.is_file() else ''
        passed = passed and '<title>detekt report</title>' in contents and 'STALE REPORT' not in contents
        (logs / f'{label}-detekt.html').write_text(contents)
        if scenario == 'staged-formatting-html-name':
            passed = passed and '&lt;unsafe&gt;' in contents and '<unsafe>' not in contents
        if expects_browser:
            expected_calls = 2 if browser_exit and sys.platform == 'darwin' else 1
            passed = passed and len(calls) == expected_calls and all(Path(call).resolve() == detekt_report.resolve() for call in calls)
        passed = passed and result.stdout.rfind('> Task :showPreCommitReport') > result.stdout.rfind(f'> Task :{expected_report}')
    if expects_browser and browser_exit and not safari_fallback:
        passed = passed and f'Could not open detekt HTML report (exit {browser_exit})' in result.stdout
    if safari_fallback:
        passed = passed and 'trying Safari.' in result.stdout and 'Opened detekt HTML report.' in result.stdout
    results.append({'scenario': label, 'passed': passed, 'exit_code': result.returncode,
                    'expected_success': should_pass, 'git_index_unchanged': unchanged,
                    'legacy_summary_removed': not report.exists(), 'detekt_report': expected_report,
                    'browser_calls': len(calls)})
    print(f'{"PASS" if passed else "FAIL"}: {label} (exit {result.returncode}, index unchanged={unchanged})', flush=True)
    if not passed:
        print(result.stdout[-8000:], flush=True)


with tempfile.TemporaryDirectory(prefix='gradle-git-hook-tests-') as temporary:
    for scenario in options.scenario or cases:
        should_pass, expected = cases[scenario]
        fixture = Path(temporary) / scenario / '日本語 workspace'
        native = fixture / 'native'
        native.mkdir(parents=True)
        (fixture / '.git-hook-functional-fixture').touch()
        browser_bin = fixture / 'browser-bin'
        browser_bin.mkdir()
        for name in ['open', 'xdg-open']:
            opener = browser_bin / name
            opener.write_text('''#!/bin/sh
set -eu
for argument do report=$argument; done
[ -f "$report" ] || exit 90
printf '%s\\n' "$report" >>"$PRECOMMIT_BROWSER_LOG"
if [ "$1" = '-a' ]; then exit "$PRECOMMIT_BROWSER_FALLBACK_EXIT"; fi
exit "${PRECOMMIT_BROWSER_EXIT:-0}"
''')
            opener.chmod(0o755)
        config = fixture / 'native/gradle/config/detekt'
        config.mkdir(parents=True)
        for name in ['detekt.yml', 'formatting.yml', 'autocorrect.yml']:
            (config / name).write_text((workspace / 'native/gradle/config/detekt' / name).read_text())
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
        if scenario == 'analysis-failed':
            run_case(fixture, scenario, False, expected, label='browser-disabled',
                     properties=('-PreuseFixture=true', '-PopenPreCommitReport=false'))
            run_case(fixture, scenario, False, expected, label='ci-skips-browser',
                     properties=('-PreuseFixture=true',), ci=True)
            run_case(fixture, scenario, False, expected, label='browser-failure-preserves-analysis-failure',
                     properties=('-PreuseFixture=true',), browser_exit=7)
            if sys.platform == 'darwin':
                run_case(fixture, scenario, False, expected, label='browser-fallback-succeeds',
                         properties=('-PreuseFixture=true',), browser_exit=7, safari_fallback=True)
summary = {'passed': sum(r['passed'] for r in results), 'total': len(results), 'results': results}
(logs / 'results.json').write_text(json.dumps(summary, indent=2) + '\n')
if not all(r['passed'] for r in results):
    raise SystemExit('Gradle Git hook functional verification failed')
