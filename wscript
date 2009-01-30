#! /usr/bin/env python
# -*- coding: utf-8; indent-tabs-mode: nil; python-indent: 2 -*-

import os

import Task,TaskGen,Node
from TaskGen import *
import pproc

# the following two variables are used by the target "waf dist"
VERSION='0.1.0'
APPNAME='jgir'

# these variables are mandatory ('/' are converted automatically)
srcdir = '.'
blddir = 'build'

@taskgen
def add_gir_file(self, filename):
	if not hasattr(self, 'gir_lst'): self.gir_lst = []
	self.meths.add('process_gir')
	self.gir_lst.append([filename])

@taskgen
@before('apply_core')
def process_gir(self):
	for i in getattr(self, 'gir_lst', []):
		env = self.env.copy()
		node = self.path.find_resource(i[0])

		if not node:
			raise Utils.WafError('file not found on gir obj '+i[0])

                tgt_name = os.path.splitext(node.name)[0] + '-metadata.c'
                tgt_node = self.path.find_or_declare(tgt_name)
                self.allnodes.append(tgt_node)
                task = self.create_task('gir_compiler', env)
                task.set_inputs(node)
                task.set_outputs(tgt_node)

Task.simple_task_type('gir_scanner', 'g-ir-scanner -v ${INCLUDES} --namespace=${NAMESPACE} --library=${LIB} ${SRC} --output ${TGT}', color='BLUE')
Task.simple_task_type('gir_compiler', 'g-ir-compiler ${SRC} -o ${TGT}', color='BLUE')
Task.simple_task_type('jgir_compile', 'jgir-compile ${SHLIB} ${NAMESPACE} ${TGT}', color='BLUE')

def set_options(opt):
  pass

def get_pkgconfig_var(conf, module, var):
  conf.env[var] = pproc.Popen(['pkg-config', '--variable='+var, module],
                              stdout=pproc.PIPE).communicate()[0].strip()

def configure(conf):
  conf.check_tool('gcc gnome java misc')

  conf.check_pkg('gobject-introspection-1.0', destvar='GI', mandatory=True)
  get_pkgconfig_var(conf, 'gobject-introspection-1.0', 'typelibdir')

  conf.require_java_class('java.lang.Object')
  asm_deps = ['asm', 'asm-util', 'asm-tree', 'asm-commons', 'asm-analysis']
  for dep in asm_deps:
    conf.require_jpackage_module('objectweb-asm/%s' % (dep,))
  conf.require_jpackage_module('jna')
  conf.require_jpackage_module('junit')
  conf.require_jpackage_module('gnu.getopt')
  print "Using CLASSPATH: %r" % (conf.env['CLASSPATH'],)

def build(bld):
  jsrc = bld.new_task_gen('java')
  jsrc.install_path = '${PREFIX}/share/java'
  jsrc.source = '/[A-Za-z]+\.java$'
  jsrc.jarname = 'jgir.jar'
  jsrc.source_root = 'src'

  full_cp = bld.env['CLASSPATH'] + ':' + bld.env['PREFIX'] + '/share/java/jgir.jar'
  compscript = bld.new_task_gen('subst')
  compscript.install_path = "${PREFIX}/bin"
  compscript.chmod = 0755
  compscript.source = 'src/jgir-compile-all.in'
  compscript.target = 'jgir-compile-all'
  compscript.dict = {'CLASSPATH': full_cp, 'PREFIX': bld.env['PREFIX'],
                     'TYPELIBDIR': bld.env['typelibdir']}

  compscript = bld.new_task_gen('subst')
  compscript.install_path = "${PREFIX}/bin"
  compscript.chmod = 0755
  compscript.source = 'src/jgir-compile.in'
  compscript.target = 'jgir-compile'
  compscript.dict = {'CLASSPATH': full_cp, 'TYPELIBDIR': bld.env['typelibdir']}

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
  pass
