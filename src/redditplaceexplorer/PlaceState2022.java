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
public class PlaceState2022 extends PlaceStateImpl {

    public PlaceState2022() throws FileNotFoundException, IOException {
        super(400000);
    }

    @Override
    protected int getActionByteLength() {
        return 8;
    }
    
    @Override
    protected void parseActionData() {
        int action = (actionData.nextAsInteger() << 24) | (actionData.nextAsInteger() << 16) | (actionData.nextAsInteger() << 8) | actionData.nextAsInteger();
        int colorXor = (action & 0xFF);
        int y = ((action >>> 8) & 0xFFF);
        int x = ((action >>> 20) & 0xFFF);

        if (y == 0xFFF) y = -1;
        if (x == 0xFFF) x = -1;
        
        currentX = (short)x;
        currentY = (short)y;
        currentColorXor = (byte)colorXor;
    }

    @Override
    protected int getYear() {
        return 2022;
    }

    
}
