STRUCTURE :
We have 3 main classes: DatabaseManager, Server, and Client.
Server + DatabaseManager basically act as the back end, and the Client is the front end.
All UI pages talk only to the Client class.
DatabaseManager : all methods that directly communicate with the postgress database server are here, the queries strings, the checks the fetches all here to call upon
Server : this class handles accepts the socket connections and uses threads to work with multiple clients simultaneously, and takes diffrent commands the clients sends and acts accordingly
Client : holds all communication, and exposes simple methods that the ui can call



DATABASE :
The database holds 2 main tables :
Users : [id, first_name, last_name, username, password]
Messages : [id, sender_id, receiver_id, message, timestamp]
The code also has Models that has variables matching the table colomns for conveniency in the logic



SERVER AND SOCKETS :
After the server connects with a client, the first input is ALWAYS a command, this command represents the method the client wants to run.
We use a switch on that command, and each case reads as many inputs as that method needs, so:
First line = command
After that = however many argument lines that command needs
Some DatabaseManager methods return stuff like List<user> (getActiveUsers()) or List<message> (getConversation()) and since we can't send direct Lost<> ellements via sockets without usign a objectinput i decided to srialize them to a json file which i turn to a string that can be sent with the sokcets and deserialized on the client side, the serialization and deserialization is done with Gson yo which the .jar file is included inside the all.
Note: while serializing i discovered the type localdatetime (the type used for the timestamp variable of the message model) can't be serialized directly without a special gson adapter, so as a work around i turn the timestamp value to a ISO string and turn it back on the client side



UI :
The ui has 3 pages : Loginpage, SignupPage, ChatMenu
loginapage : after a successful login it creates an instant of the Client and directs the user to ChatMenu
SignupPage : checks if inputed info are correct and creates a new user in the db then directs the user to LoginPage(conditions built in to the UI itself)
ChatMenu : takes a client instant as an argument and uses it to communicate with the db using its exposed methods



DEPLOYMENT AND NETWORK :
Port forwarding is set up on the router, the apps port is 51102
Set up a DHCP reservation for my machine using the mac adress and chose an ip to be my static ip that the router can direct packages going to our port into my machine
We can't have a static public ip (Merci algerie Télécom) sop we set up a duckdns adress which is :
brxdesktopapp.duckdns.org

As a fallback and backup my laptop has his own duckdns adress which is :
brxlaptopapp.duckdns.org

The code tried the desktop, if it fails falls back to the laptop, if that fails too it goes to localhost(mainly for debugging)
