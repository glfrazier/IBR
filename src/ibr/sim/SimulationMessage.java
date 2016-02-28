package ibr.sim;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;

import ibr.core.Message;

public class SimulationMessage implements Message {

	private InetAddress srcAddr;
	private InetAddress dstAddr;
	private int srcPort;
	private int dstPort;

	private Type type;

	private final long UUID;
	private Role role;
	private SimulationMessage inResponseTo;

	private static AtomicLong index = new AtomicLong(1);

	public enum Type {
		NONE, APPLICATION, ATTACK
	};

	public enum Role {
		NONE, REQUEST, RESPONSE
	};

	private SimulationMessage(Role role, Type type) {
		this.role = role;
		this.type = type;
		this.UUID = index.getAndIncrement();
	}

	public SimulationMessage(Role role, Type type, InetAddress srcAddr, InetAddress dstAddr, int srcPort, int dstPort) {
		this(role, type);
		this.srcAddr = srcAddr;
		this.srcPort = srcPort;
		this.dstAddr = dstAddr;
		this.dstPort = dstPort;
	}

	private SimulationMessage(Type application, SimulationMessage msg) {
		this(Role.RESPONSE, application);
		inResponseTo = msg;
		this.srcAddr = msg.dstAddr;
		this.srcPort = msg.dstPort;
		this.dstAddr = msg.srcAddr;
		this.dstPort = msg.srcPort;
	}

	@Override
	public InetAddress getSrcAddr() {
		return srcAddr;
	}

	@Override
	public InetAddress getDstAddr() {
		return dstAddr;
	}

	@Override
	public int getSrcPort() {
		return srcPort;
	}

	@Override
	public int getDstPort() {
		return dstPort;
	}

	public Type getType() {
		return type;
	}

	public SimulationMessage getInResponseTo() {
		if (role != Role.RESPONSE) {
			return null;
		}
		return inResponseTo;
	}

	public SimulationMessage createResponse() {
		return new SimulationMessage(Type.APPLICATION, this);
	}

	public String toString() {
		return type + "." + role + "(" + UUID + ")";
	}

}
