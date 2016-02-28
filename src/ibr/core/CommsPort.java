package ibr.core;

import java.net.InetAddress;

public interface CommsPort {

	public void sendMsg(Message m);
	
	Message getNextMsg() throws InterruptedException;

	public int getPort();

}
