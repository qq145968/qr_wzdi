import os
import subprocess
import urllib.request
import ssl
import zipfile
import shutil
import stat

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Set up environment
gradle_home = r'C:\temp\gradle-home'
os.environ['JAVA_HOME'] = r'C:\JDK17'
os.environ['ANDROID_HOME'] = r'C:\AndroidSDK'
os.environ['ANDROID_SDK_ROOT'] = r'C:\AndroidSDK'
os.environ['GRADLE_USER_HOME'] = gradle_home
os.environ['PATH'] = r'C:\JDK17\bin;C:\AndroidSDK\platform-tools;C:\PortableGit\cmd;' + os.environ.get('PATH', '')

project_dir = r'e:\工作路径\扫码机器人'

# Step 1: Download and extract Gradle 8.7 directly
print("=== Step 1: Download Gradle 8.7 ===")
gradle_version = "8.7"
gradle_zip = r"C:\temp\gradle-8.7-bin.zip"
gradle_dir = r"C:\temp\gradle-8.7"

if not os.path.exists(gradle_dir):
    if not os.path.exists(gradle_zip):
        url = f"https://services.gradle.org/distributions/gradle-{gradle_version}-bin.zip"
        print(f"  Downloading from {url}...")
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, context=ctx, timeout=300) as response:
            with open(gradle_zip, 'wb') as f:
                while True:
                    chunk = response.read(65536)
                    if not chunk:
                        break
                    f.write(chunk)
        print(f"  Downloaded: {os.path.getsize(gradle_zip) / (1024*1024):.1f} MB")

    print("  Extracting...")
    with zipfile.ZipFile(gradle_zip, 'r') as z:
        z.extractall(r"C:\temp")
    # The zip extracts as "gradle-8.7"
    print(f"  Extracted to: {gradle_dir}")
else:
    print(f"  Already exists: {gradle_dir}")

gradle_bin = os.path.join(gradle_dir, "bin", "gradle.bat")
print(f"  gradle.bat: {os.path.exists(gradle_bin)}")

# Step 2: Pre-create all directories that Gradle might need
print("\n=== Step 2: Pre-creating directories ===")
dirs_to_create = [
    gradle_home,
    os.path.join(gradle_home, "wrapper"),
    os.path.join(gradle_home, "wrapper", "dists"),
    os.path.join(gradle_home, "caches"),
    os.path.join(gradle_home, "daemon"),
    os.path.join(project_dir, ".gradle"),
    os.path.join(project_dir, "app", "build"),
    os.path.join(project_dir, "app", "build", "outputs"),
    os.path.join(project_dir, "app", "build", "outputs", "apk"),
    os.path.join(project_dir, "app", "build", "outputs", "apk", "debug"),
]
for d in dirs_to_create:
    os.makedirs(d, exist_ok=True)
    # Set permissions
    try:
        os.chmod(d, stat.S_IRWXU | stat.S_IRWXG | stat.S_IRWXO)
    except:
        pass

print(f"  Created {len(dirs_to_create)} directories")

# Step 3: Write local.properties
local_props = os.path.join(project_dir, 'local.properties')
with open(local_props, 'w') as f:
    f.write(f'sdk.dir=C:\\AndroidSDK\n')

# Step 4: Build using gradle directly (not wrapper)
print("\n=== Step 3: Building APK with Gradle ===")
print(f"  JAVA_HOME: {os.environ['JAVA_HOME']}")
print(f"  ANDROID_HOME: {os.environ['ANDROID_HOME']}")
print(f"  GRADLE_USER_HOME: {os.environ['GRADLE_USER_HOME']}")

# Try running gradle directly
result = subprocess.run(
    [gradle_bin, 'assembleDebug', '--no-daemon', '--stacktrace', '--info'],
    cwd=project_dir,
    env=os.environ,
    capture_output=True,
    text=True,
    timeout=600
)

print(f"\n  Exit code: {result.returncode}")

# Print last part of output
stdout_lines = result.stdout.split('\n') if result.stdout else []
stderr_lines = result.stderr.split('\n') if result.stderr else []

print(f"\n  STDOUT (last 50 lines):")
for line in stdout_lines[-50:]:
    print(f"    {line}")

print(f"\n  STDERR (last 30 lines):")
for line in stderr_lines[-30:]:
    print(f"    {line}")

# Check for APK
apk_path = os.path.join(project_dir, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
if os.path.exists(apk_path):
    size = os.path.getsize(apk_path) / (1024 * 1024)
    print(f"\n  APK created: {apk_path}")
    print(f"  Size: {size:.1f} MB")
else:
    print(f"\n  APK not found")
    # Search for any APK
    for root, dirs, files in os.walk(os.path.join(project_dir, 'app', 'build')):
        for f in files:
            if f.endswith('.apk'):
                print(f"  Found: {os.path.join(root, f)}")
