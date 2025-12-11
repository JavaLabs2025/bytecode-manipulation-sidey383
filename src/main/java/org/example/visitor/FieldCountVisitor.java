package org.example.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import java.util.HashMap;

import static org.objectweb.asm.Opcodes.ASM8;

public class FieldCountVisitor extends ClassVisitor {

    private final HashMap<String, Long> fieldCount = new HashMap<>();

    private String currentClass;

    private long currentFieldCount;

    public FieldCountVisitor(ClassVisitor visitor) {
        super(ASM8, visitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        currentClass = name;
        currentFieldCount = 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        currentFieldCount++;
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        fieldCount.put(currentClass, currentFieldCount);
        currentClass = null;
        currentFieldCount = 0;
    }

    public double getAverageFieldCount() {
        return fieldCount.values().stream()
                .mapToLong(v -> v)
                .average()
                .orElse(0);
    }

}
