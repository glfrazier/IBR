package ibr.core;

import java.net.InetAddress;

import ibr.sim.SimCommsPort;
import ibr.sim.Simulation;

public class ComponentFactory {

	public enum Context {
		SIMULATION, LINUX, WINDOWS
	};

	private static Context context;

	public static void setContext(Context context) {
		ComponentFactory.context = context;
	}

	public static CommsPort createCommsPort(InetAddress addr, int port) {
		switch (context) {
		case SIMULATION:
			return new SimCommsPort(Simulation.getHost(addr), port);
		default:
			throw new UnsupportedOperationException("CommsPort not implemented for context " + context);
		}
	}
}
