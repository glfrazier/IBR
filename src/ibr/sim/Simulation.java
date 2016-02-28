package ibr.sim;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ibr.core.ComponentFactory;
import ibr.core.Node;

public class Simulation {

	private static final Random RANDOM = new Random();
	private static boolean running = true;
	private static Map<InetAddress, Host> hostMap = new HashMap<>();
	private static long startTime;

	public static boolean isRunning() {
		return running;
	}

	public static void terminate() {
		running = false;
	}

	public static Random getRandom() {
		return RANDOM;
	}

	public static void addHost(Host h) {
		System.out.println("Adding " + h + " to the simulation.");
		hostMap.put(h.getAddr(), h);
	}

	public static Host getHost(InetAddress addr) {
		Host h = hostMap.get(addr);
		return h;
	}

	public static float getElapsedSeconds() {
		return ((float) (System.currentTimeMillis() - startTime)) / 1000.0f;
	}

	public static long getElapsedMillis() {
		return System.currentTimeMillis() - startTime;
	}

	public static void main(String[] args) throws UnknownHostException, IOException {
		ComponentFactory.setContext(ComponentFactory.Context.SIMULATION);
		String filename = args[0];
		ApplicationConfigurationServer config = new ApplicationConfigurationServer(filename);
		for (InetAddress addr : config.getHostAddrs()) {
			Node node = new Node();
			Host h = new Host(addr, node, config);
			hostMap.put(addr, h);
		}
		startTime = System.currentTimeMillis();
		for (Host h : hostMap.values()) {
			h.simulate();
		}
	}

}
