package org.gnome.gir.compiler;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_DEPRECATED;
import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;
import static org.objectweb.asm.Type.getType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.gnome.gir.gobject.GErrorException;
import org.gnome.gir.gobject.GErrorStruct;
import org.gnome.gir.gobject.GList;
import org.gnome.gir.gobject.GObjectAPI;
import org.gnome.gir.gobject.GSList;
import org.gnome.gir.gobject.GType;
import org.gnome.gir.gobject.GenericGList;
import org.gnome.gir.gobject.GlibAPI;
import org.gnome.gir.gobject.GlibRuntime;
import org.gnome.gir.gobject.NativeObject;
import org.gnome.gir.gobject.annotation.Return;
import org.gnome.gir.repository.ArgInfo;
import org.gnome.gir.repository.BaseInfo;
import org.gnome.gir.repository.BoxedInfo;
import org.gnome.gir.repository.CallableInfo;
import org.gnome.gir.repository.CallbackInfo;
import org.gnome.gir.repository.ConstantInfo;
import org.gnome.gir.repository.Direction;
import org.gnome.gir.repository.EnumInfo;
import org.gnome.gir.repository.FieldInfo;
import org.gnome.gir.repository.FieldInfoFlags;
import org.gnome.gir.repository.FlagsInfo;
import org.gnome.gir.repository.FunctionInfo;
import org.gnome.gir.repository.FunctionInfoFlags;
import org.gnome.gir.repository.InterfaceInfo;
import org.gnome.gir.repository.ObjectInfo;
import org.gnome.gir.repository.PropertyInfo;
import org.gnome.gir.repository.RegisteredTypeInfo;
import org.gnome.gir.repository.Repository;
import org.gnome.gir.repository.SignalInfo;
import org.gnome.gir.repository.StructInfo;
import org.gnome.gir.repository.Transfer;
import org.gnome.gir.repository.TypeInfo;
import org.gnome.gir.repository.TypeTag;
import org.gnome.gir.repository.UnionInfo;
import org.gnome.gir.repository.ValueInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.util.CheckClassAdapter;

import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.ptr.PointerByReference;

@SuppressWarnings("serial")
public class CodeFactory {
	
	static final Logger logger = Logger.getLogger("org.gnome.gir.Compiler");
	
	private static final Set<String> GOBJECT_METHOD_BLACKLIST = new HashSet<String>() {
		{
			add("ref");
			add("unref");
		}
	};
	
	private final Repository repo;
	private final Set<String> alreadyCompiled = new HashSet<String>();
	private final Set<String> loadFailed = new HashSet<String>();
	private final Set<String> pendingCompilation = new HashSet<String>();	
	private final Map<String, StubClassCompilation> writers = new HashMap<String, StubClassCompilation>();
	private final Map<String, GlobalsCompilation> globals = new HashMap<String, GlobalsCompilation>();
	private final Map<String,String> namespaceShlibMapping = new HashMap<String, String>();
	
	private CodeFactory(Repository repo) {
		this.repo = repo;
		this.alreadyCompiled.add("GLib");
	}
	

	public StubClassCompilation getCompilation(String namespace, String version, String name) {
		String peerInternalName = GType.getInternalName(namespace, name);
		StubClassCompilation ret = writers.get(peerInternalName);
		if (ret == null) {
			ret = new StubClassCompilation(namespace, version, name);
			writers.put(peerInternalName, ret);
		}
		return ret;
	}	
	
	public StubClassCompilation getCompilation(BaseInfo info) {
		String namespace = info.getNamespace();
		String version = repo.getNamespaceVersion(namespace);
		return getCompilation(info.getNamespace(), version, info.getName());
	}
	
	public String getGlobalsName(String namespace) {
		return GType.getInternalName(namespace, namespace+"Globals");
	}
	
	public GlobalsCompilation getGlobals(String namespace) {
		return globals.get(namespace);
	}
	
	public String getInternals(BaseInfo info) {
		return getGlobalsName(info.getNamespace()) + "$Internals";
	}
	
	public ClassCompilation getCompilation(FunctionInfo info) {
		return getGlobals(info.getNamespace());
	}
	
	private static final Map<Repository,List<ClassCompilation>> loadedRepositories 
		= new WeakHashMap<Repository, List<ClassCompilation>>();
	
	private static String getInternalName(BaseInfo info) {
		return GType.getInternalName(info.getNamespace(), info.getName());
	}
	
	static String getInternalNameMapped(BaseInfo info) {
		return GType.getInternalNameMapped(info.getNamespace(), info.getName());
	}	
	
	static boolean writeConversionToJava(MethodVisitor mv, TypeInfo info, Transfer transfer) {
		TypeTag infoTag = info.getTag();
		if (infoTag.equals(TypeTag.GLIST) || infoTag.equals(TypeTag.GSLIST)) {
			if (infoTag.equals(TypeTag.GLIST)) {			
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(GList.class), "fromNative",
						Type.getMethodDescriptor(getType(GList.class), new Type[] { getType(Pointer.class) }));
			} else {
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(GSList.class), "fromNative",
						Type.getMethodDescriptor(getType(GSList.class), new Type[] { getType(Pointer.class) }));				
			}
			mv.visitFieldInsn(GETSTATIC, getType(Transfer.class).getInternalName(), 
					transfer.name(), getType(Transfer.class).getDescriptor());
			TypeInfo param = info.getParamType(0);
			TypeTag paramTag = param.getTag();
			if (paramTag.equals(TypeTag.UTF8)) {
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(GlibRuntime.class), "convertListUtf8",
						Type.getMethodDescriptor(getType(List.class), new Type[] { getType(GenericGList.class), getType(Transfer.class) }));
				return true;
			} else if (paramTag.equals(TypeTag.INTERFACE)) {
				BaseInfo paramInfo = param.getInterface();
				Type eltClass = TypeMap.typeFromInfo(paramInfo);
				mv.visitLdcInsn(eltClass);
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(GlibRuntime.class), "convertListGObject",
						Type.getMethodDescriptor(getType(List.class), new Type[] { getType(GenericGList.class), getType(Transfer.class), getType(Class.class) }));				
				return true;
			}
		}

		return false;		
	}

	private void compile(EnumInfo info) {
		ClassCompilation compilation = getCompilation(info);
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL + ACC_SUPER + ACC_ENUM, compilation.internalName, 
				"Ljava/lang/Enum<L" + compilation.internalName + ";>;", "java/lang/Enum", 
				null);
		ValueInfo[] values = info.getValueInfo();
		for (ValueInfo valueInfo : values) {
			String name = NameMap.enumNameToUpper(info.getName(), valueInfo.getName());			
			FieldVisitor fv = compilation.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM, 
						name, "L" + compilation.internalName + ";", null, null);
			fv.visitEnd();				
		}
		
		/* And now a HUGE chunk of stuff to comply with the enum spec */
		
		String arrayDescriptor = "[L" + compilation.internalName + ";";		
		
		FieldVisitor fv = compilation.writer.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC + ACC_SYNTHETIC, 
				"ENUM$VALUES", arrayDescriptor, null, null);
		fv.visitEnd();
		
		MethodVisitor mv = compilation.writer.visitMethod(ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ILOAD, 2);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", arrayDescriptor, null, l0, l1, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
		
		mv = compilation.writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		int i = 0;
		for (ValueInfo valueInfo : values) {
			String name = NameMap.enumNameToUpper(info.getName(), valueInfo.getName());
			mv.visitTypeInsn(NEW, compilation.internalName);
			mv.visitInsn(DUP);
			mv.visitLdcInsn(name);
			mv.visitIntInsn(BIPUSH, i);
			i++;
			mv.visitMethodInsn(INVOKESPECIAL, compilation.internalName, "<init>", "(Ljava/lang/String;I)V");
			mv.visitFieldInsn(PUTSTATIC, compilation.internalName, 
						name, "L" + compilation.internalName+ ";");			
		}
		mv.visitIntInsn(BIPUSH, values.length);
		mv.visitTypeInsn(ANEWARRAY, compilation.internalName);
		i = 0;
		for (ValueInfo valueInfo : values) {
			String name = NameMap.enumNameToUpper(info.getName(), valueInfo.getName());			
			mv.visitInsn(DUP);			
			mv.visitIntInsn(BIPUSH, i);
			i++;
			mv.visitFieldInsn(GETSTATIC, compilation.internalName, name, 
						"L" + compilation.internalName + ";");
			mv.visitInsn(AASTORE);			
		}
		mv.visitFieldInsn(PUTSTATIC, compilation.internalName, "ENUM$VALUES", arrayDescriptor);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();	
		
		mv = compilation.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "values", "()" + arrayDescriptor, null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitFieldInsn(GETSTATIC, compilation.internalName, "ENUM$VALUES", arrayDescriptor);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ASTORE, 0);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ISTORE, 1);
		mv.visitTypeInsn(ANEWARRAY, compilation.internalName);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ASTORE, 2);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ILOAD, 1);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
		mv.visitVarInsn(ALOAD, 2);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		mv = compilation.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "valueOf", "(Ljava/lang/String;)L" + compilation.internalName + ";", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLdcInsn(Type.getType("L" + compilation.internalName + ";"));
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;");
		mv.visitTypeInsn(CHECKCAST, compilation.internalName);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		/* For NativeMapped */
		mv = compilation.writer.visitMethod(ACC_PUBLIC, "fromNative", "(Ljava/lang/Object;Lcom/sun/jna/FromNativeContext;)Ljava/lang/Object;", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
		mv.visitVarInsn(ASTORE, 3);
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitMethodInsn(INVOKESTATIC, compilation.internalName, "values", 
				"()[L" + compilation.internalName +  ";");
		mv.visitVarInsn(ALOAD, 3);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
		mv.visitInsn(AALOAD);
		mv.visitInsn(ARETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("nativeValue", "Ljava/lang/Object;", null, l0, l2, 1);
		mv.visitLocalVariable("context", "Lcom/sun/jna/FromNativeContext;", null, l0, l2, 2);
		mv.visitLocalVariable("val", "Ljava/lang/Integer;", null, l1, l2, 3);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
		
		mv = compilation.writer.visitMethod(ACC_PUBLIC, "nativeType", "()Ljava/lang/Class;", "()Ljava/lang/Class<*>;", null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Integer;"));
		mv.visitInsn(ARETURN);
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "Lorg/gnome/gir/compiler/TestEnum;", null, l0, l1, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		mv = compilation.writer.visitMethod(ACC_PUBLIC, "toNative", "()Ljava/lang/Object;", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, compilation.internalName, "ordinal", "()I");
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
		mv.visitInsn(ARETURN);
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
		
		compilation.close();
	}
	
	private void compile(FlagsInfo info) {
		ClassCompilation compilation = getCompilation(info);
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, compilation.internalName, null, "java/lang/Object", null);
		ValueInfo[] values = info.getValueInfo();
		for (ValueInfo valueInfo : values) {
			FieldVisitor fv = compilation.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, 
						NameMap.enumNameToUpper(info.getName(), valueInfo.getName()), "J", null, valueInfo.getValue());
			fv.visitEnd();				
		}
		compilation.close();
	}	
	
	private void compileDefaultConstructors(ObjectInfo info, ClassCompilation compilation) {		
		BaseInfo parent = info.getParent(); 
		String parentInternalType = getInternalNameMapped(parent);
		
		MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, compilation.internalName, "getGType", "()Lorg/gnome/gir/gobject/GType;");
		mv.visitInsn(ACONST_NULL);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/GType;[Ljava/lang/Object;)V");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l2, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/Object;)V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, compilation.internalName, "getGType", "()Lorg/gnome/gir/gobject/GType;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/GType;[Ljava/lang/Object;)V");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L"+ compilation.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l2, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/util/Map;)V", 
				"(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, compilation.internalName, "getGType", "()Lorg/gnome/gir/gobject/GType;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/GType;Ljava/util/Map;)V");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L"+ compilation.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("args", "Ljava/util/Map;", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", l0, l2, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
		
		mv = compilation.writer.visitMethod(ACC_PROTECTED, "<init>", "(Lorg/gnome/gir/gobject/GType;[Ljava/lang/Object;)V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/GType;[Ljava/lang/Object;)V");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L"+ compilation.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("gtype", "Lorg/gnome/gir/gobject/GType;", null, l0, l2, 1);		
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l2, 2);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		mv = compilation.writer.visitMethod(ACC_PROTECTED, "<init>", "(Lorg/gnome/gir/gobject/GType;Ljava/util/Map;)V", 
				"(Lorg/gnome/gir/gobject/GType;Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/GType;Ljava/util/Map;)V");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L"+ compilation.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("gtype", "Lorg/gnome/gir/gobject/GType;", null, l0, l2, 1);		
		mv.visitLocalVariable("args", "Ljava/util/Map;", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", l0, l2, 2);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
	}
	
	private void writeStaticConstructor(ObjectInfo info, ClassCompilation compilation, FunctionInfo fi) {	
		String globalInternalsName = getInternals(info);

		ArgInfo[] argInfos = fi.getArgs();
		CallableCompilationContext ctx = tryCompileCallable(fi, null, false, true, null);
		if (ctx == null)
			return;
		List<Type> args = ctx.argTypes;	 
		String descriptor = ctx.getDescriptor();
		
		int nArgs = args.size();
		
		String name = ctx.name;
		if (name.equals("new"))
			name = "newDefault";
		MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, name, descriptor, null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitTypeInsn(NEW, compilation.internalName);
		mv.visitInsn(DUP);
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "library", "Lcom/sun/jna/NativeLibrary;");
		mv.visitLdcInsn(fi.getSymbol());
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/NativeLibrary", "getFunction", "(Ljava/lang/String;)Lcom/sun/jna/Function;");
		mv.visitLdcInsn(Type.getType("Lcom/sun/jna/Pointer;"));
		mv.visitIntInsn(BIPUSH, args.size());
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		int argOffset = 0;
		for (int i = 0; i < nArgs; i++) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, i);
			Type argType = args.get(i);
			LocalVariable.writeLoadArgument(mv, argOffset, argType);
			argOffset += argType.getSize();			
			mv.visitInsn(AASTORE);			
		}
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "invocationOptions", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invoke", "(Ljava/lang/Class;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, "com/sun/jna/Pointer");
		mv.visitMethodInsn(INVOKESTATIC, compilation.internalName, "initializer", 
			"(Lcom/sun/jna/Pointer;)Lorg/gnome/gir/gobject/Handle$Initializer;");		
		mv.visitMethodInsn(INVOKESPECIAL, compilation.internalName, "<init>", "(Lorg/gnome/gir/gobject/Handle$Initializer;)V");
		mv.visitInsn(ARETURN);
		Label l4 = new Label();
		mv.visitLabel(l4);
		for (int i = 0; i < nArgs; i++) {
			mv.visitLocalVariable(argInfos[i].getName(), args.get(i).toString(), null, l0, l4, i);
		}
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
	}	
	
	private void writeConstructor(ObjectInfo info, ClassCompilation compilation, FunctionInfo fi) {	
		String globalInternalsName = getInternals(info);

		CallableCompilationContext ctx = tryCompileCallable(fi);
		if (ctx.throwsGError) {
			logger.warning(String.format("Skipping constructor %s which uses GError", 
					fi.getIdentifier()));
			return;
		}
		List<Type> args = ctx.argTypes;
		BaseInfo parent = info.getParent(); 
		String parentInternalType = getInternalNameMapped(parent);
		String descriptor = ctx.getDescriptor();
		
		int nArgs = args.size();
		
		MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "library", "Lcom/sun/jna/NativeLibrary;");
		mv.visitLdcInsn(fi.getSymbol());
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/NativeLibrary", "getFunction", "(Ljava/lang/String;)Lcom/sun/jna/Function;");
		mv.visitLdcInsn(Type.getType(Pointer.class));
		mv.visitIntInsn(BIPUSH, args.size());
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		LocalVariableTable locals = ctx.allocLocals();
		for (int i = 0; i < nArgs; i++) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, i);
			LocalVariable var = locals.get(i+1);
			var.writeLoadArgument(mv);
			mv.visitInsn(AASTORE);			
		}
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "invocationOptions", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invoke", "(Ljava/lang/Class;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, "com/sun/jna/Pointer");
		mv.visitMethodInsn(INVOKESTATIC, compilation.internalName, "initializer", 
			"(Lcom/sun/jna/Pointer;)Lorg/gnome/gir/gobject/Handle$Initializer;");		
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/Handle$Initializer;)V");
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitInsn(RETURN);
		Label l4 = new Label();
		mv.visitLabel(l4);
		locals.writeLocals(mv, l0, l4);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
	}
	
	private void compileSignal(StubClassCompilation compilation, CallableCompilationContext ctx, SignalInfo sig,
				boolean isInterfaceSource, boolean isInterfaceTarget) {
		String rawSigName = sig.getName();
		String sigName = rawSigName.replace('-', '_');
		String sigClass = NameMap.ucaseToPascal(sigName);
		String sigHandlerName = "on" + sigClass;
		String descriptor = Type.getMethodDescriptor(ctx.returnType, ctx.argTypes.toArray(new Type[0]));
		String internalName = ctx.argTypes.get(0).getInternalName() + "$" + sigClass;
				
		if (!isInterfaceTarget) {
			InnerClassCompilation sigCompilation = compilation.newInner(sigClass);
			compilation.writer.visitInnerClass(sigCompilation.internalName, compilation.internalName, sigClass, 
					ACC_PUBLIC + ACC_ABSTRACT + ACC_STATIC + ACC_INTERFACE);
			sigCompilation.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, 
					sigCompilation.internalName, null, "java/lang/Object", new String[] { "com/sun/jna/Callback" });
			sigCompilation.writer.visitInnerClass(sigCompilation.internalName, compilation.internalName, 
					sigClass, ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT + ACC_INTERFACE);
		
			writeJnaCallbackTypeMapper(sigCompilation);

			MethodVisitor mv = sigCompilation.writer.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, sigHandlerName, descriptor, null, null);
			mv.visitEnd();			
			
			sigCompilation.writer.visitEnd();			
		}
		
		/* public final long connect(SIGCLASS proxy) */
		int access = ACC_PUBLIC;
		if (isInterfaceSource)
			access += ACC_ABSTRACT;
		MethodVisitor mv = compilation.writer.visitMethod(access, "connect", "(L"+ internalName + ";)J", null, null);
		if (!isInterfaceSource) {
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitLdcInsn(rawSigName);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, compilation.internalName, "connect", "(Ljava/lang/String;Lcom/sun/jna/Callback;)J");
			mv.visitInsn(LRETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L"+ compilation.internalName + ";", null, l0, l1, 0);
			mv.visitLocalVariable("c", "L" + internalName + ";", null, l0, l1, 1);
			mv.visitMaxs(0, 0);
		}
		mv.visitEnd();
	}
	
	private void writeHandleInitializer(ClassCompilation compilation, String parentInternalName) {
		MethodVisitor mv = compilation.writer.visitMethod(ACC_PROTECTED, "<init>", "(Lorg/gnome/gir/gobject/Handle$Initializer;)V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", "(Lorg/gnome/gir/gobject/Handle$Initializer;)V");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("init", "Lorg/gnome/gir/gobject/Handle$Initializer;", null, l0, l2, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();	
	}
	
	private void writeProperties(StubClassCompilation compilation, Type objectType,
			PropertyInfo[] props, Set<String> sigs, 
			boolean isInterfaceSource, boolean isInterfaceTarget) {
		for (PropertyInfo prop : props) {
			Type type = TypeMap.toJava(prop.getType());
			if (type == null) {
				logger.warning(String.format("Skipping unhandled property type %s of %s", prop.getName(), compilation.internalName));
				continue;
			}
			int propFlags = prop.getFlags();
			Class<?> propBox = TypeMap.getPrimitiveBox(type);
			Type propTypeBox;
			if (propBox != null)
				propTypeBox = Type.getType(propBox);
			else
				propTypeBox = type;
			if ((propFlags & FieldInfoFlags.READABLE) != 0) {
				String propPascal = NameMap.ucaseToPascal(prop.getName());
				
				writePropertyNotify(compilation, objectType, prop, type, propBox,
						isInterfaceSource, isInterfaceTarget);				
				
				String getterName = "get" + propPascal;
				String descriptor = Type.getMethodDescriptor(type, new Type[] {});
				String signature = TypeMap.getUniqueSignature(getterName, type, Arrays.asList(new Type[] {}));
				if (sigs.contains(signature)) {
					logger.warning("Getter " + getterName + " duplicates signature: " 
								+ signature);
					continue;
				}
				sigs.add(signature);
				int access = ACC_PUBLIC;
				if (isInterfaceSource)
					access += ACC_ABSTRACT;				
				MethodVisitor mv = compilation.writer.visitMethod(access, getterName, 
					descriptor, null, null);
				if (!isInterfaceSource) {
					mv.visitCode();
					Label l0 = new Label();
					mv.visitLabel(l0);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitLdcInsn(prop.getName());
					mv.visitMethodInsn(INVOKEVIRTUAL, compilation.internalName, "get", 
							Type.getMethodDescriptor(getType(Object.class), new Type[] { getType(String.class) }));
					mv.visitTypeInsn(CHECKCAST, propTypeBox.getInternalName());
					if (propBox != null)
						mv.visitMethodInsn(INVOKEVIRTUAL, propTypeBox.getInternalName(), type.getClassName() + "Value",
								"()" + type.getDescriptor());
					mv.visitInsn(type.getOpcode(IRETURN));
					Label l1 = new Label();
					mv.visitLabel(l1);
					mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
					mv.visitMaxs(0, 0);
				}
				mv.visitEnd();		
			}
			if ((propFlags & FieldInfoFlags.WRITABLE) != 0) {
				String setterName = "set" + NameMap.ucaseToPascal(prop.getName());
				String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { type });
				String signature = TypeMap.getUniqueSignature(setterName, Type.VOID_TYPE, Arrays.asList(new Type[] { type }));
				if (sigs.contains(signature)) {
					logger.warning("Setter " + setterName + " duplicates signature: " 
								+ signature);
					continue;
				}
				sigs.add(signature);
				int access = ACC_PUBLIC;
				if (isInterfaceSource)
					access += ACC_ABSTRACT;					
				MethodVisitor mv = compilation.writer.visitMethod(access, setterName, 
					descriptor, null, null);
				if (!isInterfaceSource) {
				mv.visitCode();
					Label l0 = new Label();
					mv.visitLabel(l0);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitLdcInsn(prop.getName());
					mv.visitVarInsn(type.getOpcode(ILOAD), 1);
					if (propBox != null)
						mv.visitMethodInsn(INVOKESTATIC, propTypeBox.getInternalName(), "valueOf", "("
								+ type.getDescriptor() + ")" + propTypeBox.getDescriptor());
					mv.visitMethodInsn(INVOKEVIRTUAL, compilation.internalName, "set",
							Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { getType(String.class), getType(Object.class) }));
					mv.visitInsn(RETURN);
					Label l1 = new Label();
					mv.visitLabel(l1);
					mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
					mv.visitLocalVariable("arg", type.getDescriptor(), null, l0, l1, 1);
					mv.visitMaxs(0, 0);
				}
				mv.visitEnd();
			};
		}		
	}
		
	private void writePropertyNotify(StubClassCompilation compilation, Type objectType, 
			PropertyInfo prop, Type propType, Class<?> propBox,
			boolean isInterfaceSource, boolean isInterfaceTarget) {
		String propPascal = NameMap.ucaseToPascal(prop.getName());		
		String notifyClass = propPascal + "PropertyNotify";
		String sigHandlerName = "on" + notifyClass;
		String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { objectType, propType } );
		String internalName = objectType.getInternalName() + "$" + notifyClass;
				
		if (!isInterfaceTarget) {
			InnerClassCompilation sigCompilation = compilation.newInner(notifyClass);
			compilation.writer.visitInnerClass(sigCompilation.internalName, compilation.internalName, notifyClass, 
					ACC_PUBLIC + ACC_ABSTRACT + ACC_STATIC + ACC_INTERFACE);
			sigCompilation.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, 
					sigCompilation.internalName, null, "java/lang/Object", new String[] { "com/sun/jna/Callback" });
			sigCompilation.writer.visitInnerClass(sigCompilation.internalName, compilation.internalName, 
					notifyClass, ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT + ACC_INTERFACE);
		
			writeJnaCallbackTypeMapper(sigCompilation);

			MethodVisitor mv = sigCompilation.writer.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, sigHandlerName, descriptor, null, null);
			mv.visitEnd();			
			
			sigCompilation.writer.visitEnd();			
		}
		
		/* public final long connectNotify(SIGCLASS proxy) */
		int access = ACC_PUBLIC;
		if (isInterfaceSource)
			access += ACC_ABSTRACT;
		MethodVisitor mv = compilation.writer.visitMethod(access, "connectNotify", "(L"+ internalName + ";)J", null, null);
		if (!isInterfaceSource) {
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitLdcInsn(prop.getName());
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, compilation.internalName, "connectNotify", "(Ljava/lang/String;Lcom/sun/jna/Callback;)J");
			mv.visitInsn(LRETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L"+ compilation.internalName + ";", null, l0, l1, 0);
			mv.visitLocalVariable("c", "L" + internalName + ";", null, l0, l1, 1);
			mv.visitMaxs(0, 0);
		}
		mv.visitEnd();
	}

	private void compile(ObjectInfo info) {
		StubClassCompilation compilation = getCompilation(info);
		
		String internalName = getInternalName(info);
		BaseInfo parent = info.getParent();
		String parentInternalName;
				
		parentInternalName = getInternalNameMapped(parent);
		
		String[] interfaces = null;
		List<InterfaceInfo> giInterfaces = TypeMap.getUniqueInterfaces(info);
		if (giInterfaces.size() > 0) {
			interfaces = new String[giInterfaces.size()];
		}
		for (int i = 0; i < giInterfaces.size(); i++) {
			interfaces[i] = getInternalNameMapped(giInterfaces.get(i));
		}
		
		int flags = ACC_PUBLIC + ACC_SUPER;
		boolean isAbstract = info.isAbstract();
		if (isAbstract)
			flags += ACC_ABSTRACT;
		compilation.writer.visit(V1_6, flags, internalName, null, parentInternalName, interfaces);
		
		if (isAbstract) {
			/* We need to write out a concrete implementation, just in case a method returns an abstract
			 * class and we don't have one mapped.  An example is GFileMonitor from Gio.  This
			 * is similar to the interface case.
			 */			
			InnerClassCompilation anonProxy = compilation.newInner("AnonStub");
			compilation.writer.visitInnerClass(anonProxy.internalName,
					compilation.internalName, "AnonStub", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);
			anonProxy.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER + ACC_FINAL, anonProxy.internalName, null, compilation.internalName, null);
			writeHandleInitializer(anonProxy, compilation.internalName);
		}
		
		for (SignalInfo sig : info.getSignals()) {
			CallableCompilationContext ctx = tryCompileCallable(sig);
			if (ctx == null)
				continue;
			// Insert the object as first parameter
			ctx.argTypes.add(0, TypeMap.typeFromInfo(info));				
			compileSignal(compilation, ctx, sig, false, false);
		}
		
		writeGetGType(info, compilation);
		
		writeHandleInitializer(compilation, parentInternalName);
		
		compileDefaultConstructors(info, compilation);
		
		FunctionInfo baseNewCtor = null;
		Set<FunctionInfo> extraCtors = new HashSet<FunctionInfo>();
		
		/* Our strategy with constructors is that we only treat "new" as special.
		 * Everything else gets turned into a static method.
		 */
		for (FunctionInfo fi : info.getMethods()) {
			CallableCompilationContext ctx = tryCompileCallable(fi);
			if (ctx == null || !ctx.isConstructor)
				continue;

			if (ctx.argTypes.size() == 0) {
				logger.fine("Skipping 0-args constructor: " + fi.getName());
				continue;
			}
			
			if (fi.getName().equals("new") && baseNewCtor == null)
				baseNewCtor = fi;
			else
				extraCtors.add(fi);
		}
		
		if (baseNewCtor != null) {
			writeConstructor(info, compilation, baseNewCtor);
		}		
			
		for (FunctionInfo ctor : extraCtors) {
			writeStaticConstructor(info, compilation, ctor);
		}
		
		Set<String> sigs = new HashSet<String>();
		
		// Write out property getters and setters - we do this before methods
		// because we want them to override any extant getters or setters with
		// unknown transfer properties
		writeProperties(compilation, Type.getObjectType(compilation.internalName),
				info.getProperties(), sigs, false, false);		
		
		// Now do methods		
		for (FunctionInfo fi : info.getMethods()) {	
			if (GOBJECT_METHOD_BLACKLIST.contains(fi.getName()))
				continue;
			CallableCompilationContext ctx = tryCompileCallable(fi, info, true, false, sigs);
			if (ctx == null || ctx.isConstructor)
				continue;
			int callableFlags = ACC_PUBLIC;
			if (!ctx.isMethod)
				callableFlags |= ACC_STATIC;
			writeCallable(callableFlags, compilation, fi, ctx);
		}
		for (InterfaceInfo iface : giInterfaces) {
			if (TypeMap.introspectionImplements(info.getParent(), iface))
				continue;
			for (FunctionInfo fi: iface.getMethods()) {
				CallableCompilationContext ctx = tryCompileCallable(fi, iface, true, false, sigs);
				if (ctx == null)
					continue;
				ctx.isInterfaceMethod = true;
				ctx.targetInterface = iface;
				writeCallable(ACC_PUBLIC, compilation, fi, ctx);
			}
			Type ifaceType = TypeMap.typeFromInfo(iface);
			for (SignalInfo sig : iface.getSignals()) {
				CallableCompilationContext ctx = tryCompileCallable(sig, null);
				if (ctx == null)
					continue;
				// Insert the object as first parameter
				ctx.argTypes.add(0, ifaceType);				
				compileSignal(compilation, ctx, sig, false, true);
			}
			writeProperties(compilation, ifaceType, iface.getProperties(), sigs,
					false, true);
		}
		
		compilation.close();	
	}
	
	private void compile(InterfaceInfo info) {
		StubClassCompilation compilation = getCompilation(info);
		GlobalsCompilation globals = getGlobals(info.getNamespace());
		
		String internalName = getInternalName(info);
		
		List<String> extendsList = new ArrayList<String>();
		extendsList.add("org/gnome/gir/gobject/GObject$GObjectProxy");
		
		for (BaseInfo prereq : info.getPrerequisites()) {
			if (!(prereq instanceof InterfaceInfo))
				continue;
			InterfaceInfo prereqIface = (InterfaceInfo) prereq;
			Type prereqType = TypeMap.typeFromInfo(prereqIface);
			extendsList.add(prereqType.getInternalName());
		}
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, internalName, null, "java/lang/Object", 
				extendsList.toArray(new String[]{}));
		globals.interfaceTypes.put(internalName, info.getTypeInit());
		
		Type ifaceType = TypeMap.typeFromInfo(info);
		for (SignalInfo sig : info.getSignals()) {
			CallableCompilationContext ctx = tryCompileCallable(sig, null);
			if (ctx == null)
				continue;
			// Insert the object as first parameter
			ctx.argTypes.add(0, ifaceType);				
			compileSignal(compilation, ctx, sig, true, false);
		}		
		
		Set<String> sigs = new HashSet<String>();
		
		writeProperties(compilation, ifaceType, 
				info.getProperties(), sigs, true, false);
		
		for (FunctionInfo fi : info.getMethods()) {
			CallableCompilationContext ctx = tryCompileCallable(fi, info, true, false, sigs);
			if (ctx == null)
				continue;			
			String name = NameMap.ucaseToCamel(fi.getName());
			String descriptor = Type.getMethodDescriptor(ctx.returnType, ctx.argTypes.toArray(new Type[0]));
			MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, name, descriptor, null, null);
			mv.visitEnd();			
		}

		InnerClassCompilation anonProxy = compilation.newInner("AnonStub");
		compilation.writer.visitInnerClass(anonProxy.internalName,
				compilation.internalName, "AnonStub", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);
		anonProxy.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER + ACC_FINAL, anonProxy.internalName, null, "org/gnome/gir/gobject/GObject", 
				new String[] { compilation.internalName });
		writeHandleInitializer(anonProxy, "org/gnome/gir/gobject/GObject");
		sigs = new HashSet<String>();		
		for (FunctionInfo fi: info.getMethods()) {
			CallableCompilationContext ctx = tryCompileCallable(fi, info, true, false, sigs);
			if (ctx == null)
				continue;
			ctx.isInterfaceMethod = false;
			ctx.targetInterface = info;
			writeCallable(ACC_PUBLIC, anonProxy, fi, ctx);
		}		
	
		compilation.close();
	}
	
	static final class CallableCompilationContext {
		CallableInfo info;
		boolean isMethod;
		boolean isConstructor;
		String name;
		Type returnType;
		ArgInfo[] args;
		Type thisType;
		List<Type> argTypes;
		String argSignature;
		List<String> argNames = new ArrayList<String>();
		boolean throwsGError;
		boolean isInterfaceMethod = false;
		InterfaceInfo targetInterface = null;
		Map<Integer, Integer> lengthOfArrayIndices = new HashMap<Integer,Integer>();
		Map<Integer, Integer> arrayToLengthIndices = new HashMap<Integer,Integer>();
		Set<Integer> userDataIndices = new HashSet<Integer>();
		Map<Integer,Integer> destroyNotifyIndices = new HashMap<Integer,Integer>();		
		
		public CallableCompilationContext() {
			// TODO Auto-generated constructor stub
		}

		public String getDescriptor() {
			return Type.getMethodDescriptor(this.returnType, argTypes.toArray(new Type[] {}));
		}
		
		public String getSignature() {
			return TypeMap.getUniqueSignature(name, returnType, argTypes);
		}
		
		public Set<Integer> getAllEliminiated() {
			Set<Integer> eliminated = new HashSet<Integer>();
			eliminated.addAll(lengthOfArrayIndices.keySet());
			eliminated.addAll(userDataIndices);
			eliminated.addAll(destroyNotifyIndices.keySet());
			return eliminated;
		}
		
		public int argOffsetToApi(int offset) {
			/* Calculate how many arguments we deleted */
			int nEliminated = 0;
			for (Integer i : getAllEliminiated()) {
				if (offset >= i)
					nEliminated++;
			}
			
			/* And simply subtract that from the offset */
			return offset - nEliminated;
		}

		public LocalVariableTable allocLocals() {
			return new LocalVariableTable(this);
		}
	}
	
	private CallableCompilationContext tryCompileCallable(CallableInfo si) {
		return tryCompileCallable(si, true, null);
	}
	
	private CallableCompilationContext tryCompileCallable(CallableInfo si, Set<String> seenSignatures) {
		return tryCompileCallable(si, true, seenSignatures);
	}
	
	private CallableCompilationContext tryCompileCallable(CallableInfo si, boolean allowError,
			Set<String> seenSignatures) {
		return tryCompileCallable(si, null, allowError, false, seenSignatures);
	}
	
	private CallableCompilationContext tryCompileCallable(CallableInfo si, RegisteredTypeInfo thisType,
			boolean allowError,
			boolean isStaticCtor,
			Set<String> seenSignatures) {
		CallableCompilationContext ctx = new CallableCompilationContext();
		if (si instanceof FunctionInfo) {
			FunctionInfo fi = (FunctionInfo) si;
			
			if (fi.isDeprecated())
				return null;
			
			int flags = fi.getFlags();
			ctx.isConstructor = !isStaticCtor && (flags & FunctionInfoFlags.IS_CONSTRUCTOR) != 0;
			ctx.isMethod = !ctx.isConstructor && (flags & FunctionInfoFlags.IS_METHOD) != 0;
			ctx.throwsGError = (flags & FunctionInfoFlags.THROWS) != 0;
		}
		ctx.info = si;
		ctx.args = si.getArgs();
		if (ctx.isConstructor) {
			ctx.returnType = Type.VOID_TYPE;
			ctx.thisType = TypeMap.getCallableReturn(si); 
		} else {
			if (ctx.isMethod && thisType != null)
				ctx.thisType = Type.getObjectType(getInternalNameMapped(thisType));
			ctx.returnType = TypeMap.getCallableReturn(si);		
			if (ctx.returnType != null && TypeMap.visitCallableReturnSignature(si, null)) {
				SignatureVisitor visitor = new SignatureWriter();
				SignatureVisitor returnVisitor = visitor.visitReturnType();
				returnVisitor.visitClassType(ctx.returnType.getInternalName());
				if (TypeMap.visitCallableReturnSignature(si, returnVisitor)) {
					visitor.visitEnd();
					ctx.argSignature = visitor.toString();
				}
			}
		}
		if (ctx.returnType == null) {
			logger.warning("Skipping callable with unhandled return signature: "+ si.getIdentifier());
			return null;
		}
		ArgInfo[] args = ctx.args;
		
		List<Type> types = new ArrayList<Type>();		
		int firstSeenCallback = -1;
		for (int i = 0; i < args.length; i++) {
			ArgInfo arg = args[i];
			Type t;
			TypeInfo info = arg.getType();
			TypeTag tag = info.getTag();			
			if (tag.equals(TypeTag.ERROR)) {
				if (allowError)
					continue;
				logger.warning("Skipping callable with invalid error argument: " + si.getIdentifier());
				return null;
			}
			int argOffset = i;
			if (ctx.isMethod) argOffset++;			
			if (tag.equals(TypeTag.VOID) && arg.getName().contains("data")) {
				ctx.userDataIndices.add(argOffset);
				continue;
			} else if (TypeMap.isDestroyNotify(arg)) {
				if (firstSeenCallback == -1) {
					logger.warning("Skipping callable with unpaired DestroyNotify: " + si.getIdentifier());
					return null;
				}
				ctx.destroyNotifyIndices.put(argOffset, firstSeenCallback);
				continue;
			} else if (arg.getDirection() == Direction.IN &&
					info.getTag().equals(TypeTag.INTERFACE) &&
					info.getInterface() instanceof CallbackInfo) {
				if (firstSeenCallback >= 0) {
					int off = ctx.isMethod ? firstSeenCallback - 1 : firstSeenCallback;
					logger.warning("Skipping callable with multiple callbacks: " + si.getIdentifier() +
							" first:" + args[off] + " second:" + arg);
					return null;					
				}			
				firstSeenCallback = argOffset;
			} else if (tag.equals(TypeTag.ARRAY) && arg.getDirection().equals(Direction.IN)) {
				int lenIdx = arg.getType().getArrayLength();
				if (lenIdx >= 0) {
					ctx.lengthOfArrayIndices.put(lenIdx, argOffset);
					ctx.arrayToLengthIndices.put(argOffset, lenIdx);
				}
			}	
			t = TypeMap.toJava(arg);
			if (t == null) {
				logger.warning(String.format("Unhandled argument %s in callable %s", arg, si.getIdentifier()));
				return null;
			}
			types.add(t);
			ctx.argNames.add(arg.getName());
		}
		
		/* Now go through and remove array length indices */
		List<Type> filteredTypes = new ArrayList<Type>();
		for (int i = 1; i < types.size()+1; i++) {
			Integer index = ctx.lengthOfArrayIndices.get(i);
			if (index == null) {		
				filteredTypes.add(types.get(i-1));
			}
		}
		
		ctx.argTypes = filteredTypes;
		
		ctx.name = NameMap.ucaseToCamel(si.getName());
		
		if (seenSignatures != null) {
			String signature = TypeMap.getUniqueSignature(ctx.name, ctx.returnType, ctx.argTypes);
			if (seenSignatures.contains(signature)) {
				logger.warning(String.format("Callable %s duplicates signature: %s", 
						si.getIdentifier(), signature));
				return null;
			}
			seenSignatures.add(signature);
		}

		return ctx;
	}
	
	private void writeCallable(int accessFlags, ClassCompilation compilation, FunctionInfo fi,
			CallableCompilationContext ctx) {
		String descriptor = ctx.getDescriptor();
		String name = ctx.name;
		
		String[] exceptions = null;
		if (ctx.throwsGError) {
			exceptions = new String[] { Type.getInternalName(GErrorException.class) };
		}
		
		if (fi.isDeprecated()) {
			accessFlags += ACC_DEPRECATED;
		}
		MethodVisitor mv = compilation.writer.visitMethod(accessFlags, 
				name, descriptor, ctx.argSignature, exceptions);
		if (fi.isDeprecated()) {
			AnnotationVisitor av = mv.visitAnnotation(Type.getType(Deprecated.class).getDescriptor(), true);
			av.visitEnd();
		}
		
		String globalInternalsName = getInternals(fi);	
		String symbol = fi.getSymbol();
		
		Transfer returnTransfer = fi.getCallerOwns();
		TypeInfo returnGIType = fi.getReturnType();
		TypeTag returnTypeTag = returnGIType.getTag();
		Class<?> primitiveBox = TypeMap.getPrimitiveBox(ctx.returnType);
		Type nativeReturnType;
		if (primitiveBox != null) {
			nativeReturnType = Type.getType(primitiveBox);
		} else if /* Now test for special return value transfer handling */   
			((returnTransfer.equals(Transfer.NOTHING) &&
				 (returnTypeTag.equals(TypeTag.INTERFACE))) ||
				(returnTransfer.equals(Transfer.EVERYTHING) && 
				 returnTypeTag.equals(TypeTag.UTF8)) || 
			 returnTypeTag.equals(TypeTag.GLIST) ||
			 returnTypeTag.equals(TypeTag.GSLIST)) {
			nativeReturnType = Type.getType(Pointer.class);
		} else { 
			nativeReturnType = ctx.returnType;
		}
		
		if (primitiveBox == null) {
			AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Return.class), true);
			av.visitEnum("transfer", Type.getDescriptor(Transfer.class), returnTransfer.name());
			av.visitEnd();
		}
		
		mv.visitCode();
		LocalVariableTable locals = ctx.allocLocals();
		int functionOffset = locals.allocTmp("function", Type.getType(Function.class));
		int argsOffset = locals.allocTmp("args", Type.getType(Object[].class));
		int errorOffset = 0;
		int nInvokeArgs = ctx.args.length;
		if (ctx.isMethod)
			nInvokeArgs += 1;		
		int nInvokeArgsNoError = nInvokeArgs;		
		if (ctx.throwsGError) {
			errorOffset = locals.allocTmp("error", Type.getType(PointerByReference.class));
			nInvokeArgs += 1;
		}
		Label jtarget;
		Label l0 = new Label();
		mv.visitLabel(l0);
		if (ctx.throwsGError) {
			mv.visitTypeInsn(NEW, Type.getInternalName(PointerByReference.class));
			mv.visitInsn(DUP);
			mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(PointerByReference.class), 
					"<init>", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { getType(Pointer.class) }));	
			mv.visitVarInsn(ASTORE, errorOffset);			
		}
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "library", Type.getDescriptor(NativeLibrary.class));
		mv.visitLdcInsn(symbol);
		mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(NativeLibrary.class), "getFunction", 
				Type.getMethodDescriptor(getType(Function.class), new Type[] { getType(String.class)} ));				
		mv.visitVarInsn(ASTORE, functionOffset);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitIntInsn(BIPUSH, nInvokeArgs);
		mv.visitTypeInsn(ANEWARRAY, Type.getInternalName(Object.class));
		for (int i = 0; i < nInvokeArgsNoError; i++) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, i);
			Integer arraySource = ctx.lengthOfArrayIndices.get(i);
			Integer lengthOfArray = ctx.arrayToLengthIndices.get(i);
			Integer callbackIdx = ctx.destroyNotifyIndices.get(i);
			if (arraySource != null) {
				ArgInfo source = ctx.args[arraySource - (ctx.isMethod ? 1 : 0)];
				assert source.getType().getTag().equals(TypeTag.ARRAY);
				int offset = ctx.argOffsetToApi(arraySource);
				LocalVariable var = locals.get(offset);
				var.writeLoadArgument(mv);
				mv.visitInsn(ARRAYLENGTH);
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf", 
						Type.getMethodDescriptor(getType(Integer.class), new Type[] { Type.INT_TYPE }));
			} else if (lengthOfArray != null) {
				LocalVariable var = locals.get(lengthOfArray);
				var.writeLoadArgument(mv);
			} else if (ctx.userDataIndices.contains(i)) {
				/* Always pass null for user datas - Java allows environment capture */
				mv.visitInsn(ACONST_NULL);
			} else if (callbackIdx != null) {
				int offset = ctx.argOffsetToApi(callbackIdx);
				LocalVariable var = locals.get(offset);
				var.writeLoadArgument(mv);				
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(GlibRuntime.class), "createDestroyNotify", 
						Type.getMethodDescriptor(getType(GlibAPI.GDestroyNotify.class), new Type[] { getType(Callback.class) } ));
			} else if (!ctx.isMethod || i > 0) {
				int localOff = ctx.argOffsetToApi(i);
				LocalVariable var = locals.get(localOff);	
				var.writeLoadArgument(mv);	
			} else {
				mv.visitVarInsn(ALOAD, 0);
			}
			mv.visitInsn(AASTORE);
		}
		if (ctx.throwsGError) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, nInvokeArgsNoError);
			mv.visitVarInsn(ALOAD, errorOffset);
			mv.visitInsn(AASTORE);
		}
		mv.visitVarInsn(ASTORE, argsOffset);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitVarInsn(ALOAD, functionOffset);
		if (ctx.returnType.equals(Type.VOID_TYPE)) {
			mv.visitLdcInsn(Type.getType(Void.class));
		} else {
			mv.visitLdcInsn(nativeReturnType);
		}
		mv.visitVarInsn(ALOAD, argsOffset);
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "invocationOptions", Type.getDescriptor(Map.class));
		mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Function.class), "invoke", 
				Type.getMethodDescriptor(getType(Object.class), new Type[] { getType(Class.class), getType(Object[].class), getType(Map.class) }));
		Label l3 = new Label();
		mv.visitLabel(l3);	
		if (ctx.returnType.equals(Type.VOID_TYPE)) {
			mv.visitInsn(POP);
		} else {
			mv.visitTypeInsn(CHECKCAST, nativeReturnType.getInternalName());
			if (primitiveBox != null) {
				/* Turn primitive boxeds into the corresponding primitive */
				mv.visitMethodInsn(INVOKEVIRTUAL, nativeReturnType.getInternalName(), 
						ctx.returnType.getClassName() + "Value", "()" + ctx.returnType.getDescriptor());
			} else if (nativeReturnType != ctx.returnType) {
				/* Special handling for return types; see above where nativeReturnType is calculated. */
				if (returnTypeTag.equals(TypeTag.INTERFACE)) {
					/* These are objects for which we do *not* own a reference.  */
					mv.visitLdcInsn(ctx.returnType);
					mv.visitInsn(ICONST_0);
					mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(NativeObject.Internals.class), "objectFor", 
							Type.getMethodDescriptor(getType(NativeObject.class), new Type[] { getType(Pointer.class), getType(Class.class), Type.BOOLEAN_TYPE }));
					mv.visitTypeInsn(CHECKCAST, ctx.returnType.getInternalName());					
				} else if (returnTypeTag.equals(TypeTag.UTF8)) {
					/* Strings which are *not* const and must be g_free'd */
					mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(GlibRuntime.class), "toStringAndGFree", 
							Type.getMethodDescriptor(getType(String.class), new Type[] { getType(Pointer.class) }));
				} else if (returnTypeTag.equals(TypeTag.GLIST) || returnTypeTag.equals(TypeTag.GSLIST)) {
					writeConversionToJava(mv, returnGIType, returnTransfer);
				} else {
					throw new IllegalArgumentException(String.format("Unhandled nativeReturn %s vs public %s", nativeReturnType, ctx.returnType));
				}
			}
		}
		if (ctx.throwsGError) {
			jtarget = new Label();
			mv.visitVarInsn(ALOAD, errorOffset);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PointerByReference.class), 
					"getValue", Type.getMethodDescriptor(getType(Pointer.class), new Type[] {}));	
			mv.visitJumpInsn(IFNULL, jtarget);
			mv.visitTypeInsn(NEW, Type.getInternalName(GErrorException.class));
			mv.visitInsn(DUP);
			mv.visitTypeInsn(NEW, Type.getInternalName(GErrorStruct.class));
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, errorOffset);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PointerByReference.class), "getValue", 
					Type.getMethodDescriptor(getType(Pointer.class), new Type[] {}));
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(GErrorStruct.class), "<init>", 
					Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { getType(Pointer.class) }));
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(GErrorException.class), "<init>", 
					Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { getType(GErrorStruct.class) }));
			mv.visitInsn(ATHROW);
			mv.visitLabel(jtarget);
		}
		mv.visitInsn(ctx.returnType.getOpcode(IRETURN));
		Label l4 = new Label();
		mv.visitLabel(l4);
		locals.writeLocals(mv, l0, l4);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private void writeGetGType(RegisteredTypeInfo rti, ClassCompilation compilation) {
		String globalInternalsName = getInternals(rti);		
		MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "getGType", "()Lorg/gnome/gir/gobject/GType;", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "library", "Lcom/sun/jna/NativeLibrary;");
		mv.visitLdcInsn(rti.getTypeInit());
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/NativeLibrary", "getFunction", "(Ljava/lang/String;)Lcom/sun/jna/Function;");
		mv.visitLdcInsn(Type.getType("Lorg/gnome/gir/gobject/GType;"));
		mv.visitInsn(ICONST_0);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "invocationOptions", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invoke", "(Ljava/lang/Class;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, "org/gnome/gir/gobject/GType");
		mv.visitInsn(ARETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
	}
	
	private void compileGlobal(ClassCompilation compilation, FunctionInfo fi,
			Set<String> globalSigs) {
		CallableCompilationContext ctx = tryCompileCallable(fi, globalSigs);
		if (ctx == null)
			return;	
		writeCallable(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, compilation, fi, ctx);
	}
	
	private void writeStructUnionInnerCtor(InnerClassCompilation inner, String parentInternalName, FieldInfo[] fields) {
		/* First a no-args constructor */
		MethodVisitor mv = inner.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, "org/gnome/gir/gobject/GTypeMapper", "getInstance", "()Lorg/gnome/gir/gobject/GTypeMapper;");		
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", "(Lcom/sun/jna/TypeMapper;)V");				
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + inner.internalName + ";", null, l0, l1, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
	}

	private void writeStructUnion(RegisteredTypeInfo info, StubClassCompilation compilation, String type,
			FunctionInfo[] methods,
			FieldInfo[] fields) {
		
		String internalName = getInternalName(info);
		String typeInit = info.getTypeInit();
		boolean isRegistered = typeInit != null;
		String parentInternalName;
		boolean hasFields = fields.length > 0;		
		if (isRegistered) {
			if (hasFields)
				parentInternalName = "org/gnome/gir/gobject/Boxed" + type;
			else
				parentInternalName = "org/gnome/gir/gobject/GBoxed";	
		} else {
			if (hasFields)
				parentInternalName = "com/sun/jna/" + type;				
			else
				parentInternalName = "com/sun/jna/PointerType";
		}
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalName, null, 
				parentInternalName, null);
		
		if (!hasFields) {
			/* Write out a no-args ctor, though people shouldn't use this */
			MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", "()V");
			mv.visitInsn(RETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
			mv.visitMaxs(0, 0);
			mv.visitEnd();			
		}
		
		if (isRegistered) {			
			/* constructor; protected, taking GType, Pointer, TypeMapper; used in GValue */			
			MethodVisitor mv = compilation.writer.visitMethod(ACC_PROTECTED, "<init>", 
					Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(GType.class), Type.getType(Pointer.class), Type.getType(TypeMapper.class)} ), 
					null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 3);			
			mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", 
					Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(GType.class), Type.getType(Pointer.class), Type.getType(TypeMapper.class)} ));
			mv.visitInsn(RETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
			mv.visitLocalVariable("gtype", Type.getDescriptor(GType.class), null, l0, l1, 0);		
			mv.visitLocalVariable("ptr", Type.getDescriptor(Pointer.class), null, l0, l1, 0);				
			mv.visitLocalVariable("mapper", Type.getDescriptor(TypeMapper.class), null, l0, l1, 0);		
			mv.visitMaxs(0, 0);
			mv.visitEnd();			
			
			writeGetGType(info, compilation);
		}
		
		if (hasFields) {
			InnerClassCompilation byRef = compilation.newInner("ByReference");
			compilation.writer.visitInnerClass(compilation.internalName + "$ByReference", compilation.internalName,
					"ByReference", ACC_PUBLIC + ACC_STATIC);
			byRef.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, byRef.internalName, null, compilation.internalName,
					new String[] { "com/sun/jna/Structure$ByReference" });
			writeStructUnionInnerCtor(byRef, internalName, fields);

			InnerClassCompilation byValue = compilation.newInner("ByValue");
			compilation.writer.visitInnerClass(compilation.internalName + "$ByValue", compilation.internalName,
					"ByValue", ACC_PUBLIC + ACC_STATIC);
			byValue.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, byValue.internalName, null, compilation.internalName,
					new String[] { "com/sun/jna/Structure$ByValue" });
			writeStructUnionInnerCtor(byValue, internalName, fields);

			/* constructor; public no-args */
			MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESTATIC, "org/gnome/gir/gobject/GTypeMapper", "getInstance",
					"()Lorg/gnome/gir/gobject/GTypeMapper;");
			mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", "(Lcom/sun/jna/TypeMapper;)V");
			mv.visitInsn(RETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
			mv.visitMaxs(0, 0);
			mv.visitEnd();

			/* constructor; protected, taking TypeMapper */			
			mv = compilation.writer.visitMethod(ACC_PROTECTED, "<init>", 
					Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(TypeMapper.class)} ), null, null);
			mv.visitCode();
			l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", 
					Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(TypeMapper.class)} ));
			mv.visitInsn(RETURN);
			l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
			mv.visitLocalVariable("mapper", Type.getDescriptor(TypeMapper.class), null, l0, l1, 0);		
			mv.visitMaxs(0, 0);
			mv.visitEnd();

			/* constructor that takes all of the fields */
			LocalVariableTable locals = new LocalVariableTable(Type.getObjectType(compilation.internalName), null, null);			
			List<Type> args = new ArrayList<Type>();
			args.add(Type.getObjectType(compilation.internalName));
			boolean allArgsPrimitive = true;
			for (FieldInfo field : fields) {
				Type argType = TypeMap.toJava(field);
				if (argType == null || TypeMap.getPrimitiveBox(argType) == null) {
					allArgsPrimitive = false;
					break;
				}
				args.add(argType);
				locals.add(field.getName(), argType);
			}
			
			if (allArgsPrimitive) {
				String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, args.subList(1, args.size()).toArray(
						new Type[0]));
				mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
				mv.visitCode();
				l0 = new Label();
				mv.visitLabel(l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESTATIC, "org/gnome/gir/gobject/GTypeMapper", "getInstance",
						"()Lorg/gnome/gir/gobject/GTypeMapper;");
				mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", "(Lcom/sun/jna/TypeMapper;)V");
				Label l2 = new Label();
				mv.visitLabel(l2);
				for (int i = 1; i < args.size(); i++) {
					mv.visitVarInsn(ALOAD, 0);
					LocalVariable local = locals.get(i);
					mv.visitVarInsn(local.type.getOpcode(ILOAD), local.offset);
					mv.visitFieldInsn(PUTFIELD, compilation.internalName, fields[i - 1].getName(), local.type
							.getDescriptor());
				}
				Label l3 = new Label();
				mv.visitLabel(l3);
				mv.visitInsn(RETURN);
				Label l4 = new Label();
				mv.visitLabel(l4);
				locals.writeLocals(mv, l0, l4);
				mv.visitMaxs(0, 0);
				mv.visitEnd();
			}
		}
		
		Set<String> sigs = new HashSet<String>();		
		for (FunctionInfo fi : methods) {
			CallableCompilationContext ctx = tryCompileCallable(fi, info, true, false, sigs);
			if (ctx == null)
				continue;			
			writeCallable(ACC_PUBLIC, compilation, fi, ctx);	
		}
		for (FieldInfo fi : fields) {
			String name = NameMap.ucaseToCamel(fi.getName());
			Type fieldType = TypeMap.toJava(fi);
			if (fieldType.equals(Type.VOID_TYPE)) // FIXME Temporary hack for GdkAtom
				fieldType = Type.getType(Pointer.class);
			FieldVisitor fv = compilation.writer.visitField(ACC_PUBLIC, name, fieldType.getDescriptor(), null, null);
			fv.visitEnd();				
		}		
	}
		
	private void compile(StructInfo info) {
		StubClassCompilation compilation = getCompilation(info);

		writeStructUnion(info, compilation, "Structure", info.getMethods(), info.getFields());
		
		compilation.close();
	}	
	
	private void compile(UnionInfo info) {
		StubClassCompilation compilation = getCompilation(info);
		
		writeStructUnion(info, compilation, "Union", info.getMethods(), info.getFields());
		
		compilation.close();
	}	
	
	private void compile(BoxedInfo info) {
		ClassCompilation compilation = getCompilation(info);
		
		String internalName = getInternalName(info);
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalName, null, "org/gnome/gir/gobject/GBoxed", null);
		
		MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "org/gnome/gir/gobject/GBoxed", "<init>", "()V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();			
		
		compilation.close();	
	}
	
	private void compile(ConstantInfo info) {
		GlobalsCompilation globals = getGlobals(info.getNamespace());
		InnerClassCompilation compilation = globals.getConstants();
		Type type =  TypeMap.toJava(info.getType());
		if (type == null) {
			logger.warning("Unhandled constant type " + type);
			return;
		}
		String fieldName = NameMap.fixIdentifier("n", info.getName());
		Object value = info.getValue();		
		FieldVisitor fv = compilation.writer.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
				fieldName, type.getDescriptor(), null, value);
		fv.visitEnd();
	}	
	
	private void writeJnaCallbackTypeMapper(ClassCompilation compilation) {
		FieldVisitor fv = compilation.writer.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, "TYPE_MAPPER", "Lcom/sun/jna/TypeMapper;", null, null);
		fv.visitEnd();
		
		MethodVisitor mv = compilation.writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitMethodInsn(INVOKESTATIC, "org/gnome/gir/gobject/GTypeMapper", "getInstance", "()Lorg/gnome/gir/gobject/GTypeMapper;");
		mv.visitFieldInsn(PUTSTATIC, compilation.internalName, "TYPE_MAPPER", "Lcom/sun/jna/TypeMapper;");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();		
	}
	
	private void compile(CallbackInfo info) {
		MethodVisitor mv;		
		ClassCompilation compilation = getCompilation(info);
		
		String internalName = getInternalName(info);
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, internalName, null, "java/lang/Object", 
				new String[] { "com/sun/jna/Callback" });
		
		CallableCompilationContext ctx = tryCompileCallable(info);

		writeJnaCallbackTypeMapper(compilation);
		
		if (ctx != null) {
			String descriptor = Type.getMethodDescriptor(ctx.returnType, ctx.argTypes.toArray(new Type[0]));			
			mv = compilation.writer.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, 
					"callback", descriptor, null, null);
			mv.visitEnd();
		}
		
		compilation.close();	
	}	
	
	private boolean requireNamespace(String namespace, String version) {
		if (alreadyCompiled.contains(namespace))
			return true;
		try {
			repo.require(namespace, version);
		} catch (GErrorException e) {
			if (!loadFailed.contains(namespace)) {
				logger.log(Level.SEVERE, "Failed to load namespace=" + namespace, e);
				loadFailed.add(namespace);
				return false;
			}
			return false;
		}
		pendingCompilation.add(namespace);		
		return true;
	}
	
	private void compileNamespaceComponents(String namespace) {
		BaseInfo[] infos = repo.getInfos(namespace);
		Set<String> globalSigs = new HashSet<String>();
		for (BaseInfo baseInfo : infos) {
			logger.fine("Compiling " + baseInfo);
			if (baseInfo instanceof EnumInfo) {
				compile((EnumInfo) baseInfo);
			} else if (baseInfo instanceof FlagsInfo) {
				compile((FlagsInfo) baseInfo);				
			} else if (baseInfo instanceof ObjectInfo) {
				compile((ObjectInfo) baseInfo);				
			} else if (baseInfo instanceof FunctionInfo) {
				compileGlobal(getGlobals(namespace), (FunctionInfo) baseInfo, globalSigs);
			} else if (baseInfo instanceof StructInfo) {
				compile((StructInfo) baseInfo);
			} else if (baseInfo instanceof UnionInfo) {
				compile((UnionInfo) baseInfo);				
			} else if (baseInfo instanceof BoxedInfo) {
				compile((BoxedInfo) baseInfo);
			} else if (baseInfo instanceof InterfaceInfo) {
				compile((InterfaceInfo) baseInfo);
			} else if (baseInfo instanceof CallbackInfo) {
				compile((CallbackInfo) baseInfo);
			} else if (baseInfo instanceof ConstantInfo) {
				compile((ConstantInfo) baseInfo);				
			} else {
				logger.warning("unhandled info " + baseInfo.getName());
			}
		}		
	}
	
	private void initGlobalsClass(GlobalsCompilation globals) {
		Label l0, l1, l2, l3;
		MethodVisitor mv;

		/* We have two inner classes - one is Internals, and one is an anonymous HashMap inside Internals */
		InnerClassCompilation internals = globals.newInner("Internals");		
		InnerClassCompilation internalsInner = globals.newInner("Internals$1");

		globals.writer.visitInnerClass(internals.internalName, globals.internalName, "Internals", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);
		internals.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL + ACC_SUPER, 
				internals.internalName, null, "java/lang/Object", null);
		internals.writer.visitInnerClass(internals.internalName, globals.internalName, "Internals", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);		

		internals.writer.visitInnerClass(internalsInner.internalName, null, null, 0);
		internalsInner.writer.visit(V1_6, ACC_FINAL + ACC_SUPER, internalsInner.internalName,
				"Ljava/util/HashMap<Ljava/lang/Object;Ljava/lang/Object;>;", "java/util/HashMap", null);
		internalsInner.writer.visitOuterClass(internals.internalName, null, null);
		internalsInner.writer.visitInnerClass(internalsInner.internalName, null, null, 0);		

		
		/* private constructor */
		mv = globals.writer.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		mv.visitInsn(RETURN);
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + globals.internalName + ";", null, l0, l1, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		/*
		public static final class Internals {
			public static final NativeLibrary library = NativeLibrary.getInstance("gtk-2.0");
			public static final Repository repo = Repository.getDefault();
			public static final String namespace = "Gtk";
			public static final Map<Object,Object> invocationOptions = new HashMap<Object,Object>() {
				private static final long serialVersionUID = 1L;

				{	
					put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
				}
			};
		};
		*/		
		
		FieldVisitor fv = globals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "library", "Lcom/sun/jna/NativeLibrary;", null, null);
		fv.visitEnd();		

		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "library", "Lcom/sun/jna/NativeLibrary;", null, null);
		fv.visitEnd();

		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "repo", "Lorg/gnome/gir/repository/Repository;", null, null);
		fv.visitEnd();

		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "namespace", "Ljava/lang/String;", null, globals.namespace);
		fv.visitEnd();
		
		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "nsversion", "Ljava/lang/String;", null, globals.nsversion);
		fv.visitEnd();		

		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "invocationOptions", 
				"Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", null);
		fv.visitEnd();		

		mv = internalsInner.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitLdcInsn("type-mapper");
		mv.visitTypeInsn(NEW, "org/gnome/gir/gobject/GTypeMapper");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "org/gnome/gir/gobject/GTypeMapper", "<init>", "()V");
		mv.visitMethodInsn(INVOKEVIRTUAL, internalsInner.internalName, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
		mv.visitInsn(POP);
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitInsn(RETURN);
		l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLocalVariable("this", "L" + internalsInner.internalName + ";", null, l0, l3, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		mv = internals.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		mv.visitInsn(RETURN);
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + internals.internalName + ";", null, l0, l1, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		mv = internals.writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		/* This goop is to deal with new comma-separated list of shared libraries;
		 * really, we should rely on the library loading inside GIRepository, and have
		 * JNA just open the process.
		 */
		String shlibList = repo.getSharedLibrary(globals.namespace);
		String shlib;
		if (shlibList == null)
			shlib = null;
		else {
			String[] shlibs = shlibList.split(",");
			shlib = shlibs[0];
		}
		if (shlib == null)
			shlib = namespaceShlibMapping.get(globals.namespace);
		mv.visitLdcInsn(shlib);
		mv.visitMethodInsn(INVOKESTATIC, "com/sun/jna/NativeLibrary", "getInstance", "(Ljava/lang/String;)Lcom/sun/jna/NativeLibrary;");
		mv.visitFieldInsn(PUTSTATIC, internals.internalName, "library", "Lcom/sun/jna/NativeLibrary;");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitMethodInsn(INVOKESTATIC, "org/gnome/gir/repository/Repository", "getDefault", "()Lorg/gnome/gir/repository/Repository;");
		mv.visitFieldInsn(PUTSTATIC, internals.internalName, "repo", "Lorg/gnome/gir/repository/Repository;");
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitTypeInsn(NEW, internalsInner.internalName);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, internalsInner.internalName, "<init>", "()V");
		mv.visitFieldInsn(PUTSTATIC, internals.internalName, "invocationOptions", "Ljava/util/Map;");

		mv.visitMethodInsn(INVOKESTATIC, "org/gnome/gir/repository/Repository", "getDefault", "()Lorg/gnome/gir/repository/Repository;");
		mv.visitLdcInsn(globals.namespace);
		mv.visitLdcInsn(globals.nsversion);
		mv.visitMethodInsn(INVOKEVIRTUAL, "org/gnome/gir/repository/Repository", "requireNoFail", 
				Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { getType(String.class), getType(String.class) }));		
		
		globals.clinit = mv;
	}

	private static final class PrivateNamespaceException extends Exception {};
	
	private void compileNamespaceSingle(String namespace, String version) throws PrivateNamespaceException {
		alreadyCompiled.add(namespace);
		
		try {
			repo.require(namespace, version);
			logger.info("Loaded typelib from " + repo.getTypelibPath(namespace));			
		} catch (GErrorException e) {
			throw new RuntimeException(e);
		}
		
		if (repo.getSharedLibrary(namespace) == null)
			throw new PrivateNamespaceException();
		
		String globalName = namespace + "Globals";
		String peerInternalName = GType.getInternalName(namespace, globalName);
		GlobalsCompilation global = new GlobalsCompilation(namespace, version, globalName);
		writers.put(peerInternalName, global);		
		globals.put(namespace, global);
		global.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL + ACC_SUPER, global.internalName, null, "java/lang/Object", null);
		initGlobalsClass(global);

		compileNamespaceComponents(namespace);
		
		global.close();
	}	
	
	private List<ClassCompilation> compileNamespace(String namespace, String version) throws PrivateNamespaceException {
		compileNamespaceSingle(namespace, version);
		return finish();
	}
	
	private List<ClassCompilation> finish() {
		logger.info("Compiled " + writers.size() + " info objects");
		List<ClassCompilation> ret = new LinkedList<ClassCompilation>();
		for (StubClassCompilation infoc : writers.values()) {
			infoc.close();
			ret.add(infoc);
			for (InnerClassCompilation inner : infoc.innerClasses) {
				ret.add(inner);
			}
		}
		return ret;		
	}
	
	private List<ClassCompilation> compileNamespaceRecursive(String namespace) throws PrivateNamespaceException {
		pendingCompilation.add(namespace);
		while (pendingCompilation.size() > 0) {
			String pending = pendingCompilation.iterator().next();
			logger.info("Namespace: " + pending);
			requireNamespace(pending, null);
			compileNamespaceSingle(pending, null);	
			pendingCompilation.remove(pending);
		}
		logger.info("Compiled " + writers.size() + " info objects");
		return finish();
	}
	
	private static List<ClassCompilation> getStubsUnlocked(Repository repo, String namespace) throws PrivateNamespaceException {
		List<ClassCompilation> ret = loadedRepositories.get(namespace);
		if (ret != null) {
			return ret;
		}
		
		logger.info("Starting from namespace: " + namespace);
		
		CodeFactory cf = new CodeFactory(repo);
		ret = cf.compileNamespaceRecursive(namespace);
		loadedRepositories.put(repo, ret);
		
		return ret;
	}
	
	public static List<ClassCompilation> getNativeStubs(Repository repo, String namespace) throws PrivateNamespaceException {
		synchronized (loadedRepositories) {
			return getStubsUnlocked(repo, namespace);
		}
	}
	
	public static List<ClassCompilation> compile(Repository repo, String namespace, String version) throws PrivateNamespaceException {
		CodeFactory cf = new CodeFactory(repo);
		return cf.compileNamespace(namespace, version);
	}
	
	public static ClassLoader getLoader(List<ClassCompilation> stubs) {
		final Map<String,byte[]> map = new HashMap<String,byte[]>();
		for (ClassCompilation stub: stubs)
			map.put(stub.getPublicName(), stub.getBytes());
		return new ClassLoader() {
			@Override
			public Class<?> findClass(String name) throws ClassNotFoundException {
				byte[] bytes = map.get(name);
				if (bytes == null)
					return super.findClass(name);
				return defineClass(name, bytes);
			}
			
			protected Class<?> defineClass(String name, byte[] bytes) {
				return defineClass(name, bytes, 0, bytes.length);
			}			
		};
	}

	public static File compile(String namespace, String version) throws GErrorException, IOException {
		Repository repo = new Repository();
		File destFile = null;		
		
		repo.require(namespace, version);
		String typelibPathName = repo.getTypelibPath(namespace);
		File typelibPath = new File(typelibPathName);
		long typelibLastModified = typelibPath.lastModified();
		
		if (destFile == null) {
			destFile = getJarPath(repo, namespace);
			if (destFile == null)
				return null;
			logger.info("Will install to: " + destFile);
		}
		
		if (destFile.exists() && destFile.lastModified() > typelibLastModified) {
			logger.info("Skipping already-compiled namespace: " + namespace);
			return destFile;
		}
		
		logger.info(String.format("Compiling namespace: %s version: %s", namespace, version));
		List<ClassCompilation> stubs;
		try {
			stubs = CodeFactory.compile(repo, namespace, version);
		} catch (PrivateNamespaceException e) {
			logger.info(String.format("Skipping namespace %s with no shared library", namespace));
			return null;
		}
		
		Set<String> classNames = new HashSet<String>();
		ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(destFile));
		for (ClassCompilation stub : stubs) {
			byte[] code = stub.getBytes();
			String className = stub.getPublicName().replace('.', '/');
			classNames.add(className);
			String classFilename = className + ".class";
			zo.putNextEntry(new ZipEntry(classFilename));
			zo.write(code);
			zo.closeEntry();
		}	
		zo.close();
		return destFile;
	}
	
	public static void verifyJarFiles(List<File> jarPaths) throws Exception {
		logger.info(String.format("Verifing %d jars", jarPaths.size()));			
		List<URL> urls = new ArrayList<URL>();
		Map<String, InputStream> allClassnames = new HashMap<String, InputStream>();		
		List<ZipFile> zips = new ArrayList<ZipFile>();
		for (File jarPath : jarPaths) {
			urls.add(jarPath.toURI().toURL());		
			ZipFile zf = new ZipFile(jarPath);
			for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				String name = entry.getName();				
				if (name.endsWith(".class")) {
					String className = name.replace('/', '.').substring(0, name.length()-6);
					allClassnames.put(className, zf.getInputStream(entry));
				}
			}
			zips.add(zf);
		}
		ClassLoader loader = new URLClassLoader(urls.toArray(new URL[] {}), CodeFactory.class.getClassLoader());		
		Method verify;
		try {
			verify = CheckClassAdapter.class.getMethod("verify", new Class[] { ClassReader.class, ClassLoader.class, 
					boolean.class, PrintWriter.class });
		} catch (NoSuchMethodException e) {
			logger.warning("Failed to find ASM with extended verify; skipping verification");
			verify = null;
		}

		int nClasses = 0;
		for (Map.Entry<String,InputStream> entry : allClassnames.entrySet()) {
			if (verify != null) {
				try {
					ClassReader reader = new ClassReader(entry.getValue());					
					verify.invoke(null, new Object[] { reader, loader, false, new PrintWriter(System.err) } );
					nClasses++;
				} catch (InvocationTargetException e) {
					System.err.println("Failed to verify " + entry.getKey());
					e.printStackTrace();
					throw e;
				}
			} else {
				/* Fall back to the JVM's ClassLoader basic verification */
				loader.loadClass(entry.getKey());
			}
		}
		for (ZipFile zip: zips)
			zip.close();
		logger.info(String.format("Verified %d classes", nClasses));
	}
	
	public static File getJarPath(Repository repo, String namespace) {
		String path = repo.getTypelibPath(namespace);
		if (path == null)
			return null;
		File typelibPath = new File(path);
		String version = repo.getNamespaceVersion(namespace);
		return new File(typelibPath.getParent(), String.format("%s-%s.jar", namespace, version));		
	}
	
	private static boolean namespaceIsExcluded(String namespace) {
		return namespace.equals("GLib") || namespace.equals("GObject");		
	}
	
	private static File getTypelibDir() {
		return new File(System.getenv("TYPELIBDIR"));
	}
	
	public static void verifyAll() throws Exception {
		File[] jars = getTypelibDir().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		Repository.getDefault().disableRequires();
		verifyJarFiles(Arrays.asList(jars));
	}
	
	private static final class NsVer {
		public String namespace;
		public String version;
		
		public static NsVer parse(String base) {
			int dash = base.indexOf('-');
			NsVer nsver = new NsVer();
			nsver.namespace = base.substring(0, dash);		
			nsver.version = base.substring(dash+1, base.lastIndexOf('.'));
			return nsver;
		}
	}
	
	public static void compileAll() throws GErrorException, IOException {
		File[] typelibs = getTypelibDir().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".typelib");
			}
		});
		for (File typelib : typelibs) {
			String base = typelib.getName();

			NsVer nsver = NsVer.parse(base);
			if (namespaceIsExcluded(nsver.namespace))
				continue;

			compile(nsver.namespace, nsver.version);
		}
	}
	
	public static void main(String[] args) throws Exception {
		GObjectAPI.gobj.g_type_init();
		if (args[0].equals("--compileall"))
			compileAll();
		else if (args[0].equals("--verifyall"))
			verifyAll();
		else if (args[0].endsWith(".typelib")) {
			File typelib = new File(args[0]);
			String base = typelib.getName();
			NsVer nsver = NsVer.parse(base);
			String parent = typelib.getParent();
			if (parent == null)
				parent = System.getProperty("user.dir");
			Repository.getDefault().prependSearchPath(parent);
			compile(nsver.namespace, nsver.version);
		} else {
			String namespace = args[0];
			String version = args[1];
			if (!namespaceIsExcluded(namespace))
				compile(namespace, version);
		}
	}
}
