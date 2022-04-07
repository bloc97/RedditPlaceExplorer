/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package redditplaceexplorer;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

/**
 *
 * @author bowen
 */
public class MainGUI {
    
    final private static Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
    
    private static int posX = 0;
    private static int posY = 0;
    private static int zoomLevel = 1;
    
    public static String[] timeDeltasName = new String[] {"1 second", "4 seconds", "15 seconds", "1 minute", "4 minutes", "15 minutes", "1 hour", "4 hours"};
    public static long[] timeDeltas = new long[] {1000, 4000, 15000, 60000, 240000, 900000, 3600000, 14400000};
    public static int chosenTimeDelta = 3;
    
    public static long multiplier = 1;
    
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    
    private static PlaceStateImpl placeData;
    private static PlaceStateImpl placeDataSwap;
    
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
    private static Date dateObject = new Date();
    
    private static Font boldFont = new Font("Monospaced", Font.BOLD, 20);
    private static Font thinFont = new Font("Monospaced", Font.PLAIN, 20);
    private static Font slantedFont = new Font("Monospaced", Font.ITALIC, 20);
    
    private static int mouseX = Integer.MIN_VALUE;
    private static int mouseY = Integer.MIN_VALUE;
    
    private static int[] savedPresetsCamera = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    private static long[] savedPresetsState = new long[]{Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
    private static PlaceState[] savedPresetsPlace = new PlaceState[]{null, null, null, null, null, null, null, null, null, null};
    
    private static boolean stateChange = true;
    private static boolean firstInput = true;
    
    
    private static int seekTimeDirection = 0;
    
    private static boolean isSpaceDown = false;
    
    
    private static int uiHiddenLevel = 0;
    private static boolean showHelp = true;
    private static boolean showSeekBar = true;
    private static int pickPositionX = Integer.MIN_VALUE;
    private static int pickPositionY = Integer.MIN_VALUE;
    
    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
        
        System.setProperty( "sun.java2d.uiScale", "1.0" );
        
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        System.out.print("Loading 2017 dataset... ");
        placeDataSwap = new PlaceState2017();
        System.out.println("Done");
        System.out.print("Loading 2022 dataset... ");
        placeData = new PlaceState2022();
        System.out.println("Done\n");
        
        placeDataSwap.buildCache();
        placeData.buildCache();
        
        placeDataSwap.reset();
        placeData.reset();
        
        BufferedImage canvas = new BufferedImage(placeData.getWidth(), placeData.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        JFrame frame = initWindow("r/place Viewer", canvas);
        Container container = frame.getContentPane();
        lastWidth = container.getWidth();
        lastHeight = container.getHeight();
        
        resetCamera(container);
        
        canvas.getGraphics().fillRect(0, 0, placeData.getWidth(), placeData.getHeight());
        
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_CONTROL:
                        multiplier /= 100;
                        break;
                    case KeyEvent.VK_SHIFT:
                        multiplier /= 10;
                        break;
                    case KeyEvent.VK_A:
                        seekTimeDirection = 0;
                        break;
                    case KeyEvent.VK_D:
                        seekTimeDirection = 0;
                        break;
                    case KeyEvent.VK_SPACE:
                        isSpaceDown = false;
                        break;
                }
                if (multiplier < 0) {
                    multiplier = 1;
                }
                container.repaint();
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                firstInput = false;
                
                multiplier = 1;
                if (e.isShiftDown()) {
                    multiplier *= 10;
                }
                if (e.isControlDown()) {
                    multiplier *= 100;
                }
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_T:
                        PlaceStateImpl temp = placeData;
                        placeData = placeDataSwap;
                        placeDataSwap = temp;
                        stateChange = true;
                        break;
                    case KeyEvent.VK_F2:
                        uiHiddenLevel++;
                        if (uiHiddenLevel > 2) {
                            uiHiddenLevel = 0;
                        }
                        stateChange = true;
                        break;
                    case KeyEvent.VK_F3:
                        showSeekBar = !showSeekBar;
                        stateChange = true;
                        break;
                    case KeyEvent.VK_F11:
                        if (frame.isUndecorated()) {
                            frame.dispose();
                            Dimension dimension = TOOLKIT.getScreenSize();
                            frame.setSize((int)(dimension.width * 0.9), (int)(dimension.height * 0.9));
                            frame.setUndecorated(false);
                            frame.setVisible(true);
                            int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
                            int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
                            frame.setLocation(x, y);
                        } else {
                            frame.dispose();
                            frame.setLocation(0, 0);
                            frame.setSize(TOOLKIT.getScreenSize());
                            frame.setUndecorated(true);
                            frame.setVisible(true);
                        }
                        stateChange = true;
                        break;
                    case KeyEvent.VK_F1:
                        showHelp = !showHelp;
                        stateChange = true;
                        break;
                    case KeyEvent.VK_C:
                        resetCamera(container);
                        stateChange = true;
                        break;
                    case KeyEvent.VK_V:
                        if (pickPositionX != Integer.MIN_VALUE && pickPositionY != Integer.MIN_VALUE) {
                            jumpCameraTo(container, pickPositionX, pickPositionY);
                            stateChange = true;
                        }
                        break;
                    case KeyEvent.VK_R:
                        placeData.reset();
                        stateChange = true;
                        break;
                    case KeyEvent.VK_W:
                        chosenTimeDelta += 1;
                        if (chosenTimeDelta >= timeDeltas.length) {
                            chosenTimeDelta = timeDeltas.length - 1;
                        }
                        break;
                    case KeyEvent.VK_S:
                        chosenTimeDelta -= 1;
                        if (chosenTimeDelta < 0) {
                            chosenTimeDelta = 0;
                        }
                        break;
                    case KeyEvent.VK_Q:
                        placeData.seekIndex(placeData.getIndex() - multiplier);
                        stateChange = true;
                        break;
                    case KeyEvent.VK_E:
                        placeData.seekIndex(placeData.getIndex() + multiplier);
                        stateChange = true;
                        break;
                    case KeyEvent.VK_A:
                        seekTimeDirection = -1;
                        break;
                    case KeyEvent.VK_D:
                        seekTimeDirection = 1;
                        break;
                    case KeyEvent.VK_SPACE:
                        isSpaceDown = true;
                        break;
                    case KeyEvent.VK_PAGE_UP:
                    case KeyEvent.VK_Z:
                        int oldZoomLevel = zoomLevel;
                        zoomLevel = zoomLevel * (2 * (int)multiplier) ;
                        if (zoomLevel > 128) {
                            zoomLevel = 128;
                        }
                        
                        double zoomAlpha = (double)zoomLevel / (double)oldZoomLevel;
                        int cameraPosX = container.getWidth() / 2;
                        int cameraPosY = container.getHeight() / 2;
                        
                        double relPosX = posX - cameraPosX;
                        double relPosY = posY - cameraPosY;

                        posX = (int)(relPosX * zoomAlpha + cameraPosX);
                        posY = (int)(relPosY * zoomAlpha + cameraPosY);
                        
                        stateChange = true;
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                    case KeyEvent.VK_X:
                        oldZoomLevel = zoomLevel;
                        zoomLevel = zoomLevel / (2 * (int)multiplier) ;
                        if (zoomLevel < 1) {
                            zoomLevel = 1;
                        }
                        
                        zoomAlpha = (double)zoomLevel / (double)oldZoomLevel;
                        cameraPosX = container.getWidth() / 2;
                        cameraPosY = container.getHeight() / 2;
                        
                        relPosX = posX - cameraPosX;
                        relPosY = posY - cameraPosY;

                        posX = (int)(relPosX * zoomAlpha + cameraPosX);
                        posY = (int)(relPosY * zoomAlpha + cameraPosY);
                        
                        stateChange = true;
                        break;
                    case KeyEvent.VK_UP:
                        posY += multiplier * zoomLevel;
                        stateChange = true;
                        break;
                    case KeyEvent.VK_DOWN:
                        posY -= multiplier * zoomLevel;
                        stateChange = true;
                        break;
                    case KeyEvent.VK_LEFT:
                        posX += multiplier * zoomLevel;
                        stateChange = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                        posX -= multiplier * zoomLevel;
                        stateChange = true;
                        break;
                    case KeyEvent.VK_F:
                        
                        if (uiHiddenLevel == 0) {
                            pickPositionX = ((container.getWidth() / 2 - posX + zoomLevel / 2) / zoomLevel);
                            pickPositionY = ((container.getHeight() / 2 - posY + zoomLevel / 2) / zoomLevel);
                        }
                        if (e.isAltDown()) {
                            pickPositionX = Integer.MIN_VALUE;
                            pickPositionY = Integer.MIN_VALUE;
                        }
                        stateChange = true;
                        break;
                }
                if (seekTimeDirection != 0) {
                    placeData.seekTime(placeData.getTime() + timeDeltas[chosenTimeDelta] * multiplier * seekTimeDirection);
                    stateChange = true;
                }
                
                if (isSpaceDown) {
                    if (placeData.isValid()) {
                        if (zoomLevel < 32) {
                            zoomLevel = 32;
                        }
                        jumpCameraTo(container, placeData.getX(), placeData.getY());
                        
                        stateChange = true;
                    }
                }
                checkPreset(e);
                container.repaint();
            }
            
        });
        
        MouseInputAdapter mouse = new MouseInputAdapter() {

            private boolean mouseLeftButton = false;
            private boolean mouseRightButton = false;
            private boolean mouseSeekBar = false;
            
            @Override
            public void mousePressed(MouseEvent e) {
                firstInput = false;
                super.mouseClicked(e);
                mouseX = e.getX();
                mouseY = e.getY();
                switch (e.getButton()) {
                    case MouseEvent.BUTTON1 -> mouseLeftButton = true;
                    case MouseEvent.BUTTON2 -> mouseRightButton = true;
                    case MouseEvent.BUTTON3 -> mouseRightButton = true;
                }
                if (mouseRightButton) {
                    if (uiHiddenLevel == 0) {
                        pickPositionX = ((mouseX - posX + zoomLevel / 2) / zoomLevel);
                        pickPositionY = ((mouseY - posY + zoomLevel / 2) / zoomLevel);
                    }
                    stateChange = true;
                    container.repaint();
                }
                
                if (mouseLeftButton) {
                    checkClickSeekBar();
                }
            }
            
            private void checkClickSeekBar() {
                int seekBarWidth = container.getWidth() - container.getWidth() / 8;
                int seekBarXStart = container.getWidth() / 16;
                int seekBarHeight = 20;
                int seekBarYStart = container.getHeight() - 50;

                if (mouseX >= seekBarXStart - 10 && mouseX <= seekBarXStart + seekBarWidth + 10 && mouseY >= seekBarYStart - 10 && mouseY <= seekBarYStart + seekBarHeight + 10 && showSeekBar && uiHiddenLevel < 2) {
                    double timePercent = (double)(mouseX - seekBarXStart) / seekBarWidth;
                    if (timePercent < 0) {
                        timePercent = 0;
                    } else if (timePercent > 1) {
                        timePercent = 1;
                    }
                    long newTime = (long)(timePercent * (placeData.getEndTime() - placeData.getUserTime())) + placeData.getUserTime();

                    placeData.seekTime(newTime);
                    stateChange = true;
                    mouseSeekBar = true;
                    container.repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                firstInput = false;
                super.mouseReleased(e);
                mouseX = e.getX();
                mouseY = e.getY();
                switch (e.getButton()) {
                    case MouseEvent.BUTTON1 ->  {
                        mouseLeftButton = false;
                        mouseSeekBar = false;
                    }
                    case MouseEvent.BUTTON2 -> mouseRightButton = false;
                    case MouseEvent.BUTTON3 -> mouseRightButton = false;
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                firstInput = false;
                super.mouseMoved(e);
                mouseX = e.getX();
                mouseY = e.getY();
                container.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                firstInput = false;
                super.mouseDragged(e);
                if (mouseLeftButton) {
                    if (mouseSeekBar && showSeekBar && uiHiddenLevel < 2) {
                        mouseX = e.getX();
                        mouseY = e.getY();
                        
                        int seekBarWidth = container.getWidth() - container.getWidth() / 8;
                        int seekBarXStart = container.getWidth() / 16;
                        double timePercent = (double)(mouseX - seekBarXStart) / seekBarWidth;
                        if (timePercent < 0) {
                            timePercent = 0;
                        } else if (timePercent > 1) {
                            timePercent = 1;
                        }
                        long newTime = (long)(timePercent * (placeData.getEndTime() - placeData.getUserTime())) + placeData.getUserTime();

                        placeData.seekTime(newTime);
                    } else {
                        posX += e.getX() - mouseX;
                        posY += e.getY() - mouseY;
                    }
                    mouseX = e.getX();
                    mouseY = e.getY();
                    stateChange = true;
                } else if (mouseRightButton) {
                    mouseX = e.getX();
                    mouseY = e.getY();
                    if (showSeekBar && uiHiddenLevel < 2) {
                        pickPositionX = ((mouseX - posX + zoomLevel / 2) / zoomLevel);
                        pickPositionY = ((mouseY - posY + zoomLevel / 2) / zoomLevel);
                    }
                    stateChange = true;
                }
                container.repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                firstInput = false;
                super.mouseWheelMoved(e);
                int oldZoomLevel = zoomLevel;
                zoomLevel = (e.getWheelRotation() < 0) ? zoomLevel * 2 : zoomLevel / 2;
                if (zoomLevel < 1) {
                    zoomLevel = 1;
                } else if (zoomLevel > 128) {
                    zoomLevel = 128;
                }
                double zoomAlpha = (double)zoomLevel / (double)oldZoomLevel;
                
                double relPosX = posX - mouseX;
                double relPosY = posY - mouseY;
                
                posX = (int)(relPosX * zoomAlpha + mouseX);
                posY = (int)(relPosY * zoomAlpha + mouseY);
                
                stateChange = true;
                container.repaint();
            }
        };
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                stateChange = true;
                container.repaint();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                stateChange = true;
                container.repaint();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                stateChange = true;
                container.repaint();
            }
        });
        container.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                firstInput = false;
                super.componentResized(e);
                int deltaX = e.getComponent().getWidth() / 2 - lastWidth / 2;
                int deltaY = e.getComponent().getHeight() / 2 - lastHeight / 2;
                
                posX += deltaX;
                posY += deltaY;
                
                lastWidth = e.getComponent().getWidth();
                lastHeight = e.getComponent().getHeight();
                
                stateChange = true;
                container.repaint();
            }
        });
        
        
        container.addMouseListener(mouse);
        container.addMouseMotionListener(mouse);
        container.addMouseWheelListener(mouse);
        
        frame.setVisible(true);
        
        stateChange = true;
        firstInput = true;
        container.repaint();
        Thread.sleep(100);
        stateChange = true;
        firstInput = true;
        container.repaint();
    }
    
    
    
    private static void checkPreset(KeyEvent e) {
        int saveSlot = -1;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_1 -> saveSlot = 0;
            case KeyEvent.VK_2 -> saveSlot = 1;
            case KeyEvent.VK_3 -> saveSlot = 2;
            case KeyEvent.VK_4 -> saveSlot = 3;
            case KeyEvent.VK_5 -> saveSlot = 4;
            case KeyEvent.VK_6 -> saveSlot = 5;
            case KeyEvent.VK_7 -> saveSlot = 6;
            case KeyEvent.VK_8 -> saveSlot = 7;
            case KeyEvent.VK_9 -> saveSlot = 8;
            case KeyEvent.VK_0 -> saveSlot = 9;
        }
        if (saveSlot == -1) {
            return;
        }
        
        if (e.isControlDown()) {
            savedPresetsCamera[saveSlot * 3] = posX;
            savedPresetsCamera[saveSlot * 3 + 1] = posY;
            savedPresetsCamera[saveSlot * 3 + 2] = zoomLevel;
        }
        if (e.isShiftDown()) {
            savedPresetsPlace[saveSlot] = placeData;
            savedPresetsState[saveSlot] = placeData.getIndex();
        }
        if (e.isAltDown()) {
            savedPresetsCamera[saveSlot * 3] = -1;
            savedPresetsCamera[saveSlot * 3 + 1] = -1;
            savedPresetsCamera[saveSlot * 3 + 2] = -1;
            savedPresetsState[saveSlot] = Long.MIN_VALUE;
            savedPresetsPlace[saveSlot] = null;
        }
        
        if (savedPresetsCamera[saveSlot * 3] != -1) {
            posX = savedPresetsCamera[saveSlot * 3];
            posY = savedPresetsCamera[saveSlot * 3 + 1];
            zoomLevel = savedPresetsCamera[saveSlot * 3 + 2];
            stateChange = true;
        }
        if (savedPresetsState[saveSlot] != Long.MIN_VALUE) {
            if (savedPresetsPlace[saveSlot] != placeData) {
                PlaceStateImpl temp = placeData;
                placeData = placeDataSwap;
                placeDataSwap = temp;
            }
            placeData.seekIndex(savedPresetsState[saveSlot]);
            stateChange = true;
        }
    }
    
    private static void jumpCameraTo(Container container, int x, int y) {
        posX = (-x * zoomLevel + container.getWidth() / 2);
        posY = (-y * zoomLevel + container.getHeight() / 2);
        
        stateChange = true;
    }
    
    private static void resetCamera(Container container) {
        posX = ((container.getWidth() - placeData.getWidth()) / 2);
        posY = ((container.getHeight() - placeData.getHeight()) / 2);
        zoomLevel = 1;
    }
    
    private static JFrame initWindow(String title, BufferedImage canvas) {
        ImageIcon img = new ImageIcon("data/icon.png");
        JFrame frame = new JFrame(title);
        frame.setIconImage(img.getImage());
        
        Color backgroundColor = new Color(39, 39, 39);
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                //super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                
                if (stateChange || firstInput) {
                    //super.paintComponent(g);
                    g2.setColor(backgroundColor);
                    g2.fillRect(0, 0, this.getWidth(), this.getHeight());
                    g2.drawImage(placeData.getImage(), posX - zoomLevel / 2, posY - zoomLevel / 2, placeData.getWidth() * zoomLevel, placeData.getHeight() * zoomLevel, frame);
                    stateChange = false;
                }
                
                Color textColor = Color.YELLOW;
                
                if (pickPositionX != Integer.MIN_VALUE && pickPositionY != Integer.MIN_VALUE && uiHiddenLevel == 0) {

                    int crossPosX = pickPositionX * zoomLevel + posX;
                    int crossPosY = pickPositionY * zoomLevel + posY;
                    int sizeLevel = zoomLevel > 16 ? zoomLevel : 16;

                    g2.setColor(Color.BLACK);
                    g2.fillRect(crossPosX - 2, crossPosY - sizeLevel/2, 4, sizeLevel);
                    g2.fillRect(crossPosX - sizeLevel/2, crossPosY - 2, sizeLevel, 4);
                    g2.setColor(textColor);
                    g2.fillRect(crossPosX - 1, crossPosY - sizeLevel/2 + 1, 2, sizeLevel - 2);
                    g2.fillRect(crossPosX - sizeLevel/2 + 1, crossPosY - 1, sizeLevel - 2, 2);
                }
                int textHeight = 20;
                if (uiHiddenLevel == 1) {
                    g2.setColor(Color.BLACK);
                    if (showHelp) {
                        g2.fillRect(0, 0, 490, 30);
                    } else {
                        g2.fillRect(0, 0, 346, 30);
                    }
                    g2.setColor(textColor);

                    g.setFont(boldFont); 
                    dateObject.setTime(placeData.getTime() <= 0 ? placeData.getStartTime() : placeData.getTime());
                    g2.drawString(dateFormat.format(dateObject), 10, textHeight); textHeight+=30;
                } else if (uiHiddenLevel == 0) {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(0, 0, 490, 30 * 13);
                    g2.setColor(textColor);

                    g.setFont(boldFont); 
                    dateObject.setTime(placeData.getTime() <= 0 ? placeData.getStartTime() : placeData.getTime());
                    g2.drawString(dateFormat.format(dateObject), 10, textHeight); textHeight+=30;

                    g.setFont(thinFont);
                    g2.drawString("Unix timestamp:", 10, 50);
                    g2.drawString("Iteration:", 10, 80);
                    g2.drawString("Time step:", 10, 110);
                    g2.drawString("Seek multiplier:", 10, 140);
                    g2.drawString("Zoom factor:", 10, 170);
                    g2.drawString("Camera position:", 10, 200);
                    g2.drawString("Cursor position:", 10, 230);
                    g2.drawString("Cursor color:", 10, 260);

                    g2.drawString("Modified position:", 10, 320);
                    g2.drawString("Modified color:", 10, 350);
                    //g2.drawString("Modified color:", 5, 305);

                    g.setFont(thinFont); 
                    
                    g2.drawString("" + (placeData.getTime() <= 0 ? placeData.getStartTime() : placeData.getTime()), 250, textHeight); textHeight+=30;
                    g2.drawString("" + placeData.getUserIndex() + "/" + (placeData.getSize() - placeData.getStartIndex()), 250, textHeight); textHeight+=30;
                    g2.drawString("" + timeDeltasName[chosenTimeDelta], 250, textHeight); textHeight+=30;

                    g2.drawString("x" + multiplier, 250, textHeight); textHeight+=30;

                    int relCamPosX = ((this.getWidth() / 2 - posX + zoomLevel / 2) / zoomLevel);
                    int relCamPosY = ((this.getHeight() / 2 - posY + zoomLevel / 2) / zoomLevel);
                    g2.drawString("x" + zoomLevel, 250, textHeight); textHeight+=30;
                    g2.drawString("" + relCamPosX + " " + relCamPosY, 250, textHeight); textHeight+=30;


                    int relMousePosX = ((mouseX - posX + zoomLevel / 2) / zoomLevel);
                    int relMousePosY = ((mouseY - posY + zoomLevel / 2) / zoomLevel);
                    if (pickPositionX != Integer.MIN_VALUE && pickPositionY != Integer.MIN_VALUE) {
                        g2.drawString("" + pickPositionX + " " + pickPositionY, 250, textHeight);
                    }
                    
                    textHeight+=30;
                    textHeight+=30;
                    if (pickPositionX != Integer.MIN_VALUE && pickPositionY != Integer.MIN_VALUE) {
                        if (placeData.isValid(pickPositionX, pickPositionY)) {
                            Color pickedColor = new Color(placeData.colorToRGB(placeData.getColor(pickPositionX, pickPositionY)));
                            g2.drawString("" + "#"+Integer.toHexString(pickedColor.getRGB()).substring(2).toUpperCase(), 250, textHeight);
                            g2.setColor(pickedColor);
                            g2.fillRect(251, textHeight - 20 - 24, 85, 20);
                            g2.setColor(textColor);
                            g2.drawRect(251, textHeight - 20 - 24, 85, 20);
                        }
                    }
                    /*
                    textHeight+=30;
                    if (placeData.isValid(relMousePosX, relMousePosY)) {
                        Color mouseColor = new Color(placeData.colorToRGB(placeData.getColor(relMousePosX, relMousePosY)));
                        g2.drawString("" + "#"+Integer.toHexString(mouseColor.getRGB()).substring(2).toUpperCase(), 250, textHeight);
                        g2.setColor(mouseColor);
                        g2.fillRect(251, textHeight - 20 - 24, 85, 20);
                        g2.setColor(textColor);
                        g2.drawRect(251, textHeight - 20 - 24, 85, 20);
                    }*/
                    textHeight+=30;
                    
                    
                    if (placeData.isValid()) {
                        g2.drawString("" + placeData.getX() + " " + placeData.getY(), 250, textHeight);
                        Color oldColor = new Color(placeData.colorToRGB((byte)(placeData.getColor() ^ placeData.getColorXOR())));
                        Color newColor = new Color(placeData.colorToRGB(placeData.getColor()));
                        g2.drawString("" + "#"+Integer.toHexString(oldColor.getRGB()).substring(2).toUpperCase() + " -> " + "#"+Integer.toHexString(newColor.getRGB()).substring(2).toUpperCase(), 250, textHeight + 30);
                        g2.drawString("" + "#"+Integer.toHexString(oldColor.getRGB()).substring(2).toUpperCase() + " -> " + "#"+Integer.toHexString(newColor.getRGB()).substring(2).toUpperCase(), 250, textHeight + 60);
                        g2.setColor(oldColor);
                        g2.fillRect(251, textHeight - 20 - 24 + 60, 85, 20);
                        g2.setColor(textColor);
                        g2.drawRect(251, textHeight - 20 - 24 + 60, 85, 20);
                        g2.setColor(newColor);
                        g2.fillRect(381, textHeight - 20 - 24 + 60, 85, 20);
                        g2.setColor(textColor);
                        g2.drawRect(381, textHeight - 20 - 24 + 60, 85, 20);
                    }
                    textHeight+=90;
                }
                
                if (showHelp && uiHiddenLevel != 2) {
                    g.setFont(slantedFont); 
                    g2.setColor(Color.BLACK);
                    if (uiHiddenLevel == 1) {
                        g2.fillRect(0, textHeight - 20, 490, 780);
                    } else {
                        g2.fillRect(0, textHeight - 20, 490, 780);
                    }
                    g2.setColor(textColor);
                    g2.drawString("Controls", 10, textHeight); textHeight+=30;
                    g2.drawString("T: toggle between 2017/2022 canvas", 10, textHeight); textHeight+=30;
                    g2.drawString("F1: toggle help menu", 10, textHeight); textHeight+=30;
                    g2.drawString("F2: toggle hud", 10, textHeight); textHeight+=30;
                    g2.drawString("F3: toggle seek bar", 10, textHeight); textHeight+=30;
                    g2.drawString("F11: toggle fullscreen", 10, textHeight); textHeight+=30;
                     textHeight+=30;
                    g2.drawString("Mouse drag: move canvas", 10, textHeight); textHeight+=30;
                    g2.drawString("Right click: set cursor", 10, textHeight); textHeight+=30;
                    g2.drawString("Scroll wheel: zoom", 10, textHeight); textHeight+=30;
                     textHeight+=30;
                    g2.drawString("Shift/Ctrl: x10/x100 multiplier", 10, textHeight); textHeight+=30;
                    g2.drawString("Q/E: backward/forward one iteration", 10, textHeight); textHeight+=30;
                    g2.drawString("A/D: time seek", 10, textHeight); textHeight+=30;
                    g2.drawString("Arrow keys: move canvas", 10, textHeight); textHeight+=30;
                    g2.drawString("F: set cursor to camera", 10, textHeight); textHeight+=30;
                    g2.drawString("Alt+F: remove cursor", 10, textHeight); textHeight+=30;
                    g2.drawString("Z/X or PgUp/PgDown: zoom in/out", 10, textHeight); textHeight+=30;
                     textHeight+=30;
                    g2.drawString("R: reset canvas", 10, textHeight); textHeight+=30;
                    g2.drawString("C: reset camera", 10, textHeight); textHeight+=30;
                    g2.drawString("V: jump to cursor", 10, textHeight); textHeight+=30;
                    g2.drawString("0-9: load preset", 10, textHeight); textHeight+=30;
                    g2.drawString("Ctrl+0-9: save camera preset", 10, textHeight); textHeight+=30;
                    g2.drawString("Shift+0-9: save time preset", 10, textHeight); textHeight+=30;
                    g2.drawString("Alt+0-9: delete preset", 10, textHeight); textHeight+=30;
                }
                
                int seekBarWidth = this.getWidth() - this.getWidth() / 8;
                int seekBarXStart = this.getWidth() / 16;
                int seekBarHeight = 20;
                int seekBarYStart = this.getHeight() - 50;
                if (seekBarWidth > 50 && showSeekBar && uiHiddenLevel < 2) {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(seekBarXStart - 10, seekBarYStart - 10, seekBarWidth + 20, seekBarHeight + 20);
                    
                    g2.setColor(Color.RED.darker().darker().darker());
                    g2.fillRect(seekBarXStart, seekBarYStart, seekBarWidth, seekBarHeight);
                    
                    g2.setColor(Color.RED.darker());
                    for (int i=0; i<placeData.getSnapshotLength(); i++) {
                        if (placeData.getSnapshotTime(i) > 0) {
                            double percent = (double)(placeData.getSnapshotTime(i) - placeData.getUserTime()) / (placeData.getEndTime() - placeData.getUserTime());
                            if (percent < 0) {
                                percent = 0;
                            }
                            int posX = (int)(percent * seekBarWidth) + seekBarXStart;

                            g2.fillRect(posX - 1, seekBarYStart, 3, seekBarHeight);
                        }
                    }
                    
                    g2.setColor(textColor);
                    g2.drawRect(seekBarXStart, seekBarYStart, seekBarWidth, seekBarHeight);
                    
                    double timePercent = (double)(placeData.getTime() - placeData.getUserTime()) / (placeData.getEndTime() - placeData.getUserTime());
                    if (timePercent < 0) timePercent = 0;
                    int posX = (int)(timePercent * seekBarWidth) + seekBarXStart;
                    
                    g2.fillRect(posX - 4, seekBarYStart - 5, 7, seekBarHeight + 10);
                }
                
            }
        };
        panel.setBackground(backgroundColor);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension dimension = TOOLKIT.getScreenSize();
        frame.setSize((int)(dimension.width * 0.9), (int)(dimension.height * 0.9));
        int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
        frame.setLocation(x, y);
        frame.setContentPane(panel);
        frame.setVisible(true);
        frame.setFocusable(true);
        frame.toFront();
        frame.requestFocus();
        
        return frame;
    }
}
