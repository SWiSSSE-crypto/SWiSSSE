package client;

import java.util.ArrayList;
import java.util.List;

public class StashMonitor {
	private  List<Integer> stash_sizes = new ArrayList<Integer>();

	public void add(Integer size) {
		stash_sizes.add(size);
	}
	
	public Integer get(Integer idx) {
		return stash_sizes.get(idx);
	}
}
