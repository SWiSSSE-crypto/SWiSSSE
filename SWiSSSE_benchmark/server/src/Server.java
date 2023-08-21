import java.io.*;
import java.net.*;

import redis.clients.jedis.Jedis;


public class Server {
    public static void main(String[] args) throws IOException {
        try (ServerSocket listener = new ServerSocket(59090)) {
            System.out.println("The server is running...");
            
            // Initialise an empty database
            Jedis database = new Jedis("localhost", 6379, 100000);
            database.flushAll();
            boolean run = true;
            
            while (run) {
                try (Socket socket = listener.accept()) {
                	// Setup a connection
                	socket.setTcpNoDelay(true);
                	DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                	DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                	
                	while (run)
                	{
	                	/* operation: 
	                	 * 0. GET: Read and delete
	                	 * 1. SET: Write
	                	 * 2. New instance
	                	 * 3. Close connection
	                	 */
	                	Integer operation = in.readInt();
	                	if (operation == 0)
	                	{
	                		Integer size = in.readInt();
	                		while (size > 0)
	                		{
	                			byte[] key = new byte[size];
	                			in.readFully(key);
	                			
	                			byte[] value = database.get(key);
	                			if (value != null)
	                			{
	                				out.writeInt(value.length);
	                				out.write(value);
	                				database.del(key);
	                			}
	                			else
	                			{
	                				out.writeInt(1);
	                				out.write(new byte[1]);
	                			}
	                			out.flush();
	                			size = in.readInt();
	                		}
	                		out.flush();
	                	}
	                	
	                	if (operation == 1)
	                	{
	                		//long t0 = System.currentTimeMillis();
	                		Integer size = in.readInt();
	                		while (size > 0)
	                		{
	                			byte[] key = new byte[size];
	                			in.readFully(key);
	                			size = in.readInt();
	                			byte[] value = new byte[size];
	                			in.readFully(value);
	                			database.set(key, value);
	                			size = in.readInt();
	                		}
	                		
	                		out.writeInt(0);
	                		out.flush();
	                		//long t1 = System.currentTimeMillis();
	                		//System.out.println(Long.toString(t1-t0));
	                		
	                	}
	                	
	                	if (operation == 2)
	                	{
	                		database.flushAll();
	                		out.writeInt(0);
	                		out.flush();
	                		System.out.println("New instance.");
	                	}
	                	
	                	if (operation == 3)
	                	{
	                		database.close();
	                		socket.close();
	                		listener.close();
	                		run = false;
	                		System.out.println("The server has shut down.");
	                	}
                	}
                }
            }
        }
    }
}