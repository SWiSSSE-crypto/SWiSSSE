# 1. Overview
This repository contains an implementation of SWiSSSE [1] and instructions on how to reproduce the results in the paper. We hope that the repository can help with future works on system-wide secure searchable symmetric encryption schemes.

[1] Zichen Gui, Kenneth G. Paterson, Sikhar Patranabis, and Bogdan Warinschi. SWiSSSE: System-Wide Security for Searchable Symmetric Encryption. PoPETs 2024. https://eprint.iacr.org/2020/1328

We give a brief overview of the repository and the workflow of the experiments below.

## 1.1. Repository structure
The repository has the following structure (sorted by file/folder name). We explain how these folders are used in 1.2 below.

```
├── benchmark_results
│   ├── outputs
├── config_files
│   ├── plaintext.conf
│   ├── redis.conf
│   ├── ...
├── emails_parsed
├── emails_parser
│   ├── Email_parser.py
│   ├── include_keywords.txt
├── emails_raw
├── plaintext_benchmark
│   ├── client
│   │   ├── ...
│   ├── server
│   │   ├── ...
├── SWiSSSE_benchmark
│   ├── client
│   │   ├── ...
│   ├── server
│   │   ├── ...
```

## 1.2. Workflow of the experiments
The repository and the instructions in this README.md are sufficient to reproduce the expeirmental results shown in Section 6.3 and Appendix E.6 of the paper. The high-level workflow of the experiments is as follows:
1. Download the Enron email corpus. 
    - The emails should be stored in `.\emails_raw\`.
2. Parse the Enron emails.
    - This process will extract keywords from the Enron emails and store the results in another folder.
    - The raw emails are stored in `.\emails_raw\`. 
    - The Python script for parsing the emails are in `.\emails_parser\`. 
    - The emails with extracted keywords will be stored in `.\emails_parsed\`.
3. Benchmark plaintext search. 
    - This is done with a Java implementation of the client in `.\plaintext_benchmark\client` and a Java implementation of the server in `.\plaintext_benchmark\server\`. 
    - The configuration files used for the experiment are in `.\config_files\`.
    - The client and the server need to be benchmarked for five times, each time with a different number of emails. The number of emails used in the original experiments are `N_emails = {10000, 50000, 100000, 200000, 400000}`.
4. Benchmark keyword search and insertion queries in SWiSSSE.
    - This is done with a Java implementation of the client in `.\SWiSSSE_benchmark\client` and a Java implementation of the server in `.\SWiSSSE_benchmark\server\`.
    - The configuration files used for the experiment are in `.\config_files\`.
    - For keyword search, the client and the server need to be benchmarked for five times, each time with a different number of emails. The number of emails used in the original experiments are `N_emails = {10000, 50000, 100000, 200000, 400000}`. The same is true for insertion queries.



## 1.3. Resources required
This section gives a rough guideline of the resources required to reproduce the experimental results.

| Task                                 | Human Time | Computer Time | Storage |  Memory   |
|--------------------------------------|------------|:-------------:|---------|:---------:|
| Download the Enron email corpus      | 5 minutes  | 20 minutes    | 2 GB    | -         |
| Parse the Enron emails               | 5 minutes  | 25 minutes    | 4 GB    | -         |
| Benchmark plaintext search           | 5 minutes  | 35 minutes    | -       | 2 GB      |
| Benchmark keyword search in SWiSSSE  | 10 minutes | 6 hours \*    | -       | 32 GB\*\* |
| Benchmark insertion query in SWiSSSE | 10 minutes | 8 hours \*    | -       | 40 GB\*\* |

\* There are five experiments in total. The time shown here is the total time required.

\*\* The memory costs are for `N_emails = 400000`. The experiments with fewer emails use considerably less memory. If you do not have sufficient memory on your machine, please run the experiments with smaller `N_emails`. You will need to adjust the plot scripts in `.\benchmark_results\` for the final visualization. We discuss optimizations on memory saving in Section 5 of `README.md`.


## 1.4. Ethical concerns
We use the Enron email corpus in our experiments. The dataset contains real emails from employees of Enron. Our experiments only uses the Enron emails to benchmark the performance of SWiSSSE in realistic workloads. We do not intend to intrude the privacy of the users in the emails. Please be sensitive to the privacy of the people in the emails when you use the dataset.


# 2. Software requirements
The following softwares are required to run the experiments.
- Java (JDK 17 and above): https://www.oracle.com/java/technologies/downloads/.
- Python (3.8 and above): https://www.python.org/downloads/.
    - non-standard Python moduels used: nltk, numpy, matplotlib. These can be installed using [pip](https://pypi.org/project/pip/). 
- Redis (7.2 and above): https://redis.io/docs/getting-started/installation/install-redis-on-linux/.


# 3. Experimental validation of SWiSSSE
This section describes how to reproduce the experimental results shown in the paper.

## 3.1. Download the Enron email corpus
1. Download the Enron email corpus with the following link: https://www.cs.cmu.edu/~enron/.
2. Unzip the emails and put them under `.\raw_emails\`. The directory structure should look like this:
```
├── raw_emails
│   ├── maildir
│   │   ├── allen-p
│   │   ├── arnold-j
...
```

## 3.2. Parse the Enron emails
1. Install `nltk` in Python by running
```bash
pip install nltk
```
(In case pip is not installed, install it by running `sudo apt install python3-pip`.)

2. Navigate to `.\email_parser\`. Run
```bash
python Email_parser.py
```
The parsed emails (400K of them) will appear in `.\email_parsed\`. The file name of the emails are replaced with counters for simplicity. Each parsed email has the structure:
```
<list of keywords>
<original text>
```
The list of keywords has the following properties:
- must be valid words in English (defined in `nltk.corpus.words`).
- all keywords are converted to lower-case.
- only keywords from the main body are extracted.

See below for an example:
<details>
    <summary>Example</summary>

    another,second,mail,already,payment
    Message-ID: <31503589.1075855667838.JavaMail.evans@thyme>
    Date: Tue, 3 Oct 2000 09:13:00 -0700 (PDT)
    From: phillip.allen@enron.com
    To: bs_stone@yahoo.com
    Subject: 
    Mime-Version: 1.0
    Content-Type: text/plain; charset=us-ascii
    Content-Transfer-Encoding: 7bit
    X-From: Phillip K Allen
    X-To: bs_stone@yahoo.com
    X-cc: 
    X-bcc: 
    X-Folder: \Phillip_Allen_Dec2000\Notes Folders\All documents
    X-Origin: Allen-P
    X-FileName: pallen.nsf

    Brenda,

    Please use the second check as the October payment.  If you have already 
    tossed it, let me know so I can mail you another.

Phillip
</details>

3. [Optional] You can replace the keyword extractor with other keyword extractor of your choice as long as the output format is kept the same.


## 3.3. Benchmark plaintext search
1. Navigate to `.\config_files\`. Start Redis by running
```bash
redis .\redis.conf
```
2. **Start a new terminal**. Navigate to `.\plaintext_benchmark\`. Compile the server with
```bash
javac -d .\server\bin\ --release 8 -classpath ".:.\server\jedis-3.3.0.jar:" .\server\src\Server.java
```
The `--release 8` in the commands are used to make the compiled class files to be as compatible with old versions of Java as possible. It is normal to see warnings when compiling the programs. You may remove `--release 8` from the commands if your Java runtime supports higher versions of Java.

The Jedis jar file is provided in `.\plaintext_benchmark\server\`. In case you want to use a different version of Jedis, download it from here: https://mvnrepository.com/artifact/redis.clients/jedis.

3. Navigate to `.\plaintext_benchmark\server\bin\`. Start the server with
```bash
java -cp ".:..\jedis-3.3.0.jar" Server
```

4. **Start a new terminal.** Navigate to `.\plaintext_benchmark\`. Compile the client with
```bash
javac -d .\client\bin\ --release 8 .\client\src\*.java .\client\src\client\*.java .\client\src\parser\*.java
```

5. Navigate to `.\plaintext_benchmark\client\bin\`. Start the client with
```bash
java Controller ..\..\..\config_files\plaintext.conf
```

6. Once the benchmark finishes, you can find the results in `.\benchmark_results\`.

7. Repeat Step 5 for databases of different sizes. In the paper, we used `N_emails = {10000, 50000, 100000, 200000, 400000}`. You need to restart the server before restarting the client.

More information about the plaintext search scheme and the usage details can be found in `.\plaintext_benchmark\README.md`.


## 3.4. Benchmark SWiSSSE
To reduce the possibility of human error. Please close all terminals used for the plaintext search benchmark. Start a new terminal.

1. Navigate to `.\config_files\`. Start Redis by running
```bash
redis .\redis.conf
```

2.  **Start a new terminal**. Navigate to `.\SWiSSSE_benchmark\`. Compile the server with
```bash
javac -d .\server\bin\ --release 8 -classpath ".:.\server\jedis-3.3.0.jar:" .\server\src\Server.java
```

3. Navigate to `.\SWiSSSE_benchmark\server\bin\`. Start the server with
```bash
java -cp ".:..\jedis-3.3.0.jar" Server
```

4. **Start a new terminal**. Navigate to `.\SWiSSSE_benchmark\`. Compile the client with
```bash
javac -d .\client\bin\ --release 8 .\client\src\*.java .\client\src\client\*.java .\client\src\crypto\*.java .\client\src\parser\*.java
```

5. [Optional] Test run SWiSSSE. Navigate to `.\SWiSSSE_benchmark\client\bin\`. Start the client with
```bash
java Client ..\..\..\config_files\SWiSSSE_test.conf
```

6. To benchmark search query performance of SWiSSSE, start the client with
```bash
java Client ..\..\..\config_files\SWiSSSE_search.conf
```
If you made a test run of SWiSSSE (Step 5), you need to restart the server first (following Step 3) before starting the client.

7. To benchmark insertion query performance of SWiSSSE, restart the server following Step 3, and start the client with
```bash
java Client ..\..\..\config_files\SWiSSSE_insert.conf
```

8. Repeat Steps 6 and 7 for databases of different sizes. In the paper, we used `N_emails = {10000, 50000, 100000, 200000, 400000}`. You can change the parameter in `.\config_files\SWiSSSE_search.conf` and `.\config_files\SWiSSSE_insert.conf` respectively.
You need to restart the server before restarting the client.

## 3.5. Plot the results
1. Navigate to `.\benchmark_results\`.
2. There are six benchmark metrics, each correspond to a Python script:
    - Setup time: run `python plot_setup.py`.
    - Search time (static SWiSSSE): run `python plot_search.py`.
    - Insertion time (dynamic SWiSSSE): run `python plot_insert.py`.
    - Write-back time (static SWiSSSE): run `python plot_write_back.py`.
    - Storage (static SWiSSSE): run `python plot_storage.py`.
    - Stash size (static SWiSSSE): run `python plot_stash.py`.


# 4. Major claims (Section 6 and Appendix E.6 of the paper)
1. **Setup time:** The setup time of SWiSSSE is two orders of magnitude slower than a plaintext database system.
2. **Query response time (search):** The query response time of SWiSSSE for search queries is about two to four times slower than a plaintext database system.
3. **Query response time (insertion):** The query response time of SWiSSSE for insertion queries is about the same as that for search queries.
4. **Write-back time:** The write-back time of SWiSSSE is typically under a second.
5. **Storage cost:** The storage cost of SWiSSSE is only slightly larger than that of a plaintext database system. The main overhead comes from the storage cost of the encrypted search index.
6. **Stash size:** The stash size for SWiSSSE is under 10 megabyte most of the times. This is reasonably small for morden devices.



# 5. Known issues and possible optimizations
1. The Java implementation of the client is not memory efficient due to the way garbage collection works in Java. An implementation in languages such as C++ will improve the memory efficiency of the client significantly.
2. The communication between the client and the server is not optimal. In the current implementation, each key and value is communicated separately (in both directions). This induces a significant overhead on the communication. We believe that a batched implementation of the communication procedure can improve the communication overhead significantly.
3. The current implementation of the server uses the standard key-value store in Redis. This is not the most efficient approach in storing a large number of key-value pairs (as a result of a large number of keyword-document pairs). One possible optimisation is to use [Redis hashes](https://redis.io/docs/data-types/hashes/) instead.
4. Currently, the server only supports atomic key-value set and get operations (i.e. set/get one key-value pair at a time). One possible optimisation is to make use of the `mset` and `mget` operations in Redis.
 


