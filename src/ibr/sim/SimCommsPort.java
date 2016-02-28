package ibr.sim;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ibr.core.CommsPort;
import ibr.core.Message;

public class SimCommsPort implements CommsPort {

	private int port;
	private Host host;
	private BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();

	public SimCommsPort(Host host, int port) {
		this.host = host;
		this.port = port;
	}
	
	@Override
	public int getPort()
	{
		return port;
	}

	@Override
	public void sendMsg(Message m) {
		host.send(m);
	}

	@Override
	public Message getNextMsg() throws InterruptedException {
		return queue.take();
	}
	
	public void deliver(Message m) {
		queue.offer(m);
	}
	

	public String toString() {
		return host.toString() + ":" + Integer.toString(port);
	}

}
