Name: Avi Chad-Fiedman
UNI: ajc2212

Running Instructions:
1) In the src directory, run "make"
2) On one machine/terminal, run the command: java InitServer <port number>
3) On any other number of other machines/terminals run the command: java InitClient <IP> <port number>

Commands:
The following commands work as specified in the assignment description:
message <user> <message>
broadcast <message>
online
block <user>
    NOTE: blocks only persist for the lifetime of the server
unblock <user>
logout
getaddress <user>
private <user> <message>

For the P2P Privacy and Consent bonus
friend <user>
    NOTE: users only remain friends for the lifetime of the server
unfriend <user>

Bonus 1) P2P Privacy and Consent Implementation:
When user A requests the address of user B with "getaddress <user>", B receives a notification
that A would like to privately chat.  B can use the "friend <user>" command to allow A to send
private messages.
A user can disallow another user from sending P2P messages with the command "unfriend <user>".

Bonus 2) Guaranteed Message Delivery:
If the client quits unexpectedly, and another client tries to message the terminated client BEFORE the
server detects the dead client, the server will recover and treat the message as an offline message.
This is done in handleDirectMessage(Message message) in the Server class.  If a ConnectException occurs, then
I update the recipient to an offline status and recursively call handleDirectMessage(Message message).  The message
will then be sent as an offline message.

HeartBeat and Timeout:
The client sends a heartbeat message to the server every HEART_RATE (30) seconds.  The server checks
every 5 seconds to see if it has lost any client's pulse (if it hasn't received a heartbeat in
TIMEOUT (45) seconds).