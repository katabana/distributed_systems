## SOCKETS

**Chat application**

Clients connect with the server via TCP. 
The server receives messages from every client and forwards them to other clients with an id/nickname of the sender.
The server is multi-threaded - connection from a client has a separate thread.

Additionally, there is a UDP channel. The server and clients open a UDP channel (with the same port number as TCP).
With a command "U" from a client, a message is sent via UDP to the server that forwards it to other clients. 

As an alternative option, there is also Multicast version of the functionality described above available with a command "M". 
Multicast sends directly to everyone via group address. Multicast messages are anonymous.

For the efficiency of this simple chat application a thread pool mechanism was used.

- - -

Only for Python 3.

**Run the server:**

``` python3 Server.py ```

Starts the server on the port 12345. 
The multicast address is '255.0.0.250:8123'. 
All values can be changed in the source code.

**Start a new client:**

``` python3 Client.py ```

Starts the client.
To send a message, write a text in the console with opened client and press 'Enter'.

**Send messages:**

*[command] message_text*

When the command is not given, the message is sent via TCP as it is described above.

**Available commands:**

*M* - Multicast message

*U* - UDP message
