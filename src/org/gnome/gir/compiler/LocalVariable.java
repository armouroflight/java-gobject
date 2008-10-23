/**
 * 
 */
package org.gnome.gir.compiler;

import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

final class LocalVariable {
	String name;
	int offset;
	Type type;
	public LocalVariable(String name, int offset, Type type) {
		super();
		this.name = name;
		this.offset = offset;
		this.type = type;
	}
	
	public Type writeLoadArgument(MethodVisitor mv) {
		return writeLoadArgument(mv, offset, type);
	}
	
	public static Type writeLoadArgument(MethodVisitor mv, int loadOffset, Type argType) {
		Class<?> box = TypeMap.getPrimitiveBox(argType);
		mv.visitVarInsn(argType.getOpcode(ILOAD), loadOffset);		
		if (box != null) {
			Type boxedType = Type.getType(box);	
			mv.visitMethodInsn(INVOKESTATIC, boxedType.getInternalName(), 
					"valueOf", "(" + argType.getDescriptor() + ")" + boxedType.getDescriptor());
			return boxedType;
		}
		return argType;
	}	
}