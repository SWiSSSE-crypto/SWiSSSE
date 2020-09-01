package server;

import java.util.*;


public class Server {
	
	private Hashtable<String, Integer> keywords = new Hashtable<String, Integer>();
	private Hashtable<String, List<Integer>> look_index = new Hashtable<String, List<Integer>>();
	private List<Document> documents = new ArrayList<Document>();

	/* Class constructor
	 */
	public Server(List<Document> documents, Timer timer_setup) 
	{
		timer_setup.start();
		
		for (int idx = 0; idx < documents.size(); idx++)
		{
			for (String keyword: documents.get(idx).get_keywords())
			{
				if (keywords.containsKey(keyword) == false)
					keywords.put(keyword, 1);
				else
					keywords.put(keyword, keywords.get(keyword)+1);
				
				if (look_index.containsKey(keyword) == false)
				{
					List<Integer> index = new ArrayList<Integer>();
					index.add(idx);
					look_index.put(keyword, index);
				}
				else
				{
					List<Integer> index = look_index.get(keyword);
					index.add(idx);
					look_index.put(keyword, index);
				}
			}
		}
		
		this.documents = documents;
		timer_setup.stop();	
	}
	

	/* Single keyword query
	 */
	public void single_keyword_query(String keyword, Timer timer_query)
	{
		timer_query.start();
		List<Integer> addresses = this.look_index.get(keyword);
		List<Document> response = new ArrayList<Document>();
		for (Integer address: addresses)
			response.add(this.documents.get(address));
		timer_query.stop();
		addresses = null;
		response = null;
	}
	
	
	
	/* Function to get all the keywords in the database
	 */
	public Hashtable<String, Integer> get_keywords()
	{
		return this.keywords;
	}
}
