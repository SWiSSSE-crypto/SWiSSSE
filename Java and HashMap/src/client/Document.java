package client;

import java.util.*;

public class Document {

	// Document = (keywords, real/fake, data, array_location)
	private List<String> keywords;
	private boolean real;
	private String data;
	public String query_keyword;
	
	// Class constructor
	public Document(List<String> keywords, boolean real, String data)
	{
		this.keywords = new ArrayList<String>(keywords);
		this.real = real;
		this.data = data;
	}
	
	// Class constructor for fake document
	public Document(List<String> keywords)
	{
		this.keywords = keywords;
		this.real = false;
		
		int keywords_len = 0;
		for (String keyword: keywords)
			keywords_len += keyword.length() + 4;
		
		this.data = new String(new char[keywords_len]).replace("\0", "A");
	}
	
	// Class constructor from string representation
	public Document(String data_raw, String query_keyword, int document_size)
	{
		String[] data_lines  = data_raw.split("\n");
		this.keywords = new ArrayList<String>(Arrays.asList(data_lines[0].split(",")));
		
		this.real = (data_lines[1].equals("0")) ? false: true;
		if (this.real == true)
		{
			this.data = "";
			for (int ii = 2; ii < data_lines.length - 1; ii++)
				data += data_lines[ii];
		}
		else
		{
			this.data = new String(new char[document_size]).replace("\0", "A");
		}
		this.query_keyword = query_keyword;
	}
	
	
	// Get keywords
	public List<String> get_keywords()
	{
		return this.keywords;
	}
	
	// Set keyword
	public void set_keyword_counter(int index, int counter)
	{
		keywords.set(index, keywords.get(index) + '|' + Integer.toString(counter)) ;
	}
	
	// Check size
	public boolean check_size(int document_size)
	{
		int total_len = 0;
		for (String keyword: this.keywords)
			total_len += keyword.length() + 4;
		total_len += data.length();
		return total_len > document_size;
	}
	
	// Get size
	public int get_size()
	{
		int total_len = 0;
		for (String keyword: this.keywords)
			total_len += keyword.length() + 4;
		total_len += data.length();
		return total_len;
	}
	
	// Get data
	public String get_data()
	{
		return data;
	}
	
	// Set data
	public void set_data(String data)
	{
		this.data = data;
	}
	
	
	// Check if real
	public boolean isReal()
	{
		return this.real;
	}
	
	// Set real
	public void setReal(boolean real)
	{
		this.real = real;
	}
	
	// Split a document into as many pieces as necessary
	public List<Document> split(int document_size)
	{
		List<Document> documents_new = new ArrayList<Document>();
		int keywords_size = 0;
		for (String keyword: this.keywords)
			keywords_size += keyword.length() + 4;
		while (document_size < 4*keywords_size)
			document_size *= 2;
		document_size -= keywords_size;
		
		int ii = 0;
		while (ii*document_size < this.data.length())
		{
			List<String> keywords_new = new ArrayList<String>(this.keywords);
			Document document_new = new Document(keywords_new, this.real, this.data.substring(ii*document_size, Math.min((ii+1)*document_size, this.data.length())));
			documents_new.add(document_new);
			ii += 1;
		}
		return documents_new;
	}
	
	// Put all data in one string
	public String get_string(int document_size)
	{
		String result = "";
		result += String.join(",", keywords) + '\n';
		result += this.real? "1\n": "0\n";
		result += data + '\n';
		
		while (document_size < result.length())
			document_size *= 16;
		int pad_length = document_size - result.length();
		
		result += new String(new char[pad_length]).replace("\0", "A");
		return result;
	}
	
	// Set the keyword used in the query
	public void set_query_keyword(String query_keyword)
	{
		this.query_keyword = query_keyword;
	}
	
	// Check if the keywords are the same
	public boolean is_query_keyword(String keyword)
	{
		return this.query_keyword.equals(keyword);
	}
	
	// Check if the document contains the given keyword
	public boolean contains_keyword(String keyword)
	{
		for (String keyword_with_counter: this.keywords)
			if (keyword_with_counter.split("\\|")[0].equals(keyword) == true)
				return true;
		return false;
	}
	
	// Add keyword
	public void add_keyword(String keyword)
	{
		this.keywords = new ArrayList<>(this.keywords);
		this.keywords.add(keyword);
	}
	
	
	public void print()
	{
		System.out.println(Arrays.toString(keywords.toArray(new String[0])));
		//System.out.println(real);
		//System.out.println(data);
	}

	/* method to remove counters from the keywords
	 * 
	 */
	public void clean_keywords() 
	{
		List<String> keywords_new = new ArrayList<String>();
		for (int ii = 0; ii < keywords.size(); ii++)
			keywords_new.add(keywords.get(ii).split("\\|")[0]);
		this.keywords = keywords_new;
	}

	/* method to add keyword counters
	*/
	public void add_keyword_counter(HashMap<String, Integer> keyword_max, HashMap<String, Integer> keyword_insert_counter) 
	{
		List<String> keywords_new = new ArrayList<String>();
		for (String keyword: keywords)
		{
			keyword_insert_counter.put(keyword, keyword_insert_counter.get(keyword)+1);
			int counter = keyword_max.get(keyword) + keyword_insert_counter.get(keyword);
			keywords_new.add(keyword + "|" + counter);
		}
		this.keywords = keywords_new;
	}
}
