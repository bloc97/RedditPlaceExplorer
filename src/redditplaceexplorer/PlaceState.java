/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package redditplaceexplorer;

import java.awt.Image;
import java.awt.image.IndexColorModel;

/**
 *
 * @author bowen
 */
public interface PlaceState {
    public abstract long getSize();
    public abstract int getWidth();
    public abstract int getHeight();
    
    default public boolean next() {
        if (!hasNext()) {
            return false;
        }
        seekIndex(getIndex() + 1);
        return true;
    }
    default public boolean previous() {
        if (!hasPrevious()) {
            return false;
        }
        seekIndex(getIndex() - 1);
        return true;
    }
    
    default public boolean hasNext() {
        return getIndex() < getSize();
    }
    default public boolean hasPrevious() {
        return getIndex() > 0;
    }
    
    public abstract long reset();
    
    default public long seekIndex(long i) {
        return seekIndex(i, false);
    }
    public abstract long seekIndex(long i, boolean useSnapshot);
    public abstract long seekTime(long t);
    
    
    public abstract long getUserTime();
    public abstract long getUserIndex();
    public abstract long getStartIndex();
    public abstract long getStartTime();
    public abstract long getEndTime();
    
    public abstract long getIndex();
    public abstract long getTime();
    public abstract int getX();
    public abstract int getY();
    public abstract byte getColorXOR();
    default public byte getColor() {
        return isValid() ? getColor(getX(), getY()) : -1;
    }
    default public boolean isValid() {
        return isValid(getX(), getY());
    }
    
    public abstract byte getColor(int x, int y);
    default public boolean isValid(int x, int y) {
        return (x >= 0 && x < getWidth() && y >= 0 && y < getHeight());
    }
    
    public abstract int colorToRGB(byte color);
    
    default public IndexColorModel getColorIndexModel(int colors) {
        if (colors <= 0 || colors > 255) {
            throw new IllegalArgumentException("8-bit IndexColorModel can only take a minimum of 1 and a maximum of 255 colors!");
        }
        
        byte[] r = new byte[colors];
        byte[] g = new byte[colors];
        byte[] b = new byte[colors];
        
        for (int i=0; i<colors; i++) {
            int rgb = colorToRGB((byte)i);
            
            r[i] = (byte)((rgb >>> 16) & 0xFF);
            g[i] = (byte)((rgb >>>  8) & 0xFF);
            b[i] = (byte)((rgb       ) & 0xFF);
        }
        return new IndexColorModel(8, colors, r, g, b);
    }
    public abstract Image getImage();
}
