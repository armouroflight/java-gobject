package org.gnome.gir.compiler;

import gobject.runtime.GErrorException;

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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.gnome.gir.repository.Repository;
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
		logger.addHandler(new StreamHandler(System.err, new Formatter() {
			@Override
			public String format(LogRecord record) {
				return String.format("%s%n", record.getMessage());
			}
		}));
		logger.setUseParentHandlers(false);
	}

	private final Repository repo;

	private DocFactory(Repository repo) {
		this.repo = repo;
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
		if ((access & Opcodes.ACC_FINAL) != 0) {
			modifiers.add("final");
		}
		if ((access & Opcodes.ACC_STATIC) != 0) {
			modifiers.add("static");
		}
		if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
			modifiers.add("synchronized");
		}
		if ((access & Opcodes.ACC_VOLATILE) != 0) {
			modifiers.add("volatile");
		}
		if ((access & Opcodes.ACC_TRANSIENT) != 0) {
			modifiers.add("transient");
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
    	String clsName;
    	
		public ClassJavafier(Writer out) {
			this.out = out;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			clsName = name;
			try {
				out.write(strAccess(access));
				if ((access & Opcodes.ACC_INTERFACE) != 0)
					out.write(" interface ");
				else if ((access & Opcodes.ACC_ENUM) != 0)
					out.write(" enum ");
				else
					out.write(" class ");
				String iname = stripInternals(name);
				int innerIdx = iname.indexOf('$');
				if (innerIdx > 0)
					iname = iname.substring(innerIdx + 1);
				out.write(iname);
				out.write(" extends ");
				out.write(replaceInternals(superName));
				if (interfaces != null && interfaces.length > 0) {
					out.write(" implements ");
					List<String> ifaces = new ArrayList<String>();
					for (String iface : Arrays.asList(interfaces)) {
						ifaces.add(replaceInternals(iface));
					}
					out.write(join(",", ifaces));
				}
				out.write(" {\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			return null;
		}

		@Override
		public void visitAttribute(Attribute arg0) {
		}

		@Override
		public void visitEnd() {			
		}
		
		public void close() {
    		try {
				out.write("\n}\n\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}			
		}

		@Override
		public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) {
			return null;
		}

		@Override
		public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if (name.equals("<clinit>"))
				return null;
			Type[] args = Type.getArgumentTypes(descriptor);
			Type retType = Type.getReturnType(descriptor);
			try {
				out.write(strAccess(access));			
				out.write(" " + toJava(retType));
				if (name.equals("<init>"))
					name = clsName; 
				out.write(" " + name + " (");
				
				for (int i = 0; i < args.length; i++) {
					out.write(toJava(args[i]));
					if (i < args.length - 1)
						out.write(", ");
				}
				out.write(");\n\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return null;
		}

		@Override
		public void visitOuterClass(String arg0, String arg1, String arg2) {
		}

		@Override
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
    
    private static final String stripInternals(String path) {
    	int idx = path.lastIndexOf('/');
    	return path.substring(idx+1);
    }
    
    private static final String replaceInternals(String path) {
    	return path.replace('/', '.');
    }
	
	private void generateOne(File jarpath, File girpath, File outpath) throws Exception {
		repo.hashCode();
		ZipFile zf = new ZipFile(jarpath);
		
		List<? extends ZipEntry> entries = Collections.list(zf.entries());
		
		for (ZipEntry entry : entries) {
			String name = stripInternals(entry.getName().replace(".class", ""));
			if (name.contains("$"))
				continue;
			
			File javaOutPath = new File(outpath, entry.getName().replace(".class", ".java"));
			javaOutPath.getParentFile().mkdirs();

			Writer javaOut = new BufferedWriter(new FileWriter(javaOutPath));
			
			InputStream is = zf.getInputStream(entry);
			ClassReader cv = new ClassReader(is);			

			ClassJavafier visitor = new ClassJavafier(javaOut);

			cv.accept(visitor, 0);
			
			String innerPrefix = name + "$";
			/* Load its inner classes */
			for (ZipEntry subEntry : entries) {
				String innerName = stripInternals(subEntry.getName().replace(".class", ""));
				if (!innerName.startsWith(innerPrefix))
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
	
	public static void generate(File girpath, File outpath) throws Exception {
		String base = girpath.getName();

		CodeFactory.NsVer nsver = CodeFactory.NsVer.parse(base);
		if (CodeFactory.namespaceIsExcluded(nsver.namespace))
			return;
		
		Repository repo = new Repository();
		try {
			repo.require(nsver.namespace, nsver.version);
		} catch (GErrorException e) {
			logger.log(Level.WARNING, "Failed to load namespace " + nsver.namespace, e);
			return;
		}
		
		File jarpath = CodeFactory.getJarPath(repo, nsver.namespace);
		
		DocFactory factory = new DocFactory(repo);

		factory.generateOne(jarpath, girpath, outpath);
	}
	
	private static File getGirDir() {
		return new File(System.getenv("GIRDIR"));
	}	

	public static void generateAll(File outpath) throws Exception {
		File[] girs = getGirDir().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".gir");
			}
		});

		for (File gir : girs) {
			generate(gir, outpath);
		}
	}

	public static void main(String[] args) throws Exception {
		if (args[0].equals("--compileall"))
			generateAll(new File(args[1]));
		else if (args[0].endsWith(".gir")) {
			File gir = new File(args[0]);
			generate(gir, new File(args[1]));
		}
	}
}
