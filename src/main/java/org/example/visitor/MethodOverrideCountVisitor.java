package org.example.visitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.*;

import static org.objectweb.asm.Opcodes.ASM8;

public class MethodOverrideCountVisitor extends ClassVisitor {

    private final Map<String, Set<MethodSignature>> classMethods = new HashMap<>();
    private final Map<String, String> parentClass = new HashMap<>();
    private final Map<String, Set<String>> classInterfaces = new HashMap<>();
    private final Set<String> baseVisitedClasses = new HashSet<>();

    private final ClassLoader classResolver;

    private String currentClass;

    private boolean isInternalVisit = false;

    public MethodOverrideCountVisitor(ClassLoader classResolver, ClassVisitor visitor) {
        super(ASM8, visitor);
        this.classResolver = classResolver;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        if (!isInternalVisit) {
            baseVisitedClasses.add(name);
        }
        parentClass.put(currentClass, superName);
        classInterfaces.put(name, Set.of(interfaces));
        currentClass = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("<init>") || name.equals("<clinit>")
                || (access & Opcodes.ACC_PRIVATE) != 0
                || (access & Opcodes.ACC_STATIC) != 0) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        var methodSignature = new MethodSignature(name, descriptor);
        classMethods.computeIfAbsent(currentClass, (ignore) -> new HashSet<>()).add(methodSignature);
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        this.currentClass = null;
    }

    public double getAverageOverrideCount() {
        return baseVisitedClasses.stream()
                .mapToLong(this::getOverrideCount)
                .average()
                .orElse(0);
    }

    public int getOverrideCount(String className) {
        var methods = classMethods.get(className);
        if (methods == null) return 0;
        int overrideCount = 0;
        Queue<String> awaitParents = new ArrayDeque<>();
        Set<String> visitedParents = new HashSet<>();
        if (parentClass.get(className) != null) {
            awaitParents.add(parentClass.get(className));
        }
        if (classInterfaces.get(className) != null) {
            awaitParents.addAll(classInterfaces.get(className));
        }
        while (!awaitParents.isEmpty()) {
            var parent = awaitParents.poll();
            if (parent == null) continue;
            if (visitedParents.contains(parent)) {
                continue;
            }
            visitedParents.add(parent);
            if (parentClass.get(parent) != null) {
                awaitParents.add(parentClass.get(parent));
            }
            if (classInterfaces.get(parent) != null) {
                awaitParents.addAll(classInterfaces.get(parent));
            }
            for (var iterator = methods.iterator(); iterator.hasNext();) {
                var method = iterator.next();
                if (hasMethod(parent, method)) {
                    iterator.remove();
                    overrideCount++;
                }
            }
        }
        return overrideCount;
    }

    private boolean hasMethod(String className, MethodSignature signature) {
        if (!classMethods.containsKey(className)) {
            String resourceName = className.replace(".", "/") + ".class";

            InputStream is = classResolver.getResourceAsStream(resourceName);
            try {
                if (is == null) {
                    is = ClassLoader.getSystemResourceAsStream(className + ".class");
                }
                if (is != null) {
                    ClassReader cr = new ClassReader(is);
                    isInternalVisit = true;
                    cr.accept(this, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    isInternalVisit = false;
                    is.close();
                }
            } catch (Throwable ignore) {}
            finally {
                try {
                    if (is != null) is.close();
                } catch (Throwable ignore) {}
            }
        }
        return classMethods.getOrDefault(className, Set.of()).contains(signature);
    }

    private record MethodSignature(String name, String descriptor) {
    }

}
