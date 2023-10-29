package org.javalens.visualizer.agent;

import org.javalens.visualizer.model.MethodCriteria;
import org.javalens.visualizer.model.MethodCriteriaRecord;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaLensClassTransformer implements ClassFileTransformer {

    private final Map<String, Map<String, List<MethodCriteriaRecord>>> methodCriteriaMap;

    public JavaLensClassTransformer(List<MethodCriteriaRecord> methodCriteriaRecords) {
        this.methodCriteriaMap = new HashMap<>();
        for (MethodCriteriaRecord methodCriteriaRecord : methodCriteriaRecords) {
            indexInstrumentedMethods(methodCriteriaRecord);
        }
    }

    private void indexInstrumentedMethods(MethodCriteriaRecord methodCriteriaRecord) {
        MethodCriteria methodCriteria = methodCriteriaRecord.methodCriteria();
        Map<String, List<MethodCriteriaRecord>> currentClassMethodCriteria =
                this.methodCriteriaMap.computeIfAbsent(methodCriteria.getMethodClass(),
                        k -> new HashMap<>());
        String methodName = methodCriteria.getMethodName();
        List<MethodCriteriaRecord> methodList = currentClassMethodCriteria.computeIfAbsent(methodName, k -> new ArrayList<>());
        methodList.add(methodCriteriaRecord);
    }

    @Override
    public byte[] transform(
            ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer
    ) {
        String normalizedClass = className.replaceAll("/", ".");
        if (!this.methodCriteriaMap.containsKey(normalizedClass)) {
            // Not instrumented class
            return classfileBuffer;
        }
        try {
            Map<String, List<MethodCriteriaRecord>> instrumentedMethods = this.methodCriteriaMap.get(normalizedClass);

            // Use ASM to read and write class bytecode
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            // Create a ClassVisitor to visit the class structure
            JavaLensClassVisitor cv = new JavaLensClassVisitor(instrumentedMethods, Opcodes.ASM9, cw);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);

            return cw.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return classfileBuffer; // Return the original bytecode if an error occurs
        }
    }


}
