import java.io.*;
import java.net.*;
import java.util.*;

public class SendThread implements Runnable
{
    public static String hostname = null;
    public static String toname = null;
    public static DatagramSocket sock;
    static HashMap<String, ArrayList<Serializable>> hm = new HashMap<String, ArrayList<Serializable>>();
    static InetAddress serverIP = null;
    static int serverPort = -1;
    static int myPort = -1;
    static boolean online = false;
    static boolean ack = false;
    static int counter = 0;
    static String timeoutmode = null;
    static String buffer = null;

    /**
     * Constructor that initializes the variables and registers the client with the server
     * @param socket - client socket
     * @param host - name of client
     * @param serverIP - server's IP
     * @param serverPort - server's Port
     * @param myPort - client's port
     */
    public SendThread(DatagramSocket socket, String host, InetAddress serverIP, int serverPort, int myPort) 
    {
        sock = socket;

        SendThread.myPort = myPort;
        
        hostname = host;
        
        SendThread.serverIP = serverIP;
        
        SendThread.serverPort = serverPort;
 
        try 
        {
			register();
			
		} 
        catch (InterruptedException e) 
        {
			e.printStackTrace();
		}
        
    }

    /**
     * Send's main thread that takes any user input, parse the commands and apply necessary actions
     */
    public void run() 
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) 
        {
        	/* Makes sure ack is received to prevent interruption of console input by receive thread */
        	if (ack)
        	{
        		/* ACK counter resets to 0 */
        		counter = 0;
	        	try 
	        	{
	        		
	        		String line = in.readLine();
	        		
	        		/* Parse commands only if online or if user tries to reg or exit after dereg */
	        		if (online || line.startsWith("reg") || line.startsWith("ctrl + c"))
	        		{
	        			/* If user wants to send */
		        		if (line.startsWith("send"))
		        		{
		        			if (line.split(" ")[1].equals(hostname))
		        			{
		        				System.out.println(">>> [Cannot send message to self!!!]");
		        				System.out.print(">>> ");
		        			}
		        			else
		        			{
			        			ack = false;	
			        			try
			        	    	{
			        				sock.send(clientLookUp(line));
			        	    	}
			        			catch (ArrayIndexOutOfBoundsException e)
			        			{
			        				ack = true;
			        				System.out.println(">>> [Please enter a clientname]");
			        				System.out.print(">>> ");
			        			}
			        			catch (StringIndexOutOfBoundsException e)
			        			{
			        				ack = true;
			        				System.out.println(">>> [Please enter a message]");
			        				System.out.print(">>> ");
			        			}
			        			catch (NullPointerException e)
			        			{
			        				ack = true;
			        				System.out.println(">>> [Client not found, type <table> to see available client(s).]");
			        				System.out.print(">>> ");
			        			}	
		        			}
		        		}
		
		        		/* table will show current clients, contact list equivalent */
		        		else if (line.equals("table"))
		        		{
		  				  System.out.println(">>> Table");
						  Iterator<String> iterator = hm.keySet().iterator(); 
						  while (iterator.hasNext())
							{
								String value = hm.get(iterator.next().toString()).toString();		
								String output = value.substring(1, value.length()-1);  						
								System.out.println(">>> " + output);		
							}
						  System.out.print(">>> ");
		        		}
		        		
		        		/* dereg will log the client out, changing the status in the table */
		        		else if (line.startsWith("dereg"))
		        		{
		        			if (!online)
		        			{
		        				System.out.println(">>> [You are currently offline.]");
		        			}
		        			else
		        			{
		        				System.out.print(">>> ");
		        				deregister();
		        				online = false;
		        			}
		        			
		        		}
		        		
		        		/* allows user to reg back online */
		        		else if (line.startsWith("reg"))
		        		{
		        			if (online)
		        				System.out.println(">>> [You are currently online.]");
		        			else
		        			{
		        				System.out.print(">>> ");
		        				String[] values = line.split(" ");
		        				try
		        				{
		        					hostname = values[1];
		        					register();
			        				online = true;
		        				}
		        				catch (ArrayIndexOutOfBoundsException e)
		        				{
			        				System.out.println("[Please enter a clientname...]");
			        				System.out.print(">>> ");
		        				}
		        				
		        			}
		        		}
		        		
		        		/* dereg then exit program entirely */
		        		else if (line.equals("ctrl + c"))
		        		{
		        			if (online)
		        			{
		        				deregister();
		        			}
		        			System.out.print(">>> ");
		        			
		        			Thread.sleep(500); //Wait for ACK
		        			
		        			System.out.println("[Exiting]");
		        			System.exit(0);
		        		}
		        		
		        		else
		        		{
		        			System.out.println(">>> [Command not recognized, please try again.]");
		        			System.out.print(">>> ");
		        		}

	        		}
	
	        		/* Refuses command while offline */
					else
					{
						System.out.println(">>> [You are currently offline.]");
						System.out.println(">>> [Please Register or exit the application.]");
						System.out.print(">>> ");
					}
				} 
	        	
	        	catch (Exception e) 
	        	{
					e.printStackTrace();
				}
        	}
        	
        	/* Timeout for both server and client connections */
        	else
        	{
        		if (timeoutmode.equals("server"))
        			serverACK();
        		else
        			clientACK();
        	}
        }
    }

    /**
     * Updates the local client hashmap
     * @param sentence - incoming info from server
     */
	public void updateHash (String sentence)
	{
		String[] values = sentence.split(", ");
		
		ArrayList<Serializable> list = new ArrayList<Serializable>();
		
		for (int i = 1; i < values.length; i++)
		{
			list.add (values[i]);
		}
  
		hm.put(values[1], list);
		
		list = new ArrayList<Serializable>();	
	}
    
	/**
	 * Searches through the local table and create a datagramPacket to client
	 * If destination is unreachable, create a datagramPacket to server instead
	 * @param input - user input
	 * @return DatagramPacket to be sent
	 */
	private static DatagramPacket clientLookUp(String input)
	{
		
		String[] list = input.split(" ");		
		String client = list[1];
		toname = client;
		//6 is the length of "send " and an additional " " after the client name
		int index = 6 + list[1].length();
		
		String message = hostname + ": " + input.substring(index);


		ArrayList<Serializable> values = hm.get(client);
		
		String ip = (String) values.get(1);
		ip = ip.substring(1);
		
		String status = (String) values.get(3);

		/* check local table first */
		if (status.equalsIgnoreCase("on"))
		{
			timeoutmode = "client";
			
			//Get the ip of current key
			InetAddress clientIP = null;

			try 
			{
				clientIP = InetAddress.getByName(ip);
			} 
			catch (UnknownHostException e) 
			{
				e.printStackTrace();
			}	
			
			//get the port of the current key
			int clientPort = Integer.parseInt((String) values.get(2)); 
			//get status

			byte[] sendData  = new byte[1024];		
			sendData = message.getBytes(); 
			buffer = ".off " + client + " " + hostname + " " + input.substring(index);
			
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort); 

			return sendPacket;
		}
		
		/* if off, initiate offline chat */
		else
		{
			timeoutmode = "server";
			
			System.out.println(">>> [" + client + " is currently off-line, message sent to server.]");
			System.out.print(">>> ");
			
			message = ".off " + client + " " + hostname + " " + input.substring(index);
			byte[] sendData  = new byte[1024];	
			
			sendData = message.getBytes();  
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, serverPort); 
	
			return sendPacket;

		}
	}
    
	/**
	 * Registers client with server
	 * @throws InterruptedException 
	 */
	public void register() throws InterruptedException
	{
		
		/* Sends current host information */
        String sentence = ".register " + hostname + " " + myPort + " " + "on";
        
        byte[] sendData = new byte[1024];
		sendData = sentence.getBytes();
   
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, serverPort); 
		
		try 
		{
			ack = false;   
			timeoutmode = "server";
			sock.send(sendPacket);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		online = true;
  
	}
	
	/**
	 * Deregisters client from server
	 * @throws SocketException 
	 */
	private static void deregister() throws SocketException
	{
		/* Sends current host information */
        String sentence = ".deregister " + hostname + " " + myPort + " " + "off";
        
        byte[] sendData = new byte[1024];
		sendData = sentence.getBytes();
   
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, serverPort); 
		
		try 
		{
			ack = false;
			timeoutmode = "server";
			sock.send(sendPacket);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		
		online = false;
	}
   
	/**
	 * Puts thread to sleep for 500msec, retries for 5 times. If no ack is received
	 * then it means server is not responding, quit gracefully.
	 */
	private static void serverACK()
	{
		if (counter < 5)
		{
			try 
			{
				Thread.sleep(500);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			counter++;
		}
		else
		{
			System.out.println("[Server not responding]");
			System.out.println(">>> [Exiting]");
			System.exit(0);
		}
	}
	
	/**
	 * Puts thread to sleep for 500msec, if still no ack, then destination client
	 * is down, and redirect packet to server.
	 */
	private static void clientACK()
	{
		if (counter < 1)
		{
			try 
			{
				Thread.sleep(500);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			counter++;
		}
		else
		{
			System.out.println(">>> [No ACK from " + toname + ", message sent to server.]");
			System.out.print (">>> ");
			
			byte[] sendData  = new byte[1024];
			sendData = buffer.getBytes();
			
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, serverPort); 
	
			try 
			{
				ack = false;
				timeoutmode = "server";
				sock.send(sendPacket);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}	
}



