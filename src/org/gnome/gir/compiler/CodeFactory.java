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
import static org.objectweb.asm.Opcodes.IFNONNULL;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.gnome.gir.gobject.GErrorException;
import org.gnome.gir.gobject.GList;
import org.gnome.gir.gobject.GObjectAPI;
import org.gnome.gir.gobject.GSList;
import org.gnome.gir.gobject.GType;
import org.gnome.gir.repository.ArgInfo;
import org.gnome.gir.repository.BaseInfo;
import org.gnome.gir.repository.BoxedInfo;
import org.gnome.gir.repository.CallableInfo;
import org.gnome.gir.repository.CallbackInfo;
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
import org.gnome.gir.repository.TypeInfo;
import org.gnome.gir.repository.TypeTag;
import org.gnome.gir.repository.UnionInfo;
import org.gnome.gir.repository.ValueInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

@SuppressWarnings("serial")
public class CodeFactory {
	
	private static final Logger logger = Logger.getLogger("org.gnome.gir.Compiler");
	
	private static final Set<String> GOBJECT_METHOD_BLACKLIST = new HashSet<String>() {
		{
			add("ref");
			add("unref");
		}
	};

	public static Type toJava(TypeTag tag) {
		if (tag == TypeTag.LONG || tag == TypeTag.ULONG ||
				tag == TypeTag.SSIZE || tag == TypeTag.SIZE)
			return Type.getType(Long.class);
		if (tag == TypeTag.ARRAY)
			return Type.getType(List.class);
		if (tag == TypeTag.GLIST)
			return Type.getType(GList.class);
		if (tag == TypeTag.GSLIST)
			return Type.getType(GSList.class);
		if (tag == TypeTag.GHASH)
			return Type.getType(Map.class);
		return toTypeBase(tag);		
	}
	
	private Type typeFromInfo(TypeInfo info) {
		BaseInfo base = info.getInterface();
		return typeFromInfo(base);
	}
	
	private Type typeFromInfo(BaseInfo info) {
		/* Unfortunately, flags are best mapped as plain Integer  for now */
		if (info instanceof FlagsInfo)
			return Type.getObjectType("java/lang/Integer");
		String internalName = GType.getInternalNameMapped(info.getNamespace(), info.getName());
		if (internalName != null)
			return Type.getObjectType(internalName);
		return null;
	}	
	
	public Type toJavaArray(TypeInfo containedType) {
		Type result = toJava(containedType);
		if (result == null)
			return null;
		String descriptor = result.getDescriptor();
		return Type.getType("[" + descriptor);
	}
	
	public Type toJava(TypeInfo type) {	
		//Transfer transfer = arg.getOwnershipTransfer();
		TypeTag tag = type.getTag();
		
		if (tag.equals(TypeTag.VOID)) {
			// FIXME - for now we change random Voids into Pointer, but this seems to
			// be a G-I bug
			return Type.getType(Pointer.class);		
		} else if (tag.equals(TypeTag.INTERFACE)) {
			return typeFromInfo(type.getInterface());
		} else if (tag.equals(TypeTag.ARRAY)) {
			return toJavaArray(type.getParamType(0));
		} else if (!type.isPointer() || (tag.equals(TypeTag.UTF8) || tag.equals(TypeTag.FILENAME))) {
			return toTypeBase(tag);
		} else if (type.isPointer()) {
			return toJavaRef(tag);
		} else {
			return toTypeBase(tag);
		}	
	}
	
	public Type toJava(FieldInfo arg) {
		TypeInfo type = arg.getType();
		if (type.getTag().equals(TypeTag.INTERFACE)) {
			BaseInfo iface = arg.getType().getInterface();
			/* Special case structure/union members; we need to use the
			 * $ByReference tag if the member is actually a pointer.
			 */
			if (iface instanceof StructInfo || iface instanceof UnionInfo) {
				boolean hasFields;
				if (iface instanceof StructInfo)
					hasFields = ((StructInfo) iface).getFields().length > 0;
				else
					hasFields = ((UnionInfo) iface).getFields().length > 0;
				if (!hasFields)
					return Type.getType(Pointer.class);
				String internalName = getInternalNameMapped(iface);
				if (type.isPointer() && internalName.startsWith(GType.dynamicNamespace))
					internalName += "$ByReference";
				return Type.getObjectType(internalName);
			} else if (iface instanceof InterfaceInfo || iface instanceof ObjectInfo ||
					iface instanceof BoxedInfo) {
				/* Interfaces/Objects/Boxed are always Pointer for now
				 */
				return Type.getType(Pointer.class);
			}
		}
		Type result = toJava(type);
		if (result == null) {
			logger.warning(String.format("Unhandled field type %s (type %s)", result, type));
		}
		return result;
	}	
	
	public Type toJava(ArgInfo arg) {
		if (arg.getDirection() == Direction.IN) {
			return toJava(arg.getType());
		} else {
			return toJavaRef(arg.getType().getTag());
		}
	}
	
	public static Type toJavaRef(TypeTag tag) {
		Type t = toJava(tag);
		if (t == null)
			return null;
		if (t.equals(Type.INT_TYPE))
			return Type.getType(IntByReference.class);
		if (t.equals(Type.LONG_TYPE))
			return Type.getType(LongByReference.class);
		if (t.equals(Type.BOOLEAN_TYPE))
			return Type.getType(IntByReference.class);
		if (t.equals(Type.BYTE_TYPE))
			return Type.getType(ByteByReference.class);
		if (t.equals(Type.SHORT_TYPE))
			return Type.getType(ShortByReference.class);
		if (t.equals(Type.FLOAT_TYPE))
			return Type.getType(FloatByReference.class);
		if (t.equals(Type.DOUBLE_TYPE))
			return Type.getType(DoubleByReference.class);
		if (t.equals(Type.getType(String.class)) || t.equals(Type.getType(File.class)))
			return Type.getType(PointerByReference.class);
		return Type.getType(Pointer.class);
	}
	
	private static Type toTypeBase(TypeTag tag) {
		if (tag == TypeTag.VOID)
			return Type.VOID_TYPE;
		if (tag == TypeTag.BOOLEAN)
			return Type.BOOLEAN_TYPE;
		if (tag == TypeTag.INT8 || tag == TypeTag.UINT8)
			return Type.BYTE_TYPE;
		if (tag == TypeTag.INT16 || tag == TypeTag.UINT16)
			return Type.SHORT_TYPE;
		if (tag == TypeTag.INT32 || tag == TypeTag.UINT32 ||
				tag == TypeTag.INT || tag == TypeTag.UINT)
			return Type.INT_TYPE;
		if (tag == TypeTag.INT64 || tag == TypeTag.UINT64
				|| tag == TypeTag.SIZE || tag == TypeTag.SSIZE)
			return Type.LONG_TYPE;
		if (tag == TypeTag.FLOAT)
			return Type.FLOAT_TYPE;
		if (tag == TypeTag.DOUBLE)
			return Type.DOUBLE_TYPE;
		if (tag == TypeTag.TIMET)
			return Type.getType(Date.class);
		if (tag == TypeTag.GTYPE)
			return Type.getType(GType.class);		
		if (tag == TypeTag.UTF8)
			return Type.getType(String.class);
		if (tag == TypeTag.FILENAME)
			return Type.getType(File.class);		
		return null;
	}

	private Type getCallableReturn(CallableInfo callable) {
		TypeInfo info = callable.getReturnType();
		if (info.getTag().equals(TypeTag.INTERFACE)) {
			return typeFromInfo(info);
		}
		return toJava(info.getTag());
	}
	
	private Class<?> getPrimitiveBox(Type type) {
		if (type.equals(Type.BOOLEAN_TYPE))
			return Boolean.class;
		if (type.equals(Type.BYTE_TYPE))
			return Byte.class;
		if (type.equals(Type.CHAR_TYPE))
			return Character.class;
		if (type.equals(Type.SHORT_TYPE))
			return Short.class;
		if (type.equals(Type.INT_TYPE))
			return Integer.class;
		if (type.equals(Type.LONG_TYPE))
			return Long.class;
		if (type.equals(Type.FLOAT_TYPE))
			return Float.class;
		if (type.equals(Type.DOUBLE_TYPE))
			return Double.class;
		return null;
	}
	
	private static abstract class ClassCompilation {
		String namespace;
		String nsversion;
		String baseName;
		String internalName;
		ClassVisitor writer;
		private ClassWriter realWriter;
		public ClassCompilation(String namespace, String baseName) {
			this.namespace = namespace;
			this.nsversion = Repository.getDefault().getNamespaceVersion(namespace);
			this.baseName = baseName;
			this.internalName = GType.getInternalName(namespace, baseName);
			this.realWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			this.writer = new CheckClassAdapter(this.realWriter);
		}

		public byte[] getBytes() {
			return realWriter.toByteArray();
		}
		
		public String getNamespace() {
			return namespace;
		}
		
		public String getPublicName() {
			return GType.getPublicNameMapped(namespace, baseName);
		}
		
		public abstract void close();
	}	
	
	private static class InnerClassCompilation extends ClassCompilation {
		public InnerClassCompilation(String namespace, String baseName) {
			super(namespace, baseName);
		}

		@Override
		public void close() {
			throw new IllegalArgumentException();
		}
		
	}
	
	private static class StubClassCompilation extends ClassCompilation {
		Set<InnerClassCompilation> innerClasses;
		String publicName;
		private boolean closed = false;
		
		public StubClassCompilation(String namespace,
				String name) {
			super(namespace, name);
			this.innerClasses = new HashSet<InnerClassCompilation>();
			this.baseName = name.substring(0, 1).toUpperCase() + name.substring(1);
			this.publicName = GType.getPublicNameMapped(namespace, name);
		}
		
		public ClassCompilation newInner() {
			int size = innerClasses.size();
			InnerClassCompilation cw = new InnerClassCompilation(namespace, baseName + "$" + size+1);
			innerClasses.add(cw);
			return cw;
		}
		
		public InnerClassCompilation newInner(String name) {
			InnerClassCompilation cw = new InnerClassCompilation(namespace, baseName + "$" + name);
			innerClasses.add(cw);
			return cw;
		}
		
		public void close() {
			if (!closed) {
				writer.visitEnd();
				closed = true;
			}
		}
	}
	
	public static final class GlobalsCompilation extends StubClassCompilation {
		Map<String,String> interfaceTypes = new HashMap<String,String>();
		public MethodVisitor clinit;
		public GlobalsCompilation(String namespace, String name) {
			super(namespace, name);
		}
	}

	public StubClassCompilation getCompilation(String namespace, String name) {
		String peerInternalName = GType.getInternalName(namespace, name);
		StubClassCompilation ret = writers.get(peerInternalName);
		if (ret == null) {
			ret = new StubClassCompilation(namespace, name);
			writers.put(peerInternalName, ret);
		}
		return ret;
	}	
	
	public StubClassCompilation getCompilation(BaseInfo info) {
		return getCompilation(info.getNamespace(), info.getName());
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
	
	private static final Map<Repository,List<ClassCompilation>> loadedRepositories 
		= new WeakHashMap<Repository, List<ClassCompilation>>();
	
	private static String getInternalName(BaseInfo info) {
		return GType.getInternalName(info.getNamespace(), info.getName());
	}
	
	private static String getInternalNameMapped(BaseInfo info) {
		return GType.getInternalNameMapped(info.getNamespace(), info.getName());
	}	

	private static final Pattern allNumeric = Pattern.compile("[0-9]+"); 	
	private static final Pattern replaceFirstNumeric = Pattern.compile("([0-9]+)([A-Za-z]+)"); 
	private String fixIdentifier(String base, String ident) {
		Matcher match = replaceFirstNumeric.matcher(ident);
		if (!match.lookingAt()) {
			if (allNumeric.matcher(ident).matches()) {
				return base + ident;
			}
			return ident;
		}
		return match.replaceFirst("$2$1");
	}
	
	private String enumNameToUpper(String base, String nick) {
		return fixIdentifier(base, nick.replace("-", "_")).toUpperCase();
	}
	
	private void compile(EnumInfo info) {
		ClassCompilation compilation = getCompilation(info);
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL + ACC_SUPER + ACC_ENUM, compilation.internalName, 
				"Ljava/lang/Enum<L" + compilation.internalName + ";>;", "java/lang/Enum", null);
		ValueInfo[] values = info.getValueInfo();
		for (ValueInfo valueInfo : values) {
			String name = enumNameToUpper(info.getName(), valueInfo.getName());			
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
			String name = enumNameToUpper(info.getName(), valueInfo.getName());
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
			String name = enumNameToUpper(info.getName(), valueInfo.getName());			
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
						enumNameToUpper(info.getName(), valueInfo.getName()), "J", null, valueInfo.getValue());
			fv.visitEnd();				
		}
		compilation.close();
	}	
	
	private static String ucaseToCamel(String ucase) {
		// So this function works on signal/property names too
		ucase = ucase.replace('-', '_');
		String[] components = ucase.split("_");
		for (int i = 1; i < components.length; i++) {
			if (components[i].length() > 0)
				components[i] = "" + Character.toUpperCase(components[i].charAt(0)) + components[i].substring(1);
		}
		StringBuilder builder = new StringBuilder();
		for (String component : components)
			builder.append(component);
		return builder.toString();
	}
	
	private static String ucaseToPascal(String ucase) {
		String camel = ucaseToCamel(ucase);
		return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
	}	
	
	private void compileDefaultConstructor(ObjectInfo info, ClassCompilation compilation) {		
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
			writeLoadArgument(mv, argOffset, argType);
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
			writeLoadArgument(mv, var.offset, var.type);
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
		String sigClass = ucaseToPascal(sigName);
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
			Type type = toJava(prop.getType());
			if (type == null) {
				logger.warning(String.format("Skipping unhandled property type %s of %s", prop.getName(), compilation.internalName));
				continue;
			}
			int propFlags = prop.getFlags();
			Class<?> propBox = getPrimitiveBox(type);
			Type propTypeBox;
			if (propBox != null)
				propTypeBox = Type.getType(propBox);
			else
				propTypeBox = type;
			if ((propFlags & FieldInfoFlags.READABLE) != 0) {
				String propPascal = ucaseToPascal(prop.getName());
				
				writePropertyNotify(compilation, objectType, prop, type, propBox,
						isInterfaceSource, isInterfaceTarget);				
				
				String getterName = "get" + propPascal;
				String descriptor = Type.getMethodDescriptor(type, new Type[] {});
				String signature = getUniqueSignature(getterName, type, Arrays.asList(new Type[] {}));
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
				String setterName = "set" + ucaseToPascal(prop.getName());
				String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { type });
				String signature = getUniqueSignature(setterName, Type.VOID_TYPE, Arrays.asList(new Type[] { type }));
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
							"(Ljava/lang/String;Ljava/lang/Object;)" + type.getDescriptor());
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
		String propPascal = ucaseToPascal(prop.getName());		
		String notifyClass = propPascal + "Notify";
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
		InterfaceInfo[] giInterfaces = info.getInterfaces();
		if (giInterfaces.length > 0) {
			interfaces = new String[giInterfaces.length];
		}
		for (int i = 0; i < giInterfaces.length; i++) {
			interfaces[i] = getInternalNameMapped(giInterfaces[i]);
		}
		
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalName, null, parentInternalName, interfaces);
		
		for (SignalInfo sig : info.getSignals()) {
			CallableCompilationContext ctx = tryCompileCallable(sig);
			if (ctx == null)
				continue;
			// Insert the object as first parameter
			ctx.argTypes.add(0, typeFromInfo(info));				
			compileSignal(compilation, ctx, sig, false, false);
		}
		
		writeGetGType(info, compilation);
		
		writeHandleInitializer(compilation, parentInternalName);
		
		compileDefaultConstructor(info, compilation);
		
		Map<String,Set<FunctionInfo>> ctors = new HashMap<String, Set<FunctionInfo>>();
		
		// First gather the set of all constructors; we need to avoid name clashes
		for (FunctionInfo fi : info.getMethods()) {
			CallableCompilationContext ctx = tryCompileCallable(fi);
			if (ctx == null || !ctx.isConstructor)
				continue;

			if (ctx.argTypes.size() == 0) {
				logger.fine("Skipping 0-args constructor: " + fi.getName());
				continue;
			}
			String descriptor = ctx.getDescriptor();
			if (!ctors.containsKey(descriptor)) {
				ctors.put(descriptor, new HashSet<FunctionInfo>());
			}
			Set<FunctionInfo> set = ctors.get(descriptor);
			set.add(fi);
		}
		
		for (Set<FunctionInfo> ctorGroup : ctors.values()) {
			FunctionInfo first = ctorGroup.iterator().next();			
			if (ctorGroup.size() == 1) {
				writeConstructor(info, compilation, first);
			} else {
				logger.info("Constructor name " + first.getSymbol() + " clashes");
				FunctionInfo defaultCtor = null;
				for (FunctionInfo ctor : ctorGroup) {
					if (ctor.getName().equals("new"))
						defaultCtor = ctor;
				}
				if (defaultCtor != null) {
					writeConstructor(info, compilation, defaultCtor);
				}
				for (FunctionInfo ctor : ctorGroup) {
					if (ctor != defaultCtor) {
						writeStaticConstructor(info, compilation, ctor);
					}
				}
			}
		}
		
		// Now do methods
		Set<String> sigs = new HashSet<String>();		
		for (FunctionInfo fi : info.getMethods()) {	
			if (GOBJECT_METHOD_BLACKLIST.contains(fi.getName()))
				continue;
			CallableCompilationContext ctx = tryCompileCallable(fi, info, true, false, sigs);
			if (ctx == null || ctx.isConstructor)
				continue;
			writeCallable(ACC_PUBLIC, compilation, fi, ctx);
		}
		for (InterfaceInfo iface : giInterfaces) {
			for (FunctionInfo fi: iface.getMethods()) {
				CallableCompilationContext ctx = tryCompileCallable(fi, iface, true, false, sigs);
				if (ctx == null)
					continue;
				ctx.isInterfaceMethod = true;
				ctx.targetInterface = iface;
				writeCallable(ACC_PUBLIC, compilation, fi, ctx);
			}
			Type ifaceType = typeFromInfo(iface);
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
		
		writeProperties(compilation, Type.getObjectType(compilation.internalName),
				info.getProperties(), sigs, false, false);
		
		compilation.close();	
	}
	
	private void compile(InterfaceInfo info) {
		StubClassCompilation compilation = getCompilation(info);
		GlobalsCompilation globals = getGlobals(info.getNamespace());
		
		String internalName = getInternalName(info);
		
		compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, internalName, null, "java/lang/Object", 
				new String[] { "org/gnome/gir/gobject/GObject$GObjectProxy" });
		globals.interfaceTypes.put(internalName, info.getTypeInit());
		
		Type ifaceType = typeFromInfo(info);
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
			String name = ucaseToCamel(fi.getName());
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
	
	private static final class CallableCompilationContext {
		CallableInfo info;
		boolean isMethod;
		boolean isConstructor;
		String name;
		Type returnType;
		ArgInfo[] args;
		Type thisType;
		List<Type> argTypes;
		List<String> argNames = new ArrayList<String>();
		boolean throwsGError;
		boolean isInterfaceMethod = false;
		InterfaceInfo targetInterface = null;
		Map<Integer, Integer> lengthOfArrayIndices = new HashMap<Integer,Integer>();
		Map<Integer, Integer> arrayToLengthIndices = new HashMap<Integer,Integer>();
		
		public CallableCompilationContext() {
			// TODO Auto-generated constructor stub
		}

		public String getDescriptor() {
			return Type.getMethodDescriptor(this.returnType, argTypes.toArray(new Type[] {}));
		}
		
		public String getSignature() {
			return getUniqueSignature(name, returnType, argTypes);
		}
		
		public int argOffsetToApi(int offset) {
			return offset - lengthOfArrayIndices.size();
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
			int flags = fi.getFlags();
			ctx.isConstructor = !isStaticCtor && (flags & FunctionInfoFlags.IS_CONSTRUCTOR) != 0;
			ctx.isMethod = !ctx.isConstructor && (flags & FunctionInfoFlags.IS_METHOD) != 0;
		}
		ctx.info = si;
		ctx.args = si.getArgs();
		if (ctx.isConstructor) {
			ctx.returnType = Type.VOID_TYPE;
			ctx.thisType = getCallableReturn(si); 
		} else {
			if (ctx.isMethod && thisType != null)
				ctx.thisType = Type.getObjectType(getInternalNameMapped(thisType));
			ctx.returnType = getCallableReturn(si);
		}
		if (ctx.returnType == null) {
			logger.warning("Skipping callable with unhandled return signature: "+ si.getIdentifier());
			return null;
		}
		ArgInfo[] args = ctx.args;
		
		ctx.throwsGError = args.length > 0 && 
			args[args.length-1].getType().getTag().equals(TypeTag.ERROR);
		
		List<Type> types = new ArrayList<Type>();		
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
			t = toJava(arg);
			if (t == null) {
				logger.warning(String.format("Unhandled argument %s in callable %s", arg, si.getIdentifier()));
				return null;
			}
			if (tag.equals(TypeTag.ARRAY) && arg.getDirection().equals(Direction.IN)) {
				int lenIdx = arg.getType().getArrayLength();
				if (lenIdx >= 0) {
					/* FIXME - remove this hack when the repository is fixed */
					int arrIdx = i;
					if (ctx.isMethod) {
						arrIdx++;
					} 
					ctx.lengthOfArrayIndices.put(lenIdx, arrIdx);
					ctx.arrayToLengthIndices.put(arrIdx, lenIdx);
				}
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
		
		ctx.name = ucaseToCamel(si.getName());
		
		if (seenSignatures != null) {
			String signature = getUniqueSignature(ctx.name, ctx.returnType, ctx.argTypes);
			if (seenSignatures.contains(signature)) {
				logger.warning(String.format("Callable %s duplicates signature: %s", 
						si.getIdentifier(), signature));
				return null;
			}
			seenSignatures.add(signature);
		}

		return ctx;
	}
	
	private static String getUniqueSignature(String name, Type returnType, List<Type> args) {
		StringBuilder builder = new StringBuilder(name);
		builder.append('/');
		builder.append(Type.getMethodDescriptor(returnType, args.toArray(new Type[] {})));
		return builder.toString();
	}
	
	private Type writeLoadArgument(MethodVisitor mv, int loadOffset, Type argType) {
		Class<?> box = getPrimitiveBox(argType);
		mv.visitVarInsn(argType.getOpcode(ILOAD), loadOffset);		
		if (box != null) {
			Type boxedType = Type.getType(box);	
			mv.visitMethodInsn(INVOKESTATIC, boxedType.getInternalName(), 
					"valueOf", "(" + argType.getDescriptor() + ")" + boxedType.getDescriptor());
			return boxedType;
		}
		return argType;
	}
	
	private static final class LocalVariable {
		String name;
		int offset;
		Type type;
		public LocalVariable(String name, int offset, Type type) {
			super();
			this.name = name;
			this.offset = offset;
			this.type = type;
		}
	}
	
	private static final class LocalVariableTable {
		private Map<String,LocalVariable> locals;
		private int lastOffset;
		
		public LocalVariableTable(Type thisType, List<Type> args, List<String> argNames) {
			lastOffset = 0;
			locals = new LinkedHashMap<String,LocalVariable>();
			if (thisType != null) {
				locals.put("this", new LocalVariable("this", 0, thisType));
				lastOffset += thisType.getSize();
			}
			int i = 0;
			if (args == null)
				return;
			for (Type arg: args) {
				String name;
				if (argNames != null)
					name = argNames.get(i);
				else
					name = "arg" + i;
				locals.put(name, new LocalVariable(name, lastOffset, arg));
				lastOffset += arg.getSize();
				i++;
			}			
		}
		
		public LocalVariableTable(CallableCompilationContext ctx) {
			this(ctx.thisType, ctx.argTypes, ctx.argNames);
		}
		
		public LocalVariable add(String name, Type type) {
			LocalVariable ret = new LocalVariable(name, lastOffset, type);
			lastOffset += type.getSize();
			locals.put(name, ret);
			return ret;
		}
		
		public int allocTmp(String name, Type type) {
			return add("tmp_" + name, type).offset;
		}
		
		public Collection<LocalVariable> getAll() {
			return locals.values();
		}
		
		public LocalVariable get(int index) {
			int i = 0;
			for (LocalVariable variable : locals.values()) {
				if (i == index)
					return variable;
				i++;
			}
			throw new IllegalArgumentException(String.format("Index %d is out of range (max %d)", index, locals.size()-1));
		}
		
		public int getOffset(String name) {
			LocalVariable var = locals.get(name);
			return var.offset;
		}
		
		private void writeLocals(MethodVisitor mv, Label start, Label end) {
			for (LocalVariable var : getAll()) {
				mv.visitLocalVariable(var.name, var.type.getDescriptor(), null, start, end, var.offset);
			}			
		}
		
		@Override
		public String toString() {
			return String.format("<locals lastOffset=%s table=%s>", lastOffset, locals);
		}
	}
	
	private void writeCallable(int accessFlags, ClassCompilation compilation, FunctionInfo fi,
			CallableCompilationContext ctx) {
		String descriptor = ctx.getDescriptor();
		String name = ctx.name;
		
		String[] exceptions = null;
		if (ctx.throwsGError) {
			exceptions = new String[] { "org/gnome/gir/gobject/GErrorException" };
		}
		
		if (fi.isDeprecated()) {
			accessFlags += ACC_DEPRECATED;
		}		
		MethodVisitor mv = compilation.writer.visitMethod(accessFlags, 
				name, descriptor, null, exceptions);
		if (fi.isDeprecated()) {
			AnnotationVisitor av = mv.visitAnnotation(Type.getType(Deprecated.class).getDescriptor(), true);
			av.visitEnd();
		}		
		
		String globalInternalsName = getInternals(fi);	
		String symbol = fi.getSymbol();
		
		Class<?> returnBox = getPrimitiveBox(ctx.returnType);
		Type returnTypeBox;
		if (returnBox != null)
			returnTypeBox = Type.getType(returnBox);
		else
			returnTypeBox = ctx.returnType;
		
		mv.visitCode();
		LocalVariableTable locals = ctx.allocLocals();
		int functionOffset = locals.allocTmp("function", Type.getType(Function.class));
		int argsOffset = locals.allocTmp("args", Type.getType(Object[].class));
		int resultOffset = 0;
		if (!ctx.returnType.equals(Type.VOID_TYPE))
			resultOffset = locals.allocTmp("result", returnTypeBox);
		int errorOffset = 0;
		if (ctx.throwsGError)
			errorOffset = locals.allocTmp("error", Type.getType(PointerByReference.class));
		int nInvokeArgs = ctx.args.length;
		if (ctx.isMethod)
			nInvokeArgs += 1;
		int nInvokeArgsNoError = nInvokeArgs - (ctx.throwsGError ? 1 : 0);
		Label jtarget;
		Label l0 = new Label();
		mv.visitLabel(l0);
		if (ctx.throwsGError) {
			mv.visitTypeInsn(NEW, "com/sun/jna/ptr/PointerByReference");
			mv.visitInsn(DUP);
			mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(INVOKESPECIAL, "com/sun/jna/ptr/PointerByReference", "<init>", "(Lcom/sun/jna/Pointer;)V");	
			mv.visitVarInsn(ASTORE, errorOffset);			
		}
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "library", "Lcom/sun/jna/NativeLibrary;");
		mv.visitLdcInsn(symbol);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/NativeLibrary", "getFunction", "(Ljava/lang/String;)Lcom/sun/jna/Function;");				
		mv.visitVarInsn(ASTORE, functionOffset);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitIntInsn(BIPUSH, nInvokeArgs);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		for (int i = 0; i < nInvokeArgsNoError; i++) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, i);
			Integer arraySource = ctx.lengthOfArrayIndices.get(i);
			Integer lengthOfArray = ctx.arrayToLengthIndices.get(i);
			if (arraySource != null) {
				ArgInfo source = ctx.args[arraySource - (ctx.isMethod ? 1 : 0)];
				assert source.getType().getTag().equals(TypeTag.ARRAY);
				int offset = ctx.argOffsetToApi(arraySource);
				LocalVariable var = locals.get(offset);
				writeLoadArgument(mv, var.offset, var.type);
				mv.visitInsn(ARRAYLENGTH);
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf", 
						Type.getMethodDescriptor(getType(Integer.class), new Type[] { Type.INT_TYPE }));
			} else if (lengthOfArray != null) {
				LocalVariable var = locals.get(lengthOfArray);
				writeLoadArgument(mv, var.offset, var.type);
			} else if (!ctx.isMethod || i > 0) {
				LocalVariable var = locals.get(i);			
				writeLoadArgument(mv, var.offset, var.type);	
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
			mv.visitLdcInsn(returnTypeBox);
		}
		mv.visitVarInsn(ALOAD, argsOffset);
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "invocationOptions", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invoke", 
				"(Ljava/lang/Class;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
		Label l3 = new Label();
		mv.visitLabel(l3);		
		if (!ctx.throwsGError) {
			if (ctx.returnType.equals(Type.VOID_TYPE)) {
				mv.visitInsn(POP);
				mv.visitInsn(RETURN);
			} else {
				mv.visitTypeInsn(CHECKCAST, returnTypeBox.getInternalName());
				if (returnBox != null)
					mv.visitMethodInsn(INVOKEVIRTUAL, returnTypeBox.getInternalName(), 
							ctx.returnType.getClassName() + "Value", "()" + ctx.returnType.getDescriptor());
				mv.visitInsn(ctx.returnType.getOpcode(IRETURN));
			}
		} else {
			jtarget = new Label();
			if (ctx.returnType.equals(Type.VOID_TYPE)) {
				mv.visitInsn(POP);
			} else {
				mv.visitTypeInsn(CHECKCAST, returnTypeBox.getInternalName());
				mv.visitInsn(DUP);
				mv.visitVarInsn(ASTORE, resultOffset);
			}
			mv.visitVarInsn(ALOAD, errorOffset);
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/ptr/PointerByReference", 
					"getPointer", "()Lcom/sun/jna/Pointer;");	
			mv.visitJumpInsn(IFNONNULL, jtarget);
			mv.visitTypeInsn(NEW, "org/gnome/gir/gobject/GErrorException");
			mv.visitInsn(DUP);
			mv.visitTypeInsn(NEW, "org/gnome/gir/gobject/GErrorStruct");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, errorOffset);
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/ptr/PointerByReference", "getValue", "()Lcom/sun/jna/Pointer;");
			mv.visitMethodInsn(INVOKESPECIAL, "org/gnome/gir/gobject/GErrorStruct", "<init>", "(Lcom/sun/jna/Pointer;)V");
			mv.visitMethodInsn(INVOKESPECIAL, "org/gnome/gir/gobject/GErrorException", "<init>", "(Lorg/gnome/gir/gobject/GErrorStruct;)V");
			mv.visitInsn(ATHROW);
			mv.visitLabel(jtarget);
			if (ctx.returnType.equals(Type.VOID_TYPE)) {
				mv.visitInsn(RETURN);
			} else {
				mv.visitVarInsn(ALOAD, resultOffset);
				if (returnBox != null)
					mv.visitMethodInsn(INVOKEVIRTUAL, returnTypeBox.getInternalName(), 
							ctx.returnType.getClassName() + "Value", "()" + ctx.returnType.getDescriptor());				
				mv.visitInsn(ctx.returnType.getOpcode(IRETURN));
			}
		}
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
		boolean hasFields = fields.length > 0;
		if (hasFields) {
			compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalName, null, 
					(isRegistered ? "org/gnome/gir/gobject/Boxed" : "com/sun/jna/") + type, null);
		} else {
			compilation.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalName, null, 
					"com/sun/jna/PointerType", null);
			/* Write out a no-args ctor, though people shouldn't use this */
			MethodVisitor mv = compilation.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "com/sun/jna/PointerType", "<init>", "()V");
			mv.visitInsn(RETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
			mv.visitMaxs(0, 0);
			mv.visitEnd();			
		}
		
		if (hasFields && isRegistered)
			writeGetGType(info, compilation);
		
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

			String parentInternalName = "com/sun/jna/" + type;
			
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
			mv = compilation.writer.visitMethod(ACC_PROTECTED, "<init>", "(Lcom/sun/jna/TypeMapper;)V", null, null);
			mv.visitCode();
			l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", "(Lcom/sun/jna/TypeMapper;)V");
			mv.visitInsn(RETURN);
			l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + compilation.internalName + ";", null, l0, l1, 0);
			mv.visitLocalVariable("mapper", "Lcom/sun/jna/TypeMapper;", null, l0, l1, 0);		
			mv.visitMaxs(0, 0);
			mv.visitEnd();
			
			/* constructor that takes all of the fields */
			LocalVariableTable locals = new LocalVariableTable(Type.getObjectType(compilation.internalName), null, null);			
			List<Type> args = new ArrayList<Type>();
			args.add(Type.getObjectType(compilation.internalName));
			boolean allArgsPrimitive = true;
			for (FieldInfo field : fields) {
				Type argType = toJava(field);
				if (argType == null || getPrimitiveBox(argType) == null) {
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
			String name = ucaseToCamel(fi.getName());
			Type fieldType = toJava(fi);
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
		/* The JNA NativeLibrary expects it without the .so */		
		if (shlib.endsWith(".so"))
			shlib = shlib.substring(0, shlib.length()-3);
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
	
	private void compileNamespaceSingle(String namespace, String version) {
		alreadyCompiled.add(namespace);
		
		try {
			repo.require(namespace, version);
			logger.info("Loaded typelib from " + repo.getTypelibPath(namespace));			
		} catch (GErrorException e) {
			throw new RuntimeException(e);
		}
		
		String globalName = namespace + "Globals";
		String peerInternalName = GType.getInternalName(namespace, globalName);
		GlobalsCompilation global = new GlobalsCompilation(namespace, globalName);
		writers.put(peerInternalName, global);		
		globals.put(namespace, global);
		global.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL + ACC_SUPER, global.internalName, null, "java/lang/Object", null);
		initGlobalsClass(global);

		compileNamespaceComponents(namespace);
		
		global.clinit.visitInsn(RETURN);
		global.clinit.visitMaxs(3, 0);
		global.clinit.visitEnd();
		
		global.close();
	}	
	
	private List<ClassCompilation> compileNamespace(String namespace, String version) {
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
	
	private List<ClassCompilation> compileNamespaceRecursive(String namespace) {
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
	
	private static List<ClassCompilation> getStubsUnlocked(Repository repo, String namespace) {
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
	
	public static List<ClassCompilation> getNativeStubs(Repository repo, String namespace) {
		synchronized (loadedRepositories) {
			return getStubsUnlocked(repo, namespace);
		}
	}
	
	public static List<ClassCompilation> compile(Repository repo, String namespace, String version) {
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
		Repository repo = Repository.getDefault();
		File destFile = null;		
		
		repo.require(namespace, version);
		String typelibPathName = repo.getTypelibPath(namespace);
		File typelibPath = new File(typelibPathName);
		long typelibLastModified = typelibPath.lastModified();
		
		if (destFile == null) {
			destFile = getJarPath(namespace);
			logger.info("Will install to: " + destFile);
		}
		
		if (destFile.exists() && destFile.lastModified() > typelibLastModified) {
			logger.info("Skipping already-compiled namespace: " + namespace);
			return destFile;
		}
		
		logger.info(String.format("Compiling namespace: %s version: %s", namespace, version));
		List<ClassCompilation> stubs;
		stubs = CodeFactory.compile(repo, namespace, version);

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
	
	public static void verifyJarFiles(Set<File> jarPaths) throws Exception {
		List<URL> urls = new ArrayList<URL>();
		Map<String, InputStream> allClassnames = new HashMap<String, InputStream>();		
		List<ZipFile> zips = new ArrayList<ZipFile>();
		for (File jarPath : jarPaths) {
			urls.add(jarPath.toURI().toURL());
			logger.info("Verifing " + jarPath);			
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
	
	public static File getJarPath(String namespace) {
		File typelibPath = new File(Repository.getDefault().getTypelibPath(namespace));
		String version = Repository.getDefault().getNamespaceVersion(namespace);
		return new File(typelibPath.getParent(), String.format("%s-%s.jar", namespace, version));		
	}
	
	private static boolean namespaceIsExcluded(String namespace) {
		return namespace.equals("GLib") || namespace.equals("GObject");		
	}
	
	public static void verifyAll(String[] nsversions) throws Exception {
		for (String nsversion : nsversions) {
			int dashIdx = nsversion.lastIndexOf('-');
			String namespace = nsversion.substring(0, dashIdx);
			String version = nsversion.substring(dashIdx+1);
			
			if (namespaceIsExcluded(namespace))
				continue;
			
			Set<File> jarPaths = new HashSet<File>();
						
			Repository.getDefault().require(namespace, version);
			jarPaths.add(getJarPath(namespace));
			
			String[] deps = Repository.getDefault().getDependencies(namespace);
			if (deps != null) {
				for (String dep : deps) {
					String depNamespace = dep.substring(0, dep.lastIndexOf('-'));
					if (!namespaceIsExcluded(depNamespace))
						jarPaths.add(getJarPath(depNamespace));
				}
			}
			verifyJarFiles(jarPaths);
		}
	}
	
	public static void main(String[] args) throws Exception {
		GObjectAPI.gobj.g_type_init();
		if (args[0].equals("--verify"))
			verifyAll(args[1].split(","));
		else {
			String namespace = args[0];
			String version = args[1];
			if (!namespaceIsExcluded(namespace))
				compile(namespace, version);
		}
	}
}
