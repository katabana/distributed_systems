## SOCKETS

**Chat application**

Clients connect with server via TCP. 
Server receives messages from every client and forwards them to other clients with an id/nickname of sender.
Server is multi-threaded - connection from client has a separate thread.

Additionally, there is a UDP channel. Server and clients open a UDP channel (with the same port number as TCP).
With a command "U" from client a message is sent via UDP to the server that forwards it to other clients. 

As an alternative option there is also Multicast version of the functionality described above available with a command "M". 
Multicast sends directly to everyone via group address. Multicast messages are anonymou

For the efficiency of this simple chat application thread pool was used.

- - -

Only for Python 3.

**Run the server:**

``` python3 Server.py ```

Starts the server on the port 12345. 
Multicast address is '255.0.0.250:8123'. 
All values can be changed in source code.

**Start a new client:**

``` python3 Client.py ```

Starts the client.
To send message write a text in the console with opened client and press 'Enter'.

**Send messages:**

*[command] message_text*

When command is not given the message is sent via TCP as it is described above.

**Available commands:**

*M* - Multicast message

*U* - UDP message
