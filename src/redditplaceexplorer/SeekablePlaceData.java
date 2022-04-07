/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package redditplaceexplorer;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author bowen
 */
public class SeekablePlaceData extends SeekableByteData {

    public SeekablePlaceData(String filepath, int bufferSize) throws FileNotFoundException, IOException {
        super(filepath, bufferSize);
    }
    
    public int nextInteger() {
        int value = 0;
        for (int i=0; i<4 && hasNext(); i++) {
            value |= nextAsInteger() << (24 - i*8);
        }
        return value;
    }
    public int previousInteger() {
        int value = 0;
        for (int i=0; i<4 && hasPrevious(); i++) {
            value |= previousAsInteger() << (i*8);
        }
        return value;
    }
    
    public long nextLong() {
        long value = 0;
        for (int i=0; i<8 && hasNext(); i++) {
            value |= nextAsLong() << (56 - i*8);
        }
        return value;
    }
    public long previousLong() {
        long value = 0;
        for (int i=0; i<8 && hasPrevious(); i++) {
            value |= previousAsLong() << (i*8);
        }
        return value;
    }
    
    
    public long nextVariableLong() {
        long value = 0;
        int bits = 0;
        while (true) {
            if (!hasNext()) {
                return value;
            }
            long b = nextAsLong();
            value |= (b & 0x7F) << bits;
            bits += 7;
            if ((b & 0x80) == 0) {
                return value;
            }
        }
    }
    
    public long previousVariableLong() {
        int length = hasPrevious() ? 1 : 0;
        previous(); //Skip previous byte, should be a end-of-integer byte.
        while (hasPrevious()) {
            long b = previousAsLong();
            if ((b & 0x80) == 0) {
                next();
                break;
            }
            length++;
        }
        
        long value = 0;
        int bits = 0;
        int i = 0;
        while (hasNext() && i < length) {
            long b = nextAsLong();
            value |= (b & 0x7F) << bits;
            bits += 7;
            i++;
            if ((b & 0x80) == 0) {
                break;
            }
        }
        
        seek(getPosition() - length);
        return value;
    }
    
}
