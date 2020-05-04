package client;

public class Index {
	
	private Integer time_stamp;
	private Integer array_location;
	
	public Index(Integer time_stamp, Integer array_location)
	{
		this.time_stamp = time_stamp;
		this.array_location = array_location;
	}
	
	public String get_time_stamp()
	{
		return Integer.toString(this.time_stamp);
	}
	
	public String get_location()
	{
		return Integer.toString(this.array_location);
	}
}
