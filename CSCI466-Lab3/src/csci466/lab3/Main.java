/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package csci466.lab3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alperst
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
        String windowSizeString;
        String maxSequenceString;
        String droppedPacketsString;
        try {
            System.out.print("Enter the window's size on the sender: ");
            windowSizeString = br.readLine();
            System.out.print("Enter the maximum sequence number on the sender: ");
            maxSequenceString = br.readLine();
            System.out.print("Select the packet(s) that will be dropped: ");
            droppedPacketsString = br.readLine();
        } catch (IOException e) {
            System.out.println("I/O Error - Exiting...");
            return;
        }
        
        Integer windowSize = new Integer(windowSizeString);
        Integer maxSequence = new Integer(maxSequenceString);
        String[] skipList = droppedPacketsString.split(",");
        
        List<Boolean> skipPackets = new ArrayList<>();
        for (int i = 0; i < maxSequence; i++) {
            skipPackets.add(false);
        }
        
        for (int i = 0; i < skipList.length; i++) {
            try {
                skipPackets.set(new Integer(skipList[i]), Boolean.TRUE);
            } catch (Exception e) {
                
            }
        }
        
        
        Thread sender = null;
        try {
            sender = new Thread(new Sender(windowSize, maxSequence, 4243, "localhost", 4242, skipPackets));
        } catch (UnknownHostException e) {
            System.out.println("Unknown host");
        }
        
        if (sender != null) {
            sender.start();
        }
    }
    
}
