import java.io.*;
import java.util.*;
import parser.*;
import server.*;
import server.Timer;


public class Controller {

	public static void main(String[] args) throws IOException {
		// Parse documents
		List<Document> documents = new ArrayList<Document>();
		Parser.parse_documents(documents, args[0], args[1]);
		System.out.println("Read done.");
		
		// Initialise the database
		Timer timer_setup = new Timer();
		Server server = new Server(documents, timer_setup);
		System.out.println("Setup done.");
		
		// Executing the queries
		List<String> keywords = new ArrayList<String>(server.get_keywords().keySet());
		Collections.sort(keywords);
		int chunk_idx = Integer.parseInt(args[3]);
		int chunk_size = keywords.size() / Integer.parseInt(args[4]);
		Timer timer_query = new Timer();
		for (int idx = chunk_idx * chunk_size; idx < (chunk_idx + 1) * chunk_size; idx++)
		{
			server.single_keyword_query(keywords.get(idx), timer_query);
		}
		
		print_performance_log(args, keywords, server.get_keywords(), timer_setup, timer_query);
	}

	private static void print_performance_log(String[] args, List<String> keywords, Hashtable<String, Integer> keywords_counter, Timer timer_setup, Timer timer_query) throws IOException 
	{
		FileWriter writer = new FileWriter(args[2]);
		writer.write("===================Basic Info===================\n");
		writer.write("Setup in nanoseconds: " + timer_setup.get_total_time() + "\n");
		writer.write("Single keyword query in nanoseconds: " + timer_query.get_total_time() + "\n");
		
		writer.write("===================Keywords Info===================\n");
		int chunk_idx = Integer.parseInt(args[3]);
		int chunk_size = keywords.size() / Integer.parseInt(args[4]);
		for (int idx = chunk_idx * chunk_size; idx < (chunk_idx + 1) * chunk_size; idx++)
			writer.write(keywords.get(idx) + "," + keywords_counter.get(keywords.get(idx)) + "," + timer_query.get_time((idx%chunk_size)) + "\n");
		writer.close();
	}

}
