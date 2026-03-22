package DroneRemoteUnit;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

class DashboardView extends JFrame implements ActionListener  {

	private JButton takeOffButton;
	private JButton landButton;
	
	private JTextField droneState;
	private JTextField hangarState;
	private JTextField distance;
	
	private DashboardController controller;
	
	public DashboardView(){
		super(".:: Drone Hangar Control ::.");
		setSize(600,200);
		this.setResizable(false);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(Box.createRigidArea(new Dimension(0,20)));
		
		JPanel infoLine = new JPanel();
		infoLine.setLayout(new BoxLayout(infoLine, BoxLayout.LINE_AXIS));
		droneState = new JTextField("--");
		droneState.setEditable(false);
		droneState.setPreferredSize(new Dimension(200,25));
		infoLine.add(new JLabel("Drone State:"));
		infoLine.add(droneState);
		hangarState = new JTextField("--");
		hangarState.setEditable(false);
		hangarState.setPreferredSize(new Dimension(200,25));
		infoLine.add(new JLabel("Hangar State:"));
		infoLine.add(hangarState);
		
		mainPanel.add(infoLine);
		
		JPanel distLine = new JPanel();
		distLine.setLayout(new BoxLayout(distLine, BoxLayout.LINE_AXIS));
		distance = new JTextField("--");
		distance.setEditable(false);
		distance.setPreferredSize(new Dimension(200,25));
		distLine.add(new JLabel("Distance (m):"));
		distLine.add(distance);
		
		mainPanel.add(distLine);
		mainPanel.add(Box.createRigidArea(new Dimension(0,20)));
		mainPanel.setPreferredSize(new Dimension(200,20));

		JPanel buttonPanel = new JPanel();
		takeOffButton = new JButton("TAKEOFF");
		takeOffButton.addActionListener(this);
		landButton = new JButton("LAND");
		landButton.addActionListener(this);
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));	    
		buttonPanel.add(takeOffButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(20,0)));
		buttonPanel.add(landButton);
		
		mainPanel.add(buttonPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0,20)));
		setContentPane(mainPanel);	
		
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent ev){
				System.exit(-1);
			}
		});
	}
	
	public void display() {
		SwingUtilities.invokeLater(() -> {
			this.setVisible(true);
		});
	}

	public void registerController(DashboardController contr){
		this.controller = contr;
	}

	public void setDroneState(String state){
		SwingUtilities.invokeLater(() -> {
			droneState.setText(state); 
		});
	}

	public void setHangarState(String state){
		SwingUtilities.invokeLater(() -> {
			hangarState.setText(state);
		});
	}

	public void setDistance(double dist){
		SwingUtilities.invokeLater(() -> {
			if (dist < 0) {
				distance.setText("--");
			} else {
				distance.setText(String.format("%.2f", dist));
			}
		});
	}
	
	public void actionPerformed(ActionEvent ev){
		  try {
			  if (ev.getSource() == takeOffButton){
				  controller.notifyTakeOff();
			  } else if (ev.getSource() == landButton){
				  controller.notifyLand();
			  } 
		  } catch (Exception ex){
			  ex.printStackTrace();

		  }
	}
}
