/*
 * Receiver Class 
 * Travis Alpers and Brian Lamb
 * CSCI466 - Networks
 * Lab 3
 */
package receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class Receiver implements Runnable {
    
    private DatagramSocket socket;
    
    private final int port;
    
    private int windowSize;
    
    private int windowPos;
    
    private int packetCount;
    
    private boolean headerReceived = false;
    
    private List<Boolean> rcvdPackets;
    
    /*
     * ctor 
     */
    public Receiver(int port) {
        this.port = port;
    }
    
    /* 
     * Initialize the socket 
     */
    private void init() throws SocketException {
        socket = new DatagramSocket(port);
    }
    
    /*
     * Sends packet on instance socket
     */
    private void sendPacket(DatagramPacket packet) throws SocketException {
        //Check alive
        if (socket == null || !socket.isBound()) {
            init();
        }
        //Try send packet
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Error sending ACK Packet " + packet.getData()[0]);
        }
    }
    
    /*
     * Send Acknowledgement, Reusing same packet that was received
     */
    private void sendACK(DatagramPacket packet) {
        
        try {
            sendPacket(packet);
            //System.out.println("Packet " + packet.getData()[0] + " ACK Sent " + buildWindowString());
        } catch (SocketException e) {
            System.out.println("Whoops!");
        }
    }
    
    /*
     * Build string to show current Window
     */
    private String buildWindowString() {
        String window = "[";
        String delimiter = "";
        for (int i = 0; i < windowSize; i++) {
            if (windowPos + i >= rcvdPackets.size()) {
                window += delimiter;
                window += "-";
                delimiter = ", ";
                continue;
            }
            window += delimiter;

            window += windowPos + i;
            if (rcvdPackets.get(windowPos + i)) {
                window += "#";
            }
            delimiter = ", ";
        }
        window += "]";
        return window;
    }
    
    /*
     * Recieve packet 
     */
    private void recvPacket(DatagramPacket packet) throws SocketException {
        //Check alive
        if (socket == null || !socket.isBound()) {
            init();
        }
        
        //try receive - this is blocking
        try {
            socket.receive(packet);
        } catch (Exception e) {
            System.out.println("Error receiving data on Receiver");
        }
    }
    
    /*
     * Check if all packets have been received
     * Packet informatin is given in header packet
     */
    private boolean allPacketsRcvd() {
        boolean res = true;
        for (int i = 0; i < packetCount; i++) {
            if (!rcvdPackets.get(i)) {
                res = false;
            }
        }
        return res;
    }

    /*
     * Main work method
     * Loop until header is received and then all packets are received
     */
    @Override
    public void run() {  
        //Debug startup message
        System.out.println("Receiver Listening...");
        
        //Main Loop
        while (true) {
            
            //Check if we are done - all packets received after header
            if (headerReceived && allPacketsRcvd()) {
                break;
            }
            
            //Create array for recieving data
            byte[] data = new byte[1024];
            //Create packet for receiving
            DatagramPacket packet = new DatagramPacket(data, data.length);
            
            //Try receive
            try {
                recvPacket(packet);
            } catch (SocketException e) {
                System.out.println("Whoops!");
            }
            
            //We have defined a header packet
            //as one which has -1 as first byte value
            if (packet.getData()[0] == -1) {
                //Header Packet Structure
                //Second byte is packetCount
                packetCount = packet.getData()[1];
                //Third byte is windowSize
                windowSize = packet.getData()[2];
                //Restart windowPos
                windowPos = 0;
                //Init received packet array
                rcvdPackets = new ArrayList<>();
                for (int i = 0; i < packetCount; i++) {
                    rcvdPackets.add(Boolean.FALSE);
                }
                //Mark header packet received
                headerReceived = true;
           
                continue;
            } 
            
            //If we haven't received the header packet do nothing
            if (!headerReceived) {
                continue;
            }  
            
            //Mark received
            rcvdPackets.set(packet.getData()[0], true);

            //Send ACK
            sendACK(packet);

            //Advance window
            while (windowPos < rcvdPackets.size() && rcvdPackets.get(windowPos)) {
                windowPos++;
            }      
            
            //Print packet received
            System.out.println("Received Packet" + packet.getData()[0] + " Send ACK " + packet.getData()[0] + " " + buildWindowString());   
            
        }
        //Debug message to state done
        System.out.println("Receiver done.");
    }
    
}
