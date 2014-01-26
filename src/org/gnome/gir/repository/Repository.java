package org.gnome.gir.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.ParseException;

import gobject.internals.GErrorStruct;
import gobject.internals.GObjectAPI;
import gobject.internals.GlibRuntime;
import gobject.internals.NativeObject;
import gobject.runtime.GErrorException;
import gobject.runtime.GObject;


import com.sun.jna.NativeLong;
import com.sun.jna.ptr.PointerByReference;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Pattern;

public class Repository extends GObject {

	static {
		GlibRuntime.init();		
	}
	
	public Repository(Initializer init) {
		super(init);
	}

	private boolean disableRequires = false;
	
	/* Needed for the compiler to be able to verify classes without loading typelibs 
	 * which could potentially conflict. */
	public void disableRequires() {
		disableRequires = true;
	}
	
	public void prependSearchPath(String path) {
		GIntrospectionAPI.gi.g_irepository_prepend_search_path(path);
	}
	
	public BaseInfo findByName(String namespace, String name) {
		return GIntrospectionAPI.gi.g_irepository_find_by_name(this, namespace, name);
	}
	
	public BaseInfo findByGType(NativeLong g_type) {
		return GIntrospectionAPI.gi.g_irepository_find_by_gtype(this, g_type);
	}
		
	public void require(String namespace, String version) throws GErrorException {
		if (disableRequires)
			return;
		PointerByReference error = new PointerByReference(null);
		if (GIntrospectionAPI.gi.g_irepository_require(this, namespace, version, 0, error) == null) {
			throw new GErrorException(new GErrorStruct(error.getValue()));
		}
	}
	
	public void requireNoFail(String namespace, String version) {
		try {
			require(namespace, version);
		} catch (GErrorException e) {
			throw new RuntimeException(e);
		}
	}
	
	public BaseInfo[] getInfos(String namespace) {
		int nInfos = GIntrospectionAPI.gi.g_irepository_get_n_infos(this, namespace);
		BaseInfo[] ret = new BaseInfo[nInfos];
		for (int i = 0; i < nInfos; i++) {
			ret[i] = GIntrospectionAPI.gi.g_irepository_get_info(this, namespace, i);
		}
		return ret;
	}
	
	public String[] getNamespaces()
	{
		return GIntrospectionAPI.gi.g_irepository_get_loaded_namespaces(this);
	}
	
	public String getSharedLibrary(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_shared_library(this, namespace);
	}
	
	public String[] getDependencies(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_dependencies(this, namespace);
	}
	
	public String getTypelibPath(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_typelib_path(this, namespace);
	}
	
	public boolean isRegistered(String targetNamespace) {
		return GIntrospectionAPI.gi.g_irepository_is_registered(this, targetNamespace);
	}
	
	public String getNamespaceVersion(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_version(this, namespace);
	}

	static GIntrospectionAPI getNativeLibrary() {
		return GIntrospectionAPI.gi;
	}
	
	public static synchronized Repository getDefault() {
		return (Repository) NativeObject.Internals.objectFor(getNativeLibrary().g_irepository_get_default(),
									  Repository.class, false, false);
	}
	
	public Repository() {
		super(GIntrospectionAPI.gi.g_irepository_get_type(), new Object[] {});
	}
	
	public static void main(String[] args) throws IOException {
		final File targetDir = new File("/tmp/gen/src");
		targetDir.mkdirs();
		final File basePackageDir = new File(targetDir, "gobjects");
		
		GIntrospectionAPI.gi.g_irepository_prepend_search_path("/opt/local/lib/girepository-1.0");
		GObjectAPI.gobj.g_type_init();
		Repository repo = getDefault();
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getName().endsWith(".typelib");
			}
		};
		final MessageFormat format = new MessageFormat("{0}-{1}.typelib");
		for (File f : new File("/opt/local/lib/girepository-1.0").listFiles(filter))
		{
			Path path = f.toPath();
			final String filename = path.getFileName().toString();
			
			Object[] parts;
			try {
				parts = format.parse(filename);
			} catch (ParseException e1) {
				e1.printStackTrace();
				continue;
			}
			final String name = (String) parts[0];
			final String version = (String) parts[1];
			try {
				repo.require(name, version);
			} catch (GErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (String ns : repo.getNamespaces())
		{
			if (!ns.contains("gst") && !ns.contains("Gst"))
			{
				continue;
			}
			
			final File packageFolder = new File(basePackageDir, ns);
			packageFolder.mkdirs();
			final File nativePackageFolder = new File(packageFolder, "native");
			nativePackageFolder.mkdirs();
			
			System.out.println("=============");
			System.out.println(ns);
			System.out.println("=============");
			final BaseInfo[] infos = repo.getInfos(ns);
			for (BaseInfo info : infos)
			{
				if (info instanceof ObjectInfo)
				{
					final ObjectInfo obj = (ObjectInfo) info;
					
					final File classFile = new File(packageFolder, obj.getName() + ".java");
					final File nativeFile = new File(nativePackageFolder, obj.getName() + "API.java");
					try (final FileWriter fosNative = new FileWriter(nativeFile);
							final FileWriter fos = new FileWriter(classFile);)
					{
						fos.write("package gobjects." + ns + ";\n");
						
						fos.write("import gobjects." + ns + ".native.*;");
						fos.write("public class " + obj.getName() + "\n");
						fos.write("{\n");
						for (FunctionInfo func : obj.getMethods())
						{
							boolean isConstructor = (func.getFlags() & FunctionInfoFlags.IS_CONSTRUCTOR) != 0;
				            if (isConstructor)
				                continue;
							fos.write("\tpublic " + func.getReturnType() + " " + methodName(func) + "(" + ") {\n");
							fos.write("\t\t" + "\n");
							fos.write("\t}\n");
						}
						fos.write("}\n");
						
						fosNative.write("package gobjects.native." + ns + ";\n");
						fosNative.write("public interface " + obj.getName() + "API\n");
						fosNative.write("{\n");
						for (FunctionInfo func : obj.getMethods())
						{
							final TypeInfo type = func.getReturnType();
							final String returnString;
							if (type.getTag() == TypeTag.VOID)
							{
								returnString = "void";
							}
							else
							{
								returnString = type.toString();
							}
							fosNative.write("\tpublic " + returnString + " " + func.getSymbol() + "(" + ");\n");
						}
						fosNative.write("}\n");
						
						String parent = null;
						if (obj.getParent() != null)
						{
							parent = obj.getParent().getName();
						}
						System.out.println(toStr(obj));
					}
				}
//				System.out.println(info.getName() + " is a " + info.getClass().getName());
			}
		}
	}
	
	private static String ucaseToCamel(String ucase) {
        String[] components = ucase.split("_");
        for (int i = 1; i < components.length; i++)
            components[i] = "" + Character.toUpperCase(components[i].charAt(0)) + components[i].substring(1);
        StringBuilder builder = new StringBuilder();
        for (String component : components)
            builder.append(component);
        return builder.toString();
    }

	private static String methodName(FunctionInfo func) {
		return ucaseToCamel(func.getName());
	}
	
	private static String toStr(ObjectInfo obj)
	{
		final StringBuilder out = new StringBuilder();
		out
			.append(obj.getNamespace())
			.append(":")
			.append(obj.getName());
		if (obj.getParent() != null)
		{
			out.append(" -> ")
				.append(toStr(obj.getParent()));
		}
		return out.toString();
	}
}
