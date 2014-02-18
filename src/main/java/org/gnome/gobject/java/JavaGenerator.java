package org.gnome.gobject.java;

import gobject.internals.GObjectAPI;
import gobject.runtime.GErrorException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.gnome.gir.repository.BaseInfo;
import org.gnome.gir.repository.FlagsInfo;
import org.gnome.gir.repository.FunctionInfo;
import org.gnome.gir.repository.FunctionInfoFlags;
import org.gnome.gir.repository.ObjectInfo;
import org.gnome.gir.repository.Repository;
import org.gnome.gir.repository.StructInfo;

public class JavaGenerator {
	public static void main(String[] args) throws IOException {
		final File targetDir = new File("/tmp/gen/src");
		targetDir.mkdirs();
		final File basePackageDir = new File(
				new File(new File(targetDir, "org"), "gnome"), 
				"gobjects");
		
//		GIntrospectionAPI.gi.g_irepository_prepend_search_path("/opt/local/lib/girepository-1.0");
		GObjectAPI.gobj.g_type_init();
		Repository repo = Repository.getDefault();
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
		
		final VelocityEngine engine = new VelocityEngine();
		engine.setProperty("resource.loader", "classpath");
		engine.setProperty("classpath.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init();
		for (String ns : repo.getNamespaces())
		{
			if (!ns.contains("gst") && !ns.contains("Gst")
					&& !ns.contains("GObject"))
			{
				continue;
			}
			
			final File packageFolder = new File(basePackageDir, ns);
			packageFolder.mkdirs();
			final File nativePackageFolder = new File(packageFolder, "native");
			nativePackageFolder.mkdirs();
			
			final List<FunctionInfo> globalFunctions = new ArrayList<>();
			System.out.println("=============");
			System.out.println(ns);
			System.out.println("=============");
			final BaseInfo[] infos = repo.getInfos(ns);
			for (BaseInfo info : infos)
			{
				if (info instanceof ObjectInfo)
				{
					final ObjectInfo obj = (ObjectInfo) info;
					final List<NativeFunctionMapping> ctors = new ArrayList<>();
					final List<NativeFunctionMapping> functions = new ArrayList<>();
					final List<NativeFunctionMapping> methods = new ArrayList<>();
					for (FunctionInfo finfo : obj.getMethods())
					{
						NativeFunctionMapping function = null;
						try
						{
							if ((finfo.getFlags() & FunctionInfoFlags.IS_CONSTRUCTOR) != 0)
							{
								function = new NativeFunctionMapping(finfo);
								ctors.add(function);
							}
							else if ((finfo.getFlags() & FunctionInfoFlags.IS_METHOD) != 0)
							{
								function = new NativeFunctionMapping(finfo, obj.getName());
								methods.add(function);
							}
							else
							{
								System.err.println("Static method? " + finfo.getName() + " " + finfo.getFlags());
							}
							if (function != null)
							{
								functions.add(function);
							}
						}
						catch (UnknownTypeException e)
						{
							e.printStackTrace();
							continue;
						}
					}
					final File classFile = new File(packageFolder, obj.getName() + ".java");
					try (final FileWriter fos = new FileWriter(classFile);)
					{
						// TODO should pick the template based on fallback
						// So we can override a general template for a
						// Particular object etc.
						final Template t = engine.getTemplate("templates/object.vm");
						VelocityContext context = new VelocityContext();
						context.put("packagePrefix", "org.gnome.gobjects");
						context.put("namespace", ns);
						context.put("obj", obj);
						context.put("functions", functions);
						context.put("methods", methods);
						context.put("constructors", ctors);
						
						t.merge(context, fos);

//						fos.write("package gobjects." + ns + ";\n");
//						
//						fos.write("import gobjects." + ns + ".native.*;");
//						fos.write("public class " + obj.getName() + "\n");
//						fos.write("{\n");
//						
//						for (FunctionInfo func : obj.getMethods())
//						{
//							boolean isConstructor = (func.getFlags() & FunctionInfoFlags.IS_CONSTRUCTOR) != 0;
//				            if (isConstructor)
//				                continue;
//							fos.write("\tpublic " + func.getReturnType() + " " + methodName(func) + "(" + ") {\n");
//							fos.write("\t\t" + "\n");
//							fos.write("\t}\n");
//						}
//						fos.write("}\n");
//						
//						fosNative.write("package gobjects.native." + ns + ";\n");
//						fosNative.write("public interface " + obj.getName() + "API extends Library\n");
//						fosNative.write("{\n");
//						fosNative.write("\tpublic static final " + obj.getName() + "API lib = GNative.loadLibrary(" + obj.getNamespace() + ", " + obj.getName() + "API.class);\n");
//						for (FunctionInfo func : obj.getMethods())
//						{
//							final TypeInfo type = func.getReturnType();
//							final String returnString;
//							if (type.getTag() == TypeTag.VOID)
//							{
//								returnString = "void";
//							}
//							else
//							{
//								returnString = type.toString();
//							}
//							fosNative.write("\tpublic " + returnString + " " + func.getSymbol() + "(" + ");\n");
//						}
//						fosNative.write("}\n");
//						
//						System.out.println(toStr(obj));
					}
					
				}
				else if (info instanceof StructInfo)
				{
					final StructInfo sinfo = (StructInfo) info;
					
				}
				else if (info instanceof FunctionInfo)
				{
					final FunctionInfo finfo = (FunctionInfo) info;
					if ((finfo.getFlags() & FunctionInfoFlags.IS_METHOD) != 0
							|| finfo.getFlags() == 0)
					{
						globalFunctions.add(finfo);
					}
					else
					{
						System.err.println("Unknown global flags " + finfo.getNamespace() + ":" + finfo.getName() + " " + finfo.getFlags());
					}
				}
				else
				{
					System.err.println("UNHANDLED " + info.getNamespace() + ":" + info.getName() + " " + info.getClass());
				}
//				System.out.println(info.getName() + " is a " + info.getClass().getName());
			
			}
			
			final ObjectParameters obj = new ObjectParameters();
			obj.setName(ns + "Globals");
			final File classFile = new File(packageFolder, obj.getName() + ".java");
			try (final FileWriter fos = new FileWriter(classFile);)
			{
				// TODO should pick the template based on fallback
				// So we can override a general template for a
				// Particular object etc.
				final Template t = engine.getTemplate("templates/globals.vm");
				VelocityContext context = new VelocityContext();
				context.put("packagePrefix", "org.gnome.gobject");
				context.put("namespace", ns);
				context.put("obj", obj);
				context.put("functions", globalFunctions);
				context.put("methods", Collections.EMPTY_LIST);
				context.put("constructors", Collections.EMPTY_LIST);
				
				t.merge(context, fos);
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
