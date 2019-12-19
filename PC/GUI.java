package rtpuse;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import javax.swing.JFrame;

public class GUI {
	JFrame frame;
	JButton client_button;
	JButton server_button;
	public GUI(){
		FrameInit();
		PanelInit();
	}
	public static void main(String[] args){
		GUI gui = new GUI();
		gui.pack();
	}
	
	private void FrameInit(){
		frame = new JFrame("RTP Real-Time Sound Transport");
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(500, 500, 500, 90);
		frame.setBackground(Color.blue);
		
	}
	
	private void PanelInit(){
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel,BorderLayout.NORTH);
		client_button = new JButton("客户端");
		client_button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SenderGUI.main(null);
			}
		});
		panel.add(client_button);
		server_button = new JButton("服务器");
		server_button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ServerThread st = new ServerThread();
				st.start();
			}
		});
		panel.add(server_button);
	}
	
	public void pack(){
		//frame.pack();
		frame.setVisible(true);
	}
	
}



class ServerThread extends Thread{
	@Override
	public void run(){
		ServerGUI.main(null);
	}
}
