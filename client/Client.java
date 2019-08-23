package client;																	// part of client package

import java.io.*; 																// for input/output
import java.util.Scanner; 														// scanning in user input
import java.net.*;																// creating tcp sockets
import java.nio.charset.StandardCharsets;										// for utf-8 stream

public class Client {

	private String host = "localhost";											// server is run on localhost
	private int port = 1234;													// on port 1234

	private Socket serverSocket; 												// define connections
	private PrintWriter serverOut;
	private BufferedReader serverIn;
	private Scanner scan;														// define scanner
	private String dir; 														// where client should store files


	public Client() {

		serverSocket = null; 													// initialise connections as null
		serverOut = null;
		serverIn = null;
		dir = "client/files/"; 													// client files should be kept here

		scan = new Scanner(System.in); 											// for user input

		/* Connects to Server */
		try {
			connectToServer(); 													// create socket to server

			/* CLIENT'S PROMPT USER FOR SELECTION STATE */
			while(true) {														// loop till client wants to exit
				System.out.print("\n===== Selection =====\n" + 					// ask for input
					"1. CONN: Connect to Server\n" +
					"2. UPLD: Upload File\n" +
					"3. LIST: List Files\n" +
					"4. DWLD: Downlaod File\n" +
					"5. DELF: Delete File\n" +
					"6. QUIT: Close connection and exit\n" +
					"Choice (number or 4 letter abbreviation): ");

				String userInput = scan.nextLine().toUpperCase();				// get user choice and force upper case

				/* CHECKS INPUT WAS CORRECT AND TURNS NUMBERS INTO 4 LETTER ABBREVIATIONS */
				switch(userInput) {
					case "1": userInput = "CONN"; break;
					case "CONN": break;
					case "2": userInput = "UPLD"; break;
					case "UPLD": break;
					case "3": userInput = "LIST"; break;
					case "LIST": break;
					case "4": userInput = "DWLD"; break;
					case "DWLD": break;
					case "5": userInput = "DELF"; break;
					case "DELF": break;
					case "6": userInput = "QUIT"; break;
					case "QUIT": break;
					default: userInput = "ERROR"; break;
				}

				if(userInput.equals("ERROR")) {								    // if user input wasn't correct
					System.out.println("\nIncorrect input, please choose again.");
					continue; 													// continue while loop
				}

				switch(userInput) {												// else run corresponding function
					/* Runs function based on User Input */
					case "CONN": connect(); break;
					case "UPLD": upload(); break;
					case "LIST": list(); break;
					case "DWLD": download(); break;
					case "DELF": deleteFile(); break;
					case "QUIT": quit(); break;
					default: break;
				}

				if (userInput.equals("QUIT")) 									// break out of while loop if user wants to quit
					break;
			}
		}  catch (Exception e) {												// if there is a connection error try to reconnect
			new Client(); 														// by launching new client
		} finally {
			System.out.println("\nThanks for using mxcm21's FTP server.\n" + 	// inform user session is closed when ended
					"Session has been closed. Bye.");
		}

	}

	private void connect() throws IOException  {
		System.out.println("Reinitialise server socket");
		quit();																	// quit connection
		connectToServer(); 														// reconnect
		sendToServer("CONN"); 													// make server ready
		recieveFromServer();													// get server's response
	}

	private void connectToServer() {
		/* LAUNCHES CONNECTION TO FTP SERVER
		 * TRIES TO RECONNECT IF CONNECTION NOT ESTABLISHED */
		try {
			System.out.println("Connecting to: "+host+":"+port);
			serverSocket = new Socket(host, port); 								// connect to ftp server
			serverOut = new PrintWriter(serverSocket.getOutputStream(), true); 	// output from server socket
			serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream())); 	// input stream for server socket
			System.out.println(recieveFromServer());
		} catch (Exception e) {
			System.out.println("Check that the server is running");				// if error, most likely cause is server is not running
			connectToServer(); 													// try to connect again
		}
	}

	private void upload() throws IOException {
		/* UPLOADS FILE TO SERVER */
		File file = null;
		String fileName = "";

		do {
			System.out.println("Filename that you would like to upload (including the extension): "); 	// ask user for input
			fileName = scan.nextLine();												// get user input

			if (fileName.indexOf("/") == -1 && fileName.indexOf("\\") == -1)
				fileName = dir + fileName; 											// add path if just a file name

			file = new File(fileName); 												// get the file

			if(!file.exists()) 														// ask again if doen't exist
				System.out.println("That file doesn't exist, make sure the file is in the client 'files'" +
						" folder or provide a direct path.\nType QUIT to return to main menu.");
		} while (!file.exists() && !fileName.equals(dir + "QUIT"));

		if (file.exists()) {														// if file exists
			String[] fileparts = fileName.split("[\\\\/]"); 						// extract file name from directory path
			fileName = fileparts[fileparts.length - 1];

			sendToServer("UPLD"); 													// make server ready
			sendToServer(Integer.toString(fileName.length()));						// send the server the file name length
			sendToServer(fileName);													// send the server the file name
			sendToServer(Long.toString(file.length())); 							// send server the file length

			byte[] fileBytes = new byte[1024]; 										// initialise byte array buffer
			InputStream fileIn = new FileInputStream(file); 						// get input stream from file
			OutputStream fileOut = serverSocket.getOutputStream();					// get output stream from server
			int count;																// declare count

			String resp = recieveFromServer();										// wait for server ready
			if (resp.equals("READY")) {
				while((count = fileIn.read(fileBytes)) > 0) 						// read bytes while there is bytes
					fileOut.write(fileBytes, 0, count);

				fileOut.flush();													// flush and close
				fileIn.close();

				System.out.println(recieveFromServer()); 							// get server confirmation
			}
		}
	}

	private void list() throws IOException {
		/* LISTS ALL FILES ON SERVER */
		sendToServer("LIST"); 														// call server to produce list
		byte[] buffer = new byte[1024];												// make a new buffer
		int bytesLeft = Integer.parseInt(recieveFromServer()); 						// keep track of how many bytes are left to read
		InputStream in = serverSocket.getInputStream(); 							// get input stream
		ByteArrayOutputStream listing = new ByteArrayOutputStream(); 				// make new output stream
		int count;

		sendToServer("READY");														// send ready message
		while(bytesLeft > 0) { 														// while bytes still need to be read
			count = in.read(buffer); 												// read into buffer
			listing.write(buffer, 0, count); 										// write from the buffer to the file
			bytesLeft -= count; 													// decrement bytes left
		}

		listing.flush();
		String directories = new String(listing.toByteArray(), StandardCharsets.UTF_8); // turn byte stream into string

		System.out.print("\n" + directories); 										// output to client
	}

	private void download() throws IOException {
		/* DOWNLOADS SELECTED FILE FROM SERVER */
		System.out.println("Filename that you would like to download (including the extension): "); 	// ask user for input
		String fileName = scan.nextLine();											// get user input

		sendToServer("DWLD"); 														// make server ready
		sendToServer(Integer.toString(fileName.length()));							// send the server the file name length
		sendToServer(fileName);														// send the server the file name

		int fileLength = Integer.parseInt(recieveFromServer()); 					// get the number of bytes in the file

		if(fileLength == -1) {														// if -1 file not on server
			System.out.println("File ("+fileName+") not on server. Please try " +
					"again or view the list of available files.");
			return;
		} else {
			long startTime = System.currentTimeMillis(); 							// get start time

			File file = new File(dir + fileName); 									// create new file
			FileOutputStream fileOut = new FileOutputStream(file); 					// get an output stream for it
			InputStream fileIn = serverSocket.getInputStream(); 					// get direct server input stream

			/* read in file using byte buffer */
			byte[] buffer = new byte[1024];											// make a new buffer
			int count;
			int bytesLeft = fileLength; 											// keep track of how many bytes are left to read

			sendToServer("READY");

			/* write file out to new file on server */
			while(bytesLeft > 0) { 													// while bytes still need to be read
				count = fileIn.read(buffer); 										// read into buffer
				fileOut.write(buffer, 0, count); 									// write from the buffer to the file
				bytesLeft -= count; 												// decrement bytes left
			}

			fileOut.close(); 														// close out file

			long endTime = System.currentTimeMillis(); 								// get end time
			String time = Double.toString((endTime - startTime) / 1000.0); 			// turn to seconds

			System.out.println(fileName + " transfered: " + Integer.toString(fileLength) +
					" bytes in " + time + " seconds.");								// send process results
		}
	}

	private void deleteFile() throws IOException {
		/* DELETES FILE FROM SERVER */
		System.out.println("Filename that you would like to delete (including the extension): "); 	// ask user for input
		String fileName = scan.nextLine();											// get user input

		sendToServer("DELF"); 														// make server ready
		sendToServer(Integer.toString(fileName.length()));							// send the server the file name length
		sendToServer(fileName);														// send the server the file name

		int resp = Integer.parseInt(recieveFromServer()); 							// get server response

		if(resp == -1) {															// if -1 file not on server
			System.out.println("The file ("+fileName+") does not exist on the " +
					"Server. Please try again or view the list of available files.");
			return;
		} else {
			String confirm = "";

			while (!confirm.equals("Y") && !confirm.equals("N")) { 					// loop till y or n recieved
				System.out.println("File ("+fileName+") exists on the Server. Are you " + 	// ask for confirmation
						"sure you want to delete it, this can not be undo. (Yes/No)");

				confirm = scan.nextLine().toUpperCase();						// make input upper case
				if (confirm.equals("YES")) 										// map yes to y
					confirm = "Y";
				else if (confirm.equals("NO")) 									// and no to n
					confirm = "N";
				else if (!confirm.equals("Y") && !confirm.equals("N")) 			// if not y or n
					System.out.println("Please enter Yes or No."); 				// ask again
			}

			if (confirm.equals("N")) { 											// if N
				System.out.println("Delete abondoned by the user!");
				sendToServer("CANCELLED");										// tell server delete abandoned
			} else {															// else
				sendToServer("CONTINUE"); 										// tell the server to continue
				System.out.println(recieveFromServer()); 						// output server message
			}
		}
	}

	private void quit() {
		/* CLOSES CONNECTION WITH SERVER */
		sendToServer("QUIT"); 													// make server ready
		try {
			System.out.println("Server: "+recieveFromServer()); 				// recieve from server
			serverIn.close(); 													// close everything
			serverOut.close();
			serverSocket.close();
		} catch (Exception e) {
			System.out.println("Error closing connection.");
		}
	}

	private void sendToServer(String message) {
		/* SENDS TO SERVER AND FLUSHES */
		try {
			serverOut.println(message); 										// print message to server
			serverOut.flush();													// flush print writer
		} catch (Exception e) {
			System.out.println("Error sending message to Server.");
		}
	}

	private String recieveFromServer() throws IOException {
		/* GETS SERVER INPUT */
		String serverAns = ""; 													// initial string empty
		try {
			serverAns = serverIn.readLine(); 									// read in server answer
		} catch (Exception e) {
			System.out.println("Error getting message from Server.");
			throw new IOException();
		}
		return serverAns; 														// return answer
	}

	public static void main(String[] args) throws IOException {
		new Client(); 															// launch new Client
	}
}
