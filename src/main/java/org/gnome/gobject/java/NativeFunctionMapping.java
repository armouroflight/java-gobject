package org.gnome.gobject.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.gnome.gir.repository.ArgInfo;
import org.gnome.gir.repository.FunctionInfo;
import org.gnome.gir.repository.FunctionInfoFlags;
import org.gnome.gir.repository.TypeInfo;

/**
 * Class that provides Java mappings for functions
 * 
 * @author armouroflight
 */
public class NativeFunctionMapping {
	/** GIR function information */
	private final FunctionInfo functionInfo;
	private final JavaType returnType;
	private final List<JavaArg> args;
	private final String parent;

	/**
	 * Constructor for a member method
	 * The native method has the object as the first parameter
	 * @param functionInfo Function
	 * @param parent Parent type
	 * @throws UnknownTypeException
	 */
	public NativeFunctionMapping(FunctionInfo functionInfo, final String parent) throws UnknownTypeException
	{
		this.functionInfo = functionInfo;
		
		if ((functionInfo.getFlags() & FunctionInfoFlags.IS_CONSTRUCTOR) != 0)
		{
			// TODO assert that type is compatible
			returnType = new JavaType("com.sun.jna.Pointer", false);
		}
		else
		{
			returnType = new JavaType(functionInfo.getReturnType());
		}
		args = createArgsList(functionInfo.getArgs());
		this.parent = parent;
	}
	/**
	 * Constructor for non-member methods
	 * The native method will not have object as first parameter.
	 * 
	 * In Java this would be a constructor or static method generally.
	 * @param functionInfo
	 * @throws UnknownTypeException
	 */
	public NativeFunctionMapping(FunctionInfo functionInfo) throws UnknownTypeException
	{
		this(functionInfo, null);
	}
	
	
	private static List<JavaArg> createArgsList(ArgInfo[] argInfos) throws UnknownTypeException {
		final List<JavaArg> arguments = new ArrayList<>(argInfos.length);
		for (ArgInfo argInfo : argInfos)
		{
			arguments.add(new JavaArg(argInfo));
		}
		return Collections.unmodifiableList(arguments);
	}

	
	/**
	 * Native symbol name
	 * @return Name of the native symbol
	 */
	public String getSymbol() {
		return functionInfo.getSymbol();
	}
	public String getName() {
		return functionInfo.getName();
	}
	
	public String getReturnType()
	{
		return returnType.getTypeDecl();
	}
	
	public boolean isVoidReturn()
	{
		return returnType.isVoid();
	}
	
	public String getVoidReturn()
	{
		return Boolean.toString(returnType.isVoid());
	}
	
	public List<JavaArg> getArgs() {
		return args;
	}
	
	public List<JavaArg> getNativeArgs() {
		final List<JavaArg> nativeArgs = new ArrayList<>(args.size() + 1);
		nativeArgs.add(new JavaArg(parent, "self"));
		nativeArgs.addAll(args);
		return Collections.unmodifiableList(nativeArgs);
		
	}
}
