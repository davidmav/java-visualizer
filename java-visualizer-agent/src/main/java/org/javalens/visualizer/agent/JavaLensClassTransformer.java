package org.javalens.visualizer.agent;

import org.javalens.visualizer.model.MethodCriteria;
import org.javalens.visualizer.model.MethodCriteriaRecord;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaLensClassTransformer implements ClassFileTransformer {


    private final Map<String, Map<String, MethodCriteriaRecord>> methodCriteriaMap;

    public JavaLensClassTransformer(List<MethodCriteriaRecord> methodCriteriaRecords) {
        this.methodCriteriaMap = new HashMap<>();
        for (MethodCriteriaRecord methodCriteriaRecord : methodCriteriaRecords) {
            MethodCriteria methodCriteria = methodCriteriaRecord.methodCriteria();
            Map<String, MethodCriteriaRecord> currentClassMethodCriteria =
                    this.methodCriteriaMap.computeIfAbsent(methodCriteria.getMethodClass(),
                            k -> new HashMap<>());
            String methodName = methodCriteria.getMethodName();
            currentClassMethodCriteria.put(methodName, methodCriteriaRecord);
        }
    }

    @Override
    public byte[] transform(
            ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer
    ) throws IllegalClassFormatException {
        if (!this.methodCriteriaMap.containsKey(className)) {
            // Not instrumented class
            return classfileBuffer;
        }
        try {
            Map<String, MethodCriteriaRecord> instrumentedMethods = this.methodCriteriaMap.get(className);
            // Use ASM to read and write class bytecode
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            // Create a ClassVisitor to visit the class structure
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, cw) {

                // Override visitMethod to instrument selected methods
                @Override
                public MethodVisitor visitMethod(
                        int access, String name, String descriptor, String signature, String[] exceptions
                ) {
                    if (!instrumentedMethods.containsKey(name)) {
                        return super.visitMethod(access, name, descriptor, signature, exceptions);
                        // Not instrumented method
                    } else {
                        LocalVariablesSorter mv = new LocalVariablesSorter(access, descriptor, super.visitMethod(access, name, descriptor, signature, exceptions));
                        MethodCriteriaRecord methodCriteriaRecord = instrumentedMethods.get(name);
                        MethodCriteria methodCriteria = methodCriteriaRecord.methodCriteria();
                        MethodCriteria.EventTypeEnum eventType = methodCriteria.getEventType();

                        mv.visitCode();

                        if (eventType == MethodCriteria.EventTypeEnum.START) {
                            injectEventCapture(mv, MethodCriteria.EventTypeEnum.START);
                        }

                        // Call the original method
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, name, descriptor, false);

                        // Add logic at the method end
                        if (eventType == MethodCriteria.EventTypeEnum.END) {
                            injectEventCapture(mv, MethodCriteria.EventTypeEnum.END);
                        }

                        mv.visitEnd();

                        return mv;
                    }
                }
            };

            cr.accept(cv, 0);

            // Return the modified bytecode
            return cw.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return classfileBuffer; // Return the original bytecode if an error occurs
        }
    }

    private static void injectEventCapture(LocalVariablesSorter mv, MethodCriteria.EventTypeEnum type) {
        // Create a local variable to capture the thread name
        int threadNameVar = mv.newLocal(Type.getType(String.class));
        // Add logic to capture the thread name
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, threadNameVar);

        // Add logic at the method start
        mv.visitLdcInsn("EventName"); // Replace with your event name
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/javalens/visualizer/agent/EventRecorder$EventType", type.name(), "Lorg" +
                "/javalens/visualizer/agent/EventRecorder$EventType;");
        mv.visitVarInsn(Opcodes.ALOAD, threadNameVar);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/javalens/visualizer/agent/EventRecorder", "recordEvent", "(Ljava/lang/String;Lorg/javalens/visualizer/agent/EventRecorder$EventType;Ljava/lang/String;J)V", false);
    }
}
