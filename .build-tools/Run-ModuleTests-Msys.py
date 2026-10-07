"""Run unchanged module tests through Git MSYS using temporary path adapters."""
from contextlib import contextmanager
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
GIT_BIN = Path(r'C:\Users\27818\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\git\usr\bin')
SH = GIT_BIN / 'sh.exe'
REAL_RUN = subprocess.run


def posix_path(path):
    value = str(path).replace('\\', '/')
    if len(value) > 2 and value[1] == ':':
        return '/' + value[0].lower() + value[2:]
    return value


@contextmanager
def msys_test_environment():
    with tempfile.TemporaryDirectory(prefix='module-msys-', dir=ROOT / '.build-tools') as folder:
        scope = Path(folder)
        adapters = scope / 'adapters'
        adapters.mkdir()
        adapter_python = adapters / 'commands.py'
        adapter_python.write_bytes(b'''import hashlib
import os
from pathlib import Path
import sys

command, *args = sys.argv[1:]
if command == 'chmod':
    mode = int(args[0], 8)
    for name in args[1:]:
        os.chmod(name, mode)
elif command == 'sha256sum':
    for name in args:
        print(f'{hashlib.sha256(Path(name).read_bytes()).hexdigest()}  {name}')
else:
    raise SystemExit(f'Unsupported adapter command: {command}')
''')
        for command in ('chmod', 'sha256sum'):
            adapter = adapters / command
            adapter.write_bytes((f'exec "{Path(sys.executable).as_posix()}" "{adapter_python.as_posix()}" {command} "$@"\n').encode())

        counts = {'processes': 0, 'normalized_files': 0}

        def compatible_run(args, **kwargs):
            if not isinstance(args, (list, tuple)) or len(args) != 2 or args[0] != 'sh':
                return REAL_RUN(args, **kwargs)
            script = Path(args[1]).resolve()
            if not script.is_relative_to(scope):
                raise RuntimeError(f'Test shell script is outside the isolated fixture directory: {script}')
            base = script.parent
            for file in base.rglob('*'):
                if not file.is_file() or not (file.suffix == '.sh' or file.name in {'getprop', 'resetprop', 'pm', 'config'}):
                    continue
                content = file.read_bytes()
                normalized = content.replace(b'\r\n', b'\n')
                normalized = normalized.replace((str(base) + '\\').encode(), b'./').replace(str(base).encode(), b'.')
                if normalized.startswith(b'#!/bin/sh\n'):
                    normalized = ('#!' + posix_path(SH) + '\n').encode() + normalized[len(b'#!/bin/sh\n'):]
                if normalized != content:
                    file.write_bytes(normalized)
                    counts['normalized_files'] += 1

            env = dict(kwargs.get('env', os.environ))
            raw_path = env['PATH']
            test_prefix = str(base) + ':'
            if not raw_path.startswith(test_prefix):
                raise RuntimeError('Expected the original test fixture to prepend its command directory to PATH')
            remaining_path = raw_path[len(test_prefix):]
            entries = ['.', Path(os.path.relpath(adapters, base)).as_posix(), posix_path(GIT_BIN)]
            entries.extend(posix_path(entry) for entry in remaining_path.split(os.pathsep) if entry)
            env['PATH'] = ':'.join(entries)
            for name in ('CALLS', 'TEST_LOG'):
                if name in env:
                    env[name] = Path(env[name]).relative_to(base).as_posix()
            kwargs['env'] = env
            kwargs['cwd'] = str(base)
            if 'stdout' not in kwargs and 'stderr' not in kwargs and not kwargs.get('capture_output'):
                kwargs['capture_output'] = True
            result = REAL_RUN([str(SH), './' + script.name], **kwargs)
            counts['processes'] += 1
            if result.returncode and result.stderr:
                error = result.stderr.decode(errors='replace') if isinstance(result.stderr, bytes) else result.stderr
                print(f'Shell diagnostic ({script.name}, exit {result.returncode}):\n{error}', file=sys.stderr)
            return result

        original_tempdir = tempfile.tempdir
        original_run = subprocess.run
        tempfile.tempdir = str(scope)
        subprocess.run = compatible_run
        try:
            yield counts
        finally:
            tempfile.tempdir = original_tempdir
            subprocess.run = original_run


def main():
    if not SH.is_file():
        raise SystemExit(f'Missing Git MSYS shell: {SH}')
    sources = sorted((ROOT / 'tools').glob('test_*.py'))
    sources += sorted((ROOT / 'app/src/main/assets/module').glob('*.sh'))
    before = {file: hashlib.sha256(file.read_bytes()).digest() for file in sources}
    with msys_test_environment() as counts:
        suite = unittest.defaultTestLoader.discover(str(ROOT / 'tools'), pattern='test_*.py')
        result = unittest.TextTestRunner(verbosity=2).run(suite)
    for file, digest in before.items():
        if hashlib.sha256(file.read_bytes()).digest() != digest:
            raise SystemExit(f'Protected test or production script changed: {file}')
    print(f'MSYS executions: {counts["processes"]}; normalized temporary fixtures: {counts["normalized_files"]}')
    print('Original tests and production shell scripts are unchanged. Temporary fixtures removed.')
    print('chmod uses Windows os.chmod; POSIX permission enforcement is outside this validation.')
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    sys.dont_write_bytecode = True
    raise SystemExit(main())
