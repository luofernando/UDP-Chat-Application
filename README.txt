##### How to Use #####

1. java UdpChat -s <port number>           

e.g. "java UdpChat -s 9876"

Will initiate a server that uses the current machine's local loopback IP 
(in most cases: 127.0.0.1) and uses the port 9876. If successful, console 
should display:

>>> [Server initiated...]


----------------------------------------------------------------------------


2. java UdpChat -c <nickname> <server-IP> <server-Port> <my port>

e.g. "java UdpChat -c client1 127.0.0.1 9876 2000"

Will initiate a client that connects to the IP (for testing purposes, 127.0.0.1 is used.
To use server remotely, server's IP needs to be static and port needs to be forwarded to the server.)
The client will be listening on port 2000. If connected, client should see the following lines:

>>> [Registering...]
>>> [Client table updated.]
>>> [Welcome, You are registered.]
>>> 


The server will acknowledge the registration request and checks for duplicates. If a name is taken and online, 
refuse registration. If a name is taken and offline, accepts registration. If a name doesn't exist, accepts 
registration as well. If accepted, the server will send a table or the "contact list" to the client.
The user can now start to input commands.

Server can handle any number of clients provide that the machine has enough memory and adequate bandwidth.
At registration, client will attempt to connect, if however, no ack is received, then client will shutdown.




####### Project documentation ###########

-------- SendThread.java ----------------

The sendthread will run concurrently with a receive thread. The sendthread will wait for user input. 
The input is parsed for commands. If input is incorrect, the user will be prompted for a correct input.
Java's HashMap is used to store a client's local table of contacts. HashMap is easy to implement and 
useful to update a key's values without much overhead. 

A ">>> " means that the user is ready to input commands.

"send <client> <message>" will go through the current HashMap to locate the IP, port, and status of the client.
If client is online, attempt to send a message, set Acknowledgement to false. If an ack is received within 500msec,
then all is well. If no ack is received, sender will initiate offline chat by sending a packet to the server.
server will first check its own table to see that status. Then it will send a packet to the destination to 
check for status. If status does not match table, accepts the offline chat, update table, and broadcast the table.
If status does match the table, rejects the offline chat, send the unchanged table back to the sender.


"table" is a additional command that allows a client to check its contact list. This is implemented to make testing
easier and clients easier to track.


"dereg <clientname>" will send a deregister request to server, if server is online, server will update hashtable,
broadcast the table, and then finally acks the client allowing full deregistration. If, however, server is offline,
and client does not receive an ack within 500msec, and after 5 retries, client will conclude that server is not
responding and exits gracefully.


"reg <clientname>" will register any client (could be same, or another client) using the same IP and port. 


"ctrl + c" is a special command that will dereg if still online, then exits program entirely.



---------- UDPServer.java ---------------

Server provides the background operations and message parsing. 

Registration packets starts with ".register" and they contain client name, client port, and "on" status.
This packet is then parsed and updated in server's HashMap and broadcasted, and Ack's are sent out.


Deregistration packets starts with ".deregister" and they contain same as above but this time, an "off" status.
HashMap is updated and broadcasted, then Acks will be sent out.


Offline chat packets starts with ".off" and they contain toClient, fromClient and the message. Server will first
look for status of toClient and compare to the table. If there is a mismatch, change and broadcast table, then 
initiate offline chat. If client is indeed off, start offline chat immediately. Each client gets its own offline
text file stored in server's localhost. If file does not exist, one is created and each offline message will be
appended to the txt file.


Retrieving offline chat occurs whenever a client registers. Server will check if that client's txt file exists.
If not, then nothing happens. If yes, then each line from the file is sent, and the file will finally be deleted.


ACK packets is exactly: ".alive!". Server sends an ".alive?" to a client, if client doesn't respond within 500 msec,
server will assume client is no longer responding, and updates and broadcast table. 



--------------  UDPClient.java ----------------------

This class contains the receive thread. Each incoming packet to the client is parsed. 


Server's HashMap update packets start with ".hash" This is not displayed, and only relevant to client's local
table. 


Server's request for status is exactly ".alive?". Once the client receives, this, it will send an ack back.


Server's ack for dereg is "[You are Offline. Bye.]". This is how the client knows that server is responding.


Server's ack for offline chat is "[Message received by the server and saved]". 


A client's ack to each other starts with "[Message received by ". This will end the 500msec wait and allows normal
operations to resume.


Finally is received a regualy message from another client, the receiver will send out an ack. 



--------------- UdpChat.java --------------------------

Main tester class that allows user to use command line arguments to initiate either server or client mode.

Arguments are parsed to see if they are of correct formats. 



################ Exception and Logic Handling  #################

--------------- SendThread.java -----------------------

- User is prevented from sending message to self which is deemed unnecessary.

- User will be informed if no client is specified, no message is specified, or if the client does not exist.

- If a user tries to reg while still online

- If a username is taken

- If a user tries to dereg while still offline

- If a user tries to do anything, but "ctrl + c" or "reg", while offline.

- If a user enters an unknown command

- If client doesn't ACK in 500 msec	(redirects packet to server for offline chat)

- If server doesn't ACK in 500 msec 	(exits - applicable whenever client tries to contact the server)


---------------- UDPServer.java ------------------------

- If a server is trying to bind to a used port	(exits)


---------------- UDPClient.java ------------------------

- If a client is trying to bind to a used port	(exits)


---------------- UdpChat.java --------------------------

- Incorrect or no arguments

- If server port is not within 1024 and 65535

- If client port is not within 1024 and 65535



##################  Testing ##################################


---------------------Case 1 (Offline Chat)------------------------------------

-start Server

-start client1

-start client2

-start client3

-all combinations of chat between client1 through client3

-dereg client1

-client2 and client3 both messages client1

-reg client1 (offline messages received, and new table broadcasted)

-all clients exit


Output (client1)

>>> [Registering...]
>>> [Client table updated.]				//only client1 is present
>>> [Welcome, You are registered.]
>>> [Client table updated.]				//client2 regs
>>> [Client table updated.]				//client3 regs
>>> client3: hello
>>> client2: hello
>>> send client3 hello
>>> [Message received by client3.]
>>> send client2 hello
>>> [Message received by client2.]
>>> dereg
>>> [You are Offline. Bye.]
>>> reg client1
>>> [Client table updated.]
>>> [Welcome, You are registered.]
>>> You Have Messages
>>> client2: <10/24/2011 02:44:17> offline hello	//offline from client2
>>> client3: <10/24/2011 02:44:27> offline hello	//offline from client3
>>> ctrl + c
>>> [You are Offline. Bye.]
>>> [Exiting]



Output (client2)
>>> [Registering...]
>>> [Client table updated.]				//only client1, client2 are present
>>> [Welcome, You are registered.]
>>> [Client table updated.]				//client3 regs
>>> client3: hello
>>> send client3 hello
>>> [Message received by client3.]
>>> send client1 hello
>>> [Message received by client1.]
>>> client1: hello
>>> [Client table updated.]				//client1 deregs
>>> send client1 offline hello
>>> [client1 is currently off-line, message sent to server.]
>>> [Message received by the server and saved]
>>> [Client table updated.]				//client1 regs
>>> [Client table updated.]				//client1 exits
>>> ctrl + c
>>> [You are Offline. Bye.]
>>> [Exiting]




Output (client3)
>>> [Registering...]
>>> [Client table updated.]				//all three are present
>>> [Welcome, You are registered.]
>>> send client1 hello
>>> [Message received by client1.]
>>> send client2 hello
>>> [Message received by client2.]
>>> client2: hello
>>> client1: hello
>>> [Client table updated.]				//client1 deregs
>>> send client1 offline hello
>>> [client1 is currently off-line, message sent to server.]
>>> [Message received by the server and saved]
>>> [Client table updated.]				//client1 regs
>>> [Client table updated.]				//client1 exits
>>> [Client table updated.]				//client2 exits
>>> ctrl + c
>>> [You are Offline. Bye.]
>>> [Exiting]



--------------------------- Case 2 (Timeouts) ---------------------------------------

-start server

-start client1

-start client2

-dereg client2

-shutdown server

-client1 sends message to client2 (client1 exits)

-client2 regs (client2 exits)

Output (client1)
>>> [Registering...]
>>> [Client table updated.]
>>> [Welcome, You are registered.]
>>> [Client table updated.]
>>> [Client table updated.]
>>> send client2 hello
>>> [client2 is currently off-line, message sent to server.]		//waits 500msec
>>> [Server not responding]
>>> [Exiting]


Output (client2)
>>> [Registering...]
>>> [Client table updated.]
>>> [Welcome, You are registered.]
>>> dereg client2
>>> [You are Offline. Bye.]
>>> reg client2								//waits 500msec
>>> [Server not responding]	
>>> [Exiting]



---------------------------- Case 3 (Timeouts) -------------------------------------

-start server

-start client1

-start client2

-client2 shutsdown unexpectedly (no deregistration)

-client1 sends message to client2 (client2 is on according to client1's table)

-Client1 receives no ack, redirects to server

-server checks status of client2, changes and broadcast table

-server initiates offline chat for client2

-client2 recovers, regs, and receives offline message


Output (client1)
>>> [Registering...]
>>> [Client table updated.]
>>> [Welcome, You are registered.]
>>> [Client table updated.]					//client2 crashes, no deregistration
>>> table							//table command that displays all contacts
>>> Table
>>> client1, /127.0.0.1, 2001, on
>>> client2, /127.0.0.1, 2002, on				//client2 is still assumed to be alive
>>> send client2 hello
>>> [No ACK from client2, message sent to server.]		//no ACK
>>> [Message received by the server and saved]			//redirects to server
>>> [Client table updated.]					//server changes client2 to offline
>>> send client2 hello again
>>> [client2 is currently off-line, message sent to server.]	//client1 now knows client2 is offline
>>> [Message received by the server and saved]
>>> [Client table updated.]


Output (client2)	//after recovery
>>> [Registering...]
>>> [Client table updated.]
>>> [Welcome, You are registered.]
>>> You Have Messages
>>> client1: <10/24/2011 02:56:14> hello
>>> client1: <10/24/2011 02:56:27> hello again




------------------------------- Case 4 (Different registration on the same client) -------------------

-start server

-start client1	//first machine

-start client2	//second machine

-dereg client1, reg client3 on the same port

-client2 and client3 sends message to client1

-client1 regs on another "machine" (different IP and port)	//third machine

-client1 updates table, receives all offline message


Output(1st Machine)
>>> [Registering...]
>>> [Client table updated.]					//client1 regs
>>> [Welcome, You are registered.]
>>> [Client table updated.]					//client2 regs
>>> dereg
>>> [You are Offline. Bye.]					//client1 offline
>>> reg client3							//client3 is not using this machine
>>> [Client table updated.]
>>> [Welcome, You are registered.]
>>> send client2 hello
>>> [Message received by client2.]
>>> client2: hello
>>> send client1 hello	
>>> [client1 is currently off-line, message sent to server.]
>>> [Message received by the server and saved]
>>> [Client table updated.]					//client1 regs from somewhere else



Output(2nd machine)
>>> [Registering...]
>>> [Client table updated.]					//client1 and client2 online
>>> [Welcome, You are registered.]
>>> [Client table updated.]					//client1 deregs
>>> [Client table updated.]					//client3 regs
>>> client3: hello
>>> send client3 hello
>>> [Message received by client3.]
>>> send client1 offline hello
>>> [client1 is currently off-line, message sent to server.]
>>> [Message received by the server and saved]
>>> [Client table updated.]					//client1 regs from somewhere else


Output(3rd machine)
>>> [Registering...]						
>>> [Client table updated.]					//all clients present
>>> [Welcome, You are registered.]
>>> You Have Messages
>>> client2: <10/24/2011 03:02:28> offline hello		//offline from client2
>>> client3: <10/24/2011 03:02:38> hello			//offline from client3
	

This test is to demonstrate that offline messages are not machine dependent and clients will not
receive messages not meant for them even if they are using the same port/IP. 

