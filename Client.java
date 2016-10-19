import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Client {
    static Socket requestSocket[];           //socket connect to the server
    static ObjectOutputStream out[];         //stream write to the socket
    static ObjectInputStream in[];          //stream read from the socket
    static String message;                //message send to the server
    static String MESSAGE;                //capitalized message read from the server

    static int peerId;
    static int ownIndex;
    static boolean[] madeConnection;
    static int[] peerIds;
    static String[] hostNames;
    static int[] portNumbers;
    static boolean[] hasFile;

    public Client(int peerId, int[] peerIds, String[] hostNames, int[] portNumbers, boolean[] hasFile) {
        this.peerId = peerId;
        this.peerIds = peerIds;
        this.hostNames = hostNames;
        this.portNumbers = portNumbers;
        this.hasFile = hasFile; 

        madeConnection = new boolean[peerIds.length];
        requestSocket = new Socket[peerIds.length];
        out = new ObjectOutputStream[peerIds.length];
        in = new ObjectInputStream[peerIds.length];

        ownIndex = Arrays.binarySearch(peerIds, peerId);
        madeConnection[ownIndex] = true;
        run();
    }

    private static void run() {
        try {
            int connectionsLeft = peerIds.length-1;
            while (connectionsLeft > 4) { //needs to be reverted to 0 after testing
                boolean connectionRefused[] = new boolean[peerIds.length];

                for (int i = 0; i < peerIds.length; i++) {
                    if (i != ownIndex && !madeConnection[i]) {
                        System.out.println("Attempting to connect to " + hostNames[i] + 
                            " on port " + portNumbers[i]);
                        try {
                            //create a socket to connect to the server
                            requestSocket[i] = new Socket(hostNames[i], portNumbers[i]);
                            System.out.println("Connected to " + hostNames[i] + 
                                " in port " + portNumbers[i]);    
                            connectionsLeft--;
                            madeConnection[i] = true;
                        } catch (ConnectException e) {
                            connectionRefused[i] = true;
                        }
                    }
                }
                System.out.println();
                for (int i = 0; i < peerIds.length; i++) {
                    if (connectionRefused[i]) {
                        System.out.println("Connection refused for " + hostNames[i] + 
                            " on port " + portNumbers[i]);
                    }
                }
                if (connectionsLeft > 0) {
                //Wait four seconds before attempting to reconnect
                    System.out.println();
                    System.out.print("Waiting to reconnect");
                    Thread.sleep(1000);
                    System.out.print(".");
                    Thread.sleep(1000);
                    System.out.print(".");
                    Thread.sleep(1000);
                    System.out.print(".");
                    Thread.sleep(1000);
                    System.out.println();
                }
            }
            System.out.println();
            System.out.println("Normally this would repeat until all connections established.");
            System.out.println("However, to allow for testing, this feature has been disabled.");
            System.out.println();
            //System.out.println("All connections have been established.");

            //initialize inputStream and outputStream
            for (int i = 0; i < peerIds.length-4; i++) {  //needs to be reverted to not -4 after testing
                if (i != ownIndex) {       
                    out[i] = new ObjectOutputStream(requestSocket[i].getOutputStream());
                    out[i].flush();
                    in[i] = new ObjectInputStream(requestSocket[i].getInputStream());
                    System.out.println(i);
                }
            }
            System.out.println("left");
            //get Input from standard input
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            //create handshake message and send it to peers
            byte[] handshakeHeader = new byte[18];
            handshakeHeader = "P2PFILESHARINGPROJ".getBytes("UTF-8");
            
            byte[] zeroBits = new byte[10];
            byte[] peeridArray = ByteBuffer.allocate(4).putInt(peerId).array();

            ByteBuffer handshakeBuffer = ByteBuffer.allocate(32);
            
            handshakeBuffer.put(handshakeHeader);
            handshakeBuffer.put(zeroBits);
            handshakeBuffer.put(peeridArray);
            
            byte[] handshake = handshakeBuffer.array();

            //create bitfield message and send it to peers
            byte[] bitfieldPayload = {(byte)1,(byte)6};
            Message bitfield = new Message(2,(byte)5, bitfieldPayload);
            System.out.println(bitfield.toString());

            for (int i = 0; i < peerIds.length-4; i++) { //needs to be reverted to not -4 after testing
                if (i != ownIndex) {
                    out[i].write(handshake);
                    out[i].write(bitfield.getMessageBytes());
                }
            }
            
            handshakeBuffer.clear();




            while(true)
            {
                System.out.print("Hello, please input a sentence: ");
                //read a sentence from the standard input
                message = bufferedReader.readLine();
                //Send the sentence to the server
                sendMessage(message, (ownIndex+1));
                //Receive the upperCase sentence from the server
                MESSAGE = (String)in[1].readObject();
                //show the message to the user
                System.out.println("Receive message: " + MESSAGE);
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
        catch(UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
        } 
        catch (InterruptedException e) {
            System.err.println("Interrupted thread execution.");
        }
        finally {
            //Close connections
            try {
                for (int i = 0; i < peerIds.length-4; i++) { //needs to be reverted to not -4 after testing
                    if (i != ownIndex) {
                        in[i].close();
                        out[i].close();
                        requestSocket[i].close();
                    }
                }
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
    //send a message to the output stream
    private static void sendMessage(String msg, int socketIndex)
    {
        try{
            //stream write the message
            out[socketIndex].writeObject(msg);
            out[socketIndex].flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
}
