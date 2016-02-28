package ibr.sim;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import ibr.core.CommsPort;
import ibr.core.ComponentFactory;
import ibr.core.Message;
import ibr.core.Node;
import jdk.net.Sockets;

public class Host {

	private Set<InetAddress> tunnels;

	private Queue<Message> unsendableQueue;

	private Map<CommsPort, Application> applications;

	private InetAddress addr;

	private AtomicInteger nextEtherealPort = new AtomicInteger(16000);

	private Map<Integer, SimCommsPort> mysocks = new HashMap<>();

	@SuppressWarnings("serial")
	public Host(InetAddress addr, Node node, ApplicationConfigurationServer configServer) {
		this.addr = addr;
		this.tunnels = new HashSet<InetAddress>() {
			public boolean contains(Object o) {
				return true;
			}
		};
		this.unsendableQueue = node.getUnsendableQueue();
		Simulation.addHost(this);
		this.applications = new HashMap<CommsPort, Application>();
		this.configure(configServer);
	}

	private void configure(ApplicationConfigurationServer configServer) {
		String[] appnames = configServer.getApplicationTypes(addr);
		System.out.println(this + " in configure, we have " + appnames.length + " apps.");
		for (String name : appnames) {
			Application app = new Application(this, name, configServer);
			SimCommsPort cport = (SimCommsPort) app.getCommsPort();
			applications.put(cport, app);
			mysocks.put(cport.getPort(), cport);
		}
	}

	public void send(Message msg) {
		InetAddress dst = msg.getDstAddr();
		if (tunnels.contains(dst)) {
			Host dstHost = Simulation.getHost(dst);
			dstHost.receive(msg);
		} else {
			System.out.println(this + " tunnel does not contain " + dst);
		}
	}

	private void receive(Message msg) {
		InetAddress src = msg.getSrcAddr();
		if (!tunnels.contains(src)) {
			return;
		}
		int port = msg.getDstPort();
		SimCommsPort cport = mysocks.get(port);
		if (cport != null) {
			cport.deliver(msg);
		} else {
			System.err.println(this + " does not have a socket on port " + port);
		}
	}

	public InetAddress getAddr() {
		return addr;
	}

	public CommsPort getEtherealPort() {
		int port = nextEtherealPort.getAndIncrement();
		return ComponentFactory.createCommsPort(addr, port);
	}

	public void simulate() {
		System.out.println(this + " is beginning simulation");
		for (Application a : applications.values()) {
			System.out.println("Starting application " + a);
			a.begin();
		}
		System.out.println("Applications started.");
	}

	public static String ipstrip(String ipaddr) {
		int i = ipaddr.indexOf('/');
		if (i >= 0) {
			return ipaddr.substring(i + 1);
		}
		return ipaddr;
	}

	public String toString() {
		return ipstrip(addr.toString());
	}

}
