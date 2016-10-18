import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Server {

    private static final int sPort = 8000;   //The server will be listening on this port number

    public Server() {
    	new ServerThread().start();
    }

    private static class ServerThread extends Thread {

    	public void run() {
	    	System.out.println("The server is running.");
	        int clientNum = 1;
	        ServerSocket listener = null;
	        try {
		    	try {
		        	listener = new ServerSocket(sPort);
		        	while(true) {
		                new Handler(listener.accept(),clientNum).start();
		                System.out.println("Client "  + clientNum + " is connected!");
		                clientNum++;
		            }
		        } catch (Exception e) {
		        	e.printStackTrace();
		        }
		         finally {
		            listener.close();
		        }
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
    	}
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    private static class Handler extends Thread {
        private String message;    //message received from the client
        private String MESSAGE;    //uppercase message send to the client
        private Socket connection;
        private ObjectInputStream in;	//stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client

        public Handler(Socket connection, int no) {
            this.connection = connection;
            this.no = no;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                try{
                    byte[] handshake = new byte[32];
                    in.readFully(handshake);

                    byte[] headerBytes = new byte[18];
                    byte[] zeroBits = new byte[10];
                    byte[] peeridBytes = new byte[4];

                    System.arraycopy(handshake,0,headerBytes,0,18);
                    System.arraycopy(handshake,18,zeroBits,0,10);
                    System.arraycopy(handshake,28,peeridBytes,0,4);

                    String header = new String(headerBytes);
                    int peerId = ByteBuffer.wrap(peeridBytes).getInt();

                    System.out.println("Header: " + header);
                    System.out.println("Zero bits: " + zeroBits.toString());
                    System.out.println("Peer id: " + peerId);


                    while(true)
                    {
                        //receive the message sent from the client
                        message = (String)in.readObject();
                        //show the message to the user
                        System.out.println("Receive message: " + message + " from client " + no);
                        //Capitalize all letters in the message
                        MESSAGE = message.toUpperCase();
                        //send MESSAGE back to the client
                        sendMessage(MESSAGE);
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(String msg)
        {
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }

}
