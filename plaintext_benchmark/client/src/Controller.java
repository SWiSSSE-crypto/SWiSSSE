import java.io.*;
import java.util.*;

import parser.*;
import client.*;
import client.Timer;

public class Controller {

	public static void main(String[] args) throws IOException {
		
		HashMap<String, String> configuration = Client.parse_configuration(args[0]);
		
		System.out.println("Running experiment with the following parameters.");
		for (String key: configuration.keySet()) {
			System.out.println(key + ": " + configuration.get(key));
		}
				
		 
		// Parse documents
		List<Document> documents = new ArrayList<Document>();
		Parser.parse_documents(documents, configuration);
		System.out.println("Read done.");
		
		// Initialise Client
		Timer timer_setup = new Timer();
		Client client = new Client(configuration);
		client.setup(documents, timer_setup);
		System.out.println("Setup done.");
		
		
		// Executing the queries
		List<String> keywords = new ArrayList<String>(client.inverted_index.keySet());
		Collections.sort(keywords);
		Timer timer_query = new Timer();
		for (Integer idx = 0; idx < Integer.parseInt(configuration.get("N_queries")); idx++)
		{
			client.single_keyword_query(keywords.get(idx), timer_query);
		}
				
		print_performance_log(documents, keywords, client.inverted_index, timer_setup, timer_query, configuration);
		
		System.out.println("Queries done.");
		
		// close connection
		client.close();

	}

	

	private static void print_performance_log(List<Document> documents, List<String> keywords, HashMap<String, List<Integer>> inverted_index, 
			Timer timer_setup, Timer timer_query, HashMap<String, String> configuration) throws IOException {
		
		Integer keyword_document_pairs = 0;
		for (String keyword: inverted_index.keySet())
			keyword_document_pairs += inverted_index.get(keyword).size();
		
		Integer document_size_total = 0;
		for (Document document: documents)
			document_size_total += document.get_data().length();
		
		
		FileWriter writer = new FileWriter(configuration.get("folder_output") + "plaintext_" + configuration.get("N_emails") + ".txt");
		writer.write("===================Basic Info===================\n");
		writer.write("KDP: " + keyword_document_pairs + "\n");
		writer.write("Document size: " + document_size_total + "\n");
		writer.write("Keywords: " + keywords.size() + "\n");
		writer.write("Setup in nanoseconds: " + timer_setup.get_total_time() + "\n");
		writer.write("Single keyword query in nanoseconds: " + timer_query.get_total_time() + "\n");
		
		writer.write("===================Keywords Info===================\n");
		for (Integer idx = 0; idx < Integer.parseInt(configuration.get("N_queries")); idx++)
			writer.write(keywords.get(idx) + "," + inverted_index.get(keywords.get(idx)).size() + "," + timer_query.get_time(idx) + "\n");
		writer.close();
	}


}
