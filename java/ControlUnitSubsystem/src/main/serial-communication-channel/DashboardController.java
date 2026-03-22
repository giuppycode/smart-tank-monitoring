package DroneRemoteUnit;

public class DashboardController  {

	static final String MSG_TAKEOFF 	= "TAKEOFF";
	static final String MSG_LAND 		= "LAND";
	
	SerialCommChannel channel;
	DashboardView view;
	LogView logger;
	
	public DashboardController(String port, DashboardView view, LogView logger) throws Exception {
		this.view = view;
		this.logger = logger;
		
		channel = new SerialCommChannel(port, 115200);		
		new MonitoringAgent(channel, view, logger).start();
			
		System.out.println("Waiting Arduino for rebooting...");		
		Thread.sleep(4000);
		System.out.println("Ready.");		
	
	}
	
	public void notifyTakeOff() {
		channel.sendMsg(MSG_TAKEOFF);
	}

	public void notifyLand() {
		channel.sendMsg(MSG_LAND);
	}

}
