package ibr.sim;

import java.net.InetSocketAddress;

import ibr.core.CommsPort;
import ibr.core.ComponentFactory;
import ibr.core.Message;

public class Application {

	private CommsPort cport;

	private Host host;

	private String type;

	private Thread clientThread;

	private Thread receiverThread;

	private float lambda;

	private ApplicationConfigurationServer configServer;

	public Application(Host host, String appType, ApplicationConfigurationServer configServer) {
		this.host = host;
		this.type = appType;
		this.configServer = configServer;
		if (configServer.isServer(type)) {
			int port = configServer.getPort(type);
			cport = ComponentFactory.createCommsPort(host.getAddr(), port);
			receiverThread = new ServerThread(this);
			clientThread = null;
		} else {
			cport = host.getEtherealPort();
			Float oLambda = configServer.getLambda(type);
			if (oLambda != null) {
				lambda = oLambda;
				clientThread = new ClientThread(this);
			}
			receiverThread = new ReceiverThread(this);
		}
		receiverThread.start();
	}

	public void begin() {
		if (clientThread != null) {
			System.out.println(this + " launching its client thread.");
			clientThread.start();
		} else {
			System.out.println(this + " does not have a client thread.");
		}
	}

	public Host getHost() {
		return host;
	}

	public CommsPort getCommsPort() {
		return cport;
	}

	private static class ServerThread extends Thread {

		private Application app;

		public ServerThread(Application application) {
			this.app = application;
		}

		public void run() {
			try {
				while (Simulation.isRunning()) {
					SimulationMessage msg = (SimulationMessage) app.cport.getNextMsg();
					app.messageReceived(msg);
					switch (msg.getType()) {
					case ATTACK:
					default:
					}
					SimulationMessage response = msg.createResponse();
					app.cport.sendMsg(response);
					app.messageSent(response);
				}
			} catch (InterruptedException e) {
				Simulation.terminate();
				return;
			}
		}
	}

	private static class ClientThread extends Thread {

		private Application app;

		public ClientThread(Application application) {
			this.app = application;
		}

		public void run() {
			System.out.println("Entered run() for " + this);
			while (Simulation.isRunning()) {
				InetSocketAddress dst = app.configServer.getDestinationAddress(app.type);
				SimulationMessage msg = new SimulationMessage(SimulationMessage.Role.REQUEST,
						SimulationMessage.Type.APPLICATION, app.host.getAddr(), dst.getAddress(), app.cport.getPort(), dst.getPort());
				app.messageSent(msg);
				app.cport.sendMsg(msg);
				try {
					/*
					 * To calculate time-to-next-transaction for a given lambda:
					 * 
					 * <pre> x := rand(0..1) // x is a random float between 0
					 * and 1 x = 1 - e^(-lt) // x is the cumulative probability
					 * for some time t 1 - x = e^(-lt) // move the '1' to the
					 * other side and flip signs ln(1-x) = -lt // natural log of
					 * both sides t = -ln(1-x)/l // the equation for t, time to
					 * next transaction </pre>
					 */
					float x = Simulation.getRandom().nextFloat();
					float t = -(float) (Math.log(x) / app.lambda);
					int sleepTime_ms = (int) (t * 1000);
					Thread.sleep(sleepTime_ms);
				} catch (InterruptedException e) {
					Simulation.terminate();
					return;
				}
			}
		}

	}

	private static class ReceiverThread extends Thread {

		private Application app;

		public ReceiverThread(Application application) {
			this.app = application;
		}

		public void run() {
			try {
				while (Simulation.isRunning()) {
					SimulationMessage msg = (SimulationMessage) app.cport.getNextMsg();
					app.messageReceived(msg);
					switch (msg.getType()) {
					case ATTACK:
					default:
					}
				}
			} catch (InterruptedException e) {
				Simulation.terminate();
				return;
			}
		}

	}

	public void messageReceived(Message msg) {
		System.out.println(Simulation.getElapsedSeconds() + ": " + this + " << " + msg);
	}

	public void messageSent(SimulationMessage msg) {
		System.out.println(Simulation.getElapsedSeconds() + ": " + this + " >> " + msg);
	}

	public String toString() {
		return type + "(" + cport + ")";
	}

}
