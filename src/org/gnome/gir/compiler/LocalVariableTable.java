/**
 * 
 */
package org.gnome.gir.compiler;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gnome.gir.compiler.CodeFactory.CallableCompilationContext;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

final class LocalVariableTable {
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
	
	void writeLocals(MethodVisitor mv, Label start, Label end) {
		for (LocalVariable var : getAll()) {
			mv.visitLocalVariable(var.name, var.type.getDescriptor(), null, start, end, var.offset);
		}			
	}
	
	@Override
	public String toString() {
		return String.format("<locals lastOffset=%s table=%s>", lastOffset, locals);
	}
}