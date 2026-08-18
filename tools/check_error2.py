import zipfile

log_path = r'e:\Git-Hub\扫码机器人\build_logs\failed_run_logs.zip'

with zipfile.ZipFile(log_path, 'r') as zf:
    for name in zf.namelist():
        if '6_Build' in name:
            content = zf.read(name).decode('utf-8', errors='replace')
            lines = content.split('\n')
            for i, line in enumerate(lines):
                stripped = line.strip()
                # Look for Kotlin compiler errors (e:), FAILURE, or error messages
                if stripped.startswith('e:') or 'FAILURE:' in stripped or 'BUILD FAILED' in stripped or 'error: ' in stripped.lower() or 'Unresolved' in stripped:
                    start = max(0, i-1)
                    end = min(len(lines), i+2)
                    for j in range(start, end):
                        print(lines[j])
                    print("---")
