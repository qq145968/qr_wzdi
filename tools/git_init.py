import subprocess, os

GIT = r"C:\Users\Administrator\Desktop\PortableGit\bin\git.exe"
REPO_DIR = r"e:\Git-Hub\扫码机器人"

env = os.environ.copy()
env["PATH"] = r"C:\Users\Administrator\Desktop\PortableGit\bin;" + r"C:\Users\Administrator\Desktop\PortableGit\mingw64\bin;" + env.get("PATH","")
env["HOME"] = "C:\\Users\\Administrator"
env["GIT_EXEC_PATH"] = r"C:\Users\Administrator\Desktop\PortableGit\mingw64\libexec\git-core"
env["GIT_CONFIG_NOSYSTEM"] = "1"

def run(args, cwd=REPO_DIR, timeout=30):
    try:
        r = subprocess.run([GIT]+args, capture_output=True, text=True, env=env, cwd=cwd, timeout=timeout)
        out = r.stdout.strip() + ("|ERR:" + r.stderr.strip() if r.stderr.strip() else "")
        print(f"git {' '.join(args[:3])} => exit={r.returncode} {out[:200]}")
        return r.returncode
    except subprocess.TimeoutExpired:
        print(f"git {' '.join(args[:3])} => TIMEOUT")
        return -1
    except Exception as e:
        print(f"git {' '.join(args[:3])} => ERROR: {e}")
        return -2

run(["init"])
run(["config","user.name","qq145968"])
run(["config","user.email","qq145968@users.noreply.github.com"])
print("=== init done ===")
sys.stdout.flush()
