package rtpuse;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.InetAddress;

import javax.swing.*;

import rtpuse.SoundSender;

public class SenderGUI {
	public static JFrame frame;
	public static TextField tf1;
	public static TextField tf2;
	public static String TargetIP = "127.0.0.1";
	public static String Filename = "D:/大三上/计算机网络/final_project/project/1.wav";
	public static boolean server_found_flag = false;
	public static String ServerIP = null;
	public static Client c;
	public static MainThread mt;
	
	public SenderGUI(){
		FrameInit();
		
		SearchTargetPanel();
		ChooseFilePanel();
		SendPanel();
	}
	
	private void FrameInit(){
		frame = new JFrame("RTP Sound Sender");
		JFrame.setDefaultLookAndFeelDecorated(true);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(500, 500, 500, 600);
		frame.setBackground(Color.blue);
		
	}
	private void SearchTargetPanel(){
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel,BorderLayout.NORTH);
		JButton button = new JButton("检测服务器");
		SearchServerAction action = new SearchServerAction();
		button.addActionListener(action);
		panel.add(button);
		tf1 = new TextField("请开始检测服务器！                                      ");
		tf1.setSize(5, 20);
		panel.add(tf1);
	}
	
	private void ChooseFilePanel(){
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel,BorderLayout.CENTER);
		JLabel label = new JLabel("选择发送音频：");
		panel.add(label);
		
		tf2 = new TextField(Filename);
		panel.add(tf2);
		JButton button = new JButton("...");
		ChooseFileAction action = new ChooseFileAction();
		button.addActionListener(action);
		panel.add(button);
	}
	
	private void SendPanel(){
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel,BorderLayout.SOUTH);
		JButton button = new JButton("发送");
		SendAction action = new SendAction();
		button.addActionListener(action);
		panel.add(button);
		JButton button1 = new JButton("暂停");
		button1.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SoundSender.stop_flag = true;
			}
		});
		panel.add(button1);
		JButton button2 = new JButton("继续");
		button2.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				SoundSender.stop_flag = false;
				System.out.println(SoundSender.stop_flag);
			}
		});
		panel.add(button2);
		JButton button3 = new JButton("停止");
		button3.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				mt.stop();
				if(c != null){
					c.sendStop();
					c.stop();
				}
				System.exit(0);
			}
		});
		panel.add(button3);
	}
	
	public void pack(){
		frame.pack();
		frame.setVisible(true);
	}
	
	public static void SearchServer(){
		
		c.searchServer(new Client.SearchListener() {
			
			@Override
			public void onServerNotFound() {
				// TODO Auto-generated method stub
				server_found_flag = false;
			}
			
			@Override
			public void onServerFound(InetAddress address) {
				// TODO Auto-generated method stub
				server_found_flag = true;
				ServerIP = address.getHostAddress();
				tf1.setText("服务器IP地址：" + ServerIP);
				c.start(address, new Client.EventListener(){

					@Override
					public void onServerDisconnect() {
						// TODO Auto-generated method stub
						System.out.println("Server disconnect!");
					}

					@Override
					public void onPlayOk() {
						// TODO Auto-generated method stub
						System.out.println("Ok to send!");
					}
					
				});
				c.sendPlay();
			}
		});
		
		
	}
	
	
	public static void main(String[] args){
		mt = new MainThread();
		c = new Client();
		
		SenderGUI gui = new SenderGUI();
		gui.pack();
	}

}





class SearchServerAction implements ActionListener{

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		SenderGUI.SearchServer();
		if(SenderGUI.server_found_flag && SenderGUI.ServerIP!=null ){
			SenderGUI.tf1.setText("服务器IP地址：" + SenderGUI.ServerIP);
			SenderGUI.TargetIP = SenderGUI.ServerIP;
			
		}
		else{
			SenderGUI.tf1.setText("未找到服务器");
		}
	}
}


class ChooseFileAction implements ActionListener{

	@Override
	public void actionPerformed(ActionEvent arg0) {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.showDialog(SenderGUI.frame, "选择");
		File file = jfc.getSelectedFile();
		if(file.isFile()){
			SenderGUI.Filename = file.getAbsolutePath();
			SenderGUI.tf2.setText(SenderGUI.Filename);
		}
	}
	
}

class SendAction implements ActionListener{

	@Override
	public void actionPerformed(ActionEvent arg0) {
		SenderGUI.mt.start();
	}
	
}

class MainThread extends Thread{
	@Override
	public void run(){
		SenderGUI.Filename = SenderGUI.tf2.getText();
		SoundSender.stop_flag = false;
		String[] args;
		args = new String[4];
		args[1] = SenderGUI.ServerIP;
		args[0] = SenderGUI.Filename;
		args[2] = "10060";
		args[3] = "10061";
		SoundSender.main(args);
	}
}

class StopThread extends Thread{
	@Override
	public void run(){
		SoundSender.stop_flag = true;
	}
}
