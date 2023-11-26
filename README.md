# Java Visualizer Agent
## Overview
The Java Visualizer Agent is a tool designed for visualizing and analyzing event data in Java applications. It allows users to capture event data, including start and end times, and export this information into insightful reports. This tool is particularly useful for understanding the performance characteristics and behavior of Java applications.

## Configuration
Before running the agent, you must provide a configuration file in JSON format. Here's a sample configuration:

```json
{
  "sink" : {
    "objectType" : "FileSystemSinkConfig",
    "chunkSize" : 1000,
    "path" : "/path/to/where/to/save/events"
  },
  "events" : [ {
    "eventName" : "ConsumeData",
    "eventStart" : {
      "objectType" : "MethodCriteria",
      "methodClass" : "org.javalens.dummyapp.consumer.DataConsumer",
      "methodName" : "consumeData",
      "eventType" : "START",
      "arguments" : [ ],
      "traceArguments" : [ {
        "argumentPath" : "[0].requestId",
        "argumentType" : "METHOD"
      } ],
      "eventArguments" : [ {
        "argumentPath" : "[0].requestId",
        "argumentType" : "METHOD"
      }, {
        "argumentPath" : "[0].sequence",
        "argumentType" : "METHOD"
      } ]
    },
    "eventEnd" : {
      "objectType" : "MethodCriteria",
      "methodClass" : "org.javalens.dummyapp.consumer.DataConsumer",
      "methodName" : "consumeData",
      "eventType" : "END",
      "arguments" : [ ],
      "traceArguments" : [ {
        "argumentPath" : "[0].requestId",
        "argumentType" : "METHOD"
      } ],
      "eventArguments" : [ {
        "argumentPath" : "[0].requestId",
        "argumentType" : "METHOD"
      }, {
        "argumentPath" : "[0].sequence",
        "argumentType" : "METHOD"
      } ]
    }
  }, {
    "eventName" : "ProduceData",
    "eventStart" : {
      "objectType" : "MethodCriteria",
      "methodClass" : "org.javalens.dummyapp.producer.RandomDataProducer",
      "methodName" : "produceData",
      "eventType" : "START",
      "arguments" : [ ],
      "traceArguments" : [ {
        "argumentPath" : "[0]",
        "argumentType" : "METHOD"
      } ],
      "eventArguments" : [ {
        "argumentPath" : "[0]",
        "argumentType" : "METHOD"
      }, {
        "argumentPath" : "[1]",
        "argumentType" : "METHOD"
      } ]
    },
    "eventEnd" : {
      "objectType" : "MethodCriteria",
      "methodClass" : "org.javalens.dummyapp.producer.RandomDataProducer",
      "methodName" : "produceData",
      "eventType" : "END",
      "arguments" : [ ],
      "traceArguments" : [ {
        "argumentPath" : "[0]",
        "argumentType" : "METHOD"
      } ],
      "eventArguments" : [ {
        "argumentPath" : "[0]",
        "argumentType" : "METHOD"
      }, {
        "argumentPath" : "[1]",
        "argumentType" : "METHOD"
      } ]
    }
  } ]
}
```
This configuration file includes details about the sink for storing event data and the events to be captured.

## Getting Started
Clone the Repository and build the library:
```bash
git clone https://github.com/davidmv/java-visualizer-agent.git
```
Navigate to the Project Directory:
```bash
cd java-visualizer-agent
```
Build / Install the library with Maven
```bash
mvn install
```

Use the uber jar that was created ```java-visualizer-agent-uber.jar``` in the java-visualizer-agent/target directory.


Run the Agent:

Use the following Java option in your JVM application:
```bash
-javaagent:/path/to/java-visualizer-agent-uber.jar -Djavalens.configurationFile=/path/to/json/configuration
```

## Exporting Reports
To export a report from captured event data, use the following command:

```bash
Copy code
java -jar java-visualizer-agent-1.0-SNAPSHOT-jar-with-dependencies.jar
Options
-d, --destination <arg>: The path to store the output report (local file system only).
-l, --limit <arg>: Limit the number of exported traces (default is 1000).
-p, --percentile <arg>: Filter events by overall latency percentile.
-s, --source <arg>: The path of the events payload (file:// or s3://).
```

After collecting enough event data, export the report using the command-line options above.

Sample Report
A sample report can be viewed here: [a relative link](sample_report.html)

Contribution
Contributions to the project are welcome. Please follow the standard GitHub pull request process to submit your changes.

License
This project is licensed under the MIT License.