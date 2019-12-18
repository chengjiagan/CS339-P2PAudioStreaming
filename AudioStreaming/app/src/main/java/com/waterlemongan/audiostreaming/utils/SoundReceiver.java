package com.waterlemongan.audiostreaming.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.waterlemongan.audiostreaming.jlibrtp.*;

import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Iterator;


public class SoundReceiver implements RTPAppIntf {
	private static final String TAG = "SoundReceiver";
	public RTPSession rtpSession = null;
	private Position curPosition;
	int rtpPort;
	int rtcpPort;
	int nBytesRead = 0;
	int pktCount = 0;
	int index_mix = 0;
	int buffer_size = 30;
	long remove;
	AudioTrack audioTrack;
	Buffer buffer_mix;
	Buffer buffer_rec;
	long SSRC;
	HashMap<Long, Buffer> map_mix = new HashMap<Long, Buffer>();
	HashMap<Long, Buffer> map_rec = new HashMap<Long, Buffer>();
	byte[][] mixed;

	private Thread mThread;

	public void setRtpPort(int rtpPort, int rtcpPort) {
		this.rtpPort = rtpPort;
		this.rtcpPort = rtcpPort;
	}
	
	public void Init() {
		mixed = new byte[buffer_size][];
		for(int i=0;i<buffer_size;i++) {
			mixed[i]=new byte[1024];
		}
		buffer_mix = new Buffer();
		buffer_rec = new Buffer();
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
		Log.d(TAG, "on receiveData");
		//System.out.println(p.getRtpSocketAddress().getAddress());
		remove = 0;
		long SSRC = p.getSSRC();
		//System.out.println(SSRC);
		Log.d(TAG, String.valueOf(SSRC));
		if(!map_mix.containsKey(SSRC)) {
			Buffer buffer = new Buffer();
			map_mix.put(SSRC,buffer);
		}
		if(!map_rec.containsKey(SSRC)) {
			Buffer buffer = new Buffer();
			map_rec.put(SSRC,buffer);
		}
		Iterator<Buffer> iter = map_mix.values().iterator();
		while(iter.hasNext()) {
			System.out.println(iter.next().number--);
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
		}
		if(audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
			byte[] data = frame.getConcatenatedData();
			System.out.println(data.length);
			//???????
			
			if(map_rec.get(SSRC).index == buffer_size) {
				map_rec.get(SSRC).index = 0;
				//???mix??
				//????????????
				if(map_mix.get(SSRC).empty) {
					for(int i=0;i<buffer_size;i++) {
							System.arraycopy(map_rec.get(SSRC).buffer[i], 0, map_mix.get(SSRC).buffer[i], 0, 1024);
						}
					System.out.println("rec->mix");
					map_mix.get(SSRC).empty = false;
					map_mix.get(SSRC).full = true;
				}
				//??????????§Ö?mix
				else {
					boolean all_full = true;
					iter = map_mix.values().iterator();
					while (iter.hasNext()) {
						//????????mix???
						if(iter.next().empty) {
						all_full = false;
						}
					}
					//mix???????????????????
					if(!all_full) {
						System.out.println("discard and import");
						for(int i=0;i<buffer_size;i++) {
							System.arraycopy(map_rec.get(SSRC).buffer[i], 0, map_mix.get(SSRC).buffer[i], 0, 1024);
						}
					}
					//mix?????????????
					else {
						System.out.println("full mix and write");
						//???
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
						//????
						for(int i=0;i<buffer_size;i++) {
							audioTrack.write(mixed[i],0,1024);
						}
						//???mix
						iter = map_mix.values().iterator();
						while (iter.hasNext()) {
							Buffer bb = iter.next();
							bb.empty = true;
							bb.full = false;
						}
						//????
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
			Log.e(TAG, "RTPSession failed to obtain port");
		}
		Log.d(TAG, String.valueOf(rtpSocket));
		Log.d(TAG, String.valueOf(rtcpSocket));
		System.out.println(rtpSocket);
		System.out.println(rtcpSocket);
		
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);
		rtpSession.naivePktReception(true);
		rtpSession.RTPSessionRegister(this, null, null);
		
		//Participant p = new Participant("127.0.0.1", 6001, 6002);		
		//rtpSession.addParticipant(p);
	}

	
	public void doStuff() {
		System.out.println("-> ReceiverDemo.doStuff()");
		Log.d(TAG, "-> ReceiverDemo.doStuff()");
		int sampleRate = 44100;
		int audioMinBufSize = AudioTrack.getMinBufferSize(sampleRate,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, audioMinBufSize, AudioTrack.MODE_STREAM);
		audioTrack.play();
		Log.d(TAG, "audioTrack established");
		if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
			Log.d(TAG, "initialized");
		}
		try {
			while (nBytesRead != -1) {

				try { Thread.sleep(1000); } catch(Exception e) { }
			}
		} finally {
			Log.d(TAG, "audiotrack released");
			this.rtpSession.endSession();
			audioTrack.pause();
			audioTrack.flush();
			audioTrack.stop();
			audioTrack.release();
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