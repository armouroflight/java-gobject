/**
 * 
 */
package org.gnome.gir.compiler;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.V1_6;

import java.util.HashMap;
import java.util.Map;

public final class GlobalsCompilation extends StubClassCompilation {
	Map<String,String> interfaceTypes = new HashMap<String,String>();
	public InnerClassCompilation constants;
	public GlobalsCompilation(String namespace, String version, String name) {
		super(namespace, version, name);
	}
	
	public InnerClassCompilation getConstants() {
		if (constants == null) {
			String constantsName = "Constants";
			constants = newInner(constantsName);
			writer.visitInnerClass(constants.internalName, internalName, constantsName, 
					ACC_PUBLIC + ACC_FINAL + ACC_STATIC);				
			constants.writer.visit(V1_6, ACC_PUBLIC + ACC_FINAL,
					constants.internalName, 
					null, "java/lang/Object", null);
			constants.writer.visitInnerClass(constants.internalName, 
					internalName, constantsName, ACC_PUBLIC + ACC_FINAL + ACC_STATIC);			
		}
		return constants;
	}
}