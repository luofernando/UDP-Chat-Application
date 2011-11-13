import java.net.InetAddress;

/**
 * Provides a main method and parses arguments to initiate the appropriate mode
 * @author Fernando
 *
 */
public class UdpChat 
{
	public static void main(String[] args) throws Exception 
	{
		
		try
		{
			String mode = null;
			mode = args[0].substring(1);

			/* Initiates the Server mode */
			if (mode.equals("s"))
			{
				int port = Integer.parseInt(args[1]);
				
				if (port < 1024)
				{
					System.out.println(">>> [Server port cannot be lower than 1024]");
					System.exit(0);
				}
				if (port > 65535)
				{
					System.out.println(">>> [Server port cannot be more than 65535]");
					System.exit(0);
				}
				
				UDPServer newServer = new UDPServer(port);	
			}
			
			/* Initiates the Client Mode */
			else if (mode.equals("c"))
			{
				String hostname = args[1];
				InetAddress serverIP = InetAddress.getByName(args[2]);
				int serverPort = Integer.parseInt(args[3]);
				int clientPort = Integer.parseInt(args[4]);
				
				if (serverPort < 1024)
				{
					System.out.println(">>> [Server port cannot be lower than 1024]");
					System.exit(0);
				}
				if (serverPort > 65535)
				{
					System.out.println(">>> [Server port " + serverPort + " does not exist!]");
					System.exit(0);
				}	
				if (clientPort < 1024)
				{
					System.out.println(">>> [Client port cannot be lower than 1024]");
					System.exit(0);
				}
				if (clientPort > 65535)
				{
					System.out.println(">>> [Port " + serverPort + " does not exist!]");
					System.exit(0);
				}

				UDPClient newClient = new UDPClient (hostname, serverIP, serverPort, clientPort);	
			}
			
			else
			{
				System.out.println(">>> [Please enter the correct arguments.]");
			}
		}
		
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.out.println(">>> [Please enter the correct arguments.]");
		}
	}
}
