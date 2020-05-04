package server;

import java.util.Comparator;
import java.util.HashMap;

public class KeywordsComparator implements Comparator<String> {
	HashMap<String, Integer> keyword_count = new HashMap<String, Integer>();
	
	public KeywordsComparator(HashMap<String, Integer> keyword_count) {
		this.keyword_count = keyword_count;
	}	
	
	@Override
	public int compare(String o1, String o2) {
		// TODO Auto-generated method stub
		if (keyword_count.get(o1) > keyword_count.get(o2))
			return -1;
		if (keyword_count.get(o1) < keyword_count.get(o2))
			return 1;
		return 0;
	}

}
