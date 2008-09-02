package org.gnome.gir.compiler;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
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
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;
import gnu.getopt.Getopt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
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
import org.gnome.gir.gobject.GList;
import org.gnome.gir.gobject.GObjectAPI;
import org.gnome.gir.gobject.GSList;
import org.gnome.gir.repository.ArgInfo;
import org.gnome.gir.repository.BaseInfo;
import org.gnome.gir.repository.BoxedInfo;
import org.gnome.gir.repository.CallableInfo;
import org.gnome.gir.repository.CallbackInfo;
import org.gnome.gir.repository.Direction;
import org.gnome.gir.repository.EnumInfo;
import org.gnome.gir.repository.FieldInfo;
import org.gnome.gir.repository.FlagsInfo;
import org.gnome.gir.repository.FunctionInfo;
import org.gnome.gir.repository.FunctionInfoFlags;
import org.gnome.gir.repository.InterfaceInfo;
import org.gnome.gir.repository.ObjectInfo;
import org.gnome.gir.repository.RegisteredTypeInfo;
import org.gnome.gir.repository.Repository;
import org.gnome.gir.repository.SignalInfo;
import org.gnome.gir.repository.StructInfo;
import org.gnome.gir.repository.TypeInfo;
import org.gnome.gir.repository.TypeTag;
import org.gnome.gir.repository.ValueInfo;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public class CodeFactory {
	
	private static final Logger logger = Logger.getLogger("org.gnome.gir.Compiler");
	
	private static final Map<String,String> overrides = new HashMap<String,String>() {
		private static final long serialVersionUID = 1L;

		{
			put("GLib.Value", "org/gnome/gir/gobject/GValue");
			put("GLib.List", "org/gnome/gir/gobject/GList");
			put("GLib.SList", "org/gnome/gir/gobject/GSList");

			put("GLib.Closure", "org/gnome/gir/gobject/GClosure");
			put("GLib.Quark", "org/gnome/gir/gobject/GQuark");
			put("GLib.TimeVal", "org/gnome/gir/gobject/GTimeVal");
			put("GLib.Scanner", "org/gnome/gir/gobject/GScanner");
			put("GLib.OptionContext", "org/gnome/gir/gobject/GOptionContext");
			put("GLib.OptionGroup", "org/gnome/gir/gobject/GOptionGroup");
			put("GLib.OptionEntry", "org/gnome/gir/gobject/GOptionEntry");
			put("GLib.String", "org/gnome/gir/gobject/GString");	
			put("GLib.Callback", "com/sun/jna/Callback");
			put("GLib.Mutex", "org/gnome/gir/gobject/GLibAPI$GMutex");
			put("GLib.StaticRecMutex", "org/gnome/gir/gobject/GLibAPI$GStaticRecMutex");			
			
			String[] glibPointerUnmapped = new String[] { "Mutex", "Cond", "FreeFunc", "DestroyNotify", "MarkupParser",
					"SpawnChildSetupFunc", "SourceFunc", "Node", "CompareFunc", "KeyFile", "PtrArray", "Func",
					"ThreadPool", "Source", "CompareDataFunc",  };
			for (String unmapped : glibPointerUnmapped)
				put("GLib." + unmapped, "com/sun/jna/Pointer");
			String[] glibIntegerUnmapped = new String[] { "SpawnFlags", "SeekType", };
			for (String unmapped : glibIntegerUnmapped)
				put("GLib." + unmapped, "java/lang/integer");			
			
			put("GObject.ParamSpec", "org/gnome/gir/gobject/GObjectAPI$GParamSpec");			
			put("GObject.Object", "org/gnome/gir/gobject/GObject");
			put("GObject.InitiallyUnowned", "org/gnome/gir/gobject/GInitiallyUnowned");					
			put("GObject.Type", "org/gnome/gir/gobject/GType");
			put("GObject.Value", "org/gnome/gir/gobject/GValue");			
			put("GObject.TypePlugin", "org/gnome/gir/gobject/GTypePlugin");
			put("GObject.TypeModule", "org/gnome/gir/gobject/GTypeModule");	
			put("GObject.TypeClass", "org/gnome/gir/gobject/GObjectAPI$GTypeClass");			
			put("GObject.TypeQuery", "org/gnome/gir/gobject/GObjectAPI$GTypeQuery");
			put("GObject.TypeInfo", "org/gnome/gir/gobject/GObjectAPI$GTypeInfo");
			put("GObject.InterfaceInfo", "org/gnome/gir/gobject/GObjectAPI$GInterfaceInfo");
			put("GObject.TypeValueTable", "org/gnome/gir/gobject/GObjectAPI$GTypeValueTable");			
			put("GObject.TypeFundamentalInfo", "org/gnome/gir/gobject/GObjectAPI$GTypeFundamentalInfo");			
			put("GObject.Class", "org/gnome/gir/gobject/GObjectAPI$GObjectClass");
			put("GObject.InitiallyUnownedClass", "org/gnome/gir/gobject/GObjectAPI$GInitiallyUnownedClass");			
			put("GObject.TypeDebugFlags", "org/gnome/gir/gobject/GObjectAPI$GTypeDebugFlags");
			put("GObject.TypeInstance", "org/gnome/gir/gobject/GObjectAPI$GTypeInstance");
			put("GObject.TypeInterface", "org/gnome/gir/gobject/GObjectAPI$GTypeInterface");			
			put("GObject.String", "org/gnome/gir/gobject/GString");
			put("GObject.Closure", "org/gnome/gir/gobject/GClosure");			
			put("GObject.SignalInvocationHint", "org/gnome/gir/gobject/GSignalAPI$GSignalInvocationHint");			
			put("GObject.EnumValue", "org/gnome/gir/gobject/GObjectAPI$GEnumValue");
			put("GObject.EnumClass", "org/gnome/gir/gobject/GObjectAPI$GEnumClass");			
			put("GObject.FlagsValue", "org/gnome/gir/gobject/GObjectAPI$GFlagsValue");
			put("GObject.FlagsClass", "org/gnome/gir/gobject/GObjectAPI$GFlagsClass");
			
			String[] gobjectUnmapped = new String[] { "Callback", "BaseInitFunc", "InstanceInitFunc", 
					"SignalAccumulator", "ClosureMarshal", "ClassInitFunc", "SignalEmissionHook",
					"IOChannel", "Date", "BaseFinalizeFunc", "ClassFinalizeFunc" };
			for (String unmapped : gobjectUnmapped)
				put("GObject." + unmapped, "com/sun/jna/Pointer");
			String[] gobjectIntegerUnmapped = new String[] { "SignalFlags", "ConnectFlags", "SignalMatchType", 
					"TypeFlags", "ParamFlags"  };
			for (String unmapped : gobjectIntegerUnmapped)
				put("GObject." + unmapped, "java/lang/integer");				
			
			for (String name : new String[] { "Context" }) {
				put("Cairo." + name, "com/sun/jna/Pointer");
			}
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
		requireNamespaceOf(info);
		/* Unfortunately, flags are best mapped as plain Integer  for now */
		if (info instanceof FlagsInfo)
			return Type.getObjectType("java/lang/Integer");
		String internalName = getInternalNameMapped(info.getNamespace(), info.getName());
		if (internalName != null)
			return Type.getObjectType(internalName);
		return null;
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
			/* Special case structure members; we need to use the
			 * $ByReference tag if the member is actually a pointer.
			 */
			if (iface instanceof StructInfo) {
				StructInfo struct = (StructInfo) iface;
				String internalName = getInternalNameMapped(struct);
				if (type.isPointer() && internalName.startsWith(dynamicNamespace))
					internalName += "$ByReference";
				return Type.getObjectType(internalName);
			} else if (iface instanceof InterfaceInfo || iface instanceof ObjectInfo ||
					iface instanceof BoxedInfo) {
				/* Interfaces/Objects/Boxed are always Pointer for now
				 */
				return Type.getType(Pointer.class);
			}
		}
		return toJava(arg.getType());
	}	
	
	public Type toJava(ArgInfo arg) {
		if (arg.getDirection() == Direction.IN) {
			return toJava(arg.getType());
		} else {
			return Type.getType(PointerType.class);
		}
	}
	
	public static Type toJavaRef(TypeTag tag) {
		Type t = toJava(tag);
		if (t.equals(Type.getType(Integer.class)))
			return Type.getType(IntByReference.class);
		if (t.equals(Type.getType(Long.class)))
			return Type.getType(LongByReference.class);
		if (t.equals(Type.getType(Boolean.class)))
			return Type.getType(IntByReference.class);
		if (t.equals(Type.getType(Byte.class)))
			return Type.getType(ByteByReference.class);
		if (t.equals(Type.getType(Short.class)))
			return Type.getType(ShortByReference.class);
		if (t.equals(Type.getType(Float.class)))
			return Type.getType(FloatByReference.class);
		if (t.equals(Type.getType(Double.class)))
			return Type.getType(Double.class);
		if (t.equals(Type.getType(String.class)) || t.equals(Type.getType(File.class)))
			return Type.getType(PointerByReference.class);
		if (t.equals(Type.VOID_TYPE))
			return Type.getType(Pointer.class);
		return t;
	}
	
	private static Type toTypeBase(TypeTag tag) {
		if (tag == TypeTag.VOID)
			return Type.VOID_TYPE;
		if (tag == TypeTag.BOOLEAN)
			return Type.getType(Boolean.class);
		if (tag == TypeTag.INT8 || tag == TypeTag.UINT8)
			return Type.getType(Byte.class);
		if (tag == TypeTag.INT16 || tag == TypeTag.UINT16)
			return Type.getType(Short.class);
		if (tag == TypeTag.INT32 || tag == TypeTag.UINT32 ||
				tag == TypeTag.INT || tag == TypeTag.UINT)
			return Type.getType(Integer.class);
		if (tag == TypeTag.INT64 || tag == TypeTag.UINT64
				|| tag == TypeTag.SIZE || tag == TypeTag.SSIZE)
			return Type.getType(Long.class);
		if (tag == TypeTag.FLOAT)
			return Type.getType(Float.class);
		if (tag == TypeTag.DOUBLE)
			return Type.getType(Double.class);
		if (tag == TypeTag.UTF8)
			return Type.getType(String.class);
		if (tag == TypeTag.FILENAME)
			return Type.getType(File.class);		
		return null;
	}

	private static final String dynamicNamespace = "org/gnome/gir/dynamic/";
	private static final String publicDynamicNamespace = "org.gnome.gir.dynamic.";
	
	private Type getCallableReturn(CallableInfo callable) {
		TypeInfo info = callable.getReturnType();
		if (info.getTag().equals(TypeTag.INTERFACE)) {
			if (!requireNamespaceOf(info.getInterface()))
				return Type.getType(Pointer.class);
			else
				return typeFromInfo(info);
		}
		return toJava(info.getTag());
	}
	
	private List<Type> getCallableArgs(CallableInfo callable, boolean isMethod,
				boolean allowError) {
		ArgInfo[] args = callable.getArgs();
		List<Type> types = new ArrayList<Type>();
		boolean skipFirst = isMethod;
		for (int i = 0; i < args.length; i++) {
			ArgInfo arg = args[i];
			Type t;
			TypeInfo info = arg.getType();
			TypeTag tag = info.getTag();			
			if (tag.equals(TypeTag.ERROR)) {
				if (allowError)
					continue;
				return null;
			}
			t = toJava(arg);
			if (t == null) {
				logger.warning("Unhandled argument: " + arg);
				return null;
			}
			if (skipFirst)
				skipFirst = false;
			else
				types.add(t);
		}
		
		return types;
	}

	public static final class RepositoryClassSet {
		Map<String,byte[]> klasses;

		public RepositoryClassSet() {
		}

		public Map<String, byte[]> getKlasses() {
			return Collections.unmodifiableMap(klasses);
		}
	}
	
	private static String getInternalNameMapped(String namespace, String name) {
		String key = namespace + "." + name;
		String val = overrides.get(key);
		if (val != null)
			return val;
		if (namespace.equals("GLib") || namespace.equals("GObject"))
			throw new RuntimeException(String.format("Unmapped internal ns=%s name=%s", namespace, name));
		return getInternalName(namespace, name);
	}
	
	private static String getInternalName(String namespace, String name) {
		String caps = name.substring(0, 1).toUpperCase() + name.substring(1);
		return dynamicNamespace + namespace + "/" + caps;
	}
	
	private static String getPublicName(String namespace, String name) {
		String caps = name.substring(0, 1).toUpperCase() + name.substring(1);
		return publicDynamicNamespace + namespace + "." + caps;
	}
	
	public interface StubClass {
		public String getNamespace();
		public String getPublicName();
		public byte[] getBytes();
	}
	
	private static final class InnerClassCompilation implements StubClass {
		String namespace;
		String baseName;
		String internalName;
		ClassWriter writer;
		public InnerClassCompilation(String namespace, String baseName) {
			this.namespace = namespace;
			this.baseName = baseName;
			this.internalName = getInternalName(namespace, baseName);
			this.writer = new ClassWriter(0);
		}
		@Override
		public byte[] getBytes() {
			return writer.toByteArray();
		}
		
		@Override
		public String getNamespace() {
			return namespace;
		}
		
		@Override
		public String getPublicName() {
			return CodeFactory.getPublicName(namespace, baseName);
		}
	}	
	
	private static final class ClassCompilation implements StubClass {
		ClassWriter writer;
		Set<InnerClassCompilation> innerClasses;
		String namespace;
		String baseName;
		String publicName;
		String internalName;
		private boolean closed = false;
		
		public ClassCompilation(String namespace,
				String name) {
			this.writer = new ClassWriter(0);
			this.innerClasses = new HashSet<InnerClassCompilation>();
			this.namespace = namespace;
			this.baseName = name.substring(0, 1).toUpperCase() + name.substring(1);
			this.publicName = CodeFactory.getPublicName(namespace, name);
			this.internalName = getInternalName(namespace, name);
		}
		
		public InnerClassCompilation newInner() {
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

		@Override
		public byte[] getBytes() {
			return writer.toByteArray();
		}
		
		@Override
		public String getNamespace() {
			return namespace;
		}

		@Override
		public String getPublicName() {
			return publicName;
		}
	}
	
	private class InfoCompilation {
		public ClassCompilation peer;
		public ClassCompilation api;
		
		private Set<String> definedApiSymbols = new HashSet<String>();
		
		protected InfoCompilation(ClassCompilation peer, ClassCompilation api) {
			this.peer = peer;
			this.api = api;
		}
		
		public boolean tryDefineApi(String symbol, String descriptor) {
			String dualString = symbol + "/" + descriptor;
			if (definedApiSymbols.contains(dualString))
				return false;
			definedApiSymbols.add(dualString);
			return true;
		}
	}

	public InfoCompilation getCompilation(String namespace, String name) {
		String peerInternalName = getInternalName(namespace, name);
		InfoCompilation ret = writers.get(peerInternalName);
		if (ret == null) {
			String peerName = name + "$1";
			ClassCompilation peer = new ClassCompilation(namespace, name);
			ClassCompilation api = new ClassCompilation(namespace, peerName);
			ret = new InfoCompilation(peer, api);
			writers.put(peerInternalName, ret);
		}
		return ret;
	}	
	
	public InfoCompilation getCompilation(BaseInfo info) {
		return getCompilation(info.getNamespace(), info.getName());
	}
	
	public String getGlobalsName(String namespace) {
		return getInternalName(namespace, namespace+"Globals");
	}
	
	public InfoCompilation getGlobals(String namespace) {
		return getCompilation(namespace, namespace + "Globals");
	}
	
	public String getInternals(BaseInfo info) {
		return getGlobalsName(info.getNamespace()) + "$Internals";
	}
	
	public InfoCompilation getCompilation(FunctionInfo info) {
		return getGlobals(info.getNamespace());
	}		
	
	private final Repository repo;
	private final Set<String> alreadyCompiled = new HashSet<String>();
	private final Set<String> loadFailed = new HashSet<String>();
	private final Set<String> pendingCompilation = new HashSet<String>();	
	private final Map<String, InfoCompilation> writers = new HashMap<String, InfoCompilation>();
	private final Map<String, InfoCompilation> globals = new HashMap<String, InfoCompilation>();
	private final Map<String,String> namespaceShlibMapping = new HashMap<String, String>();
	
	private CodeFactory(Repository repo) {
		this.repo = repo;
		this.alreadyCompiled.add("GLib");
	}
	
	private static final Map<Repository,List<StubClass>> loadedRepositories 
		= new WeakHashMap<Repository, List<StubClass>>();
	
	private static String getInternalName(BaseInfo info) {
		return getInternalName(info.getNamespace(), info.getName());
	}
	
	private static String getInternalNameMapped(BaseInfo info) {
		return getInternalNameMapped(info.getNamespace(), info.getName());
	}	
	
	@SuppressWarnings("unused")
	private static String getPublicName(BaseInfo info) {
		return getPublicName(info.getNamespace(), info.getName());
	}
	
	private String enumNameToUpper(String nick) {
		return nick.replace("-", "_").toUpperCase();
	}
	
	private void compile(EnumInfo info) {
		InfoCompilation compilation = getCompilation(info);
		compilation.peer.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL + ACC_SUPER + ACC_ENUM, compilation.peer.internalName, 
				"Ljava/lang/Enum<L" + compilation.peer.internalName + ";>;", "java/lang/Enum", null);
		ValueInfo[] values = info.getValueInfo();
		for (ValueInfo valueInfo : values) {
			String name = enumNameToUpper(valueInfo.getName());			
			FieldVisitor fv = compilation.peer.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM, 
						name, "L" + compilation.peer.internalName + ";", null, null);
			fv.visitEnd();				
		}
		
		/* And now a HUGE chunk of stuff to comply with the enum spec */
		
		String arrayDescriptor = "[L" + compilation.peer.internalName + ";";		
		
		FieldVisitor fv = compilation.peer.writer.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC + ACC_SYNTHETIC, 
				"ENUM$VALUES", arrayDescriptor, null, null);
		fv.visitEnd();
		
		MethodVisitor mv = compilation.peer.writer.visitMethod(ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
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
		mv.visitMaxs(3, 3);
		mv.visitEnd();		
		
		mv = compilation.peer.writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		int i = 0;
		for (ValueInfo valueInfo : values) {
			String name = enumNameToUpper(valueInfo.getName());
			mv.visitTypeInsn(NEW, compilation.peer.internalName);
			mv.visitInsn(DUP);
			mv.visitLdcInsn(name);
			mv.visitIntInsn(BIPUSH, i);
			i++;
			mv.visitMethodInsn(INVOKESPECIAL, compilation.peer.internalName, "<init>", "(Ljava/lang/String;I)V");
			mv.visitFieldInsn(PUTSTATIC, compilation.peer.internalName, 
						name, "L" + compilation.peer.internalName+ ";");			
		}
		mv.visitIntInsn(BIPUSH, values.length);
		mv.visitTypeInsn(ANEWARRAY, compilation.peer.internalName);
		i = 0;
		for (ValueInfo valueInfo : values) {
			String name = enumNameToUpper(valueInfo.getName());			
			mv.visitInsn(DUP);			
			mv.visitIntInsn(BIPUSH, i);
			i++;
			mv.visitFieldInsn(GETSTATIC, compilation.peer.internalName, name, 
						"L" + compilation.peer.internalName + ";");
			mv.visitInsn(AASTORE);			
		}
		mv.visitFieldInsn(PUTSTATIC, compilation.peer.internalName, "ENUM$VALUES", arrayDescriptor);
		mv.visitInsn(RETURN);
		mv.visitMaxs(4, 0);
		mv.visitEnd();	
		
		mv = compilation.peer.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "values", "()" + arrayDescriptor, null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitFieldInsn(GETSTATIC, compilation.peer.internalName, "ENUM$VALUES", arrayDescriptor);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ASTORE, 0);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ISTORE, 1);
		mv.visitTypeInsn(ANEWARRAY, compilation.peer.internalName);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ASTORE, 2);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ILOAD, 1);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
		mv.visitVarInsn(ALOAD, 2);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(5, 3);
		mv.visitEnd();
		
		mv = compilation.peer.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "valueOf", "(Ljava/lang/String;)L" + compilation.peer.internalName + ";", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLdcInsn(Type.getType("L" + compilation.peer.internalName + ";"));
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;");
		mv.visitTypeInsn(CHECKCAST, compilation.peer.internalName);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(2, 1);
		mv.visitEnd();		
		
		compilation.peer.close();
	}
	
	private void compile(FlagsInfo info) {
		InfoCompilation compilation = getCompilation(info);
		compilation.peer.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, compilation.peer.internalName, null, "java/lang/Object", null);
		ValueInfo[] values = info.getValueInfo();
		for (ValueInfo valueInfo : values) {
			FieldVisitor fv = compilation.peer.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, 
						enumNameToUpper(valueInfo.getName()), "J", null, valueInfo.getValue());
			fv.visitEnd();				
		}
		compilation.peer.close();
	}	
	
	private String ucaseToCamel(String ucase) {
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
	
	private String ucaseToPascal(String ucase) {
		String camel = ucaseToCamel(ucase);
		return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
	}	
	
	private void compileDefaultConstructor(ObjectInfo info, InfoCompilation compilation) {		
		BaseInfo parent = info.getParent(); 
		String parentInternalType = getInternalNameMapped(parent);
		
		MethodVisitor mv = compilation.peer.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, compilation.peer.internalName, "getGType", "()Lorg/gnome/gir/gobject/GType;");
		mv.visitInsn(ACONST_NULL);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/GType;[Ljava/lang/Object;)V");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L" + compilation.peer.internalName + ";", null, l0, l2, 0);
		mv.visitMaxs(3, 1);
		mv.visitEnd();
		
		mv = compilation.peer.writer.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/Object;)V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, compilation.peer.internalName, "getGType", "()Lorg/gnome/gir/gobject/GType;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/GType;[Ljava/lang/Object;)V");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L"+ compilation.peer.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l2, 1);
		mv.visitMaxs(3, 2);
		mv.visitEnd();
		
		mv = compilation.peer.writer.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/util/Map;)V", 
				"(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, compilation.peer.internalName, "getGType", "()Lorg/gnome/gir/gobject/GType;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/GType;Ljava/util/Map;)V");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L"+ compilation.peer.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("args", "Ljava/util/Map;", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", l0, l2, 1);
		mv.visitMaxs(3, 2);
		mv.visitEnd();		
		
		mv = compilation.peer.writer.visitMethod(ACC_PROTECTED, "<init>", "(Lorg/gnome/gir/gobject/GType;[Ljava/lang/Object;)V", null, null);
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
		mv.visitLocalVariable("this", "L"+ compilation.peer.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("gtype", "Lorg/gnome/gir/gobject/GType;", null, l0, l2, 1);		
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l2, 2);
		mv.visitMaxs(3, 3);
		mv.visitEnd();
		
		mv = compilation.peer.writer.visitMethod(ACC_PROTECTED, "<init>", "(Lorg/gnome/gir/gobject/GType;Ljava/util/Map;)V", 
				"(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", null);
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
		mv.visitLocalVariable("this", "L"+ compilation.peer.internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("gtype", "Lorg/gnome/gir/gobject/GType;", null, l0, l2, 1);		
		mv.visitLocalVariable("args", "Ljava/util/Map;", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", l0, l2, 2);
		mv.visitMaxs(3, 3);
		mv.visitEnd();		
	}
	
	private void compileStaticConstructor(ObjectInfo info, InfoCompilation compilation, FunctionInfo fi) {	
		String globalInternalsName = getInternals(info);

		ArgInfo[] argInfos = fi.getArgs();
		List<Type> args = getCallableArgs(fi, false, false);		 
		String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, args.toArray(new Type[0]));
		
		int nArgs = args.size();
		
		String name = ucaseToCamel(fi.getName());
		MethodVisitor mv = compilation.peer.writer.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, name, descriptor, null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitTypeInsn(NEW, compilation.peer.internalName);
		mv.visitInsn(DUP);
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "library", "Lcom/sun/jna/NativeLibrary;");
		mv.visitLdcInsn(fi.getSymbol());
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/NativeLibrary", "getFunction", "(Ljava/lang/String;)Lcom/sun/jna/Function;");
		mv.visitLdcInsn(Type.getType("Lcom/sun/jna/Pointer;"));
		mv.visitIntInsn(BIPUSH, args.size());
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		for (int i = 0; i < nArgs; i++) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, i);
			mv.visitVarInsn(ALOAD, i);
			mv.visitInsn(AASTORE);			
		}
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "invocationOptions", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invoke", "(Ljava/lang/Class;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, "com/sun/jna/Pointer");
		mv.visitMethodInsn(INVOKESTATIC, compilation.peer.internalName, "initializer", 
			"(Lcom/sun/jna/Pointer;)Lorg/gnome/gir/gobject/Handle$Initializer;");		
		mv.visitMethodInsn(INVOKESPECIAL, compilation.peer.internalName, "<init>", "(Lorg/gnome/gir/gobject/Handle$Initializer;)V");
		mv.visitInsn(ARETURN);
		Label l4 = new Label();
		mv.visitLabel(l4);
		for (int i = 0; i < nArgs; i++) {
			mv.visitLocalVariable(argInfos[i].getName(), args.get(i).toString(), null, l0, l4, i);
		}
		mv.visitMaxs(8, nArgs);
		mv.visitEnd();		
	}	
	
	private void compileConstructor(ObjectInfo info, InfoCompilation compilation, FunctionInfo fi) {	
		String globalInternalsName = getInternals(info);

		ArgInfo[] argInfos = fi.getArgs();
		List<Type> args = getCallableArgs(fi, false, false);		
		BaseInfo parent = info.getParent(); 
		String parentInternalType = getInternalNameMapped(parent);
		String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, args.toArray(new Type[0]));
		
		int nArgs = args.size();
		
		MethodVisitor mv = compilation.peer.writer.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "library", "Lcom/sun/jna/NativeLibrary;");
		mv.visitLdcInsn(fi.getSymbol());
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/NativeLibrary", "getFunction", "(Ljava/lang/String;)Lcom/sun/jna/Function;");
		mv.visitLdcInsn(Type.getType("Lcom/sun/jna/Pointer;"));
		mv.visitIntInsn(BIPUSH, args.size());
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		for (int i = 0; i < nArgs; i++) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, i);
			mv.visitVarInsn(ALOAD, i+1);
			mv.visitInsn(AASTORE);			
		}
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "invocationOptions", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invoke", "(Ljava/lang/Class;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, "com/sun/jna/Pointer");
		mv.visitMethodInsn(INVOKESTATIC, compilation.peer.internalName, "initializer", 
			"(Lcom/sun/jna/Pointer;)Lorg/gnome/gir/gobject/Handle$Initializer;");		
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalType, "<init>", "(Lorg/gnome/gir/gobject/Handle$Initializer;)V");
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitInsn(RETURN);
		Label l4 = new Label();
		mv.visitLabel(l4);
		mv.visitLocalVariable("this", "L" + compilation.peer.internalName + ";", null, l0, l4, 0);
		for (int i = 0; i < nArgs; i++) {
			mv.visitLocalVariable(argInfos[i].getName(), args.get(i).toString(), null, l0, l3, i+1);
		}
		mv.visitMaxs(9, 1+nArgs);
		mv.visitEnd();		
	}
	
	private void compileSignal(ObjectInfo info, InfoCompilation compilation, CallableCompilationContext ctx, SignalInfo sig) {
		String rawSigName = sig.getName();
		String sigName = rawSigName.replace('-', '_');
		String sigClass = ucaseToPascal(sigName);
		String sigHandlerName = "on" + sigClass;
		InnerClassCompilation sigCompilation = compilation.peer.newInner(sigClass);
		compilation.peer.writer.visitInnerClass(sigCompilation.internalName, compilation.peer.internalName, sigClass, 
				ACC_PUBLIC + ACC_ABSTRACT + ACC_STATIC + ACC_INTERFACE);
		sigCompilation.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, 
				sigCompilation.internalName, null, "java/lang/Object", new String[] { "org/gnome/gir/gobject/Closure" });
		sigCompilation.writer.visitInnerClass(sigCompilation.internalName, compilation.peer.internalName, 
				sigClass, ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT + ACC_INTERFACE);
		
		
		/* public static final String METHOD_NAME = */
		FieldVisitor fv = sigCompilation.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, 
				"METHOD_NAME", "Ljava/lang/String;", null, sigHandlerName);
		fv.visitEnd();
		String descriptor = Type.getMethodDescriptor(ctx.returnType, ctx.argTypes.toArray(new Type[0]));		
		
		MethodVisitor mv = sigCompilation.writer.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, sigHandlerName, descriptor, null, null);
		mv.visitEnd();
		
		/* public final long connect(SIGCLASS proxy) */
		mv = compilation.peer.writer.visitMethod(ACC_PUBLIC + ACC_FINAL, "connect", "(L"+ sigCompilation.internalName + ";)J", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitLdcInsn(rawSigName);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, compilation.peer.internalName, "connect", "(Ljava/lang/String;Lorg/gnome/gir/gobject/Closure;)J");
		mv.visitInsn(LRETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L"+ compilation.peer.internalName + ";", null, l0, l1, 0);
		mv.visitLocalVariable("c", "L" + sigCompilation.internalName + ";", null, l0, l1, 1);
		mv.visitMaxs(3, 2);
		mv.visitEnd();
		
		sigCompilation.writer.visitEnd();
	}
		
	private void compile(ObjectInfo info) {
		Label l0, l1, l2;
		InfoCompilation compilation = getCompilation(info);
		
		if (info.getNamespace().equals("GObject") && info.getName().equals("Object"))
			return;
		
		String internalName = getInternalName(info);
		BaseInfo parent = info.getParent();
		String parentInternalName;
				
		requireNamespaceOf(parent);
		parentInternalName = getInternalNameMapped(parent);
		
		String[] interfaces = null;
		InterfaceInfo[] giInterfaces = info.getInterfaces();
		if (giInterfaces.length > 0) {
			interfaces = new String[giInterfaces.length];
		}
		for (int i = 0; i < giInterfaces.length; i++) {
			interfaces[i] = getInternalNameMapped(giInterfaces[i]);
		}
		
		compilation.peer.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalName, null, parentInternalName, interfaces);
		
		for (SignalInfo sig : info.getSignals()) {
			CallableCompilationContext ctx = tryCompileCallable(sig);
			if (ctx == null)
				continue;
			// Insert the object as first parameter
			ctx.argTypes.add(0, typeFromInfo(info));				
			compileSignal(info, compilation, ctx, sig);
		}
		
		writeGetGType(info, compilation);
		
		MethodVisitor mv = compilation.peer.writer.visitMethod(ACC_PROTECTED, "<init>", "(Lorg/gnome/gir/gobject/Handle$Initializer;)V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, parentInternalName, "<init>", "(Lorg/gnome/gir/gobject/Handle$Initializer;)V");
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L" + internalName + ";", null, l0, l2, 0);
		mv.visitLocalVariable("init", "Lorg/gnome/gir/gobject/Handle$Initializer;", null, l0, l2, 1);
		mv.visitMaxs(2, 2);
		mv.visitEnd();	
		
		compileDefaultConstructor(info, compilation);
		
		Map<String,Set<FunctionInfo>> ctors = new HashMap<String, Set<FunctionInfo>>();
		
		// First gather the set of all constructors; we need to avoid name clashes
		for (FunctionInfo fi : info.getMethods()) {
			boolean isConstructor = (fi.getFlags() & FunctionInfoFlags.IS_CONSTRUCTOR) != 0;
			if (!isConstructor)
				continue;
			List<Type> args = getCallableArgs(fi, false, false);
			if (args == null) {
				logger.warning("Skipping constructor with unhandled arg signature: " + fi.getSymbol());
				continue;
			}
			if (args.size() == 0) {
				logger.fine("Skipping 0-args constructor: " + fi.getName());
				continue;
			}
			String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, args.toArray(new Type[0]));			
			if (!ctors.containsKey(descriptor)) {
				ctors.put(descriptor, new HashSet<FunctionInfo>());
			}
			Set<FunctionInfo> set = ctors.get(descriptor);
			set.add(fi);
		}
		
		for (Set<FunctionInfo> ctorGroup : ctors.values()) {
			FunctionInfo first = ctorGroup.iterator().next();			
			if (ctorGroup.size() == 1) {
				compileConstructor(info, compilation, first);
			} else {
				logger.info("Constructor name " + first.getSymbol() + " clashes");
				for (FunctionInfo ctor : ctorGroup) {
					compileStaticConstructor(info, compilation, ctor);
				}
			}
		}
		
		// Now do methods
		Set<String> sigs = new HashSet<String>();		
		for (FunctionInfo fi : info.getMethods()) {	
			boolean isConstructor = (fi.getFlags() & FunctionInfoFlags.IS_CONSTRUCTOR) != 0;
			if (isConstructor)
				continue;
			CallableCompilationContext ctx = tryCompileCallable(fi, sigs);
			if (ctx == null)
				continue;			
			writeCallable(ACC_PUBLIC, info, compilation, fi, ctx);
		}
		for (InterfaceInfo iface : giInterfaces) {
			for (FunctionInfo fi: iface.getMethods()) {
				CallableCompilationContext ctx = tryCompileCallable(fi, sigs);
				if (ctx == null)
					continue;
				ctx.isInterfaceMethod = true;
				ctx.targetInterface = iface;
				writeCallable(ACC_PUBLIC, info, compilation, fi, ctx);
			}
		}
		compilation.peer.close();	
	}
	
	private void compile(InterfaceInfo info) {
		InfoCompilation compilation = getCompilation(info);
		
		String internalName = getInternalName(info);
		
		compilation.peer.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, internalName, null, "java/lang/Object", null);
		Set<String> sigs = new HashSet<String>();		
		for (FunctionInfo fi : info.getMethods()) {
			CallableCompilationContext ctx = tryCompileCallable(fi, sigs);
			if (ctx == null)
				continue;			
			String name = ucaseToCamel(fi.getName());
			String descriptor = Type.getMethodDescriptor(ctx.returnType, ctx.argTypes.toArray(new Type[0]));
			MethodVisitor mv = compilation.peer.writer.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, name, descriptor, null, null);
			mv.visitEnd();			
		}
		compilation.peer.close();	
	}
	
	private static final class CallableCompilationContext {
		Type returnType;
		ArgInfo[] args;
		List<Type> argTypes;
		boolean throwsGError;
		boolean isInterfaceMethod = false;
		InterfaceInfo targetInterface = null;
		public CallableCompilationContext(Type returnType, ArgInfo[] args,
				List<Type> argTypes, boolean throwsGError) {
			this.returnType = returnType;
			this.args = args;
			this.argTypes = argTypes;
			this.throwsGError = throwsGError;
		}
	}
	
	private CallableCompilationContext tryCompileCallable(CallableInfo si) {
		Type returnType = getCallableReturn(si);
		if (returnType == null) {
			logger.warning("Skipping callable with unhandled return signature: " + si.getName());
			return null;
		}
		ArgInfo[] argInfos = si.getArgs();		
		List<Type> args = getCallableArgs(si, false, false);
		if (args == null) {
			logger.warning("Skipping callable with unhandled arg signature: " + si.getName());
			return null;
		}	
		return new CallableCompilationContext(returnType, argInfos, args, false);
	}	
	
	private CallableCompilationContext tryCompileCallable(FunctionInfo fi, Set<String> seenSignatures) {
		Type returnType = getCallableReturn(fi);
		if (returnType == null) {
			logger.warning("Skipping function with unhandled return signature: " + fi.getSymbol());
			return null;
		}
		ArgInfo[] argInfos = fi.getArgs();
		boolean throwsGError = argInfos.length > 0 && 
			argInfos[argInfos.length-1].getType().getTag().equals(TypeTag.ERROR);
		if (throwsGError && returnType.equals(Type.VOID_TYPE)) {
			logger.warning("Skipping function which returns Void and uses GError: " + fi.getSymbol());			
			return null;
		}
		List<Type> args = getCallableArgs(fi, (fi.getFlags() & FunctionInfoFlags.IS_METHOD) > 0,
					throwsGError);
		if (args == null) {
			logger.warning("Skipping function with unhandled arg signature: " + fi.getSymbol());
			return null;
		}
		StringBuilder builder = new StringBuilder(fi.getName());
		builder.append("(");
		for (Type arg: args)
			builder.append(arg.getDescriptor());
		builder.append(")");
		builder.append(returnType.getDescriptor());
		String signature = builder.toString();
		if (seenSignatures.contains(signature)) {
			logger.warning("Function " + fi.getSymbol() + " duplicates signature: " 
						+ signature);
			return null;
		}
		seenSignatures.add(signature);
		return new CallableCompilationContext(returnType, argInfos, args, throwsGError);
	}
	
	private void writeCallable(int accessFlags,
			RegisteredTypeInfo parent, InfoCompilation compilation, FunctionInfo fi,
			CallableCompilationContext ctx) {
		String descriptor = Type.getMethodDescriptor(ctx.returnType, ctx.argTypes.toArray(new Type[0]));
		String name = ucaseToCamel(fi.getName());
		
		String[] exceptions = null;
		if (ctx.throwsGError) {
			exceptions = new String[] { "org/gnome/gir/gobject/GErrorException" };
		}
		MethodVisitor mv = compilation.peer.writer.visitMethod(accessFlags, 
				name, descriptor, null, exceptions);
		
		String globalInternalsName = getInternals(fi);
		boolean includeThis = (fi.getFlags() & FunctionInfoFlags.IS_METHOD) > 0;			
		String symbol = fi.getSymbol();
		
		mv.visitCode();
		int nArgs = ctx.argTypes.size();
		int nInvokeArgs = nArgs;
		if (includeThis)
			nInvokeArgs += 1;
		int functionOffset = nInvokeArgs+1;
		int arrayOffset = functionOffset+1;
		int resultOffset = arrayOffset+1;		
		int errorOffset = resultOffset;
		if (ctx.throwsGError)
			errorOffset += 1;
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
		mv.visitIntInsn(BIPUSH, nInvokeArgs + (ctx.throwsGError ? 1 : 0));
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		if (ctx.isInterfaceMethod) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, 0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, compilation.peer.internalName, "handle", "()Lcom/sun/jna/Pointer;");
			mv.visitMethodInsn(INVOKESTATIC, getInternalName(ctx.targetInterface), 
					"getGType", "()Lorg/gnome/gir/gobject/GType;");
			mv.visitMethodInsn(INVOKESTATIC, "org/gnome/gir/gobject/GTypeInstance", 
					"peekInterface", "(Lcom/sun/jna/Pointer;Lorg/gnome/gir/gobject/GType;)Lcom/sun/jna/Pointer;");
			mv.visitInsn(AASTORE);
		}
		for (int i = ctx.isInterfaceMethod ? 1 : 0; i < nInvokeArgs; i++) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, i);
			mv.visitVarInsn(ALOAD, i);
			mv.visitInsn(AASTORE);
		}
		if (ctx.throwsGError) {
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, nInvokeArgs);
			mv.visitVarInsn(ALOAD, errorOffset);
			mv.visitInsn(AASTORE);
		}
		mv.visitVarInsn(ASTORE, arrayOffset);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitVarInsn(ALOAD, functionOffset);
		if (ctx.returnType.equals(Type.VOID_TYPE)) {
			mv.visitLdcInsn(Type.getType(Void.class));
		} else {
			mv.visitLdcInsn(ctx.returnType);
		}
		mv.visitVarInsn(ALOAD, arrayOffset);
		mv.visitFieldInsn(GETSTATIC, globalInternalsName, "invocationOptions", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invoke", "(Ljava/lang/Class;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
		Label l3 = new Label();
		mv.visitLabel(l3);		
		if (!ctx.throwsGError) {
			if (ctx.returnType.equals(Type.VOID_TYPE)) {
				mv.visitInsn(POP);
				mv.visitInsn(RETURN);
			} else {
				mv.visitTypeInsn(CHECKCAST, ctx.returnType.getInternalName());			
				mv.visitInsn(ARETURN);
			}
		} else {
			jtarget = new Label();
			mv.visitTypeInsn(CHECKCAST, ctx.returnType.getInternalName());
			mv.visitInsn(DUP);			
			mv.visitVarInsn(ASTORE, resultOffset);
			mv.visitInsn(DUP);			
			if (ctx.returnType.equals(Type.getType(Boolean.class))) {
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");				
				mv.visitJumpInsn(IFNE, jtarget);
			} else {				
				mv.visitJumpInsn(IFNONNULL, jtarget);
			}
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
			mv.visitVarInsn(ALOAD, resultOffset);
			mv.visitInsn(ARETURN);
		}
		Label l4 = new Label();
		mv.visitLabel(l4);
		if (includeThis) 
			mv.visitLocalVariable("this", "L" + compilation.peer.internalName + ";", null, l0, l4, 0);
		int off = includeThis ? 1 : 0;
		for (int i = 0; i < nArgs; i++) {
			mv.visitLocalVariable(ctx.args[i+off].getName(), ctx.argTypes.get(i).toString(), null, l0, l4, i+off);		
		}
		mv.visitLocalVariable("f", "Lcom/sun/jna/Function;", null, l1, l4, functionOffset);
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l2, l4, arrayOffset);
		if (!ctx.returnType.equals(Type.VOID_TYPE)) {		
			mv.visitLocalVariable("result", "L" + ctx.returnType.getInternalName() + ";", null, l2, l4, resultOffset);
		}
		mv.visitMaxs(8, errorOffset+1);
	}

	private void writeGetGType(RegisteredTypeInfo rti, InfoCompilation compilation) {
		String globalInternalsName = getInternals(rti);		
		MethodVisitor mv = compilation.peer.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "getGType", "()Lorg/gnome/gir/gobject/GType;", null, null);
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
		mv.visitMaxs(4, 0);
		mv.visitEnd();		
	}
	
	private void compileGlobal(InfoCompilation compilation, FunctionInfo fi,
			Set<String> globalSigs) {
		CallableCompilationContext ctx = tryCompileCallable(fi, globalSigs);
		if (ctx == null)
			return;	
		writeCallable(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, null, compilation, fi, ctx);
	}
	
	private void compile(StructInfo info) {
		MethodVisitor mv;
		InfoCompilation compilation = getCompilation(info);
		
		String internalName = getInternalName(info);
		compilation.peer.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalName, null, "com/sun/jna/Structure", null);	
		
		InnerClassCompilation byRef = compilation.peer.newInner("ByReference");				
		compilation.peer.writer.visitInnerClass(compilation.peer.internalName + "$ByReference",
				compilation.peer.internalName, "ByReference", ACC_PUBLIC + ACC_STATIC);
		byRef.writer.visit(V1_6, ACC_PUBLIC + ACC_STATIC, 
				byRef.internalName, null, compilation.peer.internalName, new String[] { "com/sun/jna/Structure$ByReference"});
		
		InnerClassCompilation byValue = compilation.peer.newInner("ByValue");				
		compilation.peer.writer.visitInnerClass(compilation.peer.internalName + "$ByValue",
				compilation.peer.internalName, "ByValue", ACC_PUBLIC + ACC_STATIC);
		byValue.writer.visit(V1_6, ACC_PUBLIC + ACC_STATIC, 
				byValue.internalName, null, compilation.peer.internalName, new String[] { "com/sun/jna/Structure$ByValue"});		
		
		/* constructor */
		mv = compilation.peer.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "com/sun/jna/Structure", "<init>", "()V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + compilation.peer.internalName + ";", null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();		
		
		Set<String> sigs = new HashSet<String>();		
		for (FunctionInfo fi : info.getMethods()) {
			CallableCompilationContext ctx = tryCompileCallable(fi, sigs);
			if (ctx == null)
				continue;			
			writeCallable(ACC_PUBLIC, info, compilation, fi, ctx);	
		}
		
		for (FieldInfo fi : info.getFields()) {
			String name = ucaseToCamel(fi.getName());
			Type type = toJava(fi);
			if (type.equals(Type.VOID_TYPE)) // FIXME Temporary hack for GdkAtom
				type = Type.getType(Pointer.class);
			FieldVisitor fv = compilation.peer.writer.visitField(ACC_PUBLIC, name, type.getDescriptor(), null, null);
			fv.visitEnd();				
		}
		
		compilation.peer.close();
	}
	
	private void compile(BoxedInfo info) {
		InfoCompilation compilation = getCompilation(info);
		
		String internalName = getInternalName(info);
		compilation.peer.writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalName, null, "org/gnome/gir/gobject/GBoxed", null);
		
		MethodVisitor mv = compilation.peer.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(6, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "org/gnome/gir/gobject/GBoxed", "<init>", "()V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + compilation.peer.internalName + ";", null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();			
		
		compilation.peer.close();	
	}
	
	private void compile(CallbackInfo info) {
		MethodVisitor mv;		
		InfoCompilation compilation = getCompilation(info);
		
		String internalName = getInternalName(info);
		compilation.peer.writer.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, internalName, null, "java/lang/Object", 
				new String[] { "com/sun/jna/Callback" });
		
		CallableCompilationContext ctx = tryCompileCallable(info);
		
		FieldVisitor fv = compilation.peer.writer.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, "TYPE_MAPPER", "Lcom/sun/jna/TypeMapper;", null, null);
		fv.visitEnd();
		
		mv = compilation.peer.writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitMethodInsn(INVOKESTATIC, "org/gnome/gir/gobject/GTypeMapper", "getInstance", "()Lorg/gnome/gir/gobject/GTypeMapper;");
		mv.visitFieldInsn(PUTSTATIC, compilation.peer.internalName, "TYPE_MAPPER", "Lcom/sun/jna/TypeMapper;");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 0);
		mv.visitEnd();		
		
		if (ctx != null) {
			String descriptor = Type.getMethodDescriptor(ctx.returnType, ctx.argTypes.toArray(new Type[0]));			
			mv = compilation.peer.writer.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, 
					"callback", descriptor, null, null);
			mv.visitEnd();
		}
		
		compilation.peer.close();	
	}	
	
	private boolean requireNamespaceOf(BaseInfo info) {
		return requireNamespace(info.getNamespace());
	}
	
	private boolean requireNamespace(String namespace) {
		if (alreadyCompiled.contains(namespace))
			return true;
		try {
			repo.require(namespace);
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
	
	private void initGlobalsClass(InfoCompilation globals) {
		Label l0, l1, l2, l3;
		MethodVisitor mv;

		/* We have two inner classes - one is Internals, and one is an anonymous HashMap inside Internals */
		InnerClassCompilation internals = globals.peer.newInner("Internals");		
		InnerClassCompilation internalsInner = globals.peer.newInner("Internals$1");

		globals.peer.writer.visitInnerClass(internals.internalName, globals.peer.internalName, "Internals", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);
		internals.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL + ACC_SUPER, 
				internals.internalName, null, "java/lang/Object", null);
		internals.writer.visitInnerClass(internals.internalName, globals.peer.internalName, "Internals", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);		

		internals.writer.visitInnerClass(internalsInner.internalName, null, null, 0);
		internalsInner.writer.visit(V1_6, ACC_FINAL + ACC_SUPER, internalsInner.internalName,
				"Ljava/util/HashMap<Ljava/lang/Object;Ljava/lang/Object;>;", "java/util/HashMap", null);
		internalsInner.writer.visitOuterClass(internals.internalName, null, null);
		internalsInner.writer.visitInnerClass(internalsInner.internalName, null, null, 0);		

		
		/* private constructor */
		mv = globals.peer.writer.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		mv.visitInsn(RETURN);
		l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + globals.peer.internalName + ";", null, l0, l1, 0);
		mv.visitMaxs(1, 1);
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
		
		FieldVisitor fv = globals.peer.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "library", "Lcom/sun/jna/NativeLibrary;", null, null);
		fv.visitEnd();		

		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "library", "Lcom/sun/jna/NativeLibrary;", null, null);
		fv.visitEnd();

		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "repo", "Lorg/gnome/gir/repository/Repository;", null, null);
		fv.visitEnd();

		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "namespace", "Ljava/lang/String;", null, globals.peer.namespace);
		fv.visitEnd();

		fv = internals.writer.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "invocationOptions", 
				"Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", null);
		fv.visitEnd();		
		
		mv = internals.writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		l0 = new Label();
		mv.visitLabel(l0);
		/* The JNA NativeLibrary expects it without the .so */
		String shlib = repo.getSharedLibrary(globals.peer.namespace);
		if (shlib == null)
			shlib = namespaceShlibMapping.get(globals.peer.namespace);
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
		l3 = new Label();
		mv.visitLabel(l3);
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 0);
		mv.visitEnd();		


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
		mv.visitMaxs(4, 1);
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
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		
		internalsInner.writer.visitEnd();
		internals.writer.visitEnd();
	}
	
	private void compileNamespaceSingle(String namespace) {
		alreadyCompiled.add(namespace);
		
		try {
			repo.require(namespace);
			logger.info("Loaded typelib from " + repo.getTypelibPath(namespace));			
		} catch (GErrorException e) {
			throw new RuntimeException(e);
		}
		
		InfoCompilation global = getGlobals(namespace);
		global.peer.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL + ACC_SUPER, global.peer.internalName, null, "java/lang/Object", null);
		initGlobalsClass(global);
		globals.put(namespace, global);
		
		compileNamespaceComponents(namespace);
		
		global.peer.close();
	}	
	
	private List<StubClass> compileNamespace(String namespace) {
		compileNamespaceSingle(namespace);
		return finish();
	}
	
	private List<StubClass> finish() {
		logger.info("Compiled " + writers.size() + " info objects");		
		List<StubClass> ret = new LinkedList<StubClass>();
		for (InfoCompilation infoc : writers.values()) {
			infoc.peer.close();
			ret.add(infoc.peer);
			for (InnerClassCompilation inner : infoc.peer.innerClasses) {
				ret.add(inner);
			}
		}
		return ret;		
	}
	
	private List<StubClass> compileNamespaceRecursive(String namespace) {
		pendingCompilation.add(namespace);
		while (pendingCompilation.size() > 0) {
			String pending = pendingCompilation.iterator().next();
			logger.info("Namespace: " + pending);
			requireNamespace(pending);
			compileNamespaceSingle(pending);	
			pendingCompilation.remove(pending);
		}
		logger.info("Compiled " + writers.size() + " info objects");
		return finish();
	}
	
	private static List<StubClass> getStubsUnlocked(Repository repo, String namespace) {
		List<StubClass> ret = loadedRepositories.get(namespace);
		if (ret != null) {
			return ret;
		}
		
		logger.info("Starting from namespace: " + namespace);
		
		CodeFactory cf = new CodeFactory(repo);
		ret = cf.compileNamespaceRecursive(namespace);
		loadedRepositories.put(repo, ret);
		
		return ret;
	}
	
	public static List<StubClass> getNativeStubs(Repository repo, String namespace) {
		synchronized (loadedRepositories) {
			return getStubsUnlocked(repo, namespace);
		}
	}
	
	public static List<StubClass> compile(Repository repo, String namespace) {
		CodeFactory cf = new CodeFactory(repo);
		return cf.compileNamespace(namespace);
	}
	
	public static ClassLoader getLoader(List<StubClass> stubs) {
		final Map<String,byte[]> map = new HashMap<String,byte[]>();
		for (StubClass stub: stubs)
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

	public static File generateJar(String namespace, String version, boolean validate) throws GErrorException, IOException {
		Repository repo = Repository.getDefault();
		File destFile = null;		
		
		repo.require(namespace);
		String typelibPathName = repo.getTypelibPath(namespace);
		File typelibPath = new File(typelibPathName);
		long typelibLastModified = typelibPath.lastModified();
		
		if (destFile == null) {
			destFile = new File(typelibPath.getParent(), namespace+".jar");
			logger.info("Will install to: " + destFile);
		}
		
		if (destFile.exists() && destFile.lastModified() > typelibLastModified) {
			logger.info("Skipping already-compiled namespace: " + namespace);
			return destFile;
		}
		
		logger.info("Compiling namespace: " + namespace);		
		List<StubClass> stubs;
		stubs = CodeFactory.compile(repo, namespace);

		Set<String> classNames = new HashSet<String>();
		ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(destFile));
		for (StubClass stub : stubs) {
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
	
	public static void verifyJarFiles(Set<File> jarPaths) throws IOException {
		Set<URL> urls = new HashSet<URL>();
		Set<String> allClassnames = new HashSet<String>();		
		logger.info("Verifying " + jarPaths.size() + " jar paths");
		for (File jarPath : jarPaths) {
			try {
				urls.add(jarPath.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
			ZipFile zf = new ZipFile(jarPath);
			for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
				String name = e.nextElement().getName();
				if (name.endsWith(".class"))
					allClassnames.add(name.replace('/', '.').substring(0, name.length()-6));
			}
		}
		for (String className : allClassnames) {
			try {
				new URLClassLoader(urls.toArray(new URL[] {})) {
					public void loadVerify(String name) throws ClassNotFoundException {
						loadClass(name, true);
					}
				}.loadVerify(className);
			} catch (ClassNotFoundException e) {
				logger.severe("Failed to verify class " + className);
				e.printStackTrace();
				break;
			}
		}		
	}
	
	public static void compileAll(boolean validate) throws IOException, GErrorException, ClassNotFoundException {
		/* Freedesktop/Unix specific */
		String datadirsPath = System.getenv("XDG_DATA_DIRS");
		String dataDirs[];
		if (datadirsPath != null)
			dataDirs = datadirsPath.split(":");
		else
			dataDirs = new String[] { "/usr/share" };
		Set<File> jarPaths = new HashSet<File>();
		for (String dir : dataDirs) {
			File typelibsDir = new File(dir, "girepository");
			for (String filename : typelibsDir.list()) {
				String namespace;
				String version = null;
				int dashIdx = filename.lastIndexOf('-');
				int dot = filename.lastIndexOf('.'); // for typelib
				if (dashIdx < 0)
					namespace = filename.substring(0, dot);
				else {
					namespace = filename.substring(0, dashIdx);
					version = filename.substring(dashIdx+1, dot);
				}
				/* Skip GObject+below for now, we manually bind */
				if (!(namespace.equals("GLib") || namespace.equals("GObject"))) {
					jarPaths.add(generateJar(namespace, version, validate));
				}
			}
		}
		verifyJarFiles(jarPaths);
	}
	
	public static void main(String[] args) throws Exception {
		GObjectAPI.gobj.g_type_init();
		
		boolean validate = false;
		
		Getopt g = new Getopt("jgir-compiler", args, "V");
		int c;
		while ((c = g.getopt()) != -1) {
			switch (c) {
			case 'V':
				validate = true;
				break;
			case '?':
				break; // getopt() already printed an error
			default:
				System.err.print("getopt() returned " + c + "\n");
			}
		}

		compileAll(validate);
	}
}
