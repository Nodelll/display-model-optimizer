#!/usr/bin/env python3
"""Build the unobfuscated client mod with a JDK; no Minecraft classes are redistributed."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent
DEPS = ROOT / "build" / "deps"
CLASSES = ROOT / "build" / "classes"
TEST_CLASSES = ROOT / "build" / "test-classes"
VERSION = "0.7.0+mc26.3"
DEPENDENCIES = [
    ("fabric-loader.jar", "https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar",
     "93044e4dd46de5d8136701292f05e868da096d2c9fddb4793e4fdbcc63efc695"),
    ("sponge-mixin.jar", "https://maven.fabricmc.net/net/fabricmc/sponge-mixin/0.17.5+mixin.0.8.7/sponge-mixin-0.17.5+mixin.0.8.7.jar",
     "03f0065023bfc3045d71bfb99872451d9ad5a0800a5a0dac34ab8c7d5c134f5d"),
    ("fastutil.jar", "https://libraries.minecraft.net/it/unimi/dsi/fastutil/8.5.18/fastutil-8.5.18.jar",
     "9094ae67d01d0ad246f886f11ad557fc2e79c72cbf3feed83e1512a8ae90a74a"),
    ("minecraft-client.jar", "https://piston-data.mojang.com/v1/objects/e877b6a07acd633fb3bb475002175cec036e7b87/client.jar",
     "4508d006323f24fa02876310c192d739af56516eb259000ac50f0909a68c9a2d"),
    ("joml.jar", "https://libraries.minecraft.net/org/joml/joml/1.10.9/joml-1.10.9.jar",
     "feca4db853371704338621c120acc5cc32300d8af635fa42414b6213301e216b"),
    ("modmenu.jar", "https://cdn.modrinth.com/data/mOgUt4GM/versions/kyy7dbrZ/modmenu-21.0.0.jar",
     "837812c6c98d66141db7b5dcae668f27fb78d557e8f4c0c89912985fc26a5e6f"),
    ("brigadier.jar", "https://libraries.minecraft.net/com/mojang/brigadier/1.3.11/brigadier-1.3.11.jar",
     "9d09cf4819de83dfcd38bcb212d5f2227131791cef3abbfca3321d6fb2b9a64d"),
    ("entityculling.jar", "https://cdn.modrinth.com/data/NNAgCjsB/versions/X1g3VhTx/entityculling-fabric-1.11.3-mc26.3.jar",
     "d2f80514ee3382b1fa7afda4df762df50743226e3cd320b829ee507357aeafbc"),
    ("gson.jar", "https://libraries.minecraft.net/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar",
     "2cbd119bf1961c28788310963dc80ba65f58cdeec1dd139c8bdb1240faa2c36f"),
]


def fetch_dependencies():
    DEPS.mkdir(parents=True, exist_ok=True)
    for name, url, expected in DEPENDENCIES:
        target = DEPS / name
        if not target.is_file():
            print("Downloading", name, flush=True)
            with urllib.request.urlopen(url, timeout=60) as response:
                target.write_bytes(response.read())
        if hashlib.sha256(target.read_bytes()).hexdigest() != expected:
            raise RuntimeError("Dependency checksum mismatch: " + name)


def java():
    binary = os.environ.get("DISPLAY_MODEL_OPTIMIZER_JAVA") or shutil.which("java")
    if not binary:
        raise RuntimeError("Java was not found. Install JDK 25 and add java to PATH.")
    return [binary, "-Xshare:off"]


def compile_sources(source_root, destination, classpath):
    sources = sorted(str(path) for path in source_root.rglob("*.java"))
    destination.mkdir(parents=True, exist_ok=True)
    subprocess.run(java() + ["-m", "jdk.compiler/com.sun.tools.javac.Main", "--release", "25",
                            "-encoding", "UTF-8", "-proc:none", "-cp", classpath,
                            "-d", str(destination)] + sources, check=True)


def add_file(archive, path, name):
    info = zipfile.ZipInfo(name, (2026, 10, 14, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    archive.writestr(info, path.read_bytes())


def main():
    fetch_dependencies()
    import sys
    if "--deps-only" in sys.argv:
        return
    # Cleaning only build-owned class directories prevents old test classes entering the mod.
    for directory in (CLASSES, TEST_CLASSES):
        if directory.exists():
            shutil.rmtree(directory)
    classpath = os.pathsep.join(str(DEPS / name) for name, _, _ in DEPENDENCIES)
    compile_sources(ROOT / "src" / "main" / "java", CLASSES, classpath)
    compile_sources(ROOT / "src" / "test" / "java", TEST_CLASSES, str(CLASSES) + os.pathsep + classpath)
    subprocess.run(java() + ["-cp", str(TEST_CLASSES) + os.pathsep + str(CLASSES) + os.pathsep + classpath,
                            "dev.nodelll.displaymodeloptimizer.DrawLookupIndexTest"], check=True)
    subprocess.run(java() + ["-cp", str(TEST_CLASSES) + os.pathsep + str(CLASSES) + os.pathsep + classpath,
                            "dev.nodelll.displaymodeloptimizer.ModelCacheTest"], check=True)
    subprocess.run(java() + ["-cp", str(TEST_CLASSES) + os.pathsep + str(CLASSES) + os.pathsep + classpath,
                            "dev.nodelll.displaymodeloptimizer.SettingsTest"], check=True)
    subprocess.run(java() + ["-cp", str(TEST_CLASSES) + os.pathsep + str(CLASSES) + os.pathsep + classpath,
                            "dev.nodelll.displaymodeloptimizer.BoundsTest"], check=True)
    subprocess.run(java() + ["-cp", str(TEST_CLASSES) + os.pathsep + str(CLASSES) + os.pathsep + classpath,
                            "dev.nodelll.displaymodeloptimizer.GitHubReleaseFeedTest"], check=True)
    subprocess.run(java() + ["-cp", str(TEST_CLASSES) + os.pathsep + str(CLASSES) + os.pathsep + classpath,
                            "dev.nodelll.displaymodeloptimizer.DisplayFrustumTest"], check=True)
    output = ROOT / "build" / "libs" / ("display-model-optimizer-" + VERSION + ".jar")
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w") as archive:
        manifest = zipfile.ZipInfo("META-INF/MANIFEST.MF", (2026, 10, 14, 0, 0, 0))
        manifest.compress_type = zipfile.ZIP_DEFLATED
        archive.writestr(manifest,
                         "Manifest-Version: 1.0\r\nFabric-Loom-Remap: false\r\n\r\n")
        for path in sorted(CLASSES.rglob("*.class")):
            add_file(archive, path, path.relative_to(CLASSES).as_posix())
        resources = ROOT / "src" / "main" / "resources"
        for path in sorted(resources.rglob("*")):
            if path.is_file():
                add_file(archive, path, path.relative_to(resources).as_posix())
        add_file(archive, ROOT / "LICENSE", "LICENSE")
    with zipfile.ZipFile(output) as archive:
        metadata = json.loads(archive.read("fabric.mod.json"))
        assert metadata["authors"] == ["Nodelll"]
        assert metadata["environment"] == "client"
        assert metadata["depends"]["minecraft"] == "26.3"
        assert metadata["version"] == VERSION
        assert all(not name.startswith(("net/minecraft/", "org/spongepowered/", "it/unimi/", "com/terraformersmc/", "com/google/", "dev/tr7zw/", "com/logisticscraft/"))
                   for name in archive.namelist())
        assert not any("Test.class" in name for name in archive.namelist())
    print("Built:", output)
    print("SHA-256:", hashlib.sha256(output.read_bytes()).hexdigest())


if __name__ == "__main__":
    main()
