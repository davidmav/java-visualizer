package org.javalens.visualizer.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class EventInstrumentationAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) throws IOException {
        EventConfigurationRepository repository = new EventConfigurationRepository();
        EventRecorder.initialize(repository);
        instrumentation.addTransformer(new JavaLensClassTransformer(repository));
    }


}
