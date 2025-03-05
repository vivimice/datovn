# FastICUE Specification Version 1.0

## Overview

Fast Interface for Computation Unit Execution (FastICUE) is an interface specification between Datovn core and external computation units. 

FastICUE is an extension to ICUE specification that is language independent and provides high performance. FastICUE is designed to support long-lived computation processes, which can befit from in-process caching and other optimizations. That's the major differences compared to ICUE, which spawns a process, use it to one computation task, then terminate it.

The initial state of a FastICUE process is more spartan than that of an ICUE process. That's because FastICUE processes are expected to be long-lived and thus should not have any pre-existing state or data. It doesn't have "DATOVN_" environment variables like ICUE. The key piece of initial state in a FastICUE process is its standard streams (stdin, stdout, stderr). These streams are used for communication between the Datovn core and the FastICUE process.

After a FastICUE process has been started, it can be interacted with a simple protocol to receive and send data. The protocol multiplexes a single transport connection via its standard streams between several independent computation requests. This supports FastICUE process that are able to process concurrent computation requests using concurrent programming techiniques.

A FastICUE process is spawned at first time when a Datovn core needs it. It is expected that the FastICUE process will remain running until it is explicitly terminated by the Datovn core, by the time the computation stage, which spawns it, is completed.

FastICUE specification is platform-independent and can be implemented on any platform that supports standard streams (stdin, stdout, stderr). 

## Specification Basics

The FastICUE protocol uses a simple text-based (with UTF-8 encoding) and line-based protocol (with CRLF EOL) over standard streams. The protocol consists of commands sent from the Datovn core to the FastICUE process and responses sent back from the FastICUE process to the Datovn core.

Though text-based protocol is less efficient than binary protocols, it is chosen for its simplicity and ease of debugging. And due to the nature that both Datovn core and FastICUE process runs in the same OS, the overhead of increased data size is negligible.

Basicly, The FastICUE protocol is inspired by HTTP/1 with additional multiplexing support.

A typical interaction looks like the following text blocks, where `>` means input from the Datovn core, `<` means output from the FastICUE process. '<' and '>' are not part of the actual protocol, they are just used to distinguish between input and output.

```text
> 01 Q | PING FastICUE/1.0      # Request id, method and protocol version
> 01 Z |                        # End of request
< 01 R | FastICUE/1.0 200 OK    # Request id and status code
< 01 Z |                        # End of response       
```

Or even more complex:

```text
> 02 Q | EXEC FastICUE/1.0  # Request id, method and protocol version
> 02 H | Unit: foo
> 02 H | Stage: stage1
> 02 H | Opaque-Id: 1a2b3c4d5e6f
> 02 H | Params-Count: 2       
> 02 H | Param-Value-0: Foo
> 02 H | Param-Value-1: Bar
> 02 Z |                              # End of request
< 02 R | FastICUE/1.0 202 Accepted    # Request id and status code
< 02 L | ---                          # Computation action output
< 02 L | type: fileAccess
< 02 L | path: foo.txt
< 02 L | mode: read
< 02 L | ---
< 02 L | type: messageOutput
< 02 L | level: error
< 02 L | mesage: |
< 02 L |   Uncaught error: "foo.txt" does not exist
< 02 L |     at /path/to/script.js:10
< 02 L |     at <anonymous>:23:17
< 02 L | ---
< 02 L | type: exit
< 02 L | exitCode: 0
< 02 Z |                              # End of computation action output
```

## Invocation

The interaction between Datovn core and FastICUE process is like a function call. The Datovn core send a request to the FastICUE process with the computation specification information, and waits for the FastICUE process to finish. The FastICUE process then returns the action output of the computation process to the Datovn core. We call this one request-then-response interaction an "invocation".

FastICUE process should support concurrent invocations. The Datovn core might start new invocations before existing invocations to finish.

There are different types of invocations, which are identified by the `method` field of the request initialization frame, which is described later. The FastICUE process should be able to handle different types of invocations appropriately.

The following table lists the types of invocations and their corresponding methods:

| Method | Description |
|--------|-------------|
| `EXEC` | Execute computation specification |
| `PING` | Check FastICUE process liveness |
| `TERM` | Inform and wait for the FastICUE process to terminate gracefully |

Method name is case-sensitive.

### EXEC Invocation

The `EXEC` invocation is used to execute the computation specification provided by the Datovn core. The FastICUE process should parse the request headers and execute the corresponding computation unit.

#### EXEC Request

The following request headers will be provided for the `EXEC` invocation:

| Header Name | Description |
|-------------|-------------|
| `Unit` | Name of the execution specification |
| `Stage` | Name of the current computation stage |
| `Opaque-Identifier`    | Opaque identifier for the execution specification |
| `Params-Count` | Number of the parameters for the execution specification |
| `Param-Value-<index>` | Value of the parameter at index `<index>`, if any |

Header name is case-sensitive. The index in `Param-Value-<index>` starts from 0. The ordering of the headers are not guaranteed.

#### EXEC Response

FastICUE should respond with one of the following status codes:

| Status Code | Description |
|-------------|-------------|
| `202`       | Computation specification accepted |
| `400`       | Invalid or malformed request, or missing required headers |
| `500`       | Computation specification failed to accept due to internal FastICUE error |
| `503`       | FastICUE is currently unable to accept the computation specification due to overload or similar reasons |
| `505`       | FastICUE does not support the requested protocol version |

### PING Invocation

## Request and Response

An invocation is initiated by a request and finished by a response. The request contains information about the computation specification as headers, while the response contains the action output of the computation process. 

Comparing to ICUE, a FastICUE request corresponds to a single ICUE execution. Computation specification params are passed as request headers, rather than environment variables in ICUE. And also, computation actions are expected to be in response body, rather than file writting to a temp file (specified by `DATOVN_ACTION_OUTPUT_FILE` environment variable) in ICUE.

A request contains exactly one request init frame (type Q) and optional header frames (type H), followed by a request termination frame (type Z). A response contains exactly one response status frame (type R) and optional computation action frames, followed by an optional termination frame.

The following diagram illustrates the layer view of a FastICUE request and response:

```text
+---------------------------------------------------+
|                   Invocation                      |
+------------------------+--------------------------+
|        Requset         |         Response         |
|------+---------+-------+--------+---------+-------+
| Init | Headers | Term. | Status | Actions | Term. |
|------+---------+-------+--------+---------+-------+
```

Since FastICUE allows multiplexing, which means there might be multiple ongoing invocations running concurrently. So each request and response has a unique invocation ID to pair them. The ID is an unsigned, non-zero integer, which can be up to 31 bits long (0x00000000 to 0x7FFFFFFF). Uniqueness of invocation IDs will be enforced during all ongoing invocation. 

## Frames

The minimum transmission unit of FastICUE is a Frame. Frames are used to encapsulate different types of information exchanged between the Datovn core and FastICUE process. Each frame has a specific type that indicates its purpose within the interaction. 

Frames are atomic, meaning it cannot be split or combined with other frames during transmission. Frames are transmitted independently of each other, allowing for concurrent processing of multiple invocations. 

Frames can be describes with the following fields (we use C struct notation for simplicity):

```c
struct Frame {
    char id[];          // Invocation ID in hexadecimal format (e.g., "01", "02").
                        // The length is not fixed and depends on the content. 
    char sep1;          // Separator, always ' ' (byte value: 0x20).
    char type;          // Type of frame.
    char sep2;          // Separator, always " | " (byte values: [0x20, 0x7c, 0x20]).
    char data[];        // Frame data string with UTF-8 encoding. CR-LF is not allowed in the data string.
    char terminator[2]; // CR-LF, indicating the end of the frame. 
                        // Regardless of the newline character of the OS, CR-LF must be used here.
}
```

The following table lists the types of frames and their corresponding descriptions:

| Type | Description |
|------|-------------|
| `Q`  | Initialize a computation request with method and protocol. |
| `H`  | Provide additional headers for the computation request. |
| `Z`  | Mark the end of a computation request or response. |
| `R`  | Indicates that a computation response has been received and processed with status code. |
| `L`  | Line based response data. |
| `B`  | Base64 encoded response data. |

### Request Init Frame

Request init frame is used to initialize an invocation. The data part of it specifies the method (e.g. `EXEC`) and the protocol version (e.g. `FastICUE/1.0`). The two parts are separated by ASCII space character (`0x20`).

An example of a request init frame is shown below (assume that the invocation ID is `0123` and method is `EXEC`):

```text
0123 Q | EXEC FastICUE/1.0
```

### Request Header Frame

Request header frame is used to provide additional headers for the invocation. The header name and value are separated by ASCII colon (`0x3a`) and optional surrounding ASCII space characters (`0x20`). Noth the header names and values are case-sensitive. 

The header names comply with the regular expression: `[a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9]`, which means the header names:

- Must start with a letter.
- Can contain letters, digits, and hyphens.
- Must end with a letter or digit.
- At least 2 characters long.

The header values can be any sequence of ASCII characters except for ASCII control characters (`0x00`-`0x1f` and `0x7f`). Also, the header values must starts and ends with non-space characters. This allows both party of the invocation can safely trim the header values.

Here list some examples of request header frames (assume that the invocation ID is `0123`):

```text
# Space after the colon (Recommended for readability)
0123 H | Params-Count: 2    

# No space around the colon
0123 H | Unit:foo

# Space around the colon
0123 H | Stage : stage1

# Space before the colon
0123 H | Opaque-Id :1a2b3c4d5e6f
```

### Termination Frame

Termination frame is used to mark the end of a computation request or response. It is always the last frame of a request or response. Termination frame has no data field.

Here is an example of a termination frame (assume that the invocation ID is `0123`):

```text
0123 Z |
```

### Response Status Frame

Response status frame is used to indicate the status of a computation response. It is always the first frame of a response. The data part of it consists of the protocol version, status code and status message. These parts are separated by ASCII space character (`0x20`). All parts are mandatory.

Protocol version is always `FastICUE/1.0`. 

Status code is a 3-digit integer, which indicates the status of the computation response. The design of status code is similar to HTTP status code, sharing the same semantics, to help the developer understand the status of the computation response.

Status message is a string, which indicates the reason of the status code. 

Here is an example of a response status frame (assume that the invocation ID is `0123`):

```text
0123 R | FastICUE/1.0 202 Accepted
```

### Response Data Frames

Response data frame is used to provide data for a computation response. A computation response can have zero or more response data frames. 

There are two types of response data frames, one is line based response data frame (type `L`), and the other is base64 encoded response data frame (type `B`). 

#### Line Based Response Data Frame

A line based response data frame (type `L`) represents a line of data in the response. The data part of it is one single line of data in the response. EOL is NOT included in the data part. Consumer of 'L' frames should append EOL to get the original line text.

Data part of `L` frame is UTF-8 encoded. No EOL is allowed in `L` frames, so multiline text data should be transmitted as multiple `L` frames.

Here is an example of a line based response data frame (assume that the invocation ID is `0123`):

```text
# The data represented by this frame is "This is a line of data.\n" in Linux, or "This is a line of data.\r\n" in Windows.
0123 L | This is a line of data.
```

#### Base64 Encoded Response Data Frame

A base64 encoded response data frame (type `B`) represents a base64 encoded data in the response. The data part of it is a base64 encoded data in the response. Consumer of 'B' frames should decode the base64 encoded data to get the original binary data.

Here is an example of a base64 encoded response data frame (assume that the invocation ID is `0123`):

```text
# The data represented by this frame is "Hello World" in base64.
0123 B | SGVsbG8gV29ybGQ=

# The data represented by this frame is base64 encoded "你好" in GBK charset.
0123 B | xOO6ww==
```

## Debugging

The text-based and line-based nature of FastICUE protocol makes it easy to debug. The invocation ID allows you to match requests with their corresponding responses, making it straightforward to follow the conversation's progression.

Usually, fasticue-dump, a simple wrapper script can be used to log the input and output frames for debugging purposes:

```bash
#!/bin/bash
LOG_FILE="fasticue.log"
tee >(awk '{print "> " $0}' >> "${LOG_FILE}") | your-fasticue-daemon | tee >(awk '{print "< " $0}' >> "${LOG_FILE}")
```

> NOTE: the above script doesn't synchronize line writes between the two `tee` commands. This may log corrupted frames incorrectly. Usually this is less likely to happen when developing or debugging, unless some frames are too long or if the traffic is very busy.
