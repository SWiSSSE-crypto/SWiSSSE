package client;

import java.util.*;

public class Timer {
	private List<Long> startTime = new ArrayList<Long>();
	private List<Long> endTime = new ArrayList<Long>();
	
	public Timer()
	{
		
	}
	
	public void start()
	{
		this.startTime.add(System.nanoTime());
	}
	
	public void stop()
	{
		this.endTime.add(System.nanoTime());
	}
	
	public long get_time(int idx)
	{
		return this.endTime.get(idx) - this.startTime.get(idx);
	}
	
	public long get_total_time()
	{
		long total_time = 0;
		for (int idx = 0; idx < this.startTime.size(); idx++)
			total_time += this.endTime.get(idx) - this.startTime.get(idx);
		return total_time;
	}
	
}
