import subprocess, os, sys

GIT = r"C:\Users\Administrator\Desktop\PortableGit\cmd\git.exe"
REPO_DIR = r"e:\Git-Hub\扫码机器人"
TOKEN = "G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"
REMOTE_URL = f"https://qq145968:{TOKEN}@github.com/qq145968/qr_wzdi.git"

env = os.environ.copy()
env["PATH"] = r"C:\Users\Administrator\Desktop\PortableGit\cmd;" + r"C:\Users\Administrator\Desktop\PortableGit\bin;" + r"C:\Users\Administrator\Desktop\PortableGit\mingw64\bin;" + r"C:\Users\Administrator\Desktop\PortableGit\mingw64\libexec\git-core;" + env.get("PATH","")
env["HOME"] = "C:\\Users\\Administrator"
env["GIT_CONFIG_NOSYSTEM"] = "1"

def run(args, cwd=REPO_DIR, timeout=30):
    try:
        r = subprocess.run([GIT]+args, capture_output=True, text=True, env=env, cwd=cwd, timeout=timeout)
        out = (r.stdout.strip()[:500] if r.stdout.strip() else "") + ("|ERR:" + r.stderr.strip()[:500] if r.stderr.strip() else "")
        print(f"git {' '.join(args[:3])} => exit={r.returncode} {out}")
        sys.stdout.flush()
        return r.returncode
    except subprocess.TimeoutExpired:
        print(f"git {' '.join(args[:3])} => TIMEOUT")
        sys.stdout.flush()
        return -1
    except Exception as e:
        print(f"git {' '.join(args[:3])} => ERROR: {e}")
        sys.stdout.flush()
        return -2

print("Starting git init...")
sys.stdout.flush()
run(["init"])
run(["config","user.name","qq145968"])
run(["config","user.email","qq145968@users.noreply.github.com"])
print("Init done. Adding remote...")
sys.stdout.flush()
run(["remote","remove","origin"])
run(["remote","add","origin",REMOTE_URL])
print("Remote done. Adding files...")
sys.stdout.flush()
run(["add","-A"])
run(["commit","-m","Initial commit: ScanRobot Android app"])
print("Commit done. Pushing...")
sys.stdout.flush()
run(["push","-u","origin","master","--force"], timeout=120)
print("=== ALL DONE ===")
sys.stdout.flush()
