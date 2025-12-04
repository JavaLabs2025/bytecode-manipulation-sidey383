package org.example.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.IntStream;

public class InheritanceCalculatingVisitor extends ClassVisitor {

    private static final List<String> baseTypes = List.of(
            Type.getType(Object.class).getClassName(),
            Type.getType(Enum.class).getClassName(),
            Type.getType(Record.class).getClassName()
    );

    private final HashMap<String, Integer> depthCache = new HashMap<>();
    private final HashMap<String, List<String>> pendingChildren = new HashMap<>();

    public InheritanceCalculatingVisitor(ClassVisitor visitor) {
        super(Opcodes.ASM9, visitor);
        baseTypes.forEach(name -> depthCache.put(name, 0));
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        calculateDepth(name, superName);
    }

    public void calculateDepth(String name, String superName) {
        if (depthCache.containsKey(name)) {
        }

        if (depthCache.containsKey(superName)) {
            int depth = depthCache.get(superName) + 1;
            depthCache.put(name, depth);
            calculateChildren(name, depth);
        }

        pendingChildren.computeIfAbsent(superName, ignore -> new ArrayList<>()).add(name);
    }

    public void calculateChildren(String superName, int depth) {
        var children = pendingChildren.remove(superName);
        if (children == null) return;
        depth += 1;
        for (var child : children) {
            depthCache.put(child, depth);
            calculateChildren(child, depth);
        }
    }

    public int getMaxDepth() {
        return depthCache.values().stream()
                .mapToInt(i -> i).max()
                .orElseGet(() -> pendingChildren.size() > 0 ? 1 : 0);
    }

    public double averageDepth() {
        return IntStream.concat(
                        pendingChildren.values().stream()
                                .flatMap(Collection::stream)
                                .mapToInt(ignore -> 1),
                        depthCache.values().stream()
                                .mapToInt(i -> i)
                ).average()
                .orElse(0);
    }


}
