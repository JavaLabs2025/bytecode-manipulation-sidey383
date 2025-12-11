package org.example.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Set;

public class ABCMetricVisitor extends ClassVisitor {

    // Assignments
    private long totalA = 0;
    // Branches
    private long totalB = 0;
    // Conditions
    private long totalC = 0;

    public ABCMetricVisitor(ClassVisitor visitor) {
        super(Opcodes.ASM8, visitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new ABCMetricMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    public double getABCMetric() {
        return Math.sqrt(totalA * totalA + totalB * totalB + totalC * totalC);
    }

    private class ABCMetricMethodVisitor extends MethodVisitor {

        private static final Set<Integer> assignments = Set.of(
                Opcodes.IASTORE,
                Opcodes.LASTORE,
                Opcodes.FASTORE,
                Opcodes.DASTORE,
                Opcodes.AASTORE,
                Opcodes.BASTORE,
                Opcodes.CASTORE,
                Opcodes.SASTORE
        );

        private static final Set<Integer> localAssignments = Set.of(
                Opcodes.ISTORE,
                Opcodes.LSTORE,
                Opcodes.FSTORE,
                Opcodes.DSTORE,
                Opcodes.ASTORE
        );

        private int methodA = 0;
        private int methodB = 0;
        private int methodC = 0;

        protected ABCMetricMethodVisitor(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
        }

        public int getMethodA() {
            return methodA;
        }

        public int getMethodB() {
            return methodB;
        }

        public int getMethodC() {
            return methodC;
        }

        @Override
        public void visitInsn(int opcode) {
            super.visitInsn(opcode);
            if (assignments.contains(opcode)) {
                methodA++;
            }
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            super.visitVarInsn(opcode, varIndex);
            if (localAssignments.contains(opcode)) {
                methodA++;
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            super.visitJumpInsn(opcode, label);
            methodB++;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            methodB++;
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            super.visitLookupSwitchInsn(dflt, keys, labels);
            methodC += keys.length;
            methodB += labels.length;
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            super.visitTableSwitchInsn(min, max, dflt, labels);
            methodB += labels.length;
            methodC += (max - min + 1);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            totalA += methodA;
            totalB += methodB;
            totalC += methodC;
        }
    }
}
