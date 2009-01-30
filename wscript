#! /usr/bin/env python
# -*- coding: utf-8; indent-tabs-mode: nil; python-indent: 2 -*-

import os, glob, shutil

import Task,TaskGen,Node
from TaskGen import *
from Configure import conf
import pproc

# the following two variables are used by the target "waf dist"
VERSION='0.1.0'
APPNAME='jgir'

# these variables are mandatory ('/' are converted automatically)
srcdir = '.'
blddir = 'build'

def with_check_msg(msg, func):
    sys.stdout.write("Checking for " + msg)
    try:
        func()
        sys.stdout.write('...ok\n')
    except:
        sys.stdout.write('\n')
        raise

def search_xdg_datadirs(path):
    datadirs = os.environ.get('XDG_DATA_DIRS', '/usr/share').split(':')
    for dirpath in datadirs:
        target = os.path.join(dirpath, path)
        if os.path.exists(target):
            return target

@conf
def find_xdg_datadir_jar(self, prefix, name):
    name += '.jar'
    def _find_jar():
        jarpath = search_xdg_datadirs(prefix+'/'+name)
        if jarpath:       
            if 'CLASSPATH' not in self.env:
                cp = jarpath
            else:        
                cp = self.env['CLASSPATH'] + os.pathsep + jarpath
            self.env['CLASSPATH'] = cp
        else:
            raise KeyError("Failed to find required jar: " + name) 
    with_check_msg(name, _find_jar)

@conf
def find_jpackage_jar(self, name):
    self.find_xdg_datadir_jar('java', name)

def set_options(opt):
  pass

@conf
def get_pkgconfig_var(self, module, var):
  self.env[var] = pproc.Popen(['pkg-config', '--variable='+var, module],
                              stdout=pproc.PIPE).communicate()[0].strip()

def configure(conf):
  conf.check_tool('gcc gnome java misc')

  conf.check_cfg(package='gobject-introspection-1.0', uselib_store='GI', args="--cflags --libs", mandatory=True)
  conf.get_pkgconfig_var('gobject-introspection-1.0', 'typelibdir')
  conf.get_pkgconfig_var('gobject-introspection-1.0', 'girdir')

  asm_deps = ['asm', 'asm-util', 'asm-tree', 'asm-commons', 'asm-analysis']
  for dep in asm_deps:
    conf.find_jpackage_jar('objectweb-asm/%s' % (dep,))
  conf.find_jpackage_jar('jna')
  conf.find_jpackage_jar('junit')
  conf.find_jpackage_jar('gnu.getopt')
  print "Using CLASSPATH: %r" % (conf.env['CLASSPATH'],)

def build(bld):
  jsrc = bld.new_task_gen(features='java',
                          name='jsrc',
                          install_path = '${PREFIX}/share/java',
                          source_root = 'src',
                          jarname = 'jgir.jar')
  bld.install_files('${PREFIX}/share/java', 'jgir.jar')

  full_cp = bld.env['CLASSPATH'] + ':' + bld.env['PREFIX'] + '/share/java/jgir.jar'
  compscript = bld.new_task_gen('subst')
  compscript.install_path = "${PREFIX}/bin"
  compscript.chmod = 0755
  compscript.source = 'src/jgir-compile-all.in'
  compscript.target = 'jgir-compile-all'
  compscript.dict = {'CLASSPATH': full_cp, 'PREFIX': bld.env['PREFIX'],
                     'TYPELIBDIR': bld.env['typelibdir'], 'GIRDIR': bld.env['girdir']}

  compscript = bld.new_task_gen('subst')
  compscript.install_path = "${PREFIX}/bin"
  compscript.chmod = 0755
  compscript.source = 'src/jgir-compile.in'
  compscript.target = 'jgir-compile'
  compscript.dict = {'CLASSPATH': full_cp, 'TYPELIBDIR': bld.env['typelibdir']}
  
  compscript = bld.new_task_gen('subst')
  compscript.install_path = "${PREFIX}/bin"
  compscript.chmod = 0755
  compscript.source = 'src/jgir-docgen.in'
  compscript.target = 'jgir-docgen'
  compscript.dict = {'CLASSPATH': full_cp, 'PREFIX': bld.env['PREFIX'],
                     'TYPELIBDIR': bld.env['typelibdir'], 'GIRDIR': bld.env['girdir']}  

  #openjdkdir = glob.glob('/usr/share/javadoc/java-1.6*openjdk/api')[0]
  #jnadir = glob.glob('/usr/share/javadoc/jna-*')[0]
  #bld.new_task_gen(name='javadoc',
  #                 sources = jsrc.
  #                 rule='javadoc -d javadoc -sourcepath ../src -classpath ' + bld.env['CLASSPATH'] + ':.' \
  #                       + ' -link ' + openjdkdir + ' -link ' + jnadir + ' gobject.runtime')

  #libinvoke = bld.new_task_gen('cc', 'shlib')
  #libinvoke.packages = ['gobject-introspection-1.0']
  #libinvoke.source = ['tests/invoke/testfns.c']
  #libinvoke.target = 'testfns'
  #libinvoke.add_gir_file('tests/invoke/testfns.gir')
  #libinvoke.uselib = 'GI'

  #testinvoke = bld.new_task_gen('java')
  #testinvoke.source = '.*java$'
  #testinvoke.source_root = 'tests/invoke'
  #testinvoke.jarname = 'testinvoke.jar'
  #testinvoke.unit_test = 1

def shutdown():
#  if Options.is_install:
#    destdir = Build.bld.get_install_path('${PREFIX}/share/javadoc')
#    try:
#      os.makedirs(destdir)
#    except OSError, e:
#      pass
#    target = os.path.join(destdir, 'jgir')
#    shutil.rmtree(target)
#    print "Installing javadoc to " + target
#    shutil.copytree('build/javadoc', target)
