#!/usr/bin/env python3
"""Reproducible offline mixin/mesh checks; add --benchmark for CPU preparation timings."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "build" / "geometry-validation"
DEPS = ROOT / "build" / "validation-deps"


def run(command, log_name):
    result = subprocess.run(command, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (OUTPUT / log_name).write_text(result.stdout, encoding="utf-8")
    if result.returncode:
        print(result.stdout[-16000:])
        raise subprocess.CalledProcessError(result.returncode, command)
    for line in result.stdout.splitlines():
        if line.startswith(("PASS:", "CPU vanilla-encoder fixture:")):
            print(line, flush=True)


def main():
    spec = importlib.util.spec_from_file_location("dmo_build", ROOT / "build.py")
    build = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(build)
    build.fetch_dependencies()
    if not build.CLASSES.is_dir():
        raise RuntimeError("Run python3 build.py first")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    DEPS.mkdir(parents=True, exist_ok=True)
    for name, url, expected in json.loads((ROOT / "validation" / "geometry-dependencies.json").read_text()):
        target = DEPS / name
        if not target.exists():
            print("Downloading validation dependency", name, flush=True)
            with urllib.request.urlopen(url, timeout=60) as response:
                target.write_bytes(response.read())
        if hashlib.sha256(target.read_bytes()).hexdigest() != expected:
            raise RuntimeError("Validation dependency checksum mismatch: " + name)
    # LaunchWrapper's global property service must precede Fabric's uninitialized Knot service.
    jars = sorted(list(build.DEPS.glob("*.jar")) + list(DEPS.glob("*.jar")),
                  key=lambda path: (path.name != "sponge-mixin.jar", path.name))
    resources = [ROOT / "validation" / "resources", ROOT / "src" / "main" / "resources"]
    classpath = os.pathsep.join(map(str, [build.CLASSES] + resources + jars))
    classes = OUTPUT / "classes"
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir()
    sources = [ROOT / "validation" / name for name in
               ("MixinSmokeTest.java", "GeometryPipelineLauncher.java", "GeometryPipelineCases.java")]
    subprocess.run(build.java() + ["-m", "jdk.compiler/com.sun.tools.javac.Main", "--release", "25", "-proc:none",
                                  "-cp", classpath, "-d", str(classes)] + list(map(str, sources)), check=True)
    # Excluding both API and renderer binaries verifies optional integration at transformation time.
    without_api = [p for p in jars if p.name not in
                   ("fabric-renderer-api.jar", "fabric-api-base.jar", "sodium-0.9.2-mc26.3.jar")]
    baseline_classpath = os.pathsep.join(map(str, [classes, build.CLASSES] + resources + without_api))
    common_args = [str(build.DEPS / "minecraft-client.jar"), str(build.CLASSES), str(resources[1])]
    smoke = "dev.nodelll.displaymodeloptimizer.validation.MixinSmokeTest"
    run(build.java() + ["-cp", baseline_classpath, smoke] + common_args + [str(OUTPUT / "group-no-api.class")],
        "without-renderer-api.log")
    run(build.java() + ["-cp", baseline_classpath, smoke] + common_args +
        [str(OUTPUT / "group-with-ec.class"), str(build.DEPS / "entityculling.jar"), str(resources[0])], "with-entity-culling.log")
    full_classpath = str(classes) + os.pathsep + classpath
    run(build.java() + ["-cp", full_classpath, "dev.nodelll.displaymodeloptimizer.validation.GeometryPipelineLauncher"]
        + (["--benchmark"] if "--benchmark" in sys.argv else []), "fabric-sodium-pipeline.log")
    print("Offline verification logs:", OUTPUT)


if __name__ == "__main__":
    main()
