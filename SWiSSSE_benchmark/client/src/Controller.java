import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

import client.*;
import client.Timer;
import parser.*;

public class Controller {
	
	public static void main(String[] args) throws NumberFormatException, IOException, GeneralSecurityException {
		// parse input
		HashMap<String, String> configuration = Client.parse_configuration(args[0]);
		
		if (configuration.get("mode").equals("search"))
			single_keyword_query_test(configuration);
		if (configuration.get("mode").equals("insert"))
			insertion_query_test(configuration);

	}
	
	
	
	public static void single_keyword_query_test(HashMap<String, String> configuration) throws IOException, NumberFormatException, GeneralSecurityException
	{
		// Parse documents
		List<Document> documents = new ArrayList<Document>();
		Parser.parse_documents(documents, configuration);
		System.out.println("Read done.");
				
		
		// Initialise the client
		Client client = new Client(configuration);
		client.initialise(documents, configuration);
		System.out.println("Initialise done.");
		
		client.add_fake_keywords_and_documents(documents);
		System.out.println("Add fake done.");
		System.out.println("Documents count after padding: " + documents.size());
		
		// Save database parameters for later
		Integer keyword_document_pair_count = 0;
		for (Document document: documents)
			keyword_document_pair_count += document.get_keywords().size();
		
		Integer document_size = 0;
		for (Document document: documents)
			document_size += document.get_size() + 28;

		// Setup the database with the server
		Timer timer_setup = new Timer();
		client.setup(documents, configuration, timer_setup);
		documents = null;
		
		// Process queries
		Timer timer_query = new Timer();
		Timer timer_write_back = new Timer();
		StashMonitor stashMonitor = new StashMonitor();
		
		int query_count = 0;
		
		List<String> keyword_list = new ArrayList<String>(client.get_keywords().keySet());
		Collections.sort(keyword_list);
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		for (int idx = 0; idx < Integer.parseInt(configuration.get("N_queries")); idx++)
		{
			if (query_count % 100 == 0)
				System.out.println("Query progress: " + query_count + "/" + configuration.get("N_queries"));
			query_count++;
			
			client.single_keyword_query(keyword_list.get(idx), random, timer_query, timer_write_back, stashMonitor);
		}
		
		client.close();

		print_performance_log1(keyword_document_pair_count, document_size, client.get_keywords(), timer_setup, timer_query, timer_write_back, stashMonitor, configuration);
	}
	
	public static void print_performance_log1(int keyword_document_pair_count, int document_size, HashMap<String, Integer> keyword_max, 
			Timer timer_setup, Timer timer_query, Timer timer_write_back, StashMonitor stashMonitor, HashMap<String, String> configuration)
	{
		try {
			FileWriter writer = new FileWriter(configuration.get("folder_output")+ configuration.get("mode") + "_" + configuration.get("N_emails") +  ".txt");
			writer.write("===================Basic Info===================\n");
			//writer.write("CPU: " + Files.lines(Paths.get("/proc/cpuinfo")).filter(line -> line.startsWith("model name")).map(line -> line.replaceAll(".*: ", "")).findFirst().orElse("") + "\n");
			writer.write("KDP: " + keyword_document_pair_count + "\n");
			writer.write("Document size: " + document_size + "\n");
			writer.write("Keywords: " + keyword_max.size() + "\n");
			writer.write("Setup in nanoseconds: " + timer_setup.get_total_time() + "\n");
			writer.write("Single keyword query in nanoseconds: " + timer_query.get_total_time() + "\n");
			writer.write("Write-back in nanoseconds: " + timer_write_back.get_total_time() + "\n");
			
			writer.write("===================Keywords Info===================\n");
			List<String> keyword_list = new ArrayList<String>(keyword_max.keySet());
			Collections.sort(keyword_list);
			for (int idx = 0; idx < Integer.parseInt(configuration.get("N_queries")); idx++)
				writer.write(keyword_list.get(idx) + "," + keyword_max.get(keyword_list.get(idx)) + "," + timer_query.get_time(2*idx) + "," 
			+ timer_query.get_time(2*idx+1) + "," + timer_write_back.get_time(idx) + "," + stashMonitor.get(idx) + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void insertion_query_test(HashMap<String, String> configuration) throws IOException, NumberFormatException, GeneralSecurityException {
		// Parse documents
		List<Document> documents = new ArrayList<Document>();
		Parser.parse_documents(documents, configuration);
		System.out.println("Read done.");
						
				
		// Initialise the client
		Client client = new Client(configuration);
		client.initialise(documents, configuration);
		System.out.println("Initialise done.");
				
		client.add_fake_keywords_and_documents(documents);
		System.out.println("Add fake done.");
		System.out.println("Documents count after padding: " + documents.size());
				
		// Save database parameters for later
		Integer keyword_document_pair_count = 0;
		for (Document document: documents)
			keyword_document_pair_count += document.get_keywords().size();
		
		Integer document_size = 0;
		for (Document document: documents)
			document_size += document.get_size() + 28;

		// Setup the database with the server
		Timer timer_setup = new Timer();
		client.setup(documents, configuration, timer_setup);
		
		// Insertion queries
		Timer timer_query = new Timer();
		Timer timer_write_back = new Timer();
		StashMonitor stashMonitor = new StashMonitor();
		
		List<Integer> query_sizes = new ArrayList<Integer>(); 
		int query_count = 0;
		//SecureRandom random = SecureRandom.getInstanceStrong();
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		
		for (int ii = 0; ii < Integer.parseInt(configuration.get("N_queries")); ii++)
		{
			//if (query_count % 10 == 0)
			System.out.println("Query progress: " + query_count + "/" + configuration.get("N_queries"));
			query_count++;
			int query_size = client.insertion_query(documents.get(ii), random, timer_query, timer_write_back, stashMonitor);
			query_sizes.add(query_size);
		}

		client.close();
		
		print_performance_log2(keyword_document_pair_count, document_size, query_sizes, timer_setup, timer_query, timer_write_back, stashMonitor, configuration);
	}
	
	public static void print_performance_log2(Integer keyword_document_pair_count, Integer document_size, List<Integer> query_sizes, 
			Timer timer_setup, Timer timer_query, Timer timer_write_back, StashMonitor stashMonitor, HashMap<String, String> configuration)
	{
		try {
			FileWriter writer = new FileWriter(configuration.get("folder_output")+ configuration.get("mode") + "_" + configuration.get("N_emails") + ".txt");
			writer.write("===================Basic Info===================\n");
			writer.write("KDP: " + keyword_document_pair_count + "\n");
			writer.write("Document size: " + document_size + "\n");
			writer.write("Setup in nanoseconds: " + timer_setup.get_total_time() + "\n");
			writer.write("Insertion query in nanoseconds: " + timer_query.get_total_time() + "\n");
			writer.write("Write-back in nanoseconds: " + timer_write_back.get_total_time() + "\n");
			
			writer.write("===================Keywords Info===================\n");
			for (int ii = 0; ii < query_sizes.size(); ii++)
				writer.write(query_sizes.get(ii) + "," + timer_query.get_time(2*ii) + "," + timer_query.get_time(2*ii+1)
					+ "," + timer_write_back.get_time(ii) + "," + stashMonitor.get(ii) + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

	
