import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MinesweeperBot {

    Robot myRobot;
    Rectangle screen;
//    final Color BLACK = new Color(0, 0, 0);
//    final Color GRAY = new Color(128, 128, 128);
//    final Color SILVER = new Color(192, 192, 192);
//    final Color WHITE = new Color(255, 255, 255);
//    final Color MAROON = new Color(128, 0, 0);
//    final Color RED = new Color(255, 0, 0);
//    final Color OLIVE = new Color(128, 128, 0);
//    final Color YELLOW = new Color(255, 255, 0);
//    final Color GREEN = new Color(0, 128, 0);
//    final Color LIME = new Color(0, 255, 0);
//    final Color TEAL = new Color(0, 128, 128);
//    final Color AQUA = new Color(0, 255, 255);
//    final Color NAVY = new Color(0, 0, 128);
//    final Color BLUE = new Color(0, 0, 255);
//    final Color PURPLE = new Color(128, 0, 128);
//    final Color FUCHSIA = new Color(255, 0, 255);

    public MinesweeperBot() {
        try {
            myRobot = new Robot();
        } catch (AWTException e) {
            //if the platform configuration does not allow low-level input control.  This exception is always thrown when GraphicsEnvironment.isHeadless() returns true
            e.printStackTrace();
        }
        screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }

    /**
     * Algorithm Pseudocode:
     * scan screen
     *
     */

    public void start(){
        BufferedImage current = myRobot.createScreenCapture(screen);
    }

    public void stuff() throws InterruptedException {
        BufferedImage current = myRobot.createScreenCapture(screen);
        BufferedImage icon = null;
        try {
            icon = ImageIO.read(getClass().getResource("chromeIcon.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Dimension locale = compareScans(current, icon); //open chrome
        myRobot.mouseMove(locale.width, locale.height);
        myRobot.mousePress(InputEvent.BUTTON1_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_MASK);
        myRobot.mousePress(InputEvent.BUTTON1_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_MASK);

        Thread.sleep(500);
        myRobot.keyPress(KeyEvent.VK_CONTROL);  //getting tabs
        myRobot.keyPress(KeyEvent.VK_SHIFT);
        myRobot.keyPress(KeyEvent.VK_T);
        myRobot.keyRelease(KeyEvent.VK_CONTROL);
        myRobot.keyRelease(KeyEvent.VK_SHIFT);
        myRobot.keyRelease(KeyEvent.VK_T);

        Thread.sleep(500);
        myRobot.keyPress(KeyEvent.VK_WINDOWS);  //minimizing
        myRobot.keyPress(KeyEvent.VK_DOWN);
        myRobot.keyRelease(KeyEvent.VK_WINDOWS);
        myRobot.keyRelease(KeyEvent.VK_DOWN);
        myRobot.keyPress(KeyEvent.VK_WINDOWS);  //done twice to ensure minimized if maximized initially
        myRobot.keyPress(KeyEvent.VK_DOWN);
        myRobot.keyRelease(KeyEvent.VK_WINDOWS);
        myRobot.keyRelease(KeyEvent.VK_DOWN);

        myRobot.keyPress(KeyEvent.VK_ALT);  //exiting window
        myRobot.keyPress(KeyEvent.VK_F4);
        myRobot.keyRelease(KeyEvent.VK_ALT);
        myRobot.keyRelease(KeyEvent.VK_F4);

        try {
            icon = ImageIO.read(getClass().getResource("chromeIcon_noArrow.png"));  //getResource needed cuz jar file
        } catch (IOException e) {
            e.printStackTrace();
        }
        current = myRobot.createScreenCapture(screen);
        locale = compareScans(current, icon);
        myRobot.mouseMove(locale.width, locale.height); //pressing the minimized chrome window with tabs
        myRobot.mousePress(InputEvent.BUTTON1_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_MASK);

        Thread.sleep(200);
        myRobot.keyPress(KeyEvent.VK_WINDOWS);  //maximizing
        myRobot.keyPress(KeyEvent.VK_UP);
        myRobot.keyRelease(KeyEvent.VK_WINDOWS);
        myRobot.keyRelease(KeyEvent.VK_UP);
    }

    /*
    scans the screen looking for a set of pixel colors that matches the icon. Uses the rgb space to do this.
    iconX/iconY -- coordinates for the icon
    x/y -- coordinates for the screen capture
        @return
            the dimensions of the 1st pixel (0,0) of the icon on the current screen
            NULL if nothing matches
     */
    public Dimension compareScans(BufferedImage current, BufferedImage icon){
        Color tempColor;
        Color tempIconColor;
        double tempDist;
        boolean matches;
        for(int x = 0; x < current.getWidth(); x++){
            for(int y = 0; y < current.getHeight(); y++){
                matches = true;
                for(int iconX = 0; iconX < icon.getWidth(); iconX++){
                    for(int iconY = 0; iconY < icon.getHeight(); iconY++){
                        if(x+iconX < current.getWidth() && y+iconY < current.getHeight()) {  //making sure not out of bounds
                            tempColor = new Color(current.getRGB(x + iconX, y + iconY));
                            tempIconColor = new Color(icon.getRGB(iconX, iconY));
                            tempDist = Math.sqrt(Math.pow(tempColor.getRed() - tempIconColor.getRed(), 2) + Math.pow(tempColor.getBlue() - tempIconColor.getBlue(), 2) + Math.pow(tempColor.getGreen() - tempIconColor.getGreen(), 2));
//                           String hexColoricon = String.format("#%06X", (0xFFFFFF & tempIconColor));
//                           String hexColor = String.format("#%06X", (0xFFFFFF & tempColor));
                            if (tempDist > 50) { //checking if they match at this pixel within given margin of error
                                matches = false;
                                break;
                            } else {
//                               myRobot.mouseMove(x + iconX, y + iconY);
                            }
                        }
                        else{    //not within bounds
                            matches = false;
                            break;
                        }
                    }
                    if(!matches){   //have to break out of both icon dimension loops
                        break;
                    }
                }
                if(matches){
                    return new Dimension(x, y);
                }
            }
        }
        return null;
    }

    public void moveMouse(int x, int y) {
        myRobot.mouseMove(x, y);
    }

    public static void main(String[] cheese) throws InterruptedException {
    }
}
