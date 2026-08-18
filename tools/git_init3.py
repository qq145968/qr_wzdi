import subprocess, os, sys

GIT = r"C:\Users\Administrator\Desktop\PortableGit\cmd\git.exe"
EXEC_PATH = r"C:\Users\Administrator\Desktop\PortableGit\mingw64\libexec\git-core"
REPO_DIR = r"e:\Git-Hub\扫码机器人"
TOKEN = "G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"

env = os.environ.copy()
env["GIT_EXEC_PATH"] = EXEC_PATH
env["HOME"] = "C:\\Users\\Administrator"
env["GIT_CONFIG_NOSYSTEM"] = "1"
env["PATH"] = r"C:\Users\Administrator\Desktop\PortableGit\cmd;" + r"C:\Users\Administrator\Desktop\PortableGit\mingw64\bin;" + env.get("PATH","")

def run(args, timeout=30):
    try:
        r = subprocess.run([GIT]+args, capture_output=True, text=True, env=env, cwd=REPO_DIR, timeout=timeout)
        out = r.stdout.strip()[:500] if r.stdout.strip() else ""
        err = r.stderr.strip()[:500] if r.stderr.strip() else ""
        print(f"git {' '.join(args[:3])} => exit={r.returncode} stdout={out} stderr={err}", flush=True)
        return r.returncode
    except subprocess.TimeoutExpired:
        print(f"git {' '.join(args[:3])} => TIMEOUT", flush=True)
        return -1
    except Exception as e:
        print(f"git {' '.join(args[:3])} => ERROR: {e}", flush=True)
        return -2

# Write to log file so we can read it
import io
log = open(r"e:\Git-Hub\扫码机器人\tools\git_log.txt", "w", encoding="utf-8")
def run2(args, timeout=30):
    try:
        r = subprocess.run([GIT]+args, capture_output=True, text=True, env=env, cwd=REPO_DIR, timeout=timeout)
        msg = f"git {' '.join(args[:3])} => exit={r.returncode} stdout={r.stdout.strip()[:500]} stderr={r.stderr.strip()[:500]}\n"
        log.write(msg); log.flush()
        return r.returncode
    except subprocess.TimeoutExpired:
        msg = f"git {' '.join(args[:3])} => TIMEOUT\n"
        log.write(msg); log.flush()
        return -1
    except Exception as e:
        msg = f"git {' '.join(args[:3])} => ERROR: {e}\n"
        log.write(msg); log.flush()
        return -2

run2(["init"])
run2(["config","user.name","qq145968"])
run2(["config","user.email","qq145968@users.noreply.github.com"])
run2(["remote","remove","origin"])
run2(["remote","add","origin",f"https://qq145968:{TOKEN}@github.com/qq145968/qr_wzdi.git"])
run2(["add","-A"])
run2(["commit","-m","Initial commit: ScanRobot Android app"])
run2(["push","-u","origin","master","--force"], timeout=120)
log.write("=== ALL DONE ===\n")
log.close()
print("Done - check git_log.txt", flush=True)
