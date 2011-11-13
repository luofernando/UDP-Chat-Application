import java.io.*; 
import java.net.*; 
import java.text.*;
import java.util.*;
  
/**
 * Server class that provides the functionalities of a UDPServer
 * @author Fernando
 *
 */
public class UDPServer 
{ 
	/* local hashmap of all clients */
	static HashMap<String, ArrayList<Serializable>> hm = new HashMap<String, ArrayList<Serializable>>();

	/* server Socket */
	static DatagramSocket serverSocket = null;
	static byte[] receiveData = new byte[1024]; 
	
	public UDPServer (int port) throws Exception 
	{ 
		System.out.println(">>> [Server initiated...]");
		
		try  
		{
			serverSocket = new DatagramSocket(port);
		}
		catch (BindException e)
		{
			System.out.println(">>> [Socket Already in use, exiting...]");
			System.exit(0);
		}

		while(true) 
		{ 
			/* Server receives a packet */
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
			serverSocket.receive(receivePacket); 
	
			String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			InetAddress IPAddress = receivePacket.getAddress(); 
			
			/* if packet is to register */
			if (sentence.startsWith(".register"))
			{
				
				String[] values = sentence.split(" ");
				boolean on = false;
				
				/* if client exists, check to see if client is on */
				if (hm.get(values[1]) != null)
				{
					ArrayList<Serializable> list = hm.get(values[1]);
					
					InetAddress toIP = (InetAddress) list.get(1);	
					int toPort = Integer.parseInt((String) list.get(2)); 

					/* if client is on, duplicate clientnames can't register */
					if (checkAlive(values[1], toIP, toPort))
					{
						String output = "[Nickname already taken.]";
						byte[] sendData  = new byte[1024];
						
						sendData = output.getBytes();  
					
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receivePacket.getPort()); 
						try 
						{
							serverSocket.send(sendPacket);
						} 
						catch (IOException e) 
						{
							e.printStackTrace();
						}
						on = true;
					}
					
					/* if client is off */
					else
					{
						on = false;
					}
										
				}
				
				/* Allow registration if client is not on or does not exist */
				if (!on)
				{
					registration (sentence, IPAddress);
					
					String output = "[Welcome, You are registered.]";
					byte[] sendData  = new byte[1024];
					
					sendData = output.getBytes();  
					
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receivePacket.getPort()); 
					try 
					{
						serverSocket.send(sendPacket);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					} 
					
					readMessage(sentence.split(" ")[1]);
				}
			}
			
			/* If a client requests to dereg */
			if (sentence.startsWith(".deregister"))
			{
				registration (sentence, IPAddress);
				
				String output = "[You are Offline. Bye.]";
				byte[] sendData  = new byte[1024];
				
				sendData = output.getBytes();  
				
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receivePacket.getPort()); 
				try 
				{
					serverSocket.send(sendPacket);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				} 
				
			}
			
			/* If a client wants to start offline chatting */
			if (sentence.startsWith(".off"))
			{
				clientLookUp(sentence, IPAddress, receivePacket.getPort());
			}
			
			/* If a client responds to checkAlive */
			if (sentence.equals(".alive!"))
			{
				ack = true;
			}
		} 
	} 
  
	/**
	 * Builds and maintains the hashmap of client information. Pushes updates out
	 * whenever the hashmap gets updated.
	 * @param sentence - incoming client information
	 * @param IPAddress - IP of client
	 */
	private static void registration(String sentence, InetAddress IPAddress)
	{
		String[] values = sentence.split(" ");
  
		ArrayList<Serializable> list = new ArrayList<Serializable>();
		
		list.add (values[1]);
		list.add(IPAddress);
		list.add (values[2]);
		list.add(values[3]);
           
		hm.put(values[1], list);
          
		list = new ArrayList<Serializable>();
  
		/* Traverse through the entire hashmap */
		Iterator<String> iterator = hm.keySet().iterator();          
		while (iterator.hasNext()) 
		{  
			//Current Key
			String key = iterator.next().toString();
			
			//Key's values
			list = (ArrayList<Serializable>) hm.get(key);	
			
			//get the status of the current key
			String status = (String) list.get(3);
			
			//Broadcast to clients that are online
			if (status.equalsIgnoreCase("on"))
			{
				//Get the ip of current key
				InetAddress ip = (InetAddress) list.get(1);	
				
				//get the port of the current key
				int p = Integer.parseInt((String) list.get(2)); 

				//traverse through the original HashMap
				Iterator<String> iterator2 = hm.keySet().iterator(); 
				while (iterator2.hasNext())
				{
					String value = hm.get(iterator2.next().toString()).toString();		
					String output = ".hash, " + value.substring(1, value.length()-1);  
					
					byte[] sendData  = new byte[1024];
					
					sendData = output.getBytes();  
					
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
					try 
					{
						//Update client with new table
						serverSocket.send(sendPacket);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					} 
				}
				
				byte[] sendData  = new byte[1024];
				String output = "[Client table updated.]";
				sendData = output.getBytes();  
				
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
				try 
				{
					serverSocket.send(sendPacket);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
	
			}
		}
	}
  
	/**
	 * Resends hashmap if a client is sending off messages to a client that is already on
	 * @param client - name of destination client
	 * @param ip - ip of out of synced client
	 * @param p - port of out of synced client
	 */
	private static void sendTable(String client, InetAddress ip, int p)
	{
		String output = "[Client " + client + " exists!!]";
		
		byte[] sendData  = new byte[1024];
		sendData = output.getBytes();  
		
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
		try 
		{
			serverSocket.send(sendPacket);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		Iterator<String> iterator = hm.keySet().iterator(); 
		while (iterator.hasNext())
		{
			String value = hm.get(iterator.next().toString()).toString();		
			output = ".hash, " + value.substring(1, value.length()-1);  
			
			sendData  = new byte[1024];
			sendData = output.getBytes();  
			
			sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
			try 
			{
				serverSocket.send(sendPacket);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
		}
		
		sendData  = new byte[1024];
		output = "[Client table updated.]";
		sendData = output.getBytes();  
		
		sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
		try 
		{
			serverSocket.send(sendPacket);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 * Looks up client and determine if client is off or not
	 * @param sentence - incoming messages
	 * @param IPAddress - IP of incoming client
	 * @param port - port incoming client
	 * @throws IOException
	 */
	private static void clientLookUp(String sentence, InetAddress IPAddress, int port) throws IOException
	{
		String[] values = sentence.split(" ");
		String toClient = values[1];
				
		ArrayList<Serializable> list = hm.get(toClient);
		
		String status = (String) list.get(3);

		InetAddress toIP = (InetAddress) list.get(1);	
		
		int toPort = Integer.parseInt((String) list.get(2)); 
		
		/* If client is off according to the table */
		if (status.equals("off"))
		{
			/* If client is off just like the table initiate offline chat*/
			if (!checkAlive (toClient, toIP, toPort))
			{
				saveMessage (sentence);
				
				String output = "[Message received by the server and saved]";
				byte[] sendData  = new byte[1024];
				sendData = output.getBytes();  
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); 
				try 
				{
					serverSocket.send(sendPacket);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
			
			/* If client is on, inform original sender, then update and broadcast table */
			else
			{
				String output = "[Client " + toClient + " exists!!]";
				
				byte[] sendData  = new byte[1024];
				sendData = output.getBytes();  
				
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); 
				try 
				{
					serverSocket.send(sendPacket);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
				
				updateHash(toClient, "on");

			}
			
		}
		
		/* If client is on according to the table */
		else
		{
			/* If client is indeed on, update the misinformed client's table */
			if (checkAlive (toClient, toIP, toPort))
				sendTable (toClient, IPAddress, port);	
			
			/* If client is off, unlike the table values; initiate offline chat and update broadcast table */
			else 
			{
				saveMessage (sentence);
				
				String output = "[Message received by the server and saved]";
				byte[] sendData  = new byte[1024];
				sendData = output.getBytes();  
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); 
				try 
				{
					serverSocket.send(sendPacket);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
				
				updateHash(toClient, "off");
			}

		}
	}
	
	/**
	 * If client is off, save messages to a txt file with timestamp and the client it is from
	 * If file doesn't exist, create a new one
	 * If a file does exit, append to it
	 * @param output
	 * @throws IOException
	 */
	public static void saveMessage(String output) throws IOException
	{
		String[] values = output.split(" ");
		String toClient = values[1];
		String fromClient = values[2];
		
		int index = 7 + values[1].length()+ values[2].length();

		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date date = new Date();
		
		String message = fromClient + ": <" +dateFormat.format(date) + "> " + output.substring(index);
		
		/* Each client get its own txt file */
		String filename = toClient+".txt";
		
		File txt = null;
		BufferedWriter writer = null;
		try
		{
			 writer = new BufferedWriter(new FileWriter(filename, true));
		}
		
		/* If file doesn't exist, create a new one */
		catch (FileNotFoundException e)
		{
			txt = new File(filename);
			writer = new BufferedWriter (new FileWriter(txt, true));
			
		}

		writer.write(message);
		writer.newLine();
		writer.close();
		
	}
	
	/**
	 * When a client goes on, check to see if a txt file exists
	 * if it does, get the messages and send them
	 * if not, do nothing
	 * @param client
	 * @throws IOException
	 */
	public static void readMessage(String client) throws IOException
	{
		String filename = client+".txt";

		/* check if txt file exists, if yes, send everything in it to client and delete file */
		try 
		{
			BufferedReader in = new BufferedReader(new FileReader(filename));
			
			
			ArrayList<Serializable> list = hm.get(client);
			
			//Get the ip of current key
			InetAddress ip = (InetAddress) list.get(1);	
			
			//get the port of the current key
			int p = Integer.parseInt((String) list.get(2));
			
			String output = "You Have Messages";
			byte[] sendData  = new byte[1024];
			sendData = output.getBytes();  
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
			try 
			{
				serverSocket.send(sendPacket);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
			

			String message;
			while ((message = in.readLine()) != null)
			{
				sendData = message.getBytes();  
				sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
				
				try 
				{
					serverSocket.send(sendPacket);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				} 
				
			}
			
			in.close();
			
			File txt = new File (filename);
			txt.delete();
			
			
		} 
		
		/* If not, do nothing */
		catch (FileNotFoundException e) 
		{
			//nothing
		}
	}
	
	/**
	 * Check if client is alive by sending a packet, wait 500msec
	 * if no response, then client is not alive.
	 * @param toClient - Destination client
	 * @param toIP - client's IP
	 * @param toPort - client's port
	 * @return online status
	 */
	public static boolean checkAlive(String toClient, InetAddress toIP, int toPort)
	{
		String output = ".alive?";
		byte[] sendData  = new byte[1024];
		sendData = output.getBytes();  
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, toIP, toPort); 

		
		try 
		{
			ack = false;
			serverSocket.send(sendPacket);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		
		boolean done = false;
		while (!done)
		{
			if (ack)
			{
				done = true;	
			}
			else
			{
				/* Wait for 500msec only once */
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
					done = true;
			}
		}
		
		counter = 0;
		
		return ack;	
	}
	
	/**
	 * If a client is on while off on the table, update and broadcast
	 * @param client - client
	 * @param newStatus - correct status
	 */
	public static void updateHash(String client, String newStatus)
	{
		ArrayList<Serializable> list = hm.get(client);
		
		list.set(3, newStatus);
           
		hm.put(client, list);
          
		Iterator<String> iterator = hm.keySet().iterator();          
		while (iterator.hasNext()) 
		{  
			//Current Key
			String key = iterator.next().toString();
			
			//Key's values
			list = (ArrayList<Serializable>) hm.get(key);	
			
			//get the status of the current key
			String status = (String) list.get(3);
			
			if (status.equalsIgnoreCase("on"))
			{
				//Get the ip of current key
				InetAddress ip = (InetAddress) list.get(1);	
				
				//get the port of the current key
				int p = Integer.parseInt((String) list.get(2)); 

				//traverse through the original HashMap
				Iterator<String> iterator2 = hm.keySet().iterator(); 
				while (iterator2.hasNext())
				{
					String value = hm.get(iterator2.next().toString()).toString();		
					String output = ".hash, " + value.substring(1, value.length()-1);  
					
					byte[] sendData  = new byte[1024];
					
					sendData = output.getBytes();  
					
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
					try 
					{
						serverSocket.send(sendPacket);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					} 
				}
				
				byte[] sendData  = new byte[1024];
				String output = "[Client table updated.]";
				sendData = output.getBytes();  
				
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, p); 
				try 
				{
					serverSocket.send(sendPacket);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
	
			}
		}
	}

	static int counter = 0;
	static boolean ack = false;
	
 }  
