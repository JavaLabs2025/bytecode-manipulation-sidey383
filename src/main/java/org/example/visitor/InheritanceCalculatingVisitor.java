package org.example.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class InheritanceCalculatingVisitor extends ClassVisitor {

    private static final List<String> baseTypes = List.of(
            Type.getType(Object.class).getClassName(),
            Type.getType(Enum.class).getClassName(),
            Type.getType(Record.class).getClassName()
    );

    private final HashMap<String, Integer> depthCache = new HashMap<>();
    private final HashMap<String, List<String>> pendingChildren = new HashMap<>();
    private final Set<String> actualClasses = new HashSet<>();

    public InheritanceCalculatingVisitor(ClassVisitor visitor) {
        super(Opcodes.ASM9, visitor);
        baseTypes.forEach(name -> depthCache.put(name, 0));
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        actualClasses.add(name);
        calculateDepth(name, superName);
    }

    public void calculateDepth(String name, String superName) {
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

    public void finalizeResults() {
        actualClasses.forEach(superName -> calculateChildren(superName, 0));
    }

    public Map<String, Integer> allDepths() {
        return actualClasses.stream()
                .collect(Collectors.toMap(
                        k -> k,
                        k -> depthCache.getOrDefault(k, 0)
                ));
    }

    public int getMaxDepth() {
        return actualClasses.stream().map(depthCache::get)
                .filter(Objects::nonNull)
                .mapToInt(i -> i).max()
                .orElse(0);
    }

    public double averageDepth() {
        return actualClasses.stream().map(depthCache::get)
                .filter(Objects::nonNull)
                .mapToInt(i -> i).average()
                .orElse(0);
    }


}
