package ibr.core;

import java.util.Queue;

public class Node {
	
	private Queue<Message> unsendableQueue;

	public Queue<Message> getUnsendableQueue() {
		return unsendableQueue;
	}

}
