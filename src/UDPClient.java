import java.net.*;
  
/**
 * Class that provides functionalities of a UDPClient
 * Also acts as a receive thread and parses all incoming messages
 * @author Fernando
 *
 */
public class UDPClient 
{
	public UDPClient(String hostname, InetAddress ip, int srvPort, int clientPort ) throws Exception 
    { 
		DatagramSocket clientSocket = null; 
		
		try
		{
			clientSocket = new DatagramSocket( clientPort );
		}
		catch (BindException e)
		{
			System.out.println(">>> [Address already in use, exiting...]");
			System.exit(0);
		}
		
		String nickname = hostname;
		int myPort = clientPort;
		byte[] receiveData = new byte[1024];
		
		InetAddress serverIP = ip;
		int serverPort = srvPort;

		SendThread sender = new SendThread (clientSocket, nickname, serverIP, serverPort, myPort);
		Thread s = new Thread (sender);
		s.start();
		
		
		System.out.println(">>> [Registering...]");
		System.out.print(">>> ");
		
		while (true)
		{

				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
	
				clientSocket.receive(receivePacket); 
				   
				String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
				   
				/* Incoming client information, does not display it on console */
				if (sentence.startsWith(".hash, "))
				{
					SendThread.ack = true;
					sender.updateHash(sentence);
				}
				
				/* Incoming check for link status, send an ACK back */
				else if (sentence.equals(".alive?"))
				{
					String out = ".alive!";
	                
		        	byte buffer[] = new byte [1024];
		        	buffer = out.getBytes();
		            InetAddress address = receivePacket.getAddress();
		            DatagramPacket ack = new DatagramPacket(buffer, buffer.length, address, receivePacket.getPort());
		                
		            clientSocket.send(ack);
				}
				  
				/* Server ACKs when received a request to dereg, set ack to true */
				else if (sentence.equals("[You are Offline. Bye.]"))
				{
					SendThread.ack = true;
					System.out.println(sentence);
					System.out.print(">>> ");
				}
				
				/* Server ACK */
				else if (sentence.equals("[Message received by the server and saved]"))
				{
					SendThread.ack = true;
					System.out.println(sentence); 
					System.out.print(">>> ");
				}
				  
				/* Receives a regular message, ACKs back */
				else if (!sentence.startsWith("[Message received by "))
	            {
	            	System.out.println(sentence);
	            	System.out.print(">>> ");
		        	String out = "[Message received by " + SendThread.hostname +".]";
		                
		        	byte buffer[] = new byte [1024];
		        	buffer = out.getBytes();
		            InetAddress address = receivePacket.getAddress();
		            DatagramPacket ack = new DatagramPacket(buffer, buffer.length, address, receivePacket.getPort());
		                
		            clientSocket.send(ack);
	            }

				/* Receives an ACK, print it, and continue */
				else
				{
					SendThread.ack = true;
					System.out.print(">>> ");
					System.out.println(sentence); 
					System.out.print(">>> ");
				}
			

		}
	}
	
 } 
