package client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import crypto.Crypto;


public class Client {
	private static final int DOCUMENT_SIZE = 1024;
	private static double Document_Expansion = 1.3;
	
	// Counters
	private SecretKey key = null;
	SecretKeySpec signingKey = null;
	private int[] array_counter = null;
	private HashMap<String, Integer> keyword_count = new HashMap<String, Integer>();
	private HashMap<String, Integer> keyword_max = new HashMap<String, Integer>();
	private HashMap<String, Integer> keyword_query_counter = new HashMap<String, Integer>();
	private HashMap<String, Integer> keyword_insert_counter = new HashMap<String, Integer>();
	
	// stash
	private int document_identifier_counter = 0;
	private HashMap<String, Index> lookup_stash1 = new HashMap<String, Index>(); // for documents that have already been uploaded
	private HashMap<String, Integer> lookup_stash2 = new HashMap<String, Integer>();  // for documents stored locally
	private Set<Integer> array_loc_stash = new HashSet<Integer>();
	private HashMap<Integer, Document> document_stash = new HashMap<Integer, Document>();

	// Network
	private DataInputStream input_stream;
	private DataOutputStream output_stream;
	
	/* Class constructor
	 */
	public Client(HashMap<String, String> configuration) throws NumberFormatException, UnknownHostException, IOException {
		@SuppressWarnings("resource")
		Socket socket = new Socket("localhost", Integer.parseInt(configuration.get("port")));
		socket.setTcpNoDelay(true);
		this.input_stream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		this.output_stream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		
	}
	
	/*
	 * Function to initialise the client with cryptographic keys
	 * It also performs document splitting and prepare for keyword bucketization
	 */
	public void initialise(List<Document> documents, HashMap<String, String> configuration) throws GeneralSecurityException
	{
		// Key generation
		this.key = Crypto.key_gen();
		this.signingKey = new SecretKeySpec(key.getEncoded(), "HmacSHA256");
		
		// Split the documents
		int index = 0;
		int index_max = documents.size();
		while (index < index_max)
		{
			if (documents.get(index).check_size(DOCUMENT_SIZE) == true)
			{
				Document document = documents.get(index);
				documents.remove(index);
				List<Document> documents_new = document.split(DOCUMENT_SIZE);
				documents.addAll(documents_new);
				index_max -= 1;
			}
			else
			{
				index += 1;
			}
		}
		
		Hashtable<String, Integer> keyword_current = new Hashtable<String, Integer>();
		for (Document document: documents)
		{
			List<String> keywords = document.get_keywords();
			
			for (int ii = 0; ii < keywords.size(); ii++)
			{
				// Initialse keyword_count
				if (keyword_count.containsKey(keywords.get(ii)))
					keyword_count.put(keywords.get(ii), keyword_count.get(keywords.get(ii))+1);
				else
					keyword_count.put(keywords.get(ii), 1);
				
				// Initialise keyword_query_counter and keyword_insert_counter
				if (keyword_query_counter.containsKey(keywords.get(ii)) == false)
					keyword_query_counter.put(keywords.get(ii), 0);
					
				if (keyword_insert_counter.containsKey(keywords.get(ii)) == false)
					keyword_insert_counter.put(keywords.get(ii), 0);
				
				// Modify keyword with counter
				if (keyword_current.containsKey(keywords.get(ii)))
					keyword_current.put(keywords.get(ii), keyword_current.get(keywords.get(ii))+1);
				else
					keyword_current.put(keywords.get(ii), 0);
				document.set_keyword_counter(ii, keyword_current.get(keywords.get(ii)));
			}
		}
		
		
		
		
		// grouping with sorting
		int Keyword_Expansion = 1;
		if (configuration.get("mode").equals("insert"))
			Keyword_Expansion = 2;
		List<String> keywords = Arrays.asList(keyword_count.keySet().toArray(new String[0]));
		Collections.sort(keywords, new KeywordsComparator(keyword_count));
		Integer group_size_large = Integer.parseInt(configuration.get("Group_size_large"));
		Integer offset = group_size_large * 6;
		Integer group_size_small = Integer.parseInt(configuration.get("Group_size_small"));
		for (int ii = 0; ii < 6; ii++)
		{
			int count_max = keyword_count.get(keywords.get((int) (ii*group_size_large)));
			for (int jj = ii*group_size_large; (jj < (ii+1) * group_size_large) & (jj < keywords.size()); jj++)
				count_max = (keyword_count.get(keywords.get(jj)) > count_max) ? (keyword_count.get(keywords.get(jj))): count_max;
				
			for (int jj = ii*group_size_large; (jj < (ii+1) * group_size_large) & (jj < keywords.size()); jj++)
				keyword_max.put(keywords.get(jj), count_max * Keyword_Expansion);
		}
		
		for (int ii = 0; ii < Math.ceil((keywords.size() - offset) / group_size_small); ii++)
		{
			int count_max = keyword_count.get(keywords.get(offset + ii*group_size_small));
			for (int jj = offset + ii*group_size_small; (jj < (ii+1) * offset + (ii+1)*group_size_small) & (jj < keywords.size()); jj++)
				count_max = (keyword_count.get(keywords.get(jj)) > count_max) ? (keyword_count.get(keywords.get(jj))): count_max;
				
			for (int jj = offset + ii*group_size_small; (jj < (ii+1) * offset + (ii+1)*group_size_small) & (jj < keywords.size()); jj++)
				keyword_max.put(keywords.get(jj), count_max * Keyword_Expansion);
		}
		
		this.array_counter = new int[(int) (documents.size()*Document_Expansion)];
		Arrays.fill(this.array_counter, 0);
		document_identifier_counter = (int) (documents.size()*Document_Expansion);
	}
	
	
	/* 
	 * Pad the database with fake keywords and fake documents
	 */
	public void add_fake_keywords_and_documents(List<Document> documents)
	{
		int keyword_sum = 0;
		Hashtable<String, Integer> keyword_pad = new Hashtable<String, Integer>();
		for (String keyword: keyword_count.keySet())
		{
			keyword_sum += keyword_max.get(keyword) - keyword_count.get(keyword);
			keyword_pad.put(keyword, keyword_max.get(keyword) - keyword_count.get(keyword));
		}
		
		
		int document_remain = array_counter.length - documents.size();
		int keyword_average = (int) keyword_sum / document_remain;
		List<String> keyword_set = new ArrayList<String>();
		keyword_set.addAll(keyword_count.keySet());
		
		Random random = new Random();
		while (document_remain > 0)
		{	
			// Get keywords for the fake document
			Set<String> keywords = new HashSet<String>();
			while ((keywords.size() < keyword_average) && (keyword_set.isEmpty() == false) && (keywords.size() < keyword_set.size() * 0.5))
			{
				int index = random.nextInt(keyword_set.size());
				if (keyword_pad.get(keyword_set.get(index)) == 0)
					keyword_set.remove(index);
				else if (keywords.contains(keyword_set.get(index)) == false)
				{
					Integer previous_val = keyword_pad.get(keyword_set.get(index));
					keyword_pad.put(keyword_set.get(index), previous_val-1);
					keywords.add(keyword_set.get(index));
				}
			}
			
			// Modify the keywords
			List<String> keywords_list = new ArrayList<String>(keywords);
			for (int ii = 0; ii < keywords.size(); ii++)
			{
				String keyword_temp = keywords_list.get(ii);
				keywords_list.set(ii, keyword_temp + '|' + keyword_count.get(keyword_temp));
				keyword_count.put(keyword_temp, keyword_count.get(keyword_temp)+1);
			}
			document_remain -= 1;
			
			// Get fake document
			Document document_fake = new Document(keywords_list);
			documents.add(document_fake);
		}
		
		// Add the keywords to random documents
		for (String keyword_add: keyword_set)
		{
			List<Integer> documents_index = new ArrayList<Integer>();
			for (int document_index = 0; document_index < documents.size(); document_index++)
				if (documents.get(document_index).contains_keyword(keyword_add) == false)
					documents_index.add(document_index);
			Collections.shuffle(documents_index);
			
			int counter = 0;
			while (keyword_pad.get(keyword_add) > 0)
			{
				int index = documents_index.get(counter);
				counter++;
				
				String keyword_with_counter = keyword_add + '|' + keyword_count.get(keyword_add);
				documents.get(index).add_keyword(keyword_with_counter);
				keyword_count.put(keyword_add, keyword_count.get(keyword_add)+1);
				keyword_pad.put(keyword_add, keyword_pad.get(keyword_add)-1);
			}
		}
	}
	
	
	
	/* setup database
	 */
	public void setup(List<Document> documents, HashMap<String, String> configuration, Timer timer_setup) throws GeneralSecurityException, UnknownHostException, IOException
	{
		timer_setup.start();
		
		//SecureRandom random = SecureRandom.getInstanceStrong();
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		output_stream.writeInt(1);
		
		for (int array_loc = 0; array_loc < documents.size(); array_loc++)
		{
			// document address and encryption
			byte[] doc_address = Crypto.SHA256(this.signingKey, Integer.toString(array_loc) + '-' + Integer.toString(array_counter[array_loc]));
			
			String document_data = documents.get(array_loc).get_string(DOCUMENT_SIZE);
			
			byte[] document_data_enc = Crypto.GCM_encrypt(key, random, document_data);
			output_stream.writeInt(doc_address.length);
			output_stream.write(doc_address);
			output_stream.writeInt(document_data_enc.length);
			output_stream.write(document_data_enc);

			
			// lookup address and encryption
			for (String keyword: documents.get(array_loc).get_keywords())
			{
				byte[] lookup_address = Crypto.SHA256(this.signingKey, keyword + "-0"); 
				byte[] lookup_data_enc = Crypto.GCM_encrypt(key, random, Integer.toString(array_loc));
				output_stream.writeInt(lookup_address.length);
				output_stream.write(lookup_address);
				output_stream.writeInt(lookup_data_enc.length);
				output_stream.write(lookup_data_enc);
			}
		}
		
		
		output_stream.writeInt(-1);
		output_stream.flush();
		input_stream.readInt();
		timer_setup.stop();
	}
	
	
	/* Single keyword query
	 */
	public void single_keyword_query(String keyword, SecureRandom random, Timer timer_query, Timer timer_write_back, StashMonitor stashMonitor) throws GeneralSecurityException, IOException
	{
		timer_query.start();
		Hashtable<String, String> keyword_to_document_address = new Hashtable<String, String>();
		get_document_addresses(keyword, keyword_to_document_address);

		Hashtable<String, Document> keyword_to_document = new Hashtable<String, Document>();
		get_documents_from_addresses(keyword_to_document_address, keyword_to_document, timer_query);
		timer_query.stop();
		
		if (keyword_insert_counter.get(keyword) > 0) 
			merge_query_results(keyword, keyword_to_document);
		keyword_insert_counter.put(keyword, 0);
		flush_to_stash(keyword_to_document);
		
		keyword_to_document_address = null;
		keyword_to_document = null;
		
		timer_write_back.start();
		write_back(random);
		timer_write_back.stop();
		
		Integer stash_size = get_stash_size();
		stashMonitor.add(stash_size);
	}
	
	
	/* Single keyword query
	 */
	public Integer insertion_query(Document document, SecureRandom random, Timer timer_query, Timer timer_write_back, StashMonitor stashMonitor) throws GeneralSecurityException, IOException
	{
		document.clean_keywords();
		timer_query.start();
		String keyword = document.get_keywords().get(0);
		for (String keyword_cand: document.get_keywords())
		{
			if (keyword_max.get(keyword_cand) < keyword_max.get(keyword))
				keyword = keyword_cand;
		}
		
		Hashtable<String, String> keyword_to_document_address = new Hashtable<String, String>();
		get_document_addresses(keyword, keyword_to_document_address);
		
		//System.out.println("Address obtained");
		
		Hashtable<String, Document> keyword_to_document = new Hashtable<String, Document>();
		get_documents_from_addresses(keyword_to_document_address, keyword_to_document, timer_query);
		timer_query.stop();
		
		//System.out.println("Documents obtained");
		
		if (keyword_insert_counter.get(keyword) > 0) 
			merge_query_results(keyword, keyword_to_document);
		keyword_insert_counter.put(keyword, 0);
		flush_to_stash(keyword_to_document, document);
		
		//System.out.println("Stash flushed");
		
		timer_write_back.start();
		write_back(random);
		timer_write_back.stop();
		
		Integer stash_size = get_stash_size();
		stashMonitor.add(stash_size);
		
		//System.out.println("Write back done");
		
		return keyword_max.get(keyword);
		
		
	}
	


	/* Get document addresses and shuffle them
	 */
	private void get_document_addresses(String keyword, Hashtable<String, String> keyword_to_document_address) throws GeneralSecurityException, IOException
	{	
		List<String> lookup_addresses_raw = new ArrayList<String>();
		for (int ii = 0; ii < keyword_max.get(keyword); ii++)
		{
			// Get the addresses for the initial insertions
			String address_raw = keyword + "|" + Integer.toString(ii);
			address_raw += "-" + Integer.toString(keyword_query_counter.get(keyword));
			lookup_addresses_raw.add(address_raw);
		}
		
		// Get the addresses for the write-backs and document insertions
		for (int ii = 0; ii < keyword_max.get(keyword) + keyword_insert_counter.get(keyword); ii++)
		{
			String address_raw = keyword + "|" + Integer.toString(ii);
			address_raw += "-" + Integer.toString(keyword_query_counter.get(keyword)+1);
			lookup_addresses_raw.add(address_raw);
		}
		
		// Shuffle and compute the addresses
		// Send the addresses to the server
		//Collections.shuffle(lookup_addresses_raw);
		output_stream.writeInt(0);
		for (String address_raw: lookup_addresses_raw)
		{
			byte[] address = Crypto.SHA256(this.signingKey, address_raw);
			output_stream.writeInt(address.length);
			output_stream.write(address);
		}
		output_stream.writeInt(-1);
		output_stream.flush();

		
		// Rolling the counters forward
		keyword_query_counter.put(keyword, keyword_query_counter.get(keyword)+2);
		keyword_insert_counter.put(keyword, 0);
		
		List<byte[]> document_addresses_enc = new ArrayList<byte[]>();
		for (int ii = 0; ii < lookup_addresses_raw.size(); ii++)
		{
			int size = input_stream.readInt();
			byte[] address_enc = new byte[size];
			input_stream.readFully(address_enc);
			document_addresses_enc.add(address_enc);
		}
			
		// Decrypt the addresses
		List<String> document_addresses_dec = new ArrayList<String>();
		for (byte[] document_address_enc: document_addresses_enc)
		{
			if (document_address_enc.length == 1)
				document_addresses_dec.add(null);
			else
				document_addresses_dec.add(Crypto.GCM_decryption(key, document_address_enc));
		}
		
		// Compute the document addresses to retrieve w/ noise
		update_document_addresses_stash(keyword, lookup_addresses_raw, document_addresses_dec, keyword_to_document_address);
	}

	
	/* Processed the retrieved document addresses by using the newest address
	 * The document addresses in the stash are used in the function too
	 * Equal number of random addresses are added
	 */ 
	private void update_document_addresses_stash(String keyword, List<String> lookup_addresses_raw, List<String> document_addresses_dec, Hashtable<String, String> keyword_to_document_address) 
	{
		for (int ii = 0; ii < lookup_addresses_raw.size(); ii++)
		{
			String[] keyword_and_counter = lookup_addresses_raw.get(ii).split("-");
			
			// Case 1:  document uploaded but lookup index is not updated
			if (lookup_stash1.containsKey(keyword_and_counter[0]))
				keyword_to_document_address.put(keyword_and_counter[0], lookup_stash1.get(keyword_and_counter[0]).get_location());
			// Case 2: document in the stash and lookup index is not updated
			else if (lookup_stash2.containsKey(keyword_and_counter[0]))
				keyword_to_document_address.put(keyword_and_counter[0], Integer.toString(lookup_stash2.get(keyword_and_counter[0])));
			// Case 3: create address if it does not exist yet
			else if (Integer.parseInt(keyword_and_counter[1]) % 2 == 0 && keyword_to_document_address.containsKey(keyword_and_counter[0]) == false && document_addresses_dec.get(ii) != null)
				keyword_to_document_address.put(keyword_and_counter[0], document_addresses_dec.get(ii));
			// Case 4: use update address if it is not null
			else if (Integer.parseInt(keyword_and_counter[1]) % 2 == 1 && document_addresses_dec.get(ii) != null)
				keyword_to_document_address.put(keyword_and_counter[0], document_addresses_dec.get(ii));
				
		}
	}
	
	/* Get documents with the document addresses 
	 */
	private void get_documents_from_addresses(Hashtable<String, String> keyword_to_document_address, Hashtable<String, Document> keyword_to_document, Timer timer_query) throws GeneralSecurityException, IOException
	{
		int void_counter = 0;
		int next_address = 0;
		int iter_counter = 0;
		Set<String> document_addresses_set = new HashSet<String>(keyword_to_document_address.values());
		Random random = new Random();
		
		Hashtable<String, String> keyword_to_document_address_add = new Hashtable<String, String>();
		for (String keyword: keyword_to_document_address.keySet())
		{
			iter_counter = 0;
			
			// Generate one fake address for a real address
			if (Integer.parseInt(keyword_to_document_address.get(keyword)) < this.array_counter.length)
			{
				next_address = random.nextInt(this.array_counter.length);
				while (document_addresses_set.contains(Integer.toString(next_address)) || array_loc_stash.contains(next_address) || iter_counter < 100) {
					iter_counter += 1;
					next_address = random.nextInt(this.array_counter.length);
				}
					
				if (iter_counter < 100) {
					document_addresses_set.add(Integer.toString(next_address));
					keyword_to_document_address_add.put("void" + Integer.toString(void_counter), Integer.toString(next_address));
					void_counter += 1;
				}
				//else {
				//	System.out.println("Slow while loop");
				//}
				
			}
			// Generate two fake addresses for a local document
			else
			{
				next_address = random.nextInt(this.array_counter.length);
				while (document_addresses_set.contains(Integer.toString(next_address)) || array_loc_stash.contains(next_address) || iter_counter < 100) {
					iter_counter += 1;
					next_address = random.nextInt(this.array_counter.length);
				}
					
				if (iter_counter < 100) {
					document_addresses_set.add(Integer.toString(next_address));
					keyword_to_document_address_add.put("void" + Integer.toString(void_counter), Integer.toString(next_address));
					void_counter += 1;
				}
				//else {
				//	System.out.println("Slow while loop");
				//}
				
				iter_counter = 0;
				next_address = random.nextInt(this.array_counter.length);
				while (document_addresses_set.contains(Integer.toString(next_address)) || array_loc_stash.contains(next_address) || iter_counter < 100) {
					iter_counter += 1;
					next_address = random.nextInt(this.array_counter.length);
				}
				
				if (iter_counter < 100) {
					document_addresses_set.add(Integer.toString(next_address));
					keyword_to_document_address_add.put("void" + Integer.toString(void_counter), Integer.toString(next_address));
					void_counter += 1;
				}
				//else {
				//	System.out.println("Slow while loop");
				//}
			}
		}
		keyword_to_document_address.putAll(keyword_to_document_address_add);
		
		
		// Compute the addresses and retrieve the documents
		List<String> keywords_list = new ArrayList<String>(keyword_to_document_address.keySet());
		Collections.shuffle(keywords_list);
		
		Integer counter = 0;
		output_stream.writeInt(0);
		for (String keyword: keywords_list)
		{
			// Only use the addresses for the documents to be fetched
			if (Integer.parseInt(keyword_to_document_address.get(keyword)) < this.array_counter.length)
			{
				String address = keyword_to_document_address.get(keyword) + "-" + Integer.toString(this.array_counter[Integer.parseInt(keyword_to_document_address.get(keyword))]);
				byte[] document_address = Crypto.SHA256(this.signingKey, address);
				output_stream.writeInt(document_address.length);
				output_stream.write(document_address);
				counter++;
				
				array_loc_stash.add(Integer.parseInt(keyword_to_document_address.get(keyword))); // add document array location to stash
				this.array_counter[Integer.parseInt(keyword_to_document_address.get(keyword))] += 1;
			}
		}
		output_stream.writeInt(-1);
		output_stream.flush();
		
		// Retrieve encrypted documents and delete content from EDB
		List<byte[]> documents_enc = new ArrayList<byte[]>(); 
		for (int ii = 0; ii < counter; ii++)
		{
			int size = input_stream.readInt();
			byte[] document_enc = new byte[size];
			input_stream.readFully(document_enc);
			documents_enc.add(document_enc);
		}
		
		timer_query.stop();
		timer_query.start();
		
		// Decrypt the documents and build the hashtable
		counter = 0;
		for (String keyword: keywords_list)
		{
			// Only use the addresses for the documents to be fetched
			if (Integer.parseInt(keyword_to_document_address.get(keyword)) < this.array_counter.length)
			{
				String document_dec = Crypto.GCM_decryption(key, documents_enc.get(counter));
				Document document_new = new Document(document_dec, keyword, DOCUMENT_SIZE);
				keyword_to_document.put(keyword, document_new);
				counter += 1;
			}
			// Fetch from local file
			else
			{
				Integer address_local = lookup_stash2.get(keyword);
				Document document_new = document_stash.get(address_local);
				document_new.set_query_keyword(keyword);
				document_stash.remove(address_local);
				keyword_to_document.put(keyword, document_new);
			}
		}
	}
	
	
	/* Function to write back the documents and update the lookup indices
	 */
	private void write_back(SecureRandom random) throws GeneralSecurityException, IOException
	{		
		// Picking address-document pairs and upload
		List<Integer> array_loc_list = Arrays.asList(array_loc_stash.toArray(new Integer[0]));
		Integer[] document_stash_list = document_stash.keySet().toArray(new Integer[0]);
		Collections.shuffle(array_loc_list);
		
		output_stream.writeInt(1);
		
		int array_loc_counter = 0;
		int write_back_counter = (int) (document_stash.size() * 0.5);
		while (write_back_counter >= 0 && array_loc_stash.isEmpty() == false && document_stash.isEmpty() == false)
		{			
			Integer array_loc = array_loc_list.get(array_loc_counter);
			Integer document_key = document_stash_list[array_loc_counter];
			Document document = document_stash.get(document_key);
			array_loc_stash.remove(array_loc);
			document_stash.remove(document_key);
			array_loc_counter++;
			
			// Newly inserted document
			if (document.query_keyword == null)
				document.add_keyword_counter(keyword_max, keyword_insert_counter);
			
			// Compute address and document encryption
			byte[] document_address = Crypto.SHA256(this.signingKey, Integer.toString(array_loc) + '-' + Integer.toString(this.array_counter[array_loc]));
			byte[] document_enc = Crypto.GCM_encrypt(key, random, document.get_string(DOCUMENT_SIZE));
			output_stream.writeInt(document_address.length);
			output_stream.write(document_address);
			output_stream.writeInt(document_enc.length);
			output_stream.write(document_enc);
			
			// Update the lookup index stash
			for (String keyword: document.get_keywords())
			{
				String keyword_without_counter = keyword.split("\\|")[0];
				Integer time_stamp = keyword_query_counter.get(keyword_without_counter);
				// Newly inserted document
				if (document.query_keyword == null)
					time_stamp += 1;
				// Other documents
				else if (document.is_query_keyword(keyword) == false)
					time_stamp += 1;
				Index index = new Index(time_stamp, array_loc);
				lookup_stash1.put(keyword, index);
				lookup_stash2.remove(keyword);
			}
			write_back_counter -= 1;
			
		}
		
		// Select a random half of the array locations and write them back
		List<String> lookup_stash_keys = new ArrayList<String>(lookup_stash1.keySet());
		Collections.shuffle(lookup_stash_keys);
		for (int ii = 0; ii < lookup_stash_keys.size() * 0.5; ii++)
		{
			Index index = lookup_stash1.get(lookup_stash_keys.get(ii));
			byte[] lookup_address = Crypto.SHA256(this.signingKey, lookup_stash_keys.get(ii) + "-" + index.get_time_stamp());
			byte[] lookup_data_enc = Crypto.GCM_encrypt(key, random, index.get_location());
			output_stream.writeInt(lookup_address.length);
			output_stream.write(lookup_address);
			output_stream.writeInt(lookup_data_enc.length);
			output_stream.write(lookup_data_enc);
			
			lookup_stash1.remove(lookup_stash_keys.get(ii));
		}
		
		output_stream.writeInt(-1);
		output_stream.flush();
		input_stream.readInt();
	}
	
	/* Merge inserted document with fake documents
	 * Keyword list of the document has to be changed for correctness
	 */
	private void merge_query_results(String keyword, Hashtable<String, Document> keyword_to_document) {
		for (String keyword_and_counter1: keyword_to_document.keySet())
		{
			String[] keyword_and_counter_array = keyword_and_counter1.split("\\|");
			
			// Find new insertion documents
			if (keyword_and_counter_array.length > 1 && Integer.parseInt(keyword_and_counter_array[1]) >= keyword_max.get(keyword_and_counter_array[0]))
			{
				Document document_insert = keyword_to_document.get(keyword_and_counter1);
				keyword_to_document.remove(keyword_and_counter1);
				
				for (String keyword_and_counter2: keyword_to_document.keySet())
				{
					// Find a fake document that contains the keyword
					String keyword_query = "";
					if (keyword_query.length() == 0 && keyword_and_counter2.split("\\|").length > 1 && keyword_to_document.get(keyword_and_counter2).isReal() == false)
					{
						Document document_fake = keyword_to_document.get(keyword_and_counter2);
						
						// Set to real document
						document_fake.setReal(true);
						
						// Add keywords to the fake document
						for (String keyword_insert: document_insert.get_keywords())
						{
							String keyword_insert_keyword = keyword_insert.split("\\|")[0];
							Integer keyword_insert_counter = Integer.parseInt(keyword_insert.split("\\|")[1]);
							// Add keyword if it is not there yet
							if (document_fake.contains_keyword(keyword_insert_keyword) == false)
								document_fake.add_keyword(keyword_insert);
							else if (keyword_insert_counter < keyword_max.get(keyword_insert_keyword))
								document_fake.add_keyword(keyword_insert);
						}
						
						// Set document content
						document_fake.set_data(document_insert.get_data());
						
						for (String keyword_fake: document_fake.get_keywords())
							if (keyword_fake.split("\\|")[0].equals(keyword))
								keyword_query = keyword_fake;
						document_fake.set_query_keyword(keyword_query);
					}
				}
			}
		}
	}
	
	
	
	/* Function to put all the retrieved documents in the stash and update the lookup index stash (2)
	 */
	private void flush_to_stash(Hashtable<String, Document> keyword_to_document) throws IOException
	{

		for (String query_keyword: keyword_to_document.keySet())
		{
			document_stash.put(this.document_identifier_counter, keyword_to_document.get(query_keyword));
			
			for (String keyword: keyword_to_document.get(query_keyword).get_keywords())
			{
				lookup_stash1.remove(keyword);
				lookup_stash2.put(keyword, this.document_identifier_counter);
			}
			this.document_identifier_counter += 1;	
		}
	}
	
	private void flush_to_stash(Hashtable<String, Document> keyword_to_document, Document doc) throws IOException
	{

		for (String query_keyword: keyword_to_document.keySet())
		{
			document_stash.put(this.document_identifier_counter, keyword_to_document.get(query_keyword));
			
			for (String keyword: keyword_to_document.get(query_keyword).get_keywords())
			{
				lookup_stash1.remove(keyword);
				lookup_stash2.put(keyword, this.document_identifier_counter);
			}
			this.document_identifier_counter += 1;	
		}
		document_stash.put(this.document_identifier_counter, doc);
		this.document_identifier_counter += 1;
	}
	
	
	/* Function to get all the keywords in the database
	 */
	public HashMap<String, Integer> get_keywords()
	{
		return this.keyword_max;
	}
	
	public void close() throws IOException
	{
		output_stream.writeInt(3);
		output_stream.flush();
	}
	
	public static HashMap<String, String> parse_configuration(String filename_input) throws IOException {
		HashMap<String, String> configuration = new HashMap<String, String>();
		
		FileReader fp = new FileReader(filename_input);
		BufferedReader config_reader = new BufferedReader(fp);
		
		String next_line = config_reader.readLine();
		while (next_line != null) {
			String[] argument = next_line.split(" = ");
			configuration.put(argument[0], argument[1]);
			next_line = config_reader.readLine();
		}
		
		fp.close();
		config_reader.close();
		return configuration;
	}
	
	
	// Not counting the counters
	private Integer get_stash_size() {
		Integer stash_size = 0;
		
		for (String keyword: lookup_stash1.keySet())
			stash_size += keyword.length() + 4;
		
		for (String keyword: lookup_stash2.keySet())
			stash_size += keyword.length() + 2;
		
		for (Integer idx: document_stash.keySet())
			stash_size += 2 + document_stash.get(idx).get_size();
		
		return stash_size;
	}
}
