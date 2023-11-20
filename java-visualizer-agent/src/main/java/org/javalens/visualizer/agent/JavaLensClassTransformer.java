package org.javalens.visualizer.agent;

import org.javalens.visualizer.model.MethodCriteriaRecord;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;

public class JavaLensClassTransformer implements ClassFileTransformer {


    private final EventConfigurationRepository methodCriteriaRepository;

    public JavaLensClassTransformer(EventConfigurationRepository methodCriteriaRepository) {
        this.methodCriteriaRepository = methodCriteriaRepository;
    }

    @Override
    public byte[] transform(
            ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer
    ) {
        String normalizedClass = className.replaceAll("/", ".");
        if (!this.methodCriteriaRepository.containsClass(normalizedClass)) {
            // Not instrumented class
            return classfileBuffer;
        }
        try {
            Map<String, List<MethodCriteriaRecord>> instrumentedMethods =
                    this.methodCriteriaRepository.getClassInstrumentedMethods(normalizedClass);

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
