package rtpuse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.util.Vector;

public class ServerGUI {
	
	public static Vector<Long> send = SoundReceiver.sendSound;
	static Vector<Long> SSRC = new Vector<Long>();
	static Vector<String> IP = new Vector<String>();
    final static JList<String> list_IP = new JList(IP);
    final static JList list_SSRC = new JList(SSRC);
    static DefaultListModel<String> dlm_IP = new DefaultListModel();
    static DefaultListModel dlm_SSRC = new DefaultListModel();
    static int num = 0;
    
	public void server() {
		Server s = new Server();
		s.start(new Server.EventListener() {
			
			@Override
			public void onStop(InetAddress address) {
				// TODO Auto-generated method stub
				//System.out.println("!!!!!!!!!!!!stop");
				dlm_IP.removeElement(address.getHostAddress());
			}
			
			@Override
			public void onPlay(InetAddress address) {
				// TODO Auto-generated method stub
				dlm_IP.addElement(address.getHostAddress());
			}

			@Override
			public void onNewDevice(InetAddress address) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDeviceDisconnect(InetAddress address) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	public ServerGUI() {
		JFrame f;
		f = new JFrame("Receive");
		f.setBounds(400, 200, 600, 400);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        JButton button = new JButton("Start Receiving");
        JLabel label_IP = new JLabel("IP in link:");
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(200,100));
        
        scrollPane.setViewportView(list_IP);
        
        panel.setLayout(null);
        button.setBounds(180, 50, 200, 50);
        label_IP.setBounds(180, 100, 200, 50);
        scrollPane.setBounds(180, 150, 200, 100);
        panel.add(button);
        panel.add(label_IP);
        panel.add(scrollPane);
        f.getContentPane().add(panel);
        
        
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				String[] args = new String[2];
				JFrame f_rec = new JFrame("Receiving");
				f_rec.setBounds(450, 250, 400, 300);

		        f_rec.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		        JPanel panel = new JPanel();
		        JLabel label_SSRC = new JLabel("SSRC in sending:");
		        
		        JScrollPane scrollPane_SSRC = new JScrollPane();
		        scrollPane_SSRC.setPreferredSize(new Dimension(200,100));
		        
		        scrollPane_SSRC.setViewportView(list_SSRC);
		        
		        panel.setLayout(null);
		        label_SSRC.setBounds(100, 20, 200, 50);
		        scrollPane_SSRC.setBounds(100, 70, 200, 100);

		        panel.add(label_SSRC);
		        panel.add(scrollPane_SSRC);
		        f_rec.getContentPane().add(panel);
		        f_rec.setVisible(true);
		        
				args[0] = "10060";
				args[1] = "10061";
				SoundReceiver.main(args);
			}
		});
		f.setVisible(true);
	}
	
	public static void main(String[] args) {
		ServerGUI receive = new ServerGUI();
		System.out.println("!!!!!!!!!!!!server");
		receive.server();
		while(true) {
			send = SoundReceiver.sendSound;
			if(num != send.size()) {
				num = send.size();
				dlm_SSRC.clear();
				for(int m=0; m<num; m++) {
					dlm_SSRC.addElement(send.get(m));
				}
				list_SSRC.setModel(dlm_SSRC);
			};
			list_IP.setModel(dlm_IP);
		}
		
	}
}
