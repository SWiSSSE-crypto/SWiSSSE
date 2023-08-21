package client;

import java.util.*;

public class Document {

	// Document = (keywords, real/fake, data, array_location)
	private String[] keywords;
	private String data;
	
	// Class constructor
	public Document(String[] keywords, String data)
	{
		this.keywords = keywords;
		this.data = data;
	}
	
	// Get keywords
	public String[] get_keywords()
	{
		return this.keywords;
	}

	
	// Get data
	public String get_data()
	{
		return data;
	}
}
