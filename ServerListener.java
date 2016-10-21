import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class ServerListener {

    public List receivedMessages; //Synchronized list of messages received which is thread safe

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
	    	//System.out.println("The server is starting." + '\n');

            try {
                listener = new ServerSocket(portNumber);

                try{
                    //initialize Input and Output streams
                    in = new ObjectInputStream(connection.getInputStream());

                    try{
                        //Endless loop that receives messages
                        while(true)
                        {
                            //receive the message sent from the client
                            message = convertToMessage((byte[])in.readObject());
                            //Add the message to our list:
                            synchronized (receivedMessages) {
                                receivedMessages.add(message);
                            }
                            //shosw the message to the user
                            System.out.println("Received message: " + message.type + " from client.");
                        }
                    }
                    //Catch unknown data types
                    catch(ClassNotFoundException classnot){
                        connected = false;
                        System.err.println("Data received in unknown format");
                    }
                }
                //
                catch(IOException ioException){
                    connected = false;
                    //System.out.println("Disconnect with Client " + no);
                }
                finally{
                    //Close connections
                    try{
                        in.close();
                        connection.close();
                        connected = false;
                    }
                    catch(IOException ioException){
                        connected = false;
                        //System.out.println("Disconnect with Client " + no);
                    }
                }

                //System.out.println("Client "  + clientNum + " is connected!");
            } catch (SocketException e) {
                connected = false;
                System.out.println(e.getStackTrace());
            } catch (IOException e ) {

            }
            finally {
                try {
                listener.close();
                } catch (Exception e) {

                }
            }
        }
    }

    private Message convertToMessage(byte[] data) {
        int length = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 3)).getInt();
        byte[] payload = Arrays.copyOfRange(data, 4, data.length);
        return new Message(length, data[5], payload);
    }
}
