package client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;


public class Client {
	// keywords
	public HashMap<String, List<Integer>> inverted_index = new HashMap<String, List<Integer>>();
	
	// Network
	private DataInputStream input_stream;
	private DataOutputStream output_stream;
	
	/*
	 * Initialise the client with configuration
	 * Starts the connection with the server
	 */
	public Client(HashMap<String, String> configuration) throws UnknownHostException, IOException {
		@SuppressWarnings("resource")
		Socket socket = new Socket("localhost", Integer.parseInt(configuration.get("port")));
		socket.setTcpNoDelay(true);
		this.input_stream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		this.output_stream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	}
	
	/*
	 * Build the inverted search index 
	 * Dump the inverted search index and the documents to the server
	 */
	public void setup(List<Document> documents, Timer timer_setup) throws IOException {
		// build an inverted lookup table 
		for (Integer ii = 0; ii < documents.size(); ii++) {
			for (String keyword: documents.get(ii).get_keywords()) {
				if (inverted_index.containsKey(keyword) == false)
					inverted_index.put(keyword, new ArrayList<Integer>());
				inverted_index.get(keyword).add(ii);
			}
		}
		if (inverted_index.containsKey(""))
			inverted_index.remove("");
		
		timer_setup.start();
		
		// dump inverted lookup table to server
		output_stream.writeInt(1);
		for (String keyword: inverted_index.keySet()) {
			output_stream.writeInt(keyword.getBytes().length);
			output_stream.write(keyword.getBytes());
			
			String values = inverted_index.get(keyword).stream().map(String::valueOf).collect(Collectors.joining(","));
			output_stream.writeInt(values.getBytes().length);
			output_stream.write(values.getBytes());
		}
		output_stream.writeInt(-1);
		output_stream.flush();
		input_stream.readInt();
		
		
		// dump documents to server
		output_stream.writeInt(1);
		for (Integer document_id = 0; document_id < documents.size(); document_id++) {
			Document document = documents.get(document_id);
			
			String doc_key = "doc" + document_id;
			output_stream.writeInt(doc_key.getBytes().length);
			output_stream.write(doc_key.getBytes());
			
			output_stream.writeInt(document.get_data().getBytes().length);
			output_stream.write(document.get_data().getBytes());
		}
		output_stream.writeInt(-1);
		output_stream.flush();
		
		input_stream.readInt();
		timer_setup.stop();
	}
	
	
	/*
	 * Single keyword query
	 * Returns the list of documents that contain the queried keyword
	 */
	public List<String> single_keyword_query(String keyword, Timer timer_query) throws IOException {
		timer_query.start();
		
		// Get the list of document identifiers
		output_stream.writeInt(0);
		output_stream.writeInt(keyword.getBytes().length);
		output_stream.write(keyword.getBytes());
		output_stream.writeInt(-1);
		output_stream.flush();
		
		Integer document_ids_size = input_stream.readInt();
		byte[] document_ids_bytes = new byte[document_ids_size];
		input_stream.readFully(document_ids_bytes);
		
		// Get the list of documents
		output_stream.writeInt(0);
		List<String> results = new ArrayList<String>();
		String[] document_ids = new String(document_ids_bytes).split(",");
		for (String document_id: document_ids) {
			String document_id_full = "doc" + document_id;
			output_stream.writeInt(document_id_full.getBytes().length);
			output_stream.write(document_id_full.getBytes());
			output_stream.flush();
			
			Integer document_size = input_stream.readInt();
			byte[] document_content = new byte[document_size];
			input_stream.readFully(document_content);
			results.add(new String(document_content));
		}
		output_stream.writeInt(-1);
		output_stream.flush();
		
		timer_query.stop();
		
		return results;
	} 

	/*
	 * Close the connection with the server
	 * Both the client and the server will shut down after this function is called
	 */
	public void close() throws IOException {
		output_stream.writeInt(3);
		output_stream.flush();
	}
	
	/* 
	 * Function to parse the configuration
	 * WARNING: there is no correctness/completeness check
	 */
	public static HashMap<String, String> parse_configuration(String filename_input) throws IOException {
		HashMap<String, String> configuration = new HashMap<String, String>();
		
		FileReader fp = new FileReader(filename_input);
		BufferedReader config_reader = new BufferedReader(fp);
		
		String next_line = config_reader.readLine();
		while (next_line != null) {
			String[] argument = next_line.split(" = ");
			configuration.put(argument[0], argument[1]);
			next_line = config_reader.readLine();
		}
		
		fp.close();
		config_reader.close();
		return configuration;
	}
}
