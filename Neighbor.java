/* To compile, run the following command:
 * javac Client.java Message.java Neighbor.java ServerListener.java PeerProcess.java
 */

//Public class to hold the data obtained from neighbors during the transfor process.
public class Neighbor {
    public byte[] bitmap;
    public boolean choked = true;
    public boolean interested = false;
    public int peerId;
    public int portNumber;

    public String hostName = "";

    public boolean hasFile = false;
    public boolean madeConnection = false;
    public boolean connectionRefused = false;
    public boolean hasHandshakeReceived = false;
    public boolean hasBitfieldReceived = false;
    public boolean hasHandshakeSent = false;
    public boolean hasBitfieldSent = false;
    public boolean waitingForPiece = false;
    public int pieceNumber = -1;

    public Neighbor() {
        
    }
}