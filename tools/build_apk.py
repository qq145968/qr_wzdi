import os
import subprocess
import sys

# Set up environment
os.environ['JAVA_HOME'] = r'C:\JDK17'
os.environ['ANDROID_HOME'] = r'C:\AndroidSDK'
os.environ['ANDROID_SDK_ROOT'] = r'C:\AndroidSDK'
os.environ['GRADLE_USER_HOME'] = r'C:\temp\gradle-home'
os.environ['PATH'] = r'C:\JDK17\bin;C:\AndroidSDK\platform-tools;C:\PortableGit\cmd;' + os.environ.get('PATH', '')

project_dir = r'e:\工作路径\扫码机器人'

# Check gradle-wrapper.jar
wrapper_jar = os.path.join(project_dir, 'gradle', 'wrapper', 'gradle-wrapper.jar')
print(f"Gradle wrapper jar exists: {os.path.exists(wrapper_jar)}")
if os.path.exists(wrapper_jar):
    print(f"  Size: {os.path.getsize(wrapper_jar) / 1024:.1f} KB")

# Check local.properties
local_props = os.path.join(project_dir, 'local.properties')
print(f"local.properties exists: {os.path.exists(local_props)}")
if os.path.exists(local_props):
    with open(local_props, 'r') as f:
        print(f"  Content: {f.read().strip()}")

# Write local.properties if needed
sdk_dir = r'C:\AndroidSDK'
with open(local_props, 'w') as f:
    f.write(f'sdk.dir={sdk_dir}\n')
print(f"  Written: sdk.dir={sdk_dir}")

# Try to build
print("\n=== Building APK ===")
print(f"  Project: {project_dir}")
print(f"  JAVA_HOME: {os.environ['JAVA_HOME']}")
print(f"  ANDROID_HOME: {os.environ['ANDROID_HOME']}")
print(f"  GRADLE_USER_HOME: {os.environ['GRADLE_USER_HOME']}")

# Use gradlew.bat on Windows
gradlew = os.path.join(project_dir, 'gradlew.bat')
print(f"  Gradlew: {gradlew}")

# Run the build
result = subprocess.run(
    [gradlew, 'assembleDebug', '--no-daemon', '--stacktrace'],
    cwd=project_dir,
    env=os.environ,
    capture_output=True,
    text=True,
    timeout=600
)

print(f"\n  Exit code: {result.returncode}")
print(f"\n  STDOUT (last 2000 chars):")
print(result.stdout[-2000:] if len(result.stdout) > 2000 else result.stdout)
print(f"\n  STDERR (last 2000 chars):")
print(result.stderr[-2000:] if len(result.stderr) > 2000 else result.stderr)

# Check if APK was created
apk_path = os.path.join(project_dir, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
if os.path.exists(apk_path):
    size = os.path.getsize(apk_path) / (1024 * 1024)
    print(f"\n  APK created: {apk_path}")
    print(f"  Size: {size:.1f} MB")
else:
    print(f"\n  APK not found at: {apk_path}")
    # Check if build dir exists
    build_dir = os.path.join(project_dir, 'app', 'build')
    if os.path.exists(build_dir):
        print(f"  Build dir exists, contents:")
        for root, dirs, files in os.walk(build_dir):
            for f in files:
                if f.endswith('.apk'):
                    print(f"    Found APK: {os.path.join(root, f)}")
