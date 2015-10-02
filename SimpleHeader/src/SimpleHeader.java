
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author alperst
 */
public class SimpleHeader {
    private InetAddress address;
    private int port;
    private byte[] data;
    public SimpleHeader(byte[] data) {
        byte[] addressBytes = new byte[] {
            data[0],
            data[1],
            data[2],
            data[3]
        };
        
        try {
            address = InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            
        }
        
       port = data[4] << 24 | data[5] << 16 | data[6] << 8 | data[7];
       
       this.data = new byte[data.length - 8];
       for (int i = 8; i < data.length; i++) {
           this.data[i-8] = data[i];
       }
    }
    
    public SimpleHeader(InetAddress address, int port, byte[] data) {
        this.address = address;
        this.port = port;
        this.data = data;
    }
    
    public byte[] serialize() {
        byte[] serialize = new byte[4 + 4 + this.data.length];
        byte[] addressBytes = address.getAddress();
        for (int i = 0; i < 4; i++) {
            serialize[i] = addressBytes[i];
        }
        serialize[4] = (byte)(port & 0xFF000000);
        serialize[5] = (byte)(port & 0x00FF0000);
        serialize[6] = (byte)(port & 0x0000FF00);
        serialize[7] = (byte)(port & 0x000000FF);
        
        for (int i = 0; i < this.data.length; i++) {
            serialize[i+8] = this.data[i];
        }
        return serialize;
    }
}
