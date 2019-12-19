package com.waterlemongan.audiostreaming.utils;

import com.waterlemongan.audiostreaming.jlibrtp.Participant;

public class AudioUtils {

    private static final String TAG = "AudioUtils";
    int rtpPort;
    int rtcpPort;
    boolean isLocal;
    private String ipAddress;
    private String srcPath;
    private SoundReceiver serDemo;
    private SoundSender cliDemo;
    private Thread mThread;

    //Client


//    /**
//     * 设置client读取文件路径
//     * @param srcPath
//     */
//    public void setSrcPath(String srcPath) {
//        this.srcPath = srcPath;
//    }
//
//    /**
//     * 设置rtp端口号
//     * @param rtpPort
//     * @param rtcpPort
//     */
//    public void setRtpPort(int rtpPort, int rtcpPort) {
//        this.rtpPort = rtpPort;
//        this.rtcpPort = rtcpPort;
//    }

    /**
     * 终止线程
     */
    private void destroyThread() {
        try{
            if (mThread != null && Thread.State.RUNNABLE == mThread.getState()) {
                try {
                    Thread.sleep(500);
                    mThread.interrupt();
                } catch (Exception e) {
                    mThread = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mThread = null;
        }
    }

    /**
     * 启动server
     */
    public void startServer(int rtpPort, int rtcpPort) {
        destroyThread();
        this.rtpPort = rtpPort;
        this.rtcpPort = rtcpPort;
        if (mThread == null) {
            mThread = new Thread(serverRunnable);
            mThread.start();
        }
    }

    public void stopServer() {
        destroyThread();
        try {
            serDemo.rtpSession.endSession();
        } catch (Exception e) {}
    }

    public void startClient(int rtpPort, int rtcpPort, String ipAddress, boolean isLocal, String srcPath) {
        destroyThread();
        this.rtpPort = rtpPort;
        this.rtcpPort = rtcpPort;
        this.ipAddress = ipAddress;
        this.isLocal = isLocal;
        this.srcPath = srcPath;
        if (mThread == null) {
            mThread = new Thread(clientRunnable);
            mThread.start();
        }
    }

    public void reversePause() {
        cliDemo.reversePause();
    }

    public void stopClient() {
        destroyThread();
        try {
            cliDemo.rtpSession.endSession();
        } catch (Exception e) {}
    }

    Runnable serverRunnable = new Runnable() {
        @Override
        public void run() {
            serDemo = new SoundReceiver(rtpPort, rtcpPort);
            serDemo.Init();
            serDemo.doStuff();
        }
    };

    Runnable clientRunnable = new Runnable() {
        @Override
        public void run() {
            cliDemo = new SoundSender(isLocal);
            cliDemo.setSrcPath(srcPath);
            Participant p = new Participant(ipAddress, rtpPort, rtcpPort);
            cliDemo.rtpSession.addParticipant(p);
            cliDemo.initMediaDecode();
            cliDemo.run();
        }
    };
}
