package org.gnome.gir.compiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class DocFactory {

	static final Logger logger = Logger.getLogger("org.gnome.gir.Compiler");
	static {
		logger.addHandler(new StreamHandler(System.out, new Formatter() {
			@Override
			public String format(LogRecord record) {
				return String.format("%s%n", record.getMessage());
			}
		}));
		logger.setUseParentHandlers(false);
	}

	private final StringTemplateGroup templates;

	private DocFactory() {
		this.templates = new StringTemplateGroup("templates");
	}
	
	public static String join(String sep, List<String> components) {
		StringBuilder builder = new StringBuilder();
		int size = components.size();
		for (int i = 0; i < size; i++) {
			String component = components.get(i);
			boolean isLast = i == size-1;
			builder.append(component);
			if (!isLast)
				builder.append(sep);
		}
		return builder.toString();
	}
	
	private StringTemplate getTemplate(String name) {
		return templates.getInstanceOf("org/gnome/gir/compiler/" + name);		
	}
	
    private static String strAccess(final int access) {
    	List<String> modifiers = new ArrayList<String>();
	    if ((access & Opcodes.ACC_PUBLIC) != 0) {
			modifiers.add("public");
		}
		if ((access & Opcodes.ACC_PRIVATE) != 0) {
			modifiers.add("private");
		}
		if ((access & Opcodes.ACC_PROTECTED) != 0) {
			modifiers.add("protected");
		}
		if ((access & Opcodes.ACC_FINAL) != 0 &&
			(access & Opcodes.ACC_ENUM) == 0) {
			modifiers.add("final");
		}
		if ((access & Opcodes.ACC_STATIC) != 0) {
			modifiers.add("static");
		}/* far too much stuff has synchronized - why? 
		if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
			modifiers.add("synchronized");
		}*/
		if ((access & Opcodes.ACC_VOLATILE) != 0) {
			modifiers.add("volatile");
		}
		if ((access & Opcodes.ACC_NATIVE) != 0) {
			modifiers.add("native");
		}
		if ((access & Opcodes.ACC_ABSTRACT) != 0 && 
		    (access & Opcodes.ACC_INTERFACE) == 0) {
			modifiers.add("abstract");
		}
	    return join(" ", modifiers);
    }
 
    private class ClassJavafier implements ClassVisitor {
    	Writer out;
    	boolean isInterface;
    	boolean isEnum;
    	boolean isInner;
    	boolean isAnonStub;
    	
    	String clsName;
    	
		public ClassJavafier(Writer out) {
			this.out = out;
		}
		
		private boolean skip() {
			return isAnonStub;
		}


		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			isInner = name.contains("$");
			
			if (!isInner) {
				StringTemplate clsToplevel = getTemplate("jclass-toplevel");
				clsToplevel.setAttribute("package", getPackage(name));
				try {
					String value = clsToplevel.toString();
					out.write(value);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				isAnonStub = false;
			} else {
				isAnonStub = name.contains("AnonStub");
			}
			
			if (skip())
				return;
			
			isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
			isEnum = (access & Opcodes.ACC_ENUM) != 0;
			clsName = stripInternals(name);
			if (isInner)
				clsName = inner1(clsName);
			
			StringTemplate clsHeader = getTemplate("jclass-decl-head");			
			clsHeader.setAttribute("access", strAccess(access));
			String clsType;
			if (isInterface)
				clsType = "interface";
			else if (isEnum)
				clsType = "enum";
			else
				clsType = "class";
			
			String iname = stripInternals(name);
			int innerIdx = iname.indexOf('$');
			if (innerIdx > 0)
				iname = iname.substring(innerIdx + 1);			
			
			clsHeader.setAttribute("clsType", clsType);
			clsHeader.setAttribute("clsName", iname);

			if (!isInterface && !isEnum) {
				clsHeader.setAttribute("extends", replaceInternals(superName));
			}
			if (interfaces != null && interfaces.length > 0) {
				if (!isInterface)
					clsHeader.setAttribute("implementsType", "implements");
				else
					clsHeader.setAttribute("implementsType", "extends");
				List<String> ifaces = new ArrayList<String>();
				for (String iface : Arrays.asList(interfaces)) {
					ifaces.add(replaceInternals(iface));
				}				
				clsHeader.setAttribute("implements", ifaces);
			}
			try {
				String value = clsHeader.toString();
				out.write(value);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			return null;
		}

		public void visitAttribute(Attribute arg0) {
		}

		public void visitEnd() {			
		}
		
		public void close() {
			if (skip())
				return;
    		try {
				out.write("\n}\n\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}			
		}

		public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) {
			return null;
		}

		public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
		}

		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if (name.equals("<clinit>") || (access & Opcodes.ACC_PRIVATE) != 0)
				return null;
			if (skip())
				return null;
			/* Skip enum methods for now, a lot of the stuff is enum internals */
			if (isEnum)
				return null;
			Type[] args = Type.getArgumentTypes(descriptor);
			Type retType = Type.getReturnType(descriptor);
			try {
				out.write(strAccess(access));	
				if (name.equals("<init>"))
					name = clsName; 
				else {
					if (!isInterface && !isInner)
						out.write(" native ");
					out.write(" " + toJava(retType));
				}

				out.write(" " + name + " (");
				
				for (int i = 0; i < args.length; i++) {
					out.write(toJava(args[i]));
					out.write(" arg" + i);
					if (i < args.length - 1)
						out.write(", ");
				}
				out.write(");\n\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return null;
		}

		public void visitOuterClass(String arg0, String arg1, String arg2) {
		}

		public void visitSource(String arg0, String arg1) {
		}
    }

    private static final String toJava(Type type) {
    	switch (type.getSort()) {
    	case Type.OBJECT:
    		return replaceInternals(type.getInternalName());
    	case Type.ARRAY:
    		return toJava(type.getElementType()) + "[]";
    	case Type.BYTE:
    		return "byte";
    	case Type.SHORT:
    		return "short";
    	case Type.CHAR:
    		return "char";
    	case Type.INT:
    		return "int";
    	case Type.LONG:
    		return "long";
    	case Type.VOID:
    		return "void";
    	case Type.BOOLEAN:
    		return "boolean";
    	case Type.FLOAT:
    		return "float";
    	case Type.DOUBLE:
    		return "double";
    	default:
    		throw new RuntimeException("" + type);
    	}
    }
    
    private static final String getPackage(String path) {
    	int idx = path.lastIndexOf('/');
    	return path.substring(0, idx).replace('/', '.');    	
    }
    
    private static final String outerClass(String name) {
    	int idx = name.indexOf('$');
    	if (idx < 0)
    		return name;
    	return name.substring(0, idx);	
    }
    
    private static final String inner1(String name) {
    	int idx = name.indexOf('$');
    	if (idx < 0)
    		return null;
    	int nextIdx = name.indexOf('$', idx+1);
    	if (nextIdx < 0)
    		return name.substring(idx+1);
    	return name.substring(idx+1, nextIdx);
    }
    
    private static final String stripInternals(String path) {
    	int idx = path.lastIndexOf('/');
    	return path.substring(idx+1);
    }
    
    private static final String replaceInternals(String path) {
    	return path.replace('$', '.').replace('/', '.');
    }
    
    private void generateJavaSource(File jarpath, File girpath, File outpath) throws Exception {
		ZipFile zf = new ZipFile(jarpath);
		
		List<? extends ZipEntry> entries = Collections.list(zf.entries());
		
		for (ZipEntry entry : entries) {
			if (entry.getName().contains("$"))
				continue;
			String fullName = entry.getName().replace(".class", "");
			String name = outerClass(stripInternals(fullName));
			
			File javaOutPath = new File(outpath, entry.getName().replace(".class", ".java"));
			javaOutPath.getParentFile().mkdirs();

			Writer javaOut = new BufferedWriter(new FileWriter(javaOutPath));
			
			InputStream is = zf.getInputStream(entry);
			ClassReader cv = new ClassReader(is);			

			ClassJavafier visitor = new ClassJavafier(javaOut);

			cv.accept(visitor, 0);

			/* Load its inner classes */
			String innerPrefix = fullName + "$";
			for (ZipEntry subEntry : entries) {
				if (!subEntry.getName().startsWith(innerPrefix))
					continue;
				String innerStripped = stripInternals(subEntry.getName().replace(".class", ""));
				String innerStrippedSuffix = innerStripped.substring(name.length()+1);
				if (innerStrippedSuffix.contains("$"))
					continue;
				
				InputStream innerInput = zf.getInputStream(subEntry);
				ClassReader innerCv = new ClassReader(innerInput);			

				ClassJavafier innerVisitor = new ClassJavafier(javaOut);
				innerCv.accept(innerVisitor, 0);
				innerVisitor.close();
			}
			
			visitor.close();
			
			javaOut.close();
			logger.info("Wrote " + javaOutPath);
		}    	
    }
    
    private void runJavadoc(String namespace, File outpath) throws Exception {
    	logger.info("Creating javadoc for " + namespace);
    	Process proc = new ProcessBuilder("javadoc", "-classpath", System.getProperty("java.class.path") + ":.",
    			"-d", outpath.toString(), "gobject.introspection." + namespace).redirectErrorStream(true)
    			.directory(outpath).start();
    	proc.getOutputStream().close();
    	for (Handler h : logger.getHandlers())
    		h.flush();
    	System.out.flush();
    	IOUtils.copy(proc.getInputStream(), System.out);
    	int ecode = proc.waitFor();
    	if (ecode != 0)
    		throw new IOException("javadoc command failed with exit code " + ecode);
    	logger.info("Done creating javadoc for " + namespace);
    }
	
	private void generateOne(String namespace, File jarpath, File girpath, File outpath) throws Exception {
		File subdir = new File(outpath, namespace);
		subdir.mkdirs();
		logger.info("Creating java source for " + namespace + " in " + subdir.toString());
		generateJavaSource(jarpath, girpath, subdir);
		
		runJavadoc(namespace, subdir);
	}
	
	private static File getJarPath(String namespace, String version) {
		return new File(CodeFactory.getTypelibDir(), String.format("%s-%s.jar", namespace, version));
	}
	
	public static void generate(File girpath, File jarpath, File outpath) throws Exception {
		String base = girpath.getName();

		CodeFactory.NsVer nsver = CodeFactory.NsVer.parse(base);
		if (CodeFactory.namespaceIsExcluded(nsver.namespace))
			return;
		
		DocFactory factory = new DocFactory();

		factory.generateOne(nsver.namespace, jarpath, girpath, outpath);
	}
	
	private static File getGirDir() {
		return new File(System.getenv("GIRDIR"));
	}

	public static void generateAll(File outpath) throws Exception {
		File[] girs = getGirDir().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".gir");
			}
		});

		logger.info("Discovered " + girs.length + " gir files");
		for (File gir : girs) {
			logger.info("Generating for " + gir.toString());
			String base = gir.getName();			
			CodeFactory.NsVer nsver = CodeFactory.NsVer.parse(base);			
			File jarpath = getJarPath(nsver.namespace, nsver.version);
			generate(gir, jarpath, outpath);
		}
	}

	public static void main(String[] args) throws Exception {
		if (args[0].equals("--compileall"))
			generateAll(new File(args[1]));
		else if (args[0].endsWith(".gir")) {
			File gir = new File(args[0]);
			File wd = gir.getParentFile();
			if (wd == null)
				wd = new File(System.getProperty("user.dir"));
			File jar = new File(wd, gir.getName().replace(".gir", ".jar"));
			generate(gir, jar, new File(args[1]));
		}
	}
}
