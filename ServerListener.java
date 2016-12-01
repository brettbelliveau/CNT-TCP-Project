/* To compile, run the following command:
 * javac Client.java Message.java Neighbor.java ServerListener.java PeerProcess.java
 */

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;

public class ServerListener {

    public List<Message> receivedMessages; //Synchronized list of messages received which is thread safe

    private int peerId;
    private String hostName;
    private int portNumber; //The listener will be listening on this port number
    private ServerSocket listener;

    private boolean connected; // Boolean to know if it is connected

    private Socket connection;
    private ObjectInputStream in;   //stream read from the socket

    private Message message;
    public ServerListener(int peerId, String hostName, int portNumber) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.portNumber = portNumber;

        //Init listener:
        listener = null;
        //Init message list:
        receivedMessages = Collections.synchronizedList(new ArrayList<Message>());
        new ListenerThread().start();
    }

    private class ListenerThread extends Thread {

    	public void run() {
            System.out.println("The server is running waiting for connections." + '\n');
            int clientNum = 0;
            ServerSocket listener = null;
            try {
                while (true) {
                    try {
                        listener = new ServerSocket(portNumber);
                        new Handler(listener.accept(),clientNum).start();
                        System.out.println("Client "  + clientNum + " is connected!");
                        clientNum++;
                    } catch (SocketException e) {
                        Thread.sleep(50);
                    }
                    finally {
                        listener.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Message convertToMessage(byte[] data, int clientID) {
        //Check for handshake:
        if (data.length >= 18) {
            byte[] bytes = Arrays.copyOfRange(data, 0, 18);
            String header = new String(bytes, Charset.forName("UTF-8"));
            //System.out.println("header: " + header);
            if (header.equalsIgnoreCase("P2PFILESHARINGPROJ")) {
                //System.out.println("Found handshake header");
                int newPeerID = ByteBuffer.allocate(4).put(Arrays.copyOfRange(data, 28, 32)).getInt(0);
                return new Message(newPeerID, (byte)Message.handshake, null, clientID);
            }
        }

        //Converts the incoming bytes into the actual Message with its associated clientID
        int length = ByteBuffer.allocate(4).put(Arrays.copyOfRange(data, 0, 4)).getInt(0);
        byte[] payload = Arrays.copyOfRange(data, 5, data.length);
        return new Message(length, data[4], payload, clientID);
    }

     /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
     private class Handler extends Thread {
        private Message message;    //message received from the client
        private Socket connection;
        private ObjectInputStream in;   //stream read from the socket
        private int clientID;     //The index number of the client

        public Handler(Socket connection, int clientID) {
            this.connection = connection;
            this.clientID = clientID;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                in = new ObjectInputStream(connection.getInputStream());

                try{

                    while(true)
                    {
                        //receive the message sent from the client
                        byte[] temp = (byte[])in.readObject();
                        /*for (int i = 0; i < temp.length; i++) {
                            System.out.println(i + " : " + temp[i]);
                        }*/
                        message = convertToMessage(temp, clientID);
                        //System.out.println("Placing message in array: " + message.toString());


                        //Add the message to our list (Synchronized makes it thread safe):
                        synchronized (receivedMessages) {
                            receivedMessages.add(message);
                        }
                        //show the message to the user
                        System.out.println("Received message: " + message.toString() + " from client " + clientID);
                        /*
                         *
                         * TODO: Add this to the logger somehow
                         *
                         */
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + clientID);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + clientID);
                }
            }
        }

    }

}
