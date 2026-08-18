import zipfile

log_path = r'e:\Git-Hub\扫码机器人\build_logs\failed_run_logs.zip'

with zipfile.ZipFile(log_path, 'r') as zf:
    for name in zf.namelist():
        if '6_Build' in name:
            content = zf.read(name).decode('utf-8', errors='replace')
            # Find the FAILURE section and print everything around it
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if 'FAILURE:' in line or 'What went wrong' in line or 'exception' in line.lower() or 'Could not' in line or 'Cannot' in line:
                    start = max(0, i-2)
                    end = min(len(lines), i+15)
                    print(f"\n=== Context at line {i} ===")
                    for j in range(start, end):
                        print(lines[j])
