package parser;

import java.io.*;
import java.util.*;

import client.Document;

public class Parser {
	public static void parse_documents(List<Document> documents, HashMap<String, String> configuration) throws IOException
	{
		Integer documents_max = Integer.parseInt(configuration.get("N_emails"));
		if (documents_max < 0)
			documents_max = Integer.MAX_VALUE;
		Integer document_count = 0;
		
		
		File folder = new File(configuration.get("folder_emails"));
		File[] listOfFiles = folder.listFiles();
		
		for (File file: listOfFiles) {
			FileReader fp = new FileReader(file);
			BufferedReader document_reader = new BufferedReader(fp);
			
			String[] keywords = document_reader.readLine().split(",");
			String main_body = "";
			String next_line = document_reader.readLine();
			while (next_line != null) {
				main_body += next_line + "\n";
				next_line = document_reader.readLine();
			}
			
			Document document = new Document(keywords, main_body);
			documents.add(document);
			
			fp.close();
			document_reader.close();
			
			document_count += 1;
			
			if (document_count >= documents_max)
				break;
			
		}
	}
}
