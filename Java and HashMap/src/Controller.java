import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;


import client.*;
import client.Timer;
import parser.*;


public class Controller {

	/*
	 * Inputs:
	 * 0: Grouping
	 * 1: Raw database directory, 2: Document number
	 * 3: Performance log directory 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException, GeneralSecurityException {
		if (args[0].equals("0"))
			single_keyword_query_test(args);
		if (args[0].equals("1"))
			insertion_query_test(args);
	}


	public static void single_keyword_query_test(String[] args) throws IOException, NumberFormatException, GeneralSecurityException
	{
		// Parse documents
		List<Document> documents = new ArrayList<Document>();
		Parser.parse_documents(documents, args[2], args[3]);
		System.out.println("Read done.");
		
		// Initialise the client
		Client client = new Client(documents, Integer.parseInt(args[1]));
		System.out.println("Initialise done.");
		
		// Add fake keywords and documents
		client.add_fake_keywords_and_documents(documents);
		System.out.println("Add fake done.");
		System.out.println("Documents count after padding: " + documents.size());
		
		// Save database parameters for later
		Integer keyword_document_pair_count = 0;
		Integer document_count = documents.size();
		for (Document document: documents)
			keyword_document_pair_count += document.get_keywords().size();

		// Database setup
		Timer timer_setup = new Timer();
		HashMap<String, byte[]> EDB = new HashMap<String, byte[]>();
		client.setup(documents, EDB, timer_setup);
		System.out.println("Setup done.");
		documents = null;
		
		
		// Process queries
		Timer timer_query = new Timer();
		Timer timer_write_back = new Timer();
		
		int query_count = 0;
		int keyword_chunk = 0;
		int keyword_chunk_size = client.get_keywords().size();
		if (args.length > 5)
		{
			keyword_chunk = Integer.parseInt(args[5]);
			keyword_chunk_size /= Integer.parseInt(args[6]);
		}
		
		List<String> keyword_list = new ArrayList<String>(client.get_keywords().keySet());
		Collections.sort(keyword_list);
		//SecureRandom random = SecureRandom.getInstanceStrong();
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		for (int idx = keyword_chunk * keyword_chunk_size; idx < (keyword_chunk+1) * keyword_chunk_size && idx < keyword_list.size(); idx++)
		{
			if (query_count % 100 == 0)
			{
				System.out.println("Query progress: " + query_count);
				System.gc();
			}
			query_count++;
			
			client.single_keyword_query(keyword_list.get(idx), random, EDB, null, timer_query, timer_write_back);
		}
		
		print_performance_log1(args, keyword_document_pair_count, document_count, client.get_keywords(), timer_setup, timer_query, timer_write_back);
	}
	
	public static void print_performance_log1(String[] args, int keyword_document_pair_count, int document_count, HashMap<String, Integer> keyword_max, 
			Timer timer_setup, Timer timer_query, Timer timer_write_back)
	{
		try {
			FileWriter writer = new FileWriter(args[4]);
			//writer.write("===================Basic Info===================\n");
			writer.write("CPU: " + Files.lines(Paths.get("/proc/cpuinfo")).filter(line -> line.startsWith("model name")).map(line -> line.replaceAll(".*: ", "")).findFirst().orElse("") + "\n");
			writer.write("Documents = " + document_count + "\n");
			writer.write("Keywords = " + keyword_max.size() + "\n");
			writer.write("KD pairs = " + keyword_document_pair_count + "\n");
			writer.write("Setup in nanoseconds: " + timer_setup.get_total_time() + "\n");
			writer.write("Single keyword query in nanoseconds: " + timer_query.get_total_time() + "\n");
			writer.write("Write-back in nanoseconds: " + timer_write_back.get_total_time() + "\n");
			
			writer.write("===================Keywords Info===================\n");
			int keyword_chunk = 0;
			int keyword_chunk_size = keyword_max.size();
			if (args.length > 5)
			{
				keyword_chunk = Integer.parseInt(args[5]);
				keyword_chunk_size /= Integer.parseInt(args[6]);
			}
			
			List<String> keyword_list = new ArrayList<String>(keyword_max.keySet());
			Collections.sort(keyword_list);
			for (int idx = keyword_chunk * keyword_chunk_size; idx < (keyword_chunk+1) * keyword_chunk_size && idx < keyword_list.size(); idx++)
				writer.write(keyword_list.get(idx) + "," + keyword_max.get(keyword_list.get(idx)) + "," + timer_query.get_time((idx%keyword_chunk_size)) + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void insertion_query_test(String[] args) throws IOException, NumberFormatException, GeneralSecurityException {
		// Parse documents
		List<Document> documents = new ArrayList<Document>();
		Parser.parse_documents(documents, args[2], args[3]);
		System.out.println("Read done.");
				
		// Initialise the client
		Client client = new Client(documents, Integer.parseInt(args[1]));
		System.out.println("Initialise done.");
		
		// Add fake keywords and documents
		client.add_fake_keywords_and_documents(documents);
		System.out.println("Add fake done.");
		System.out.println("Documents count after padding: " + documents.size());
		
		// Save database parameters for later
		Integer keyword_document_pair_count = 0;
		for (Document document: documents)
			keyword_document_pair_count += document.get_keywords().size();

		// Database setup
		Timer timer_setup = new Timer();
		HashMap<String, byte[]> EDB = new HashMap<String, byte[]>();
		client.setup(documents, EDB, timer_setup);
		System.out.println("Setup done.");
		
		// Insertion queries
		Timer timer_query = new Timer();
		List<Integer> query_sizes = new ArrayList<Integer>(); 
		int query_count = 0;
		//SecureRandom random = SecureRandom.getInstanceStrong();
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		int idx_max = 20000;
		if (documents.size() < idx_max)
			idx_max = documents.size();
		int idx_start = Integer.parseInt(args[5]) * idx_max / Integer.parseInt(args[6]);
		int idx_end   = (Integer.parseInt(args[5]) + 1) * idx_max / Integer.parseInt(args[6]);
		
		for (int ii = idx_start; ii < idx_end; ii++)
		{
			if (query_count % 100 == 0)
			{
				System.out.println("Query progress: " + query_count);
				System.gc();
			}
			query_count++;
			int query_size = client.insertion_query(documents.get(ii), random, EDB, documents, timer_query);
			query_sizes.add(query_size);
		}
		
		print_performance_log2(args, query_sizes, timer_query);
	}
	
	public static void print_performance_log2(String[] args, List<Integer> query_sizes, Timer timer_query)
	{
		try {
			FileWriter writer = new FileWriter(args[4]);
			for (int ii = 0; ii < query_sizes.size(); ii++)
				writer.write(query_sizes.get(ii) + "," + timer_query.get_time(ii) + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

}