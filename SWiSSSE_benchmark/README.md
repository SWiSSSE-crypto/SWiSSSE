# 1. Overview
This README contains
- a documentation for the main APIs in the implementation, and
- the format of the output files in the benchmark.


# 3. API documentation
This section gives information on the core APIs.

## 3.1 Client APIs
1. Client(HashMap<String, String> configuration):
    - Inputs: The configuration `configuration`.
    - Outputs: None.
    - Intended behaviour: Establish a connection with the server using the port specified in `configuration`.
2. initialise(List<Document> documents, HashMap<String, String> configuration):
    - Inputs: (1) A list of documents `documents`. (2) The configuration `configuration`.
    - Output: None.
    - Intended behaviour: (1) Generate cryptographic keys for SWiSSSE. (2) Split the documents if they are too large (and hence leak volume information). (3) Build an implicit lookup index. (4) Prepare for keyword bucketization (Section 4 of the paper).
3. add_fake_keywords_and_documents(List<Document> documents):
    - Inputs: A list of documents `documents`.
    - Outputs: None.
    - Intended behaviour: Performs keyword bucketization.
4. setup(List<Document> documents, HashMap<String, String> configuration, Timer timer_setup):
    - Inputs: (1) A list of documents `documents`. (2) The configuration `configuration`. (3) A timer `timer_setup` for benchmarking.
    - Outputs: None.
    - Intended behaviour: (1) Encrypt the documents `documents` and upload them to the server. (2) Encrypt the lookup index and upload it to the server.
5. single_keyword_query(String keyword, SecureRandom random, Timer timer_query, Timer timer_write_back, StashMonitor stashMonitor):
    - Inputs: (1) Queried keyword `keyword`. (2) A random seed `random` for write-back. (3) A timer `timer_query` for benchmarking query performance. (4) A timer `timer_write_back` for benchmarking write-back performance. (5) An object `stashMonitor` for benchmarking the stash size.
    - Outputs: None.
    - Intended behaviour: (1) Retrieves the set of encrypted documents containing the queried keyword `keyword`. (2) Decrypt the documents. (3) Perform a write-back operation.
6. insertion_query(Document document, SecureRandom random, Timer timer_query, Timer timer_write_back, StashMonitor stashMonitor):
    - Inputs: (1) Document `document` to be inserted. (2) A random seed `random` for write-back. (3) A timer `timer_query` for benchmarking query performance. (4) A timer `timer_write_back` for benchmarking write-back performance. (5) An object `stashMonitor` for benchmarking the stash size.
    - Outputs: None.
    - Intended behaviour: (1) Insert document `document` into the stash after retrieving all documents containing the least frequent keyword in document `document`. (2) Perform a write-back operation.
7. close():
    - Inputs: None.
    - Outputs: None.
    - Intended behaviour: Close the connection between the client and the server. Shut down both the client and the server.
 

## 3.2 Server APIs
1. main(String[] args):
    - Inputs: None.
    - Outputs: None.
    - Intended behaviour: Listen to the messages from the client. Relays the message to Redis.
    - Messages of operation type `operation = 0` are `get` and `del` operations. The server will fetch the values associated to the keys the client asks for and **DELETE** the keys from the database.
    - Messages of operation type `operation = 1` are `set` operations. The server will set the key-values the keys the client asks for.
    - Messages of operation type `operation = 2` are for server resetting. Upon calling, the server should delete all data stored in Redis.
    - Messages of operation type `operation = 3` are for shutting down. Upon calling, the server and the client should both be shut down.


## 3.3. Typical usage
The APIs are typically used in the following way. We focus on the client as the API for the server is trivial.
1. Parse the configuration into `HashMap<String, String> configuration`.
2. Parse the documents (emails in the experiments) into `List<Document> documents`.
3. Initialise the client with `Client()`.
4. Perform keyword bucketization with `Client.add_fake_keywords_and_documents()`.
5. Run the setup phase with `Client.setup()`.
6. Make search queries with `Client.single_keyword_query()` and/or insertion queries with `Client.insertion_query()`.
7. Shut down the client (and the server) with `Client.close()`.

Please note that in the current implementation, the cryptographic keys are not saved onto the disk at any point in time. So once the client is shut down, the encrypted database can no longer be decrypted.


# 4. Benchmark output format
## 4.1. Search query
In each benchmark run for search query, the program outputs a log file with file name of the format `search_<N_emails>.txt` in `.\benchmark_results\outputs\`. The file has the following format:
```
===================Basic Info===================
KDP: <The number of keyword-document pairs>
Document size: <The size of the documents in bytes>
Keywords: {The number of keywords}
Setup in nanoseconds: <Setup time in nanoseconds>
Single keyword query in nanoseconds: <Total search query time in nanoseconds>
Write-back in nanoseconds: <Total write-back time in nanoseconds>
===================Keywords Info===================
<keyword>, <#keyword occurrences>, <time to retrieve the document identifiers in nanoseconds>, <time to retrieve the actual documents in nanoseconds>, <time to perform the write-back operation>, <stash size after the write-back>
...
```

## 4.2. Insertion query
In each benchmark run for insertion query, the program outputs a log file with file name of the format `insert_<N_emails>.txt` in `.\benchmark_results\outputs\`. The file has the following format:
```
===================Basic Info===================
KDP: <The number of keyword-document pairs>
Document size: <The size of the documents in bytes>
Setup in nanoseconds: <Setup time in nanoseconds>
Insertion query in nanoseconds: <Total search query time in nanoseconds>
Write-back in nanoseconds: <Total write-back time in nanoseconds>
===================Keywords Info===================
<#keyword occcurrences for the least frequent keyword in the document inserted>, <time to retrieve the document identifiers in nanoseconds>, <time to retrieve the actual documents in nanoseconds>, <time to perform the write-back operation>, <stash size after the write-back>
...
```



