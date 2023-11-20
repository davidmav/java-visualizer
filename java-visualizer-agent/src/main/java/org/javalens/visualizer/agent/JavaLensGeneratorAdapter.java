package org.javalens.visualizer.agent;

import org.javalens.visualizer.model.MethodCriteria;
import org.javalens.visualizer.model.MethodCriteriaRecord;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

import static org.javalens.visualizer.agent.AdapterConstants.*;

class JavaLensGeneratorAdapter extends GeneratorAdapter {


    public static final String START_PARAM = "(";
    public static final String END_PARAM = ")";
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

        /*
         * Record Event method signature
         *
         *     recordEvent(String eventName,
         *                 EventType eventType,
         *                 String thread,
         *                 String threadMatchPattern,
         *                 Object targetObject,
         *                 Object[] methodArguments,
         *                 long epochMillis)
         *
         */

        // Inserting event name onto the stack
        visitLdcInsn(methodCriteriaRecord.eventName());

        // Inserting the event type value onto the stack
        visitFieldInsn(Opcodes.GETSTATIC, EVENT_TYPE_CLASS,
                methodCriteriaRecord.type().name(),
                TYPE_EVENT.getDescriptor());

        // Getting the current thread
        visitMethodInsn(Opcodes.INVOKESTATIC,
                JAVA_LANG_THREAD_CLASS,
                CURRENT_THREAD_METHOD_NAME,
                getMethodInvocationDescriptor(TYPE_THREAD),
                false);

        // Inserting the thread name into the stack
        visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                JAVA_LANG_THREAD_CLASS,
                GET_NAME_METHOD_NAME,
                getMethodInvocationDescriptor(TYPE_STRING),
                false);

        MethodCriteria methodCriteria = methodCriteriaRecord.methodCriteria();

        // Inserting the thread match filter criteria into the stack
        String threadMatchPattern = methodCriteria.getThreadMatchPattern();
        if (threadMatchPattern != null) {
            visitLdcInsn(threadMatchPattern);
        } else {
            visitInsn(Opcodes.ACONST_NULL);
        }

        // Inserting the target object onto the stack
        visitVarInsn(Opcodes.ALOAD, 0);

        // Inserting all method arguments as an array onto the stack
        loadArgArray();

        // Insert the current time millis into the stack
        visitMethodInsn(Opcodes.INVOKESTATIC,
                JAVA_LANG_SYSTEM_CLASS,
                CURRENT_TIME_MILLIS_METHOD_NAME,
                getMethodInvocationDescriptor(Type.LONG_TYPE),
                false);

        // Invoke the record event method
        visitMethodInsn(Opcodes.INVOKESTATIC, EVENT_RECORDER_CLASS, RECORD_EVENT_METHOD_NAME,
                getMethodInvocationDescriptor(Type.VOID_TYPE, TYPE_STRING, TYPE_EVENT, TYPE_STRING, TYPE_STRING,
                        TYPE_OBJECT, TYPE_OBJECT_ARRAY, Type.LONG_TYPE),
                false);

    }

    private static String getMethodInvocationDescriptor(Type returnType, Type... inputTypes) {
        StringBuilder descriptorBuilder = new StringBuilder();
        descriptorBuilder.append(START_PARAM);
        for (Type type : inputTypes) {
            descriptorBuilder.append(type.getSort() == Type.ARRAY ? type.getDescriptor() + DESCRIPTOR_DELIMITER :
                    type.getDescriptor());
        }
        descriptorBuilder.append(END_PARAM);
        descriptorBuilder.append(returnType.getDescriptor());
        return descriptorBuilder.toString();
    }

}