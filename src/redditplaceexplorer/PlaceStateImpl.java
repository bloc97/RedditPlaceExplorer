/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package redditplaceexplorer;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bowen
 */
public abstract class PlaceStateImpl implements PlaceState {
    final private long size;
    final private int canvasWidth;
    final private int canvasHeight;
    
    final private long startIndex;
    final private long startIndexTime;
    final private long startTime;
    final private long endTime;
    
    final private int[] colorIndex;
    
    final private long[] snapshotIndex;
    final private long[] snapshotTime;
    
    //Future TODO: Add image with user data, heatmap, time data, index data etc...
    
    private long currentIndex;
    private long currentTime;
    
    protected short currentX;
    protected short currentY;
    protected byte currentColorXor;
    
    final private BufferedImage rgbCanvas;
    
    final private byte[] dataCanvas;
    
    final private byte[] startBlankingXorCanvas;
    
    final private SeekablePlaceData metaData;
    final protected SeekablePlaceData actionData;
    final private SeekablePlaceData timeDeltaData;
    private RandomAccessFile snapshotData;
    private RandomAccessFile snapshotMetaData;
    
    public PlaceStateImpl(long snapshotInterval) throws FileNotFoundException, IOException {
        metaData = new SeekablePlaceData("data/" + getYear() + "/metadata.dat", 1024);
        actionData = new SeekablePlaceData("data/" + getYear() + "/actions.dat", 1048576);
        timeDeltaData = new SeekablePlaceData("data/" + getYear() + "/timestamps_delta.dat", 1048576);
        
        size = metaData.nextLong();
        canvasWidth = metaData.nextInteger();
        canvasHeight = metaData.nextInteger();
        startIndex = metaData.nextLong();
        startTime = metaData.nextLong();
        endTime = metaData.nextLong();
        colorIndex = new int[metaData.nextInteger()];
        
        for (int i=0; i<colorIndex.length; i++) {
            colorIndex[i] = metaData.nextInteger();
        }
        
        currentIndex = 0;
        currentTime = 0;
        currentX = -1;
        currentY = -1;
        currentColorXor = 0;
        
        rgbCanvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_BYTE_INDEXED, getColorIndexModel(colorIndex.length));
        dataCanvas = ((DataBufferByte) rgbCanvas.getRaster().getDataBuffer()).getData();
        startBlankingXorCanvas = new byte[canvasWidth * canvasHeight];
        
        try {
            snapshotData = new RandomAccessFile("cache/cache_" + getYear() + ".dat", "rw");
            snapshotMetaData = new RandomAccessFile("cache/meta_" + getYear() + ".dat", "rw");
        } catch (FileNotFoundException ex) {
            snapshotData = null;
            snapshotMetaData = null;
        }
        long numSnapshots = size / snapshotInterval;
        
        while (numSnapshots < 2) {
            snapshotInterval = snapshotInterval / 2;
            numSnapshots = size / snapshotInterval;
        }
        
        long initialSnapshot = snapshotInterval / 2;
        
        snapshotIndex = new long[(int)numSnapshots];
        snapshotTime = new long[(int)numSnapshots];
        
        for (int i=0; i<numSnapshots; i++) {
            snapshotIndex[i] = initialSnapshot + snapshotInterval * i;
            snapshotTime[i] = readSnapshotTime(i);
        }
        reset();
        startIndexTime = currentTime;
    }
    
    public int getSnapshotLength() {
        return snapshotTime.length;
    }
    public long getSnapshotTime(int sn) {
        return snapshotTime[sn];
    }
    public long getCacheSize() {
        if (snapshotData == null) {
            return -1;
        }
        try {
            return snapshotData.length();
        } catch (IOException ex) {
            return -1;
        }
    }
    public boolean checkCache() {
        for (int i=0; i<snapshotIndex.length; i++) {
            if (snapshotTime[i] <= 0) {
                return false;
            }
        }
        return true;
    }
    public boolean buildCache() {
        if (snapshotData == null) {
            System.out.println("Note: /cache/ folder missing, could not build " + getYear() + " cache.");
            return false;
        } else if (checkCache()) {
            System.out.println(getYear() + " cache already found, skipped building...");
            return false;
        }
        
        try {
            snapshotData.setLength(0);
            snapshotMetaData.setLength(0);
            System.out.println("Note: Delete /cache/ folder to prevent building cache.");
            System.out.println("Building " + getYear() + " cache...");
            System.out.println("");
            for (int i=0; i<snapshotIndex.length; i++) {
                seekIndex(snapshotIndex[i], false);
                writeSnapshot(i);
                long totalSize = (dataCanvas.length + 21) * snapshotIndex.length;
                System.out.print(humanReadableByteCountBin(snapshotData.length()) + "/" + humanReadableByteCountBin(totalSize) + "\r");
            }
            System.out.println("");
            System.out.println("Done.");
            System.out.println("");
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    
    //https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
    static CharacterIterator ci = new StringCharacterIterator("KMGTPE");
    public static String humanReadableByteCountBin(long bytes) {
        ci.first();
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }
    
    protected abstract int getActionByteLength();
    protected abstract void parseActionData();
    protected abstract int getYear();
    
    
    private int findClosestSnapshotIndex(long newIndex) {
        if (snapshotData == null) {
            return -1;
        }
        if (snapshotIndex.length == 1) {
            if (Math.abs(newIndex - snapshotIndex[0]) < Math.abs(newIndex - currentIndex)) {
                return 0;
            } else {
                return -1;
            }
        }
        int closestSn = Arrays.binarySearch(snapshotIndex, newIndex);
        
        if (closestSn < 0) {
            closestSn = -closestSn - 1;
        }
        if (closestSn >= snapshotIndex.length) {
            closestSn = snapshotIndex.length - 1;
        }
        if (closestSn == 0) {
            closestSn = 1;
        }
        
        if (Math.abs(newIndex - snapshotIndex[closestSn - 1]) < Math.abs(newIndex - snapshotIndex[closestSn])) {
            closestSn = closestSn - 1;
        }
        if (closestSn < 0 || Math.abs(newIndex - snapshotIndex[closestSn]) > Math.abs(newIndex - currentIndex)) {
            return -1;
        }
        return closestSn;
    }
    private int findClosestSnapshotTime(long newTime) {
        if (snapshotData == null) {
            return -1;
        }
        if (snapshotTime.length == 1) {
            if (Math.abs(newTime - snapshotTime[0]) < Math.abs(newTime - currentTime)) {
                return 0;
            } else {
                return -1;
            }
        }
        int closestSn = Arrays.binarySearch(snapshotTime, newTime);
        if (closestSn < 0) {
            closestSn = -closestSn - 1;
        }
        if (closestSn >= snapshotTime.length) {
            closestSn = snapshotTime.length - 1;
        }
        if (closestSn == 0) {
            closestSn = 1;
        }
        
        if (Math.abs(newTime - snapshotTime[closestSn - 1]) < Math.abs(newTime - snapshotTime[closestSn])) {
            closestSn = closestSn - 1;
        }
        if (closestSn < 0 || Math.abs(newTime - snapshotTime[closestSn]) > Math.abs(newTime - currentTime)) {
            return -1;
        }
        return closestSn;
    }
    private long readSnapshotTime(int sn) {
        if (snapshotData == null) {
            return -1;
        }
        try {
            snapshotData.seek((long)sn * 8L);
            return snapshotMetaData.readLong();
        } catch (IOException ex) {
            return -1;
        }
    }
    private boolean hasSnapshot(int sn) {
        return sn >= 0 && sn < snapshotIndex.length && snapshotTime[sn] >= 1;
    }
    private boolean readSnapshot(int sn) {
        if (snapshotData == null) {
            return false;
        }
        if (!hasSnapshot(sn)) {
            return false;
        }
        long byteLength = dataCanvas.length + 21;
        try {
            snapshotData.seek((long)sn * byteLength);
            snapshotData.read(dataCanvas);
            currentX = snapshotData.readShort();
            currentY = snapshotData.readShort();
            currentColorXor = snapshotData.readByte();
            actionData.seek(snapshotData.readLong());
            timeDeltaData.seek(snapshotData.readLong());
            
            snapshotMetaData.seek((long)sn * 8L);
            currentTime = snapshotMetaData.readLong();
            snapshotTime[sn] = currentTime;
            currentIndex = snapshotIndex[sn];
            
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
    private boolean writeSnapshot(int sn) throws IOException {
        if (snapshotData == null) {
            return false;
        }
        long byteLength = dataCanvas.length + 21;
        snapshotData.seek((long)sn * byteLength);
        snapshotData.write(dataCanvas);
        snapshotData.writeShort(currentX);
        snapshotData.writeShort(currentY);
        snapshotData.writeByte(currentColorXor);
        snapshotData.writeLong(actionData.getPosition());
        snapshotData.writeLong(timeDeltaData.getPosition());

        snapshotMetaData.seek((long)sn * 8L);
        snapshotMetaData.writeLong(currentTime);
        snapshotTime[sn] = currentTime;

        return true;
    }
    
    @Override
    public int colorToRGB(byte color) {
        int icolor = Byte.toUnsignedInt(color);
        return color < colorIndex.length ? colorIndex[icolor] : 0;
    }
    
    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getWidth() {
        return canvasWidth;
    }

    @Override
    public int getHeight() {
        return canvasHeight;
    }

    @Override
    public long reset() {
        if (getUserIndex() != 0) {
            currentIndex = 0;
            currentTime = 0;
            currentX = -1;
            currentY = -1;
            currentColorXor = 0;
            Arrays.fill(dataCanvas, (byte)0);
            Arrays.fill(startBlankingXorCanvas, (byte)0);
            actionData.seek(0);
            timeDeltaData.seek(0);
        }
        seekIndex(startIndex);
        return seekIndex(startIndex);
    }

    @Override
    public long seekIndex(long i, boolean useSnapshot) {
        
        if (i < 0) {
            i = 0;
        } else if (i > size) {
            i = size;
        }
        
        if (useSnapshot) {
            readSnapshot(findClosestSnapshotIndex(i));
        }
        
        long delta = i - currentIndex;
        long direction = Long.signum(delta);
        
        while (delta != 0) {
            final long timeDelta = direction > 0 ? timeDeltaData.nextVariableLong() : -timeDeltaData.previousVariableLong();
            currentIndex += direction;
            currentTime += timeDelta;
            
            if (currentIndex == startIndex && direction > 0) {
                System.arraycopy(dataCanvas, 0, startBlankingXorCanvas, 0, dataCanvas.length);
                Arrays.fill(dataCanvas, (byte)0);
            }
            if (direction > 0) {
                parseActionData();
                xorCanvas();
            } else {
                xorCanvas();
                if (actionData.seek(actionData.getPosition() - getActionByteLength())) {
                    parseActionData();
                } else {
                    currentX = -1;
                    currentY = -1;
                    currentColorXor = 0;
                    actionData.seek(0);
                    timeDeltaData.seek(0);
                }
            }
            
            if (currentIndex == startIndex - 1 && direction < 0) {
                System.arraycopy(startBlankingXorCanvas, 0, dataCanvas, 0, dataCanvas.length);
                //Arrays.fill(startBlankingXorCanvas, (byte)0);
            }
            
            delta = i - currentIndex;
        }
        
        return currentIndex;
    }
    
    private void xorCanvas() {
        if (isValid()) {
            dataCanvas[currentY * canvasWidth + currentX] = (byte)(dataCanvas[currentY * canvasWidth + currentX] ^ currentColorXor);
        }
    }

    @Override
    public long seekTime(long t) {
        long oldTime = currentTime;
        readSnapshot(findClosestSnapshotTime(t));
        
        if (currentTime < t) {
            while (currentTime < t && hasNext()) {
                next();
            }
        } else if (currentTime > t) {
            while (currentTime > t && hasPrevious()) {
                previous();
            }
        }
        
        //Check if stuck
        if (oldTime == currentTime && t != currentTime) {
            //Retry once without snapshotting
            if (currentTime < t) {
                while (currentTime <= t && hasNext()) {
                    next();
                }
            } else if (currentTime > t) {
                while (currentTime >= t && hasPrevious()) {
                    previous();
                }
            }
        }
        return currentTime;
    }

    @Override
    public long getIndex() {
        return currentIndex;
    }

    @Override
    public long getTime() {
        return currentTime;
    }

    @Override
    public int getX() {
        return currentX;
    }

    @Override
    public int getY() {
        return currentY;
    }

    @Override
    public byte getColorXOR() {
        return currentColorXor;
    }

    @Override
    public byte getColor(int x, int y) {
        return isValid(x, y) ? dataCanvas[y * canvasWidth + x] : -1;
    }
    
    @Override
    public Image getImage() {
        return rgbCanvas;
    }

    @Override
    public long getUserIndex() {
        return getIndex() - startIndex;
    }

    @Override
    public long getUserTime() {
        return startIndexTime <= 0 ? getStartTime() : startIndexTime;
    }

    @Override
    public long getStartIndex() {
        return startIndex;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }
    
}
