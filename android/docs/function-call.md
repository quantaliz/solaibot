# AI Edge Function Calling guide for Android

The AI Edge Function Calling SDK (FC SDK) is a library that enables developers to use function calling with on-device LLMs. Function calling lets you connect models to external tools and APIs, enabling models to call specific functions with the necessary parameters to execute real-world actions.

Rather than just generating text, an LLM using the FC SDK can generate a structured call to a function that executes an action, such as searching for up-to-date information, setting alarms, or making reservations.

The AI Edge FC SDK is available for Android and can be run completely on-device with the LLM Inference API. Start using the SDK by following the Android guide, which walks you through a basic implementation of a sample application using function calling.

## Function calling pipeline

Setting up an on-device LLM with function calling capabilities requires the following key steps:

Define function declarations: The structure and parameters of the functions that the LLM can call must be defined in your application code. This includes specifying function names, parameters, and types.
Format prompts and outputs: Input and output text can contain natural language and function calls. A formatter controls how data structures are converted to and from strings, enabling the LLM to appropriately format information.
Parse outputs: A parser detects if the generated response contains a function call and parses it into a structured data type so that the application can execute the function call.
Examine responses: If the parser detects a function call, the application calls the function with the appropriate parameters and structured data type. Otherwise, it returns natural language text.

## Key components

The FC SDK contains to following key components:

Inference Backend: An interface for running inference on a generative AI model. The FC SDK uses the LLM Inference API to execute inference on LiteRT (TFLite) models. The API uses the InferenceBackend interface.
Prompt Formatter: An interface for formatting requests and responses to and from the Generative AI model. The FC SDK provides a formatter that converts function declarations into the model-specific format required by the LLM and inserts them into the system prompt. The formatter also handles model-specific tokens to indicate user and model turns. The API uses the ModelFormatter interface.
Output Parser: The FC SDK provides a parser that detects if the model's output represents a function call and parses it into a data structure for use by the application. The API uses the ModelFormatter interface.
Constrained Decoding: An interface for creating and managing constraints to ensure that the generated output adheres to specific rules or conditions. For supported models, the FC SDK will configure the inference backend to use constrained decoding, which ensures that the model only outputs valid function names and parameters. The API uses the ConstraintProvider interface


## Warnings and Important Notes

**Attention:** The AI Edge Function Calling SDK is under active development.

**Note:** Use of the AI Edge Function Calling SDK is subject to the Generative AI Prohibited Use Policy.

## Overview

The AI Edge Function Calling SDK (FC SDK) is a library that enables developers to use function calling with on-device LLMs. Function calling lets you connect models to external tools and APIs, enabling models to call specific functions with the necessary parameters to execute real-world actions, such as searching for up-to-date information, setting alarms, or making reservations.

## Device Requirements

The LLM Inference API is optimized for high-end Android devices, such as Pixel 8 and Samsung S23 or later, and does not reliably support device emulators.

## Quickstart

The Quickstart section provides a step-by-step guide to implementing function calling in an Android application using the AI Edge Function Calling SDK. It covers downloading a model, declaring function definitions, creating an inference backend, instantiating the model, starting a chat session, and parsing model responses.

### Add Dependencies

Add the following dependencies to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.google.mediapipe:tasks-genai:0.10.24'
    implementation 'com.google.ai.edge.localagents:localagents-fc:0.1.0'
}
```

Add the following to your `AndroidManifest.xml` for devices with Android 12 or higher:

```xml
<uses-native-library android:name="libOpenCL.so" android:required="false"/>
<uses-native-library android:name="libOpenCL-car.so" android:required="false"/>
<uses-native-library android:name="libOpenCL-pixel.so" android:required="false"/>
```

### Download a Model

Download the Hammer 1B model in 8-bit quantized format from Hugging Face:
- [Hugging Face Hammer 2.1 Model](https://huggingface.co/litert-community/Hammer2.1-1.5b)

Push the model to your device using adb:

```bash
$ adb shell rm -r /data/local/tmp/llm/
$ adb shell mkdir -p /data/local/tmp/llm/
$ adb push hammer2.1_1.5b_q8_ekv4096.task /data/local/tmp/llm/hammer2.1_1.5b_q8_ekv4096.task
```

### Declare Function Definitions

Create a class with your tool functions:

```java
class ToolsForLlm {
    public static String getWeather(String location) {
        return "Cloudy, 56°F";
    }

    public static String getTime(String timezone) {
        return "7:00 PM " + timezone;
    }

    private ToolsForLlm() {}
}
```

Define your function declarations with complete schemas:

```java
var getWeather = FunctionDeclaration.newBuilder()
        .setName("getWeather")
        .setDescription("Returns the weather conditions at a location.")
        .setParameters(
            Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties(
                    "location",
                    Schema.newBuilder()
                        .setType(Type.STRING)
                        .setDescription("The location for the weather report.")
                        .build())
                .setRequired(List.of("location"))
                .build())
        .build();

var getTime = FunctionDeclaration.newBuilder()
        .setName("getTime")
        .setDescription("Returns the current time in the given timezone.")
        .setParameters(
            Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties(
                    "timezone",
                    Schema.newBuilder()
                        .setType(Type.STRING)
                        .setDescription("The timezone to get the time from.")
                        .build())
                .setRequired(List.of("timezone"))
                .build())
        .build();
```

Create a tool with your function declarations:

```java
var tool = Tool.newBuilder()
    .addFunctionDeclarations(getWeather)
    .addFunctionDeclarations(getTime)
    .build();
```

### Create the Inference Backend

Initialize the LLM inference backend with the appropriate formatter for your model:

```java
var llmInferenceOptions = LlmInferenceOptions.builder()
    .setModelPath(modelFile.getAbsolutePath())
    .build();
var llmInference = LlmInference.createFromOptions(context, llmInferenceOptions);
var llmInferenceBackend = new LlmInferenceBackend(llmInference, new GemmaFormatter());
```

### Instantiate the Model

Create a system instruction and instantiate the generative model:

```java
var systemInstruction = Content.newBuilder()
    .setRole("system")
    .addParts(Part.newBuilder().setText("You are a helpful assistant."))
    .build();

var generativeModel = new GenerativeModel(
    llmInferenceBackend,
    systemInstruction,
    List.of(tool)
);
```

### Start a Chat Session

Start a chat session and send a message:

```java
var chat = generativeModel.startChat();
var response = chat.sendMessage("How's the weather in San Francisco?");
```

### Parse the Model Response

Handle function calls in the model response:

```java
if (message.hasFunctionCall()) {
  var functionCall = message.getFunctionCall();
  var args = functionCall.getArgs().getFieldsMap();
  var result = null;

  switch (functionCall.getName()) {
    case "getWeather":
      result = ToolsForLlm.getWeather(args.get("location").getStringValue());
      break;
    case "getTime":
      result = ToolsForLlm.getTime(args.get("timezone").getStringValue());
      break;
    default:
      throw new Exception("Function does not exist:" + functionCall.getName());
  }

  var functionResponse = FunctionResponse.newBuilder()
      .setName(functionCall.getName())
      .setResponse(
          Struct.newBuilder()
              .putFields("result", Value.newBuilder().setStringValue(result).build()))
      .build();

  var functionResponseContent = Content.newBuilder()
        .setRole("user")
        .addParts(Part.newBuilder().setFunctionResponse(functionResponse))
        .build();

  var response = chat.sendMessage(functionResponseContent);
}
```

## How it Works

This section explains the core concepts of the Function Calling SDK, focusing on how models interact with function calls, the role of formatters and parsers, and the technical mechanisms behind enabling AI to call specific functions.

## Models

The SDK supports built-in formatters for the following models:

- **Gemma** - use `GemmaFormatter`
- **Llama** - use `LlamaFormatter`
- **Hammer** - use `HammerFormatter`

For unsupported models, developers must create a custom formatter and parser compatible with the LLM Inference API. See the [LLM Inference for Android Guide](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android) for more information.

## Formatting and Parsing

The `ModelFormatter` interface handles two key processes:

- Converting structured function declarations to text
- Formatting function responses
- Inserting conversation turn tokens
- Detecting and parsing function calls

## Constrained Decoding

**Note:** Only Gemma models support constrained decoding in FC SDK.

Constrained decoding is a technique that guides LLM output generation to adhere to predefined structured formats. Developers can define constraints using `ConstraintOptions` to restrict model responses to specific tool calls.

Example constraint configuration:

```java
ConstraintOptions constraintOptions = ConstraintOptions.newBuilder()
      .setToolCallOnly(ConstraintOptions.ToolCallOnly.newBuilder()
      .setConstraintPrefix("```tool_code\n")
      .setConstraintSuffix("\n```"))
      .build();
chatSession.enableConstraint(constraintOptions);
```

## API Reference

### Schema Types

The following Schema types are mentioned:

- `Type.OBJECT` - For object/struct types
- `Type.STRING` - For string types

### Key Classes and Methods

**Part.newBuilder()** examples:
- `Part.newBuilder().setText("You are a helpful assistant.")` - For text content
- `Part.newBuilder().setFunctionResponse(functionResponse)` - For function responses

## Related Resources

- [Hugging Face Hammer 2.1 Model](https://huggingface.co/litert-community/Hammer2.1-1.5b)
- [LLM Inference for Android Guide](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
- Generative AI Prohibited Use Policy

---

**Documentation Source:** https://ai.google.dev/edge/mediapipe/solutions/genai/function_calling/android
**Extracted:** 2025-10-02
**Note:** This SDK is under active development. Features and APIs are subject to change.
