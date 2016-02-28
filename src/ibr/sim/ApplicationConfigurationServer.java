package ibr.sim;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Read the application configuration from a file; set up hosts with their
 * applications.
 * 
 * @author glfrazier
 *
 */
public class ApplicationConfigurationServer {

	private Set<InetAddress> hosts = new HashSet<>();;

	private Set<String> applicationNames = new HashSet<>();

	private Set<String> attackTypes = new HashSet<>();

	private Map<InetAddress, String[]> hostToAppsMap = new HashMap<>();

	private Map<String, Integer> serverPort = new HashMap<>();

	/**
	 * Map the transaction rate to the type of host. The rate is expressed as
	 * lambda (l), were
	 * 
	 * <pre>
	 * PDF(time-to-next-transaction) = (1/l)e^(-lt)
	 * </pre>
	 * 
	 * and
	 * 
	 * <pre>
	 * CDF = 1 - e ^ (-lt)
	 * </pre>
	 * 
	 * To calculate time-to-next-transaction for a given lambda:
	 * 
	 * <pre>
	 * x := rand(0..1)    // x is a random float between 0 and 1
	 * x = 1 - e^(-lt)  // x is the cumulative probability for some time t
	 * 1 - x = e^(-lt)   // move the '1' to the other side and flip signs
	 * ln(1-x) = -lt     // natural log of both sides
	 * t = -ln(1-x)/l    // the equation for t, time to next transaction
	 * </pre>
	 */
	private Map<String, Float> transactionRateLambda = new HashMap<>();

	/**
	 * If a host is an attacker, it is an attacker of a named type.
	 */
	private Map<InetAddress, String> attackTypeByHost = new HashMap<>();

	/**
	 * A given type of attack has a pattern of attack. For now, we will
	 * characterize this as a rate.
	 */
	private Map<String, Float> attackRate = new HashMap<>();

	private Map<String, String> destinations = new HashMap<>();

	public ApplicationConfigurationServer(String configfile) throws IOException, UnknownHostException {
		InputStream in = new FileInputStream(configfile);
		Properties props = new Properties();
		props.load(in);
		initialize(props);
	}

	private synchronized void initialize(Properties props) throws UnknownHostException {
		System.out.println("Initializing the simulation.");
		String hostRegEx = props.getProperty("hosts");
		if (hostRegEx == null) {
			System.err.println("You failed to specify a 'hosts' property.");
			System.exit(-1);
		}
		System.out.println("The host specification: " + hostRegEx);
		String[] hostRegExes = hostRegEx.split(",");
		for (String regEx : hostRegExes) {
			String[] quad = regEx.split("\\.");
			for (String a : quadExpand(quad[0])) {
				for (String b : quadExpand(quad[1])) {
					for (String c : quadExpand(quad[2])) {
						for (String d : quadExpand(quad[3])) {
							InetAddress addr = InetAddress.getByName(a + "." + b + "." + c + "." + d);
							System.out.println("Adding " + addr + " to hosts.");
							hosts.add(addr);
						}
					}
				}
			}
		}
		for (InetAddress hostAddr : hosts) {
			String ip = Host.ipstrip(hostAddr.toString());
			System.out.println("Searching for the type(s) for " + ip + ".");
			String apps = props.getProperty(ip);
			System.out.println("Apps for " + ip + ": " + apps);
			if (apps != null) {
				String[] appArray = apps.split(",");
				hostToAppsMap.put(hostAddr, appArray);
				for (String type : appArray) {
					applicationNames.add(type);
				}
			}
		}
		for (String type : attackTypes) {
			if (props.containsKey(type + ".pAttack")) {
				try {
					float rate = Float.parseFloat(props.getProperty(type + ".pAttack"));
					attackRate.put(type, rate);
				} catch (Exception e) {
					// TODO log this error
					e.printStackTrace();
					System.err.println("Failure to parse property " + type + ".pAttack");
				}
			}
		}
		for (String appName : applicationNames) {
			applicationNames.add(appName);
			if (props.containsKey(appName + ".port")) {
				try {
					int port = Integer.parseInt(props.getProperty(appName + ".port"));
					System.out.println("Application " + appName + " listens to port " + port + ".");
					serverPort.put(appName, port);
				} catch (Exception e) {
					// TODO log this error
					e.printStackTrace();
					System.err.println("Failure to parse property " + appName + ".port");
				}
			}
			if (props.containsKey(appName + ".lambda")) {
				try {
					float lambda = Float.parseFloat(props.getProperty(appName + ".lambda"));
					System.out.println("Application " + appName + " transmits with lambda = " + lambda + ".");
					transactionRateLambda.put(appName, lambda);
				} catch (Exception e) {
					// TODO log this error
					e.printStackTrace();
					System.err.println("Failure to parse property " + appName + ".lambda");
				}
			} else {
				System.out.println("Application " + appName + " does not transmit.");
			}
			if (props.containsKey(appName + ".dsttype")) {
				destinations.put(appName, props.getProperty(appName + ".dsttype"));
				System.out.println(appName + " ==> " + destinations.get(appName));
			}
		}
	}

	private static String[] quadExpand(String quad) {
		if (quad.matches("\\d+")) {
			String[] result = new String[1];
			result[0] = quad;
			return result;
		}
		String[] limits = quad.split("-");
		int lower = Integer.parseInt(limits[0]);
		int upper = Integer.parseInt(limits[1]);
		String[] result = new String[upper - lower];
		for (int i = lower; i < upper; i++) {
			result[i - lower] = Integer.toString(i);
		}
		return result;
	}

	public boolean isServer(String name) {
		return serverPort.containsKey(name);
	}

	public boolean isServer(String[] names) {
		if (names == null)
			return false;
		for (String name : names) {
			if (isServer(name))
				return true;
		}
		return false;
	}

	public boolean isServer(InetAddress host) {
		return isServer(hostToAppsMap.get(host));
	}

	public boolean isTrafficGenerator(String name) {
		return transactionRateLambda.containsKey(name) && transactionRateLambda.get(name) > 0.0;
	}

	public boolean isTrafficGenerator(String[] names) {
		if (names == null)
			return false;
		for (String name : names) {
			if (isTrafficGenerator(name))
				return true;
		}
		return false;
	}

	public boolean isTrafficGenerator(InetAddress host) {
		return isTrafficGenerator(hostToAppsMap.get(host));
	}

	public synchronized float getAttackProbability(InetAddress host) {
		return attackTypeByHost.containsKey(host) ? attackRate.get(attackTypeByHost.get(host)) : 0.0f;
	}

	public synchronized void wasCorruptedBy(InetAddress host, String attackType) {
		assert (attackRate.containsKey(attackType));
		attackTypeByHost.put(host, attackType);
	}

	/**
	 * For a named traffic generator, get the number of messages (packets) to
	 * use in a request.
	 * 
	 * @param name
	 *            the name of the application
	 * @return number of packets, >= 1
	 */
	public int getRequestSize(String name) {
		return 1;
	}

	public int getResponseSize(String name) {
		return 1;
	}

	public String[] getApplicationTypes(InetAddress addr) {
		String[] result = hostToAppsMap.get(addr);
		if (result == null)
			return new String[0];
		return result;
	}

	public int getPort(String appType) {
		if (appType == null) {
			throw new IllegalArgumentException("type == null");
		}
		if (!serverPort.containsKey(appType)) {
			return -1;
		}
		return serverPort.get(appType);
	}

	public Float getLambda(String type) {
		return transactionRateLambda.get(type);
	}

	public Set<InetAddress> getHostAddrs() {
		return hosts;
	}

	public InetSocketAddress getDestinationAddress(String type) {
		if (type == null) {
			throw new IllegalArgumentException("type == null");
		}
		String dstType = destinations.get(type);
		int port = getPort(dstType);
		List<InetSocketAddress> dsts = new LinkedList<>();
		for (InetAddress addr : hosts) {
			String[] types = hostToAppsMap.get(addr);
			for (String t : types) {
				if (t.equals(dstType)) {
					dsts.add(new InetSocketAddress(addr, port));
					break;
				}
			}
		}
		int i = Simulation.getRandom().nextInt(dsts.size());
		return dsts.get(i);
	}

}
