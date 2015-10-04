/*
 * SenderMain Class for Sender
 * Travis Alpers and Brian Lamb
 * CSCI466 - Networks
 * Lab 3
 */
package csci466.lab3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SenderMain {

    public static void main(String[] args) {
        //BufferedReader for console input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
        //Strings to hold user inputted values 
        String windowSizeString;
        String maxSequenceString;
        String droppedPacketsString;
        
        //Get user input
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
        
        //Convert user input to useful data types
        Integer windowSize = new Integer(windowSizeString);
        Integer maxSequence = new Integer(maxSequenceString);
        String[] skipList = droppedPacketsString.split(",");
        
        //Build list of packets to skip
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
        
        //Create thread with sender instance
        Thread sender = null;
        try {
            sender = new Thread(new Sender(windowSize, maxSequence, 4243, "localhost", 4242, skipPackets));
        } catch (UnknownHostException e) {
            System.out.println("Unknown host");
        }
        
        //If construction successful - RUN
        if (sender != null) {
            sender.start();
        }
    }
    
}
