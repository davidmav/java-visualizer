package org.javalens.visualizer.agent;

import org.objectweb.asm.Type;

public class AdapterConstants {

    private AdapterConstants() {}

    // CLASS NAMES

    static final String EVENT_TYPE_CLASS = "org/javalens/visualizer/model/MethodCriteriaRecord$Type";
    static final String EVENT_RECORDER_CLASS = "org/javalens/visualizer/agent/EventRecorder";
    static final String JAVA_LANG_THREAD_CLASS = "java/lang/Thread";
    static final String JAVA_LANG_SYSTEM_CLASS = "java/lang/System";

    // METHOD NAMES
    static final String RECORD_EVENT_METHOD_NAME = "recordEvent";
    static final String CURRENT_THREAD_METHOD_NAME = "currentThread";
    static final String GET_NAME_METHOD_NAME = "getName";
    static final String CURRENT_TIME_MILLIS_METHOD_NAME = "currentTimeMillis";

    // TYPES
    static final Type TYPE_OBJECT = Type.getObjectType("java/lang/Object");
    static final Type TYPE_OBJECT_ARRAY = Type.getObjectType("[Ljava/lang/Object");
    static final Type TYPE_STRING = Type.getObjectType("java/lang/String");
    static final Type TYPE_EVENT = Type.getObjectType(EVENT_TYPE_CLASS);
    static final Type TYPE_THREAD = Type.getObjectType(JAVA_LANG_THREAD_CLASS);

    // MISC CONSTANTS
    static final String DESCRIPTOR_DELIMITER = ";";

}
