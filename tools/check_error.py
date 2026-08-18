import zipfile
import os

log_path = r'e:\Git-Hub\扫码机器人\build_logs\failed_run_logs.zip'

with zipfile.ZipFile(log_path, 'r') as zf:
    # Find the build log
    for name in zf.namelist():
        if 'Build' in name or 'build' in name.lower():
            content = zf.read(name).decode('utf-8', errors='replace')
            # Search for error lines
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if 'error:' in line.lower() or 'ERROR' in line or 'e:' in line.lower() or 'FAILURE:' in line or 'unresolved' in line.lower() or 'cannot find' in line.lower():
                    # Print context around the error
                    start = max(0, i-2)
                    end = min(len(lines), i+3)
                    print(f"\n=== Found error at line {i} in {name} ===")
                    for j in range(start, end):
                        print(f"  {lines[j]}")
