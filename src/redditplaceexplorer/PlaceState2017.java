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
public class PlaceState2017 extends PlaceStateImpl {

    public PlaceState2017() throws FileNotFoundException, IOException {
        super(100000);
    }

    @Override
    protected int getActionByteLength() {
        return 6;
    }
    
    @Override
    protected void parseActionData() {
        int action = (actionData.nextAsInteger() << 16) | (actionData.nextAsInteger() << 8) | actionData.nextAsInteger();
        int colorXor = (action & 0xF);
        int y = ((action >>> 4) & 0x3FF);
        int x = ((action >>> 14) & 0x3FF);

        if (y == 0xFFF) y = -1;
        if (x == 0xFFF) x = -1;

        currentX = (short)x;
        currentY = (short)y;
        currentColorXor = (byte)colorXor;
    }

    @Override
    protected int getYear() {
        return 2017;
    }

    
}
