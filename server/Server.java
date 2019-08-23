package server; 																// part of server package

import java.io.*;																// for input and output
import java.net.*; 																// for tcp socket connection
import java.nio.charset.StandardCharsets;										// for utf-8 stream

public class Server extends Thread {
	/* Server extends thread to allow for multiple clients */

	private static final int port = 1234;										// want to run on port 1234
	private static int numberClients; 											// number of clients
	private int socketNumber; 													// socket number
	private Socket client; 														// for multiple clients
	private BufferedReader clientIn; 	 										// initialise IO streams
	private PrintWriter clientOut;
	private String directory; 													// want all files in sub folder


	private Server(Socket socket) {
		/* this enables multiple clients */
		this.socketNumber = numberClients; 										// keeps track of how many clients are connected
		this.client = socket; 													// set the client to the socket
		System.out.println("===== New client "+socketNumber+" connected =====");
		numberClients++; 														// increment number of clients
		start();																// launch the new socket
	}

	public void run() {
		/* RUNS WAIT STATE AND CLOSES CONNECTION GRACEFULLY */

		try {
			directory = "server/files/"; 										// server files stored here
			clientIn = new BufferedReader(new InputStreamReader(client.getInputStream()));	// gets IO streams
			clientOut = new PrintWriter(client.getOutputStream(), true);
			conn(); 															// launch connection
			waitState();														// go into wait state
		} catch (SocketException e) {
			System.out.println("Client "+socketNumber+" closed connection."); 		// error if client closes connection
		} catch (IOException e) {
			System.out.println("Error reading input from Client "+socketNumber);	// catch errors
		} finally {
			try {
				clientIn.close();												// make sure everything is closed on exit
				clientOut.close();
				client.close();
				System.out.println("Connection with Client "+socketNumber+" closed.");
			} catch (IOException e) {}
		}
	}

	private void waitState() throws SocketException, IOException {
		/* WAIT STATE OF SERVER, EXPECTING CHOICE FROM CLIENT, LOOPS UNTIll QUIT  */
		while(true) { 															// stays in wait state
			System.out.println("===== Server is in WAIT state ======");
			String choice = recieveFromClient(); 								// get choice from client
			System.out.println("Recieved Choice: "+choice+" from Client "+socketNumber);

			String answer = "";
			switch(choice) { 													// runs appropriate function
				/* RUNS FUNCTION BASED ON USER INPUT */
				case "CONN": conn(); break;
				case "UPLD": upload(); break;
				case "LIST": list(); break;
				case "DWLD": download(); break;
				case "DELF": delete(); break;
				case "QUIT": quit(); break;
				default: answer = "ERROR incorrect input"; break;
			}

			if (choice.equals("QUIT")) 												// if quit, no longer need to be in loop
				break;																// break out of while loop
		}
	}

	private void conn() {
		/* Return successful conenction to Server */
		sendToClient("Connected to mxcm21's FTP Server!"); 							// on connect send success message to client
	}

	private void upload() {
		/* ALLOWS CLIENT TO UPLOAD A FILE IN BYTES */
		try {
			long startTime = System.currentTimeMillis(); 							// get start time

			/* get file info */
			int fileNameLength = Integer.parseInt(recieveFromClient());				// get file name length
			String fileName = recieveFromClient();  								// get the file name
			int fileLength = Integer.parseInt(recieveFromClient()); 				// get the number of bytes in the file

			/* initialise new file */
			File file = new File(directory + fileName); 							// create new file
			FileOutputStream fileOut = new FileOutputStream(file); 					// get an output stream for it
			InputStream fileIn = client.getInputStream(); 							// get direct client input stream

			/* read in file using byte buffer */
			byte[] buffer = new byte[1024];											// make a new buffer
			int count;
			int bytesLeft = fileLength; 											// keep track of how many bytes are left to read

			sendToClient("READY"); 													// tell client it is ready to send

			/* write file out to new file on server */
			while(bytesLeft > 0) { 													// while bytes still need to be read
				count = fileIn.read(buffer); 										// read into buffer
				fileOut.write(buffer, 0, count); 									// write from the buffer to the file
				bytesLeft -= count; 												// decrement bytes left
			}

			fileOut.close(); 														// close out file

			long endTime = System.currentTimeMillis(); 								// get end time
			String time = Double.toString((endTime - startTime) / 1000.0); 			// turn to seconds

			sendToClient(fileName + " transfered: " + Integer.toString(fileLength) +
					" bytes in " + time + " seconds.");								// send process results

		} catch (IOException e) {
			System.out.println("Error uploading file from Client "+socketNumber);
			sendToClient("Upload Error");
		}
	}

	private void list() {
		/* LISTS ALL FILES IN DIRECTORY */
		try {
			File folder = new File(directory); 											// path to file
			String directories = "== Directory Listing for mxcm21's FTP Server: ==\n";	// output string

			for(File f : folder.listFiles()) 											// loop through all files
				if(f.isFile())															// check it's a file
					directories += " - " + f.getName() + "\n"; 							// append to listing

			if (folder.listFiles().length == 0)											// if on file on server
				directories = "\n== No files found, try uploading a file ==\n";

			byte[] buffer = new byte[1024]; 											// initialise byte array buffer
			InputStream listing = new ByteArrayInputStream(directories.getBytes(StandardCharsets.UTF_8)); 	// get an input stream from string
			OutputStream out = client.getOutputStream();								// get output stream
			int count;

			sendToClient(Integer.toString(directories.getBytes(StandardCharsets.UTF_8).length)); // send the length of listing

			String resp = recieveFromClient(); 											// wait for client to be ready
			if (resp.equals("READY")) {
				while((count = listing.read(buffer)) > 0)  								// send listing
					out.write(buffer, 0, count);
			}

			out.flush(); 																// flush and close
			listing.close();

			System.out.println("finished");
		} catch (IOException e) {System.out.println("Error listing directories.");}
	}

	private void download() {
		/* SENDS FILE TO CLIENT */
		try {
			int fileNameLength = Integer.parseInt(recieveFromClient());					// get file name length
			String fileName = recieveFromClient();										// get file name

			File file = new File(directory + fileName); 								// path to file

			if(!file.exists()) {
				sendToClient("-1"); 													// bad response to client
				return; 																// return to wait state
			} else {
				sendToClient(Long.toString(file.length())); 							// send size of file

				byte[] fileBytes = new byte[1024]; 										// initialise byte array buffer
				InputStream fileIn = new FileInputStream(file); 						// input/output streams
				OutputStream fileOut = client.getOutputStream();
				int count;

				String resp = recieveFromClient();										// wait for client to be ready
				if (resp.equals("READY")) {
					while((count = fileIn.read(fileBytes)) > 0)							// send file as bytes
						fileOut.write(fileBytes, 0, count);

					fileOut.flush(); 													// flush and close
					fileIn.close();
				}
			}
		} catch (IOException e) {
			System.out.println("Error downloading file.");
		}
	}

	private void delete() {
		/* DELETES SPECIFIED FILE FROM SERVER */
		try {
			int fileNameLength = Integer.parseInt(recieveFromClient());					// get file name length
			String fileName = recieveFromClient();										// get file name

			File file = new File(directory + fileName); 								// path to file

			if(!file.exists()) {
				sendToClient("-1"); 													// bad response to client
				return; 																// return to wait state
			} else {
				sendToClient("1"); 														// send good response

				String resp = recieveFromClient();										// send file
				if (resp.equals("CONTINUE")) {
					file.delete();														// deletes file
					sendToClient("File ("+fileName+") successfully deleted.");
				}
			}
		} catch (IOException e) {
			System.out.println("Error downloading file.");
		}
	}

	private void quit() {
		/* Quitting the Server closes the client socket */
		sendToClient("Connection closed."); 											// on quit tell client connection is closing
	}

	private void sendToClient(String message) {
		/* SENDS TO CLIENT AND FLUSHES */
		try {
			clientOut.println(message); 												// sends message to client
			clientOut.flush(); 															// flushes print writer
		} catch (Exception e) {
			System.out.println("Error sending message to Client "+socketNumber);
		}
	}

	private String recieveFromClient() throws IOException {
		/* GETS CLIENT INPUT */
		String clientInput = "";
		try {
			clientInput = clientIn.readLine(); 									// read in client input
		} catch (Exception e) {
			System.out.println("Error getting message from Client "+socketNumber);
			throw new IOException();
		}
		return clientInput; 													// return client input
	}

	public static void main(String[] args) {
		/* Launches server to accept multiple client */
		ServerSocket portListener = null; 										// initialise server
		numberClients = 1;

		try {
			portListener = new ServerSocket(port); 								// bind to port
			System.out.println("FTP Server listening on localhost:" + port);	// announce it is listening

			while(true)  														// constantly listen for requests
				new Server(portListener.accept()); 								// create new client socket for each request

		} catch (IOException e) {
			System.out.println("Server didn't start properly, please try again."); 	// for if server didn't launch properly
			System.out.println("Check if port 1234 is already in use."); 		// can't have 2 binded on same port
		} finally {
			try {
				if (portListener != null)
					portListener.close(); 										// close the server when program closes
			} catch (IOException e) {e.printStackTrace();}
		}
	}
}
