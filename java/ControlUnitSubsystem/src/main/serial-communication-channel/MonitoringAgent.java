package DroneRemoteUnit;

public class MonitoringAgent extends Thread {

	SerialCommChannel channel;
	DashboardView view;
	LogView logger;
	
	public MonitoringAgent(SerialCommChannel channel, DashboardView view, LogView log) throws Exception {
		this.view = view;
		this.logger = log;
		this.channel = channel;
	}
	
	public void run(){
		while (true){
			try {
				String msg = channel.receiveMsg();
				
				this.logger.log(msg);
				
				if (msg.startsWith("STATE:")){
					try {
						String[] parts = msg.split("\\|");
						if (parts.length >= 3) {
							String state = parts[0].substring(6);
							String hangar = parts[1].substring(7);
							String dist = parts[2].substring(5);
							
							view.setDroneState(state);
							view.setHangarState(hangar);
							
							if (!dist.equals("---")) {
								try {
									double distance = Double.parseDouble(dist);
									view.setDistance(distance);
								} catch (NumberFormatException e) {
									view.setDistance(-1);
								}
							} else {
								view.setDistance(-1);
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						System.err.println("Error parsing msg: " + msg);
					}
				}
			} catch (Exception ex){
				ex.printStackTrace();
			}
		}
	}

}
