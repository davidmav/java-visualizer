package org.javalens.visualizer.agent;

import org.javalens.visualizer.model.MethodCriteria;
import org.javalens.visualizer.model.MethodCriteriaRecord;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

class JavaLensGeneratorAdapter extends GeneratorAdapter {

    private MethodCriteriaRecord startMethodCriteria;
    private MethodCriteriaRecord endMethodCriteria;

    public JavaLensGeneratorAdapter(List<MethodCriteriaRecord> methodCriteriaRecords,
                                    int access,
                                    String descriptor,
                                    MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor, access, descriptor, descriptor);
        for (MethodCriteriaRecord methodCriteriaRecord : methodCriteriaRecords) {
            if (methodCriteriaRecord.methodCriteria().getEventType() == MethodCriteria.EventTypeEnum.START) {
                startMethodCriteria = methodCriteriaRecord;
            } else if (methodCriteriaRecord.methodCriteria().getEventType() == MethodCriteria.EventTypeEnum.END) {
                endMethodCriteria = methodCriteriaRecord;
            }
        }
    }

    /**
     * Injects code on method start
     */
    @Override
    public void visitCode() {
        super.visitCode();
        if (startMethodCriteria != null) {
            injectEventCapture(startMethodCriteria);
        }
    }

    /**
     * Injects code on method end
     */
    @Override
    public void visitInsn(int opcode) {
        if (endMethodCriteria != null) {
            if (opcode <= Opcodes.RETURN && opcode >= Opcodes.IRETURN) {
                injectEventCapture(endMethodCriteria);
            }
        }
        super.visitInsn(opcode);
    }

    /**
     * Actual injection
     *
     * @param methodCriteriaRecord :   The method criteria
     */
    private void injectEventCapture(MethodCriteriaRecord methodCriteriaRecord) {
        // Add logic at the method start
        visitLdcInsn(methodCriteriaRecord.eventName());
        visitFieldInsn(Opcodes.GETSTATIC, "org/javalens/visualizer/agent/EventRecorder$EventType",
                methodCriteriaRecord.type().name(),
                "Lorg/javalens/visualizer/agent/EventRecorder$EventType;");
        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
        visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        visitMethodInsn(Opcodes.INVOKESTATIC, "org/javalens/visualizer/agent/EventRecorder", "recordEvent",
                "(Ljava/lang/String;Lorg/javalens/visualizer/agent/EventRecorder$EventType;Ljava/lang/String;J)V", false);
    }
}