package parser;

import java.io.*;
import java.util.*;

import server.Document;

public class Parser {
	public static void parse_documents(List<Document> documents, String path, String documents_max_string) throws IOException
	{
		Integer documents_max = Integer.parseInt(documents_max_string);
		if (documents_max < 0)
			documents_max = Integer.MAX_VALUE;
		Integer document_count = 0;
		
		
		FileReader fp = new FileReader(path);
		BufferedReader document_reader = new BufferedReader(fp);
		
		List<String> keywords = new ArrayList<String>();
		String data = "";
		
		String next_line = document_reader.readLine();
		while (next_line != null)
		{
			if (document_count >= documents_max)
				break;
			
			// Case 1: create file
			if (next_line.equals("NEW_FILE"))
			{				
				if ((data.length() > 0) && (document_count < documents_max))
				{
					Document document = new Document(keywords, data);
					documents.add(document);
					document_count++;
				}
				
				
				keywords = Arrays.asList(document_reader.readLine().split(","));
				data = document_reader.readLine() + '\n';
			}
			else 
			{
				data += next_line + '\n';
			}
			
			next_line = document_reader.readLine();
		}
		
		if (document_count < documents_max)
		{
			Document document = new Document(keywords, data);
			documents.add(document);
		}

		document_reader.close();
	}
}
