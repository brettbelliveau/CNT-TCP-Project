/**************************************

Message Class
Consumes Message String and Creates Appropriate Object
Inputs: Message String

**************************************/

import java.util.*;
import java.lang.*;

public class Message {

	public static final int choke = 0;
	public static final int unchoke = 1;
	public static final int interested = 2;
	public static final int not_interested = 3;
	public static final int have = 4;
	public static final int bitfield = 5;
	public static final int request = 6;
	public static final int piece = 7;

	public static int length;
	public static int type;
	public static String payload;

	public Message(String line) {
		Scanner sc = new Scanner(line);
		
		length = Integer.parseInt(sc.next(), 2);
		type = Integer.parseInt(sc.next(), 2);
		
		if (hasPayload(type))
			payload = sc.next();
		else
			payload = "";
	}

	public boolean hasPayload(int type) {
		return (type == have || type == bitfield 
			|| type == request || type == piece);
	}

	public String toString() {
		String str = "Length: " + length + 
		", Type = " + type + ", Payload:" + payload;
		return str;
	}

}