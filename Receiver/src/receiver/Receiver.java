/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alperst
 */
public class Receiver implements Runnable {
    
    private DatagramSocket socket;
    
    private final int port;
    
    private int windowSize;
    
    private int windowPos;
    
    private int packetCount;
    
    private boolean headerReceived = false;
    
    private List<Boolean> rcvdPackets;
    
    public Receiver(int port) {
        this.port = port;
    }
    
    private void init() throws SocketException {
        socket = new DatagramSocket(port);
    }
    
    private void sendPacket(DatagramPacket packet) throws SocketException {
        //Check alive
        if (socket == null || !socket.isBound()) {
            init();
        }
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Error sending ACK Packet" + packet.getData()[0]);
        }
    }
    
    private void sendACK(DatagramPacket packet) {
        
        try {
            sendPacket(packet);
            System.out.println("Packet " + packet.getData()[0] + " ACK Sent");
        } catch (SocketException e) {
            System.out.println("Whoops!");
        }
    }
    
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
    
    private boolean allPacketsRcvd() {
        boolean res = true;
        for (int i = 0; i < packetCount; i++) {
            if (!rcvdPackets.get(i)) {
                res = false;
            }
        }
        return res;
    }

    @Override
    public void run() {  
        System.out.println("Receiver Listening...");
        while (true) {
            //Check if we are done
            if (headerReceived && allPacketsRcvd()) {
                break;
            }
            
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                recvPacket(packet);
            } catch (SocketException e) {
                System.out.println("Whoops!");
            }
            
            //This is init packet - init vars
            if (packet.getData()[0] == -1) {
                packetCount = packet.getData()[1];
                windowSize = packet.getData()[2];
                windowPos = 0;
                rcvdPackets = new ArrayList<>();
                for (int i = 0; i < packetCount; i++) {
                    rcvdPackets.add(Boolean.FALSE);
                }
                headerReceived = true;
                continue;
            } 
            
            if (!headerReceived) {
                continue;
            }  
            
            //Mark received
            rcvdPackets.set(packet.getData()[0], true);
            System.out.println("Received Packet" + packet.getData()[0]);

            //Send ACK
            sendACK(packet);

            //Advance window
            while (windowPos < rcvdPackets.size() && rcvdPackets.get(windowPos)) {
                windowPos++;
            }         
            
        }
        System.out.println("Receiver done.");
    }
    
}
