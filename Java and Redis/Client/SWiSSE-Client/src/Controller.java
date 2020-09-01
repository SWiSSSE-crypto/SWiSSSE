import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

import client.*;
import client.Timer;
import parser.*;

public class Controller {

	static String mode;
	static String IP;
	static String grouping_mode;
	static String input_folder;
	static String documents_max;
	static String output_folder;
	static Integer index;
	static Integer total_index;
	
	public static void main(String[] args) throws NumberFormatException, IOException, GeneralSecurityException {
		// parse input
		// Sample input: 0 127.0.0.1 0 G:\Enron\database 5000 G:\Enron\performance_log 0 8
		if (args.length == 8)
		{
			mode = args[0];
			IP = args[1];
			grouping_mode = args[2];
			input_folder = args[3];
			documents_max = args[4];
			output_folder = args[5];
			index = Integer.valueOf(args[6]);
			total_index = Integer.valueOf(args[7]);
		}
		
		
		// Sample input: 0 localhost G:\Enron\database 5000 G:\Enron\performance_log 10
		if (args.length == 6)
		{
			mode = args[0];
			IP = args[1];
			grouping_mode = "1";
			input_folder = args[2];
			documents_max = args[3];
			output_folder = args[4];
			index = Integer.valueOf(args[5]);
		}
		
		if (args.length == 4)
		{
			mode = args[0];
			IP = args[1];
			grouping_mode = "1";
			input_folder = args[2];
			documents_max = "0";
			output_folder = args[3];
			index = 1000;
		}
		
		if (mode.equals("0"))
			single_keyword_query_test();
		if (mode.equals("1"))
			insertion_query_test();
		if (mode.equals("2"))
			single_keyword_query_test2();
		if (mode.equals("3"))
			insertion_query_test2();
	}
	
	
	
	public static void single_keyword_query_test() throws IOException, NumberFormatException, GeneralSecurityException
	{
		@SuppressWarnings("resource")
		Socket socket = new Socket(IP, 59090);
		socket.setTcpNoDelay(true);
		DataInputStream input_stream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		DataOutputStream output_stream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		
		// Parse documents
		List<Document> documents = new ArrayList<Document>();
		Parser.parse_documents(documents, input_folder, documents_max);
		System.out.println("Read done.");
		
		// Initialise the client
		Client client = new Client(documents, grouping_mode);
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
		client.setup(documents, input_stream, output_stream, timer_setup);
		System.out.println("Setup done.");
		documents = null;
		
		
		// Process queries
		Timer timer_query = new Timer();
		Timer timer_write_back = new Timer();
		
		int query_count = 0;
		
		List<String> keyword_list = new ArrayList<String>(client.get_keywords().keySet());
		Collections.sort(keyword_list);
		//SecureRandom random = SecureRandom.getInstanceStrong();
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		for (int idx = 0; idx < index && idx < keyword_list.size(); idx++)
		{
			if (query_count % 100 == 0)
				System.out.println("Query progress: " + query_count);
			query_count++;
			
			client.single_keyword_query(keyword_list.get(idx), random, timer_query, timer_write_back);
		}
		output_stream.writeInt(3);
		output_stream.flush();
		
		print_performance_log1(keyword_document_pair_count, document_count, client.get_keywords(), timer_setup, timer_query, timer_write_back);
	}
	
	public static void print_performance_log1(int keyword_document_pair_count, int document_count, HashMap<String, Integer> keyword_max, 
			Timer timer_setup, Timer timer_query, Timer timer_write_back)
	{
		try {
			FileWriter writer = new FileWriter(output_folder+ '/' + documents_max);
			//writer.write("===================Basic Info===================\n");
			//writer.write("CPU: " + Files.lines(Paths.get("/proc/cpuinfo")).filter(line -> line.startsWith("model name")).map(line -> line.replaceAll(".*: ", "")).findFirst().orElse("") + "\n");
			writer.write("Documents = " + document_count + "\n");
			writer.write("Keywords = " + keyword_max.size() + "\n");
			writer.write("KD pairs = " + keyword_document_pair_count + "\n");
			writer.write("Setup in nanoseconds: " + timer_setup.get_total_time() + "\n");
			writer.write("Single keyword query in nanoseconds: " + timer_query.get_total_time() + "\n");
			writer.write("Write-back in nanoseconds: " + timer_write_back.get_total_time() + "\n");
			
			writer.write("===================Keywords Info===================\n");
			int keyword_chunk_size = index;
			
			List<String> keyword_list = new ArrayList<String>(keyword_max.keySet());
			Collections.sort(keyword_list);
			for (int idx = 0; idx < keyword_chunk_size; idx++)
				writer.write(keyword_list.get(idx) + "," + keyword_max.get(keyword_list.get(idx)) + "," + timer_query.get_time(2*idx) + "," + timer_query.get_time(2*idx+1) + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void insertion_query_test() throws IOException, NumberFormatException, GeneralSecurityException {
		@SuppressWarnings("resource")
		Socket socket = new Socket(IP, 59090);
		socket.setTcpNoDelay(true);
		DataInputStream input_stream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		DataOutputStream output_stream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		
		// Parse documents
		List<Document> documents = new ArrayList<Document>();
		Parser.parse_documents(documents, input_folder, documents_max);
		System.out.println("Read done.");
				
		// Initialise the client
		Client client = new Client(documents, grouping_mode);
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
		client.setup(documents, input_stream, output_stream, timer_setup);
		System.out.println("Setup done.");
		
		// Insertion queries
		Timer timer_query = new Timer();
		List<Integer> query_sizes = new ArrayList<Integer>(); 
		int query_count = 0;
		//SecureRandom random = SecureRandom.getInstanceStrong();
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		
		for (int ii = 0; ii < index; ii++)
		{
			if (query_count % 100 == 0)
				System.out.println("Query progress: " + query_count);
			query_count++;
			int query_size = client.insertion_query(documents.get(ii), random, timer_query);
			query_sizes.add(query_size);
		}
		output_stream.writeInt(3);
		output_stream.flush();
		
		print_performance_log2(query_sizes, timer_query);
	}
	
	public static void print_performance_log2(List<Integer> query_sizes, Timer timer_query)
	{
		try {
			FileWriter writer = new FileWriter(output_folder + '/' + documents_max);
			for (int ii = 0; ii < query_sizes.size(); ii++)
				writer.write(query_sizes.get(ii) + "," + timer_query.get_time(2*ii) + "," + timer_query.get_time(2*ii+1) + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	
	public static void single_keyword_query_test2() throws IOException, NumberFormatException, GeneralSecurityException
	{
		@SuppressWarnings("resource")
		Socket socket = new Socket(IP, 59090);
		socket.setTcpNoDelay(true);
		DataInputStream input_stream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		DataOutputStream output_stream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		
		String[] documents_maxs = {"10000", "50000", "100000", "200000", "480000"};
		//String[] documents_maxs = {"1000", "1001", "1002", "1003", "1004"};
		for (int ii = 0; ii < 5; ii++)
		{
			// Parse documents
			documents_max = documents_maxs[ii];
			List<Document> documents = new ArrayList<Document>();
			Parser.parse_documents(documents, input_folder, documents_max);
			System.out.println("Read done.");
			
			// Initialise the client
			Client client = new Client(documents, grouping_mode);
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
			client.setup(documents, input_stream, output_stream, timer_setup);
			System.out.println("Setup done.");
			documents = null;
			
			
			// Process queries
			Timer timer_query = new Timer();
			Timer timer_write_back = new Timer();
			
			int query_count = 0;
			int keyword_chunk_size = index;
			
			List<String> keyword_list = new ArrayList<String>(client.get_keywords().keySet());
			Collections.sort(keyword_list);
			//SecureRandom random = SecureRandom.getInstanceStrong();
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			for (int idx = 0; idx < keyword_chunk_size; idx++)
			{
				if (query_count % 100 == 0)
					System.out.println("Query progress: " + query_count);
				query_count++;
				
				client.single_keyword_query(keyword_list.get(idx), random, timer_query, timer_write_back);
			}
			client.close();
			
			print_performance_log1(keyword_document_pair_count, document_count, client.get_keywords(), timer_setup, timer_query, timer_write_back);
		}
		
		output_stream.writeInt(3);
		output_stream.flush();
		
	}
	
	
	private static void insertion_query_test2() throws IOException, NumberFormatException, GeneralSecurityException {
		@SuppressWarnings("resource")
		Socket socket = new Socket(IP, 59090);
		socket.setTcpNoDelay(true);
		DataInputStream input_stream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		DataOutputStream output_stream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		
		String[] documents_maxs = {"10000", "50000", "100000", "200000", "480000"};
		//String[] documents_maxs = {"1000", "1001", "1002", "1003", "1004"};
		
		for (int ii = 0; ii < 5; ii++)
		{
			documents_max = documents_maxs[ii];
			// Parse documents
			List<Document> documents = new ArrayList<Document>();
			Parser.parse_documents(documents, input_folder, documents_max);
			System.out.println("Read done.");
					
			// Initialise the client
			Client client = new Client(documents, grouping_mode);
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
			client.setup(documents, input_stream, output_stream, timer_setup);
			System.out.println("Setup done.");
			
			// Insertion queries
			Timer timer_query = new Timer();
			List<Integer> query_sizes = new ArrayList<Integer>(); 
			int query_count = 0;
			//SecureRandom random = SecureRandom.getInstanceStrong();
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			for (int idx = 0; idx < index; idx++)
			{
				if (query_count % 100 == 0)
					System.out.println("Query progress: " + query_count);
				query_count++;
				int query_size = client.insertion_query(documents.get(idx), random, timer_query);
				query_sizes.add(query_size);
			}
			client.close();
			
			print_performance_log2(query_sizes, timer_query);
		}
		
		output_stream.writeInt(3);
		output_stream.flush();
	}
}
