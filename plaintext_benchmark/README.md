# 1. Overview
This README contains 
- a high-level description of the plaintext database system implemented in this repository,
- a documentation for the main APIs in the implementation, and
- the format of the output files in the benchmark.


# 2. Plaintext database system
The plaintext databse system implemented supports basic keyword search, where the keywords for a document are a pre-defined list of words identified by `.\email_parser\Email_parser.py`. 

Search in the database is achieved by building an inverted search index. Here, an inverted search index an index where the keys are keywords in the documents and the values are the document identifiers of the documents that contain those keywords. For example, if document 1 and document 2 both contains keyword `crypto`, then in the inverted search index, there will be a key `crypto`, and 1 and 2 are in the value associated to `crypto`.

In the implementation, the client builds the inverted index locally and uploads it to the server. The client also uploads the plaintext documents to the server. Later, in a query, the client can retrieve the document associated to the queried keyword in two steps. In the first step, it retrieves the list of document identifiers from the server. Then, the client retrieves the actual documents with the document identifiers. Of course, it is possible to let the server send the documents directly, but we chose our implementation to allow for client filtering in the general use case.



# 3. API documentation
This section gives information on the core APIs.

## 3.1. Client APIs
1. Client(HashMap<String, String> configuration):
    - Inputs: The configuration `configuration`.
    - Outputs: None.
    - Intended behaviour: Establish a connection with the server using the port specified in `configuration`.
2. setup(List<Document> documents, Timer timer_setup):
    - Inputs: (1) A list of documents `documents`. (2) A timer `timer_setup` for benchmarking.
    - Outputs: None.
    - Intended behaviour: Build an inverted search index. Send the inverted search index and the plaintext documents to the server.
3. single_keyword_query(String keyword, Timer timer_query):
    - Inputs: (1) Queried keyword `keyword`. (2) A timer `timer_query` for benchmarking.
    - Outputs: A list of String. Each String is a plaintext email containing the queried keyword `keyword`.
    - Intended behaviour: Returns a list of documents containing the queried keyword `keyword`.
4. close():
    - Inputs: None.
    - Outputs: None.
    - Intended behaviour: Close the connection between the client and the server. Shut down both the client and the server.
 

## 3.2. Server APIs
1. main(String[] args):
    - Inputs: None.
    - Outputs: None.
    - Intended behaviour: Listen to the messages from the client. Relays the message to Redis.
    - Messages of operation type `operation = 0` are `get` operations. The server will fetch the values associated to the keys the client asks for.
    - Messages of operation type `operation = 1` are `set` operations. The server will set the key-values the keys the client asks for.
    - Messages of operation type `operation = 2` are for server resetting. Upon calling, the server should delete all data stored in Redis.
    - Messages of operation type `operation = 3` are for shutting down. Upon calling, the server and the client should both be shut down.


## 3.3. Typical usage
The APIs are typically used in the following way. We focus on the client as the API for the server is trivial.
1. Parse the configuration into `HashMap<String, String> configuration`.
2. Parse the documents (emails in the experiments) into `List<Document> documents`.
3. Initialise the client with `Client()`.
4. Run the setup phase with `Client.setup()`.
5. Make search queries with `Client.single_keyword_query()`.
6. Shut down the client (and the server) with `Client.close()`.



# 4. Benchmark output format
In each benchmark run, the program outputs a log file with file name of the format `plaintext_<N_emails>.txt` in `.\benchmark_results\outputs\`. The file has the following format:
```
===================Basic Info===================
KDP: <The number of keyword-document pairs>
Document size: <The size of the documents in bytes>
Setup in nanoseconds: <Setup time in nanoseconds>
Single keyword query in nanoseconds: <Total search query time in nanoseconds>
===================Keywords Info===================
<keyword>, <#keyword occurrences>, <search query time in nanoseconds>
...
```


