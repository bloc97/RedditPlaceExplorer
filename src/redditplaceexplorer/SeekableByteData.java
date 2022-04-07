/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package redditplaceexplorer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author bowen
 */
public class SeekableByteData {
    
    private final RandomAccessFile file;
    private final byte[] buffer;
    private final long length;
    
    private long bufferOffset;
    private int bufferPosition;
    

    public SeekableByteData(String filepath, int bufferSize) throws FileNotFoundException, IOException {
        this.file = new RandomAccessFile(filepath, "r");
        this.buffer = new byte[bufferSize];
        this.length = this.file.length();
        this.bufferOffset = 0L;
        this.bufferPosition = 0;
        readBuffer(bufferOffset);
    }
    
    private boolean readBuffer(long newBufferOffset) {
        try {
            file.seek(newBufferOffset);
            file.read(buffer);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
    
    private boolean update(long position) {
        long newBufferOffset = position - buffer.length / 2;
        if (newBufferOffset < 0) {
            newBufferOffset = 0;
        }

        if (!readBuffer(newBufferOffset)) {
            return false;
        }

        bufferOffset = newBufferOffset;
        bufferPosition = (int)(position - newBufferOffset);
        return true;
    }
    
    public long getPosition() {
        return bufferOffset + bufferPosition;
    }

    public long getLength() {
        return length;
    }
    
    public boolean hasNext() {
        return getPosition() < length;
    }
    public boolean hasPrevious() {
        return getPosition() > 0;
    }
    
    public long nextAsLong() {
        return Byte.toUnsignedLong(next());
    }
    public long previousAsLong() {
        return Byte.toUnsignedLong(previous());
    }
    public int nextAsInteger() {
        return Byte.toUnsignedInt(next());
    }
    public int previousAsInteger() {
        return Byte.toUnsignedInt(previous());
    }
    
    public byte next() {
        byte b = buffer[bufferPosition];
        
        long position = getPosition() + 1;
        if (position > length) {
            return b;
        }
        
        if (bufferPosition < buffer.length - 1) {
            bufferPosition++;
        } else {
            update(position);
        }
        return b;
    }
    
    public byte previous() {
        long position = getPosition() - 1;
        if (position < 0) {
            return buffer[bufferPosition];
        }
        if (bufferPosition > 0) {
            bufferPosition--;
        } else {
            update(position);
        }
        
        return buffer[bufferPosition];
    }
    
    public boolean seek(long position) {
        if (position < 0 || position > length) {
            return false;
        }
        
        long positionDelta = position - getPosition();
        
        long newBufferPosition = bufferPosition + positionDelta;
        if (newBufferPosition >= 0 && newBufferPosition < buffer.length) {
            bufferPosition = (int)newBufferPosition;
        } else {
            return update(position);
        }
        return true;
    }
    
}
