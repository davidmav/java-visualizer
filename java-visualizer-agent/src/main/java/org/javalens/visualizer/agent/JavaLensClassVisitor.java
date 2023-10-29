package org.javalens.visualizer.agent;

import org.javalens.visualizer.model.MethodCriteriaRecord;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.Map;

class JavaLensClassVisitor extends ClassVisitor {

    private final Map<String, List<MethodCriteriaRecord>> instrumentedMethods;

    protected JavaLensClassVisitor(Map<String, List<MethodCriteriaRecord>> instrumentedMethods, int api,
                                   ClassVisitor classVisitor) {
        super(api, classVisitor);
        this.instrumentedMethods = instrumentedMethods;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (!this.instrumentedMethods.containsKey(name)) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
            // Not instrumented method
        } else {
            return new JavaLensGeneratorAdapter(instrumentedMethods.get(name), access, descriptor,
                    super.visitMethod(access, name,
                            descriptor,
                            signature, exceptions));
        }
    }
}
