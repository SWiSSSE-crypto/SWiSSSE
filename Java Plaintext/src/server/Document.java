package server;

import java.util.*;

public class Document {

	// Document = (keywords, real/fake, data, array_location)
	private List<String> keywords;
	private String data;
	
	// Class constructor
	public Document(List<String> keywords, String data)
	{
		this.keywords = new ArrayList<String>(keywords);
		this.data = data;
	}
	
	// Get keywords
	public List<String> get_keywords()
	{
		return this.keywords;
	}

	
	// Get data
	public String get_data()
	{
		return data;
	}
}
