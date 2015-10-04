/*
 * Main Class For Receiver 
 * Travis Alpers and Brian Lamb
 * CSCI466 - Networks
 * Lab 3
 */
package receiver;

public class Main {

    public static void main(String[] args) {
        //Create thread with Receiver instance and run 
        Thread t = new Thread(new Receiver(4242));
        t.run();
    }
    
}
