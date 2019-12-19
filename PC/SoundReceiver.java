package rtpuse;

import java.io.File;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import jlibrtp.*;


public class SoundReceiver implements RTPAppIntf {
	public static Vector<Long> sendSound = new Vector<Long>();
	RTPSession rtpSession = null;
	private Position curPosition;
	int nBytesRead = 0;
	int pktCount = 0;
	int index_mix = 0;
	int buffer_size = 30;
	long remove;
	SourceDataLine auline;
	Buffer buffer_mix;
	Buffer buffer_rec;
	long SSRC;
	HashMap<Long, Buffer> map_mix = new HashMap<Long, Buffer>();
	HashMap<Long, Buffer> map_rec = new HashMap<Long, Buffer>();
	byte[][] mixed;
	
	public void Init() {
		mixed = new byte[buffer_size][];
		for(int i=0;i<buffer_size;i++) {
			mixed[i]=new byte[1024];
		}
		buffer_mix = new Buffer();
		buffer_rec = new Buffer();
		sendSound = new Vector<Long>();
	}
	public byte[] mix(byte data1[],byte data2[]) {
		for(int i=0;i<1024;i++) {
			if( data1[i] < 0 && data2[i] < 0)  
				data1[i] = (byte) (data1[i]+data2[i] - (data1[i] * data2[i] / -127));  
			else  
				data1[i] = (byte) (data1[i]+data2[i] - (data1[i] * data2[i] / 128)); 
		}
		return data1;
	}
	
	 enum Position {
		LEFT, RIGHT, NORMAL
	};
	
	
	public void receiveData(DataFrame frame, Participant p) {
		//System.out.println("sendSound:"+sendSound);
		//System.out.println(p.getRtpSocketAddress().getAddress());
		remove = 0;
		long SSRC = p.getSSRC();
		//System.out.println(SSRC);
		if(!map_mix.containsKey(SSRC)) {
			Buffer buffer = new Buffer();
			map_mix.put(SSRC,buffer);
			sendSound.add(SSRC);
		}
		if(!map_rec.containsKey(SSRC)) {
			Buffer buffer = new Buffer();
			map_rec.put(SSRC,buffer);
		}
		Iterator<Buffer> iter = map_mix.values().iterator();
		while(iter.hasNext()) {
			//System.out.println();
			iter.next().number--;
		}
		map_mix.get(SSRC).number = 30;
		Iterator<Long> iter_key = map_mix.keySet().iterator();
		while(iter_key.hasNext()) {
			long aa = iter_key.next();
			if(map_mix.get(aa).number <= 0) {
				remove = aa;
			}
		}
		if(remove != 0) {
			map_mix.remove(remove);
			map_rec.remove(remove);
			sendSound.remove(SSRC);
		}
		if(auline != null) {
			byte[] data = frame.getConcatenatedData();
			//System.out.println(data.length);
			//接收端满
			
			if(map_rec.get(SSRC).index == buffer_size) {
				map_rec.get(SSRC).index = 0;
				//检查mix端
				//若为空，直接放入
				if(map_mix.get(SSRC).empty) {
					for(int i=0;i<buffer_size;i++) {
							System.arraycopy(map_rec.get(SSRC).buffer[i], 0, map_mix.get(SSRC).buffer[i], 0, 1024);
						}
					//System.out.println("rec->mix");
					map_mix.get(SSRC).empty = false;
					map_mix.get(SSRC).full = true;
				}
				//否则，遍历所有的mix
				else {
					boolean all_full = true;
					iter = map_mix.values().iterator();
					while (iter.hasNext()) {
						//若找到一个mix为空
						if(iter.next().empty) {
						all_full = false;
						}
					}
					//mix不全满（丢弃）直接放入
					if(!all_full) {
						//System.out.println("discard and import");
						for(int i=0;i<buffer_size;i++) {
							System.arraycopy(map_rec.get(SSRC).buffer[i], 0, map_mix.get(SSRC).buffer[i], 0, 1024);
						}
					}
					//mix全满，混合，放入
					else {
						//System.out.println("full mix and write");
						//混合
						for(int i=0;i<buffer_size;i++) {
							for(int j=0;j<1024;j++)
								mixed[i][j]=0;
						}
						for(int i=0;i<buffer_size;i++) {
							iter = map_mix.values().iterator();
							while (iter.hasNext()) {
									mixed[i]=mix(mixed[i],iter.next().buffer[i]);
							}
						}
						//播放
						for(int i=0;i<buffer_size;i++) {
							auline.write(mixed[i],0,1024);
						}
						//清空mix
						iter = map_mix.values().iterator();
						while (iter.hasNext()) {
							Buffer bb = iter.next();
							bb.empty = true;
							bb.full = false;
						}
						//放入
						for(int i=0;i<buffer_size;i++) {
							System.arraycopy(map_rec.get(SSRC).buffer[i], 0, map_mix.get(SSRC).buffer[i], 0, 1024);
							map_mix.get(SSRC).empty = false;
							map_mix.get(SSRC).full = true;
						}
					}
				}
			}
			System.arraycopy(data, 0, map_rec.get(SSRC).buffer[map_rec.get(SSRC).index], 0, data.length);
			map_rec.get(SSRC).index++;
		}
		pktCount++;
	}
	
	public void userEvent(int type, Participant[] participant) {
		//Do nothing
	}
	public int frameSize(int payloadType) {
		return 1;
	}
	
	public SoundReceiver(int rtpPort, int rtcpPort)  {
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;
		
		try {
			rtpSocket = new DatagramSocket(rtpPort);
			rtcpSocket = new DatagramSocket(rtcpPort);
		} catch (Exception e) {
			System.out.println("RTPSession failed to obtain port");
		}
		System.out.println(rtpSocket);
		System.out.println(rtcpSocket);
		
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);
		rtpSession.naivePktReception(true);
		rtpSession.RTPSessionRegister(this, null, null);
		
		//Participant p = new Participant("127.0.0.1", 6001, 6002);		
		//rtpSession.addParticipant(p);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Setup");
		
		if(args.length == 0) {
			System.out.println("Syntax:");
			System.out.println("java SoundReceiverDemo <rtpPort> <rtcpPort>");
			System.out.println("Assuming 10030 and 10031 for testing purposes");
			
			args = new String[2];
			args[0] = new String("10050");
			args[1] = new String("10051");
		}

		SoundReceiver aDemo = new SoundReceiver( Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		aDemo.Init();
		aDemo.doStuff();
		System.out.println("Done");
	}
	
	public void doStuff() {
		System.out.println("-> ReceiverDemo.doStuff()");
		
		
		File soundFile = new File("D:\\Test\\Java\\project\\1.wav");
		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		AudioFormat format = audioInputStream.getFormat();
		//AudioFormat.Encoding encoding =  new AudioFormat.Encoding("PCM_SIGNED");
		//AudioFormat format = new AudioFormat(encoding,((float) 8000.0), 16, 1, 2, ((float) 8000.0) ,false);

		
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
		try {
//			while (nBytesRead != -1) {
//
//				//try { Thread.sleep(1000); } catch(Exception e) { }
//			}
		} finally {
//			auline.drain();
//			auline.close();
		}
	}
}

class Buffer {
	int buffer_size = 30;
	public byte[][] buffer;
	public boolean empty;
	public boolean full;
	public int number;
	public int index;
	public Buffer() {
		buffer = new byte[buffer_size][];
		for(int i=0;i<buffer_size;i++) {
			buffer[i]=new byte[1024];
			empty = true;
			full = false;
			number = 30;
			index = 0;
		}
		
	}
}