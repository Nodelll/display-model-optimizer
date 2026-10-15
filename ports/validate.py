#!/usr/bin/env python3
"""Validate each port against its own game and real renderer binaries, never 26.3 fixtures."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PORTS = ROOT / 'ports'
PREFIX = 'dev/nodelll/displaymodeloptimizer/'

def java():
    return os.environ.get('DISPLAY_MODEL_OPTIMIZER_JAVA') or str(Path(os.environ['JAVA_HOME']) / 'bin/java')

def run(command, log):
    result = subprocess.run(list(map(str, command)), cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log.write_text(result.stdout, encoding='utf-8')
    if result.returncode:
        print(result.stdout[-18000:], flush=True)
        raise RuntimeError('Validation command failed; full log: ' + str(log))
    for line in result.stdout.splitlines():
        if line.startswith('PASS:'): print(line, flush=True)

def fetch(path, url, digest, algorithm='sha256'):
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        request = urllib.request.Request(url, headers={'User-Agent':'DisplayModelOptimizer-port-validation/0.8.0 (github.com/Nodelll/display-model-optimizer)'})
        for attempt in range(3):
            try:
                with urllib.request.urlopen(request, timeout=60) as response: path.write_bytes(response.read())
                break
            except OSError:
                if attempt == 2: raise
                time.sleep(attempt + 1)
    if hashlib.new(algorithm, path.read_bytes()).hexdigest() != digest:
        raise RuntimeError('Dependency checksum mismatch: ' + path.name)
    return path

def common_dependencies():
    spec = importlib.util.spec_from_file_location('stable_build', ROOT / 'build.py')
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    records = [r for r in module.DEPENDENCIES if r[0] in ('asm.jar','asm-tree.jar','sponge-mixin.jar','fabric-loader.jar')]
    wanted = ('asm-analysis.jar','asm-commons.jar','asm-util.jar','launchwrapper-1.12.jar')
    records += [r for r in json.loads((ROOT/'validation/geometry-dependencies.json').read_text()) if r[0] in wanted]
    result = [fetch(PORTS/'build/validation-deps'/name,url,digest) for name,url,digest in records]
    return sorted(result, key=lambda p: (p.name != 'sponge-mixin.jar', p.name))

def mappings(game):
    # Locate alongside Loom's minecraftMaven directory, regardless of GRADLE_USER_HOME.
    loom = next(parent for parent in game.parents if parent.name == 'fabric-loom')
    candidates = list((loom/'1.21.11').rglob('mappings.tiny'))
    path = next(p for p in candidates if p.read_text().splitlines()[0].endswith('intermediary\tnamed'))
    classes, methods = {}, {}
    owner = None
    for line in path.read_text().splitlines()[1:]:
        parts = line.split('\t')
        if parts[0] == 'c':
            owner = parts[-1]; classes[owner] = parts[-2]
        elif len(parts) > 5 and parts[1] == 'm': methods[(owner, parts[-1])] = parts[-2]
    return classes, methods

def check_jar(jar, target, version):
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None
        d = json.loads(z.read('fabric.mod.json'))
        assert d['id'] == 'display_model_optimizer' and d['version'] == version
        assert d['depends']['minecraft'] == target and d['environment'] == 'client'
        assert d['depends']['java'] == ('>=25' if target=='26.2' else '>=21')
        assert d['authors'] == ['Nodelll']
        forbidden = ('net/minecraft/','net/fabricmc/','org/spongepowered/','org/objectweb/','it/unimi/','com/terraformersmc/','com/google/','dev/tr7zw/')
        for n in z.namelist():
            assert not n.startswith(forbidden), 'Bundled dependency: ' + n
            assert not n.endswith('Test.class')
            if n.endswith('.class'):
                major = int.from_bytes(z.read(n)[6:8], 'big')
                assert major == (69 if target == '26.2' else 65), (n,major)
        manifest = z.read('META-INF/MANIFEST.MF').decode()
        if target == '1.21.11': assert 'Fabric-Mapping-Namespace: intermediary' in manifest
    jar.with_suffix('.jar.sha256').write_text(hashlib.sha256(jar.read_bytes()).hexdigest()+'  '+jar.name+'\n')
    print('PASS: production-only JAR, exact Minecraft dependency, class versions and mapping namespace:', jar.name, flush=True)

def main(target, integration_only=False):
    env = json.loads((PORTS/target/'build/validation-environment.json').read_text())
    out = PORTS/target/'build/validation'; out.mkdir(parents=True,exist_ok=True)
    classes = out/'classes'; classes.mkdir(exist_ok=True)
    common = common_dependencies()
    # Use one Mixin/ASM version and LaunchWrapper's standalone service.
    libraries = [Path(p) for p in env['compileClasspath'] if p.endswith('.jar') and not any(x in p.replace('\\','/') for x in
                 ('org.ow2.asm/','sponge-mixin/','fabric-loader/'))]
    named_game = next(p for p in libraries if p.name.startswith('minecraft-merged'))
    cp = os.pathsep.join(map(str, common + [Path(p) for p in env['classes']] + libraries))
    sources = sorted((ROOT/'src/test/java').rglob('*.java'))
    if target == '1.21.11': sources = [p for p in sources if p.stem not in ('ModelCacheTest','GuiBackgroundTest')]
    if not integration_only:
        run([java(),'-m','jdk.compiler/com.sun.tools.javac.Main','--release','25','-proc:none','-cp',cp,'-d',classes]+sources, out/'compile-tests.log')
        test_cp = str(classes)+os.pathsep+cp
        for p in sources:
            run([java(),'-cp',test_cp,'dev.nodelll.displaymodeloptimizer.'+p.stem],out/(p.stem+'.log'))
    run([java(),'-m','jdk.compiler/com.sun.tools.javac.Main','--release','21','-proc:none','-cp',cp,'-d',classes,
         PORTS/'validation/PortMixinCheck.java'],out/'compile-mixin-check.log')
    jar = PORTS/target/'build/libs'/('display-model-optimizer-'+env['version']+'.jar')
    check_jar(jar,target,env['version'])
    names, methods = ({},{}) if target == '26.2' else mappings(named_game)
    def name(n): return names.get(n,n).replace('/','.')
    def method(owner,n): return methods.get((owner,n),n)
    game = named_game if target == '26.2' else named_game.parent.parent.with_name('minecraft-merged-intermediary') / named_game.parent.name / named_game.name.replace('minecraft-merged-','minecraft-merged-intermediary-',1)
    assert game.exists(), game
    runtime_libraries = [p for p in libraries if not any(x in p.as_posix() for x in
        ('minecraft-merged','remapped_mods','fabric-api/','fabric-api-','maven.modrinth/','terraformersmc/'))]
    dep_records = json.loads((PORTS/'validation/dependencies.json').read_text())[target]
    mods = {}
    for key,record in dep_records.items(): mods[key] = fetch(out/'deps'/(key+'.jar'),record['url'],record['sha512'],'sha512')
    nested = []
    with zipfile.ZipFile(mods['fabric-api']) as z:
        for entry in z.namelist():
            if entry.startswith('META-INF/jars/') and entry.endswith('.jar'):
                p=out/'deps'/Path(entry).name; p.write_bytes(z.read(entry)); nested.append(p)
    api = next(p for p in nested if p.name.startswith('fabric-renderer-api-v1-'))
    indigo = next(p for p in nested if p.name.startswith('fabric-renderer-indigo-'))
    base = next(p for p in nested if p.name.startswith('fabric-api-base-'))
    hooks = {
        '00-entity-renderer':('net/minecraft/client/renderer/entity/EntityRenderer',[]),
        'display':('net/minecraft/world/entity/Display',[PREFIX+'DisplayDistancePolicy#isOutside=1']),
        'display-renderer':('net/minecraft/client/renderer/entity/DisplayRenderer',[PREFIX+'culling/DisplayOcclusion#bounds=2']),
        'item':('net/minecraft/client/renderer/entity/DisplayRenderer$ItemDisplayRenderer',[PREFIX+'culling/DisplayOcclusion#captureItem=1']),
        'block':('net/minecraft/client/renderer/entity/DisplayRenderer$BlockDisplayRenderer',[PREFIX+'culling/DisplayOcclusion#captureBlock=1']),
        'frame':('net/minecraft/client/renderer/GameRenderer',[PREFIX+'BlockDisplayModelCache#beginFrame=1',PREFIX+'BlockDisplayModelCache#finishFrame=1']),
        'reload':('net/minecraft/client/Minecraft',[PREFIX+'GeometryCacheLifecycle#invalidate=1']),
    }
    if target == '26.2':
        hooks.update({'group':('net/minecraft/client/renderer/feature/RenderTypeFeatureRenderer$Group',[PREFIX+'DrawLookupIndex#findFirst=1']),
                      'model':('net/minecraft/client/renderer/block/BlockModelResolver',[PREFIX+'BlockDisplayModelCache#update=1']),
                      'model-state':('net/minecraft/client/renderer/block/BlockModelRenderState',[])})
    else:
        hooks.update({'parts':('net/minecraft/client/renderer/block/ModelBlockRenderer',[PREFIX+'BlockDisplayModelCache#collect=1']),
                      'command':('net/minecraft/client/renderer/SubmitNodeStorage$BlockSubmit',[PREFIX+'LegacyDisplayScope#submitting=1']),
                      'draw':('net/minecraft/client/renderer/feature/BlockFeatureRenderer',[PREFIX+'LegacyDisplayScope#rendering=2']),
                      'reload':('net/minecraft/client/Minecraft',[PREFIX+'GeometryCacheLifecycle#invalidate=1'])})
    prod_config = json.loads(zipfile.ZipFile(jar).read('display-model-optimizer.mixins.json'))
    gui_owner = 'net/minecraft/client/gui/screens/Screen'
    wrapper = 'extractRenderStateWithTooltipAndSubtitles' if target=='26.2' else 'renderWithTooltipAndSubtitles'
    background = 'extractBackground' if target=='26.2' else 'renderBackground'
    for scenario in ['without-api','entity-culling','indigo','sodium']:
        resources = out/scenario; resources.mkdir(exist_ok=True)
        config = dict(prod_config)
        additions=[]; configs=['port-dmo.mixins.json']
        scenario_hooks=dict(hooks)
        if scenario in ('indigo','sodium'):
            # Force the same production optional mixin on with a verified compatible API; no test-only hook.
            config.pop('plugin',None)
            additions=[api,base,indigo] + ([mods['sodium']] if scenario=='sodium' else [])
            if target=='26.2':
                scenario_hooks['geometry']=('net/minecraft/client/renderer/block/model/BlockStateModelWrapper',[PREFIX+'compat/FabricBlockGeometryCache#emit=1'])
                backend={'package':'net.fabricmc.fabric.mixin.client.renderer','client':['block.model.BlockStateModelMixin','block.model.BlockStateModelPartMixin','block.render.BlockModelRenderStateMixin','block.render.BlockStateModelWrapperMixin']}
            else:
                scenario_hooks['geometry']=('net/minecraft/client/renderer/block/BlockRenderDispatcher',[PREFIX+'compat/FabricBlockGeometryCache#wrap=1'])
                # Both backends overwrite the old parts path; its redirect must gracefully yield.
                scenario_hooks['parts']=('net/minecraft/client/renderer/block/ModelBlockRenderer',[])
                backend={'package':('net.caffeinemc.mods.sodium.mixin.frapi' if scenario=='sodium' else 'net.fabricmc.fabric.mixin.client.indigo.renderer'),
                         'client':['BlockRenderDispatcherMixin','ModelBlockRendererMixin']}
            backend.update(required=True,compatibilityLevel='JAVA_'+str(env['java']),injectors={'defaultRequire':1})
            (resources/'backend.mixins.json').write_text(json.dumps(backend));configs.append('backend.mixins.json')
        if scenario=='entity-culling':
            additions=[mods['entityculling']]
            ec={'required':True,'package':'dev.tr7zw.entityculling.mixin','compatibilityLevel':'JAVA_'+str(env['java']),
                'client':['EntityRendererMixin'],'injectors':{'defaultRequire':1}}
            if target=='1.21.11': ec['refmap']='entityculling.refmap.mixins.json'
            (resources/'ec.mixins.json').write_text(json.dumps(ec));configs.append('ec.mixins.json')
        (resources/'port-dmo.mixins.json').write_text(json.dumps(config))
        checks={('target.'+k):'|'.join([name(v[0])]+v[1]) for k,v in scenario_hooks.items()}
        checks.update({'ec':str(scenario=='entity-culling').lower(),'gui.screen':name(gui_owner),
                       'gui.wrapper':method(gui_owner,wrapper),'gui.background':method(gui_owner,background)})
        checkfile=resources/'checks.properties';checkfile.write_text('\n'.join(k+'='+v for k,v in checks.items())+'\n')
        runtime_cp=os.pathsep.join(map(str,common+[classes,jar,resources,game]+runtime_libraries+additions))
        runtime_java=os.environ.get('DISPLAY_MODEL_OPTIMIZER_JAVA21',java()) if target=='1.21.11' else java()
        run([runtime_java,'-Xshare:off','-Dmixin.service=org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper',
             '-cp',runtime_cp,'dev.nodelll.displaymodeloptimizer.validation.PortMixinCheck',game,checkfile]+configs,out/(scenario+'.log'))
        print('PASS:',target,scenario,'runtime integration',flush=True)
    mesh_resources=out/'mesh';mesh_resources.mkdir(exist_ok=True)
    mesh_config={'required':True,'package':'net.caffeinemc.mods.sodium.mixin.frapi',
                 'compatibilityLevel':'JAVA_'+str(env['java']),
                 'client':['MutableQuadViewImplMixin','QuadViewImplMixin'],'injectors':{'defaultRequire':1}}
    (mesh_resources/'mesh.mixins.json').write_text(json.dumps(mesh_config))
    mesh_source=(PORTS/'validation/PortMeshCases.java.in').read_text()
    mesh_source=mesh_source.replace('FRAPI_PACKAGE','net.fabricmc.fabric.api.'+('client.' if target=='26.2' else '')+'renderer.v1')
    mesh_source=mesh_source.replace('BLOCK_VIEW_PACKAGE','net.minecraft.client.renderer.block' if target=='26.2' else 'net.minecraft.world.level')
    mesh_source=mesh_source.replace('LAYER_METHOD','chunkLayer' if target=='26.2' else 'renderLayer')
    mesh_file=out/'PortMeshCases.java';mesh_file.write_text(mesh_source)
    extra=[api,base,mods['sodium']] if target=='26.2' else []
    mesh_cp=os.pathsep.join(map(str,[classes,mesh_resources]+common+[Path(p) for p in env['classes']]+libraries+extra))
    run([java(),'-m','jdk.compiler/com.sun.tools.javac.Main','--release',str(env['java']),'-proc:none','-cp',mesh_cp,
         '-d',classes,mesh_file,PORTS/'validation/PortMeshLauncher.java'],out/'compile-mesh.log')
    run([runtime_java,'-Xshare:off','-Dmixin.service=org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper',
         '-cp',mesh_cp,'dev.nodelll.displaymodeloptimizer.validation.PortMeshLauncher'],out/'mesh-parity.log')
    print('Validation complete:',target,flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('minecraft',choices=['26.2','1.21.11'])
    parser.add_argument('--integration-only',action='store_true',help='Repeat integration checks after a harness-only change')
    args=parser.parse_args();main(args.minecraft,args.integration_only)
