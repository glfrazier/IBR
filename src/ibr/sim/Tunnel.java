package ibr.sim;

import java.net.InetAddress;

public class Tunnel {

	private final InetAddress addr;
	
	public Tunnel(InetAddress addr) {
		this.addr = addr;
	}

	public boolean equals(Object arg0) {
		if (arg0 instanceof Tunnel) {
			return addr.equals(((Tunnel)arg0).addr);
		}
		return false;
	}

	public byte[] getAddress() {
		return addr.getAddress();
	}

	public String getHostAddress() {
		return addr.getHostAddress();
	}

	public int hashCode() {
		return addr.hashCode();
	}

	public String toString() {
		return addr.toString();
	}
	
}
