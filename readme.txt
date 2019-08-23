I have two packages, in the first 'client', is my program Client.java. In the second 'server', is my program Server.java.
Make sure your computer has javac and java (you may need to first run Eclipse if on a Uni Windows machine). 

Files that you wish to transfer from the client should be in the "files" subfolder in the "client" folder.

The below can be achieved in Windows my running my batch file "compileAndRun.bat" *
	- First on the command-line navigate to the folder  "Sardar_Jaf".
	- Run and compile the server by typing "javac server\Server.java & java server.Server", it uses port 1234, so make sure nothing is currently using this port.
	- Next open a new command-line and navigate to "Sardar_Jaf" launch the client by typing "javac client\Client.java & java client.Client"

Your client will then be prompted with a menu. 
	- Using either the number or four-letter abbreviation you can perform all the commands.
	- Appropriate prompts will then instruct you on what to do, and all errors are handled appropriately. 
	- If there are any catastrophic errros the Client will close and create a new connection, if the Server goes down, it will keep trying to reconnect untill the Server is back up.

My Server works for multiple Clients.

* (If you get an "Access Denied" error trying to execute the batch file - use the commands javac & java once on the command line to fix it)
* (If you get a file not found error navigate to the folder by clicking on the J drive directly. i.e. if the address in windows explorer is \\Hudson\ instead of J:\ it won't find the file)