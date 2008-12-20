/**
 * 
 */
package org.gnome.gir.compiler;

import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import gobject.runtime.GType;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

abstract class ClassCompilation {
	String namespace;
	String nsversion;
	String baseName;
	String internalName;
	ClassVisitor writer;
	MethodVisitor clinit;
	private ClassWriter realWriter;
	boolean closed = false;		
	public ClassCompilation(String namespace, String version, String baseName) {
		this.namespace = namespace;
		this.nsversion = version;
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
	
	MethodVisitor getClinit() {
		if (clinit == null) {
			clinit = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
			clinit.visitCode();
		}
		return clinit;
	}
	
	public void close() {
		if (closed)
			return;				
		if (clinit != null) {
			clinit.visitInsn(RETURN);	
			clinit.visitMaxs(0, 0);
			clinit.visitEnd();
		}
		closed = true;
	}
}