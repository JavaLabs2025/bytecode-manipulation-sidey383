package org.example;

import org.example.visitor.*;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SuppressWarnings("ExtractMethodRecommender")
public class Example {

    public static void main(String[] args) throws IOException {
        ABCMetricVisitor cv1 = new ABCMetricVisitor(null);
        FieldCountVisitor cv2 = new FieldCountVisitor(cv1);
        InheritanceCalculatingVisitor cv3 = new InheritanceCalculatingVisitor(cv2);
        MethodOverrideCountVisitor cv4 = new MethodOverrideCountVisitor(Example.class.getClassLoader(), cv3);
        try (JarFile sampleJar = new JarFile("src/main/resources/sample.jar")) {
            Enumeration<JarEntry> enumeration = sampleJar.entries();

            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                if (entry.getName().endsWith(".class")) {
                    ClassReader cr = new ClassReader(sampleJar.getInputStream(entry));
                    cr.accept(cv4, 0);
                }
            }
        }
        System.out.println("ABC metric: " + cv1.getABCMetric());
        System.out.println("Average field count: " + cv2.getAverageFieldCount());
        cv3.finalizeResults();
//        cv3.allDepths().forEach((className, depth) -> {
//            System.out.println(className + " depth " + depth);
//        });
        System.out.println("Average depth: " + cv3.averageDepth());
        System.out.println("Max depth: " + cv3.getMaxDepth());
        System.out.println("Average override method count: " + cv4.getAverageOverrideCount());
    }
}
