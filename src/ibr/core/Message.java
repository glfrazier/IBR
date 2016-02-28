package ibr.core;

import java.net.InetAddress;

public interface Message {

	public InetAddress getSrcAddr();

	public InetAddress getDstAddr();

	public int getSrcPort();

	public int getDstPort();


}
