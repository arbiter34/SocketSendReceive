/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package csci466.lab3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alperst
 */
public class Sender  implements Runnable {    
    
    private static final int TIMEOUT = 2000;
    
    private final int packetCount;
    
    private final int windowSize;   
    
    private int windowPos;
    
    private int packetIndex;
    
    private final DatagramPacket headerPacket;
    
    private final List<DatagramPacket> packets;
    
    private final List<Boolean> skipPackets; 
    
    private List<Timer> timeoutTimers;
    
    private final List<Boolean> sentPackets;
    
    private List<Boolean> ackdPackets;
    
    private final int port;
    
    private DatagramSocket socket;
    
    private final InetAddress address;
    
    public volatile boolean running = false;
    
    private final Queue<Integer> resendPackets;
    
    public Sender(int windowSize, int packetCount, int port, String addressString, int destPort, List<Boolean> skipPackets) throws UnknownHostException {
        this.packetCount = packetCount;
        this.windowPos = 0;
        this.packetIndex = 0;
        this.windowSize = windowSize;
        this.port = port;
        this.address = InetAddress.getByName(addressString);  
        this.packets = new ArrayList<>();
        this.skipPackets = skipPackets;
        this.resendPackets = new ConcurrentLinkedQueue<>();
        this.sentPackets = new ArrayList<>();
        this.timeoutTimers = new ArrayList<>();
        this.ackdPackets = new ArrayList<>();
        
        
        byte[] data = new byte[3];
        buildHeader(data);
        headerPacket = new DatagramPacket(data, data.length, this.address, destPort);
        
        //build packets and lists
        for (int i = 0; i < packetCount; i++) {
            byte[] buf = new byte[1];
            buf[0] = (byte)i;
            this.packets.add(new DatagramPacket(buf, buf.length, this.address, destPort));
            this.skipPackets.add(false);
            this.timeoutTimers.add(null);
            this.ackdPackets.add(false);
            this.sentPackets.add(false);
        }
    }
    
    public void checkPacketTimeout(int packetIndex) {
        //Check for ACK
        if (!ackdPackets.get(packetIndex)) {
            System.out.println("Packet " + packetIndex + " Timeout");
            //No ACK - Resend
            resendPackets.add(packetIndex);
        }
    }
    
    private void init() throws SocketException {
        //Build socket
        socket = new DatagramSocket(port);    
        socket.setSoTimeout(TIMEOUT);
    }
    
    private void sendPacket(DatagramPacket packet) throws SocketException, IOException {
        //Check alive
        if (socket == null || !socket.isBound()) {
            init();
        }
        //Send
        socket.send(packet);
    }
    
    private void recvPacket() throws SocketException {
        //Check alive
        if (socket == null || !socket.isBound()) {
            init();
        }
        //byte array for data in packet
        byte[] data = new byte[1024];
        //empty packet for receiving data
        DatagramPacket packet = new DatagramPacket(data, data.length);
        
        //try receive - this is blocking - Timeout has been set on socket
        try {
            socket.receive(packet);
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                System.out.println("Socket timeout on receive.");
            }
            return;
        }
        if (packet.getData().length > 0) {
            //We defined packet structure to be 1 byte holding the packet number
            int packetACKD = packet.getData()[0];
            
            //Mark packet ACKD
            ackdPackets.set(packetACKD, true);
            
            //Move the window if necessary
            while (windowPos < ackdPackets.size() && ackdPackets.get(windowPos)) {
                windowPos++;
            }
            
            System.out.println("Packet " + packetACKD + " ACK Received." + buildWindowString());
        }       
        
    }
    
    private String buildWindowString() {
        String window = "[";
        String delimiter = "";
        for (int i = 0; i < windowSize; i++) {
            if (windowPos + i >= ackdPackets.size()) {
                window += delimiter;
                window += "-";
                delimiter = ", ";
                continue;
            }
            window += delimiter;

            window += windowPos + i;
            if (!ackdPackets.get(windowPos + i) && sentPackets.get(windowPos + i)) {
                window += "*";
            }
            delimiter = ", ";
        }
        window += "]";
        return window;
    }
    
    private void buildHeader(byte[] data) {
        data[0] = -1;
        data[1] = (byte)packetCount;
        data[2] = (byte)windowSize;
    }

    @Override
    public void run() {
        running = true;
        int index;
        boolean resendPacket = false;
        
        try {
            sendPacket(headerPacket);
        } catch (Exception e) {
            System.out.println("Whoops!");
        }
        
        while (true) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (allPacketsAckd()) {
                break;
            }
            
            String result = "";
            
            //Using resendPackets like a pseudo-interrupt
            if (!resendPackets.isEmpty()) {
                index = resendPackets.poll();
                resendPacket = true;
            } else {                      
                index = packetIndex;
                
                //Increment packetIndex if necessary else recv (end of window)
                if (packetIndex < (windowPos + windowSize) && packetIndex < packetCount - 1) {
                    packetIndex = packetIndex < packetCount - 1 ? packetIndex + 1 : packetIndex;
                } else {
                    try {
                        recvPacket();
                    } catch (IOException e) {
                        System.out.println("Whoops!");
                    }
                }
                
                //If packet has been sent and is not a resend, skip
                if (sentPackets.get(index)) {
                    continue;
                }
            }
            
              
            try {
                //resendPacket is to override the skip behavior
                if (!skipPackets.get(index) || resendPacket) {
                    sendPacket(packets.get(index));
                }
            } catch (Exception ex) {
                Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Error sending Packet " + index);
                continue;
            }
            
            resendPacket = false;
            
            //Mark Packet sent
            sentPackets.set(index, true);

            //Start timer for timeout and possible resend
            timeoutTimers.set(index, new Timer("Packet"+index));
            timeoutTimers.get(index).schedule(new RemindTask(index), TIMEOUT);


            result = "Packet " + index + " Sent";

            System.out.println(result + buildWindowString());
        }
        System.out.println("Sender finished");
    }
    
    private boolean allPacketsAckd() {
        //Assume true
        boolean res = true;
        
        //Iterate over packet acknowledgement statuses
        for (int i = 0; i < ackdPackets.size(); i++) {
            //If any are false, conditions fails and false is returned
            if (!ackdPackets.get(i)) {
                res = false;
            }
        }
        return res;
    }
    
    class RemindTask extends TimerTask {
        private int packetIndex;
        
        public RemindTask(int packetIndex) {
            super();
            this.packetIndex = packetIndex;
        }
        
        public void run() {
            checkPacketTimeout(packetIndex);
            timeoutTimers.get(packetIndex).cancel();
        }
    }
}
