import subprocess, os, sys

GIT = r"C:\Users\Administrator\Desktop\PortableGit\bin\git.exe"
REPO_DIR = r"e:\Git-Hub\扫码机器人"
TOKEN = "G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"
REMOTE_URL = f"https://qq145968:{TOKEN}@github.com/qq145968/qr_wzdi.git"

env = os.environ.copy()
env["PATH"] = r"C:\Users\Administrator\Desktop\PortableGit\bin;" + r"C:\Users\Administrator\Desktop\PortableGit\mingw64\bin;" + env.get("PATH","")
env["HOME"] = "C:\\Users\\Administrator"
env["GIT_EXEC_PATH"] = r"C:\Users\Administrator\Desktop\PortableGit\mingw64\libexec\git-core"
env["GIT_CONFIG_NOSYSTEM"] = "1"

def run(args, cwd=REPO_DIR):
    r = subprocess.run([GIT]+args, capture_output=True, text=True, env=env, cwd=cwd, timeout=60)
    print(f">>> git {' '.join(args[:3])}")
    if r.stdout.strip(): print(r.stdout.strip())
    if r.stderr.strip(): print("STDERR:", r.stderr.strip())
    print(f"exit: {r.returncode}")
    return r.returncode

# 1. init
run(["init"])
# 2. config
run(["config","user.name","qq145968"])
run(["config","user.email","qq145968@users.noreply.github.com"])
# 3. add remote
run(["remote","remove","origin"])
run(["remote","add","origin",REMOTE_URL])
# 4. add all
run(["add","-A"])
# 5. commit
run(["commit","-m","Initial commit: ScanRobot Android app"])
# 6. push
run(["push","-u","origin","master","--force"])
print("\n=== DONE ===")
