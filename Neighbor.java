//Public class to hold the data obtained from neighbors during the transfor process.
public class Neighbor {
    public byte[] bitmap;
    public boolean choked = true;
    public boolean interested = false;
    public int peerId;

    public Neighbor() {
        
    }
}