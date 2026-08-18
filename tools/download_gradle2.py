import os
import urllib.request
import ssl
import zipfile
import shutil

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

gradle_zip = r"C:\temp\gradle-8.7-new.zip"
gradle_dir = r"C:\temp\gradle-8.7"

# Download fresh
url = "https://services.gradle.org/distributions/gradle-8.7-bin.zip"
print(f"Downloading Gradle 8.7...")
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, context=ctx, timeout=300) as response:
    total = int(response.headers.get('Content-Length', 0))
    print(f"  Total size: {total / (1024*1024):.1f} MB")
    downloaded = 0
    with open(gradle_zip, 'wb') as f:
        while True:
            chunk = response.read(65536)
            if not chunk:
                break
            f.write(chunk)
            downloaded += len(chunk)
            if downloaded % (20 * 1024 * 1024) < 65536:
                print(f"  {downloaded / (1024*1024):.0f} / {total / (1024*1024):.0f} MB")
    print(f"  Downloaded: {os.path.getsize(gradle_zip) / (1024*1024):.1f} MB")

# Extract
print("Extracting...")
if os.path.exists(gradle_dir):
    shutil.rmtree(gradle_dir, ignore_errors=True)
with zipfile.ZipFile(gradle_zip, 'r') as z:
    z.extractall(r"C:\temp")

gradle_bat = os.path.join(gradle_dir, "bin", "gradle.bat")
print(f"\ngradle.bat: {os.path.exists(gradle_bat)}")
if os.path.exists(gradle_bat):
    print("Gradle 8.7 ready!")
