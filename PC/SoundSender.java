/* This file is based on 
 * http://www.anyexample.com/programming/java/java_play_wav_sound_file.xml
 * Please see the site for license information.
 */
	 
package rtpuse;


import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.lang.String;
import java.net.DatagramSocket;
import java.net.InetAddress;
import jlibrtp.*;


public class SoundSender implements RTPAppIntf  {
	public RTPSession rtpSession = null;
	
	public static boolean stop_flag = false;
	static int pktCount = 0;
	static int dataCount = 0;
	private String filename;
	private final int EXTERNAL_BUFFER_SIZE = 1024;
	SourceDataLine auline;
	enum Position {
		LEFT, RIGHT, NORMAL
	};
	private Position curPosition;
	boolean local;
	 
	
	public SoundSender(boolean isLocal)  {
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;
		
		try {
			rtpSocket = new DatagramSocket(12200);
			rtcpSocket = new DatagramSocket(12201);
		} catch (Exception e) {
			System.out.println("RTPSession failed to obtain port");
		}
		System.out.println(rtpSocket);
		System.out.println(rtcpSocket);
		
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);
		rtpSession.RTPSessionRegister(this,null, null);
		
		System.out.println("CNAME: " + rtpSession.CNAME());
		this.local = isLocal;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for(int i=0;i<args.length;i++) {
			System.out.println("args["+i+"]" + args[i]);
		}
		if(args.length == 0) {
			args = new String[4];
			args[1] = "172.20.10.10";
			args[0] = "D:/大三上/计算机网络/final_project/project/3.wav";
			args[2] = "10060";
			args[3] = "10061";
		}
		String target_IP = args[1];
		if (target_IP == null){
			target_IP = "127.0.0.1";
		}
		SoundSender aDemo = new SoundSender(false);
		Participant p = new Participant(target_IP,Integer.parseInt(args[2]),Integer.parseInt(args[2]) + 1);
		aDemo.rtpSession.addParticipant(p);
		aDemo.filename = args[0];
		aDemo.run();
		System.out.println("pktCount: " + pktCount);
		SenderGUI.c.sendStop();
	}
	
	
	
	public void receiveData(DataFrame dummy1, Participant dummy2) {
		// We don't expect any data.
	}
	
	public void userEvent(int type, Participant[] participant) {
		//Do nothing
	}
	
	public int frameSize(int payloadType) {
		return 1;
	}
	
	public void run() {
		if(RTPSession.rtpDebugLevel > 1) {
			System.out.println("-> Run()");
		} 
		File soundFile = new File(filename);
		if (!soundFile.exists()) {
			System.err.println("Wave file not found: " + filename);
			return;
		}

		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
		} catch (UnsupportedAudioFileException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

		AudioFormat format = audioInputStream.getFormat();
		//AudioFormat.Encoding encoding =  new AudioFormat.Encoding("PCM_SIGNED");
		//AudioFormat format = new AudioFormat(encoding,((float) 8000.0), 16, 1, 2, ((float) 8000.0) ,false);
		System.out.println(format.toString());
		
		
		if(! this.local) {
			// To time the output correctly, we also play at the input:
			auline = null;
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

			try {
				auline = (SourceDataLine) AudioSystem.getLine(info);
				auline.open(format);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				return;
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			if (auline.isControlSupported(FloatControl.Type.PAN)) {
				FloatControl pan = (FloatControl) auline
				.getControl(FloatControl.Type.PAN);
				if (this.curPosition == Position.RIGHT)
					pan.setValue(1.0f);
				else if (this.curPosition == Position.LEFT)
					pan.setValue(-1.0f);
			}

			auline.start();
		}
		
		int nBytesRead = 0;
		byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
		long start = System.currentTimeMillis();
		try {
			while (nBytesRead != -1) {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				
				if (nBytesRead >= 0) {
					while(stop_flag){
						try { Thread.sleep(10);} catch(Exception e) {}
					};
					rtpSession.sendData(abData);
					//if(!this.local) {	
					auline.write(abData, 0, abData.length);
					
					//dataCount += abData.length;
					
					//if(pktCount % 10 == 0) {
					//	System.out.println("pktCount:" + pktCount + " dataCount:" + dataCount);
					//
					//	long test = 0;
					//	for(int i=0; i<abData.length; i++) {
					//		test += abData[i];
					//	}
					//	System.out.println(Long.toString(test));
					//}
					
					pktCount++;
					//if(pktCount == 100) {
					//	System.out.println("Time!!!!!!!!! " + Long.toString(System.currentTimeMillis()));
					//}
					//System.out.println("yep");
				}
//				if(pktCount == 100) {
//					Enumeration<Participant> iter = this.rtpSession.getParticipants();
//					//System.out.println("iter " + iter.hasMoreElements());
//					Participant p = null;
//					
//					while(iter.hasMoreElements()) {
//						p = iter.nextElement();
//
//						String name = "name";
//						byte[] nameBytes = name.getBytes();
//						String data= "abcd";
//						byte[] dataBytes = data.getBytes();
//						
//						
//						int ret = rtpSession.sendRTCPAppPacket(p.getSSRC(), 0, nameBytes, dataBytes);
//						System.out.println("!!!!!!!!!!!! ADDED APPLICATION SPECIFIC " + ret);
//						continue;
//					}
//					if(p == null)
//						System.out.println("No participant with SSRC available :(");
//				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Time: " + (System.currentTimeMillis() - start)/1000 + " s");
		
		//try { Thread.sleep(200);} catch(Exception e) {}
		
		this.rtpSession.endSession();
		
		//try { Thread.sleep(2000);} catch(Exception e) {}
		if(RTPSession.rtpDebugLevel > 1) {
			System.out.println("<- Run()");
		} 
	}

}
