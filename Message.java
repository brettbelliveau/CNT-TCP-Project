/**************************************

Message Class
Consumes Message String and Creates Appropriate Object
Inputs: Message String

**************************************/

import java.util.*;
import java.lang.*;
import java.nio.*;

public class Message {

	public static final int choke = 0;
	public static final int unchoke = 1;
	public static final int interested = 2;
	public static final int not_interested = 3;
	public static final int have = 4;
	public static final int bitfield = 5;
	public static final int request = 6;
	public static final int piece = 7;
	public static final int handshake = 8;

	public int length;
	public int clientID = -1;//-1 means that this value has not been set
	public byte type;
	public byte[] lengthB;
	public byte[] payload;

	public Message(){}

	public Message(int length, byte type, byte[] payload, int clientID) { 
		this(length, type, payload);
		this.clientID = clientID;
	}

	public Message(int length, byte type, byte[] payload) {
		this.length = length;
		this.lengthB = ByteBuffer.allocate(4).putInt(length).array();
		this.type = type;

		if (hasPayload((int)type)) {
			this.payload = new byte[length];
			this.payload = payload;
		}
		else {
			this.payload = null;
		}

	}

	public Message(int length, byte[] data) {
		this.length = length;
		this.type = data[0];

		if(hasPayload((int)type)) {
			payload = new byte[length];
			System.arraycopy(data, 1, payload, 0, length);
		}
		else {
		    payload = null;
		}
	}

	public boolean hasPayload(int type) {
		return (type == have || type == bitfield 
			|| type == request || type == piece);
	}

	public String toString() {
		String str = "Length: " + length + 
		", Type = " + (int)type + ", Payload:" + Arrays.toString(payload);
		return str;
	}

	public byte[] getMessageBytes() {
		ByteBuffer messageBuffer = ByteBuffer.allocate(5 + length);

		messageBuffer.put(lengthB);
		messageBuffer.put(type);

		if(hasPayload((int)type)) {
			messageBuffer.put(payload);
		}

		return messageBuffer.array();
	}

}