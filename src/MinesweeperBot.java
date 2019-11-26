import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.Buffer;

public class MinesweeperBot {

    private final int LENGTH = 30;
    private final int HEIGHT = 16;
    
    private Robot myRobot;
    private Rectangle screen;
    private int cellDist;
    private Dimension originDim;
    private Cell[][] grid;
    private BufferedImage[] stateImages;
    private BufferedImage[] faceImages;
    private BufferedImage current;
    private BufferedImage flag;
    private BufferedImage question;
    private BufferedImage smile;
    private BufferedImage sunglasses;
    private BufferedImage frown;
    private BufferedImage unopened;
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
        initImages();
        grid = new Cell[HEIGHT][LENGTH];
    }
    
    public class Cell{
        public int number = -1;
        public boolean isRevealed = false;
        public boolean isFlagged = false;
        public boolean analyzed = false;
        public int neighbors = 0;
    }
    
    private void initImages() {
        BufferedImage neighbor1 = null;
        BufferedImage neighbor2 = null;
        BufferedImage neighbor3 = null;
        BufferedImage neighbor4 = null;
        BufferedImage neighbor5 = null;
        BufferedImage neighbor6 = null;
        BufferedImage neighbor7 = null;
        BufferedImage neighbor8 = null;
        try {
            flag = ImageIO.read(getClass().getResource("flag.png"));
            frown = ImageIO.read(getClass().getResource("frown.png"));
            neighbor1 = ImageIO.read(getClass().getResource("neighbor1.png"));
            neighbor2 = ImageIO.read(getClass().getResource("neighbor2.png"));
            neighbor3 = ImageIO.read(getClass().getResource("neighbor3.png"));
            neighbor4 = ImageIO.read(getClass().getResource("neighbor4.png"));
            neighbor5 = ImageIO.read(getClass().getResource("neighbor5.png"));
            neighbor6 = ImageIO.read(getClass().getResource("neighbor6.png"));
            neighbor7 = ImageIO.read(getClass().getResource("neighbor7.png"));
            neighbor8 = ImageIO.read(getClass().getResource("neighbor8.png"));
            question = ImageIO.read(getClass().getResource("question.png"));
            smile = ImageIO.read(getClass().getResource("smile.png"));
            sunglasses = ImageIO.read(getClass().getResource("sunglasses.png"));
            unopened = ImageIO.read(getClass().getResource("unopened.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        stateImages = new BufferedImage[]{
                neighbor1,
                neighbor2,
                neighbor3,
                neighbor4,
                neighbor5,
                neighbor6,
                neighbor7,
                neighbor8
        };
    }

    public void start(){
        current = myRobot.createScreenCapture(screen);
        originDim = compareScans(current, unopened);
        myRobot.mouseMove(originDim.width, originDim.height);

        //presses out of ide window TODO: Remove after program finished
        myRobot.mousePress(InputEvent.BUTTON1_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_MASK);

        Dimension tempDim = compareScans(current, originDim.width, originDim.height+1, unopened);
        myRobot.mouseMove(tempDim.width, tempDim.height);

        cellDist = tempDim.height - originDim.height;

        myRobot.mousePress(InputEvent.BUTTON1_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_MASK);

        current = myRobot.createScreenCapture(screen);

        //TODO: add checking if lost everytime a cell is clicked
        
        applyCellStat(originDim, 0, 0);

        cellTrail(0, 0, -1, -1); //pass -1, -1 if no previous cell
    }

    private void cellTrail(int row, int col, int prevRow, int prevCol) {
        if (row >= HEIGHT || row < 0 || col >= LENGTH || col < 0) {
            return;
        }
        if (grid[row][col].number == -1 && !grid[row][col].isFlagged) {
            applyCellStat(new Dimension(originDim.width + (col * cellDist), originDim.height + (row * cellDist)), col, row);
        }
        if (grid[row][col].number == -1) {
            return;
        }
        if (grid[row][col].number == 0) {
            if (prevRow != row - 1 && prevCol != col) {
                cellTrail(row - 1, col, row, col);
            }
            if (prevRow != row && prevCol != col - 1) {
                cellTrail(row, col - 1, row, col);
            }
            if (prevRow != row && prevCol != col + 1) {
                cellTrail(row, col + 1, row, col);
            }
            if (prevRow != row + 1 && prevCol != col) {
                cellTrail(row + 1, col, row, col);
            }
            return;
        }
        
        int unopened = 0;
        int flags = 0;
        for (int r = row - 1; r < row + 1; r++) {
            for (int c = col - 1; c < col + 1; c++) {
                if (grid[r][c].number == -1 && !grid[r][c].isFlagged) {
                    applyCellStat(new Dimension(originDim.width + (c * cellDist), originDim.height + (r * cellDist)), c, r);
                }
                if(grid[r][c].number == -1){
                    if(grid[r][c].isFlagged){
                        flags++;
                    }
                    else{
                        unopened++;
                    }
                }
                else if(grid[r][c].number != 0){
                    if(unopened == grid[r][c].number){
                        //flag em
                    }
                }
            }
        }

        if (grid[row][col].number == 0) {
            if (prevRow != row - 1 && prevCol != col) {
                cellTrail(row - 1, col, row, col);
            }
            if (prevRow != row && prevCol != col - 1) {
                cellTrail(row, col - 1, row, col);
            }
            if (prevRow != row && prevCol != col + 1) {
                cellTrail(row, col + 1, row, col);
            }
            if (prevRow != row + 1 && prevCol != col) {
                cellTrail(row + 1, col, row, col);
            }
            return;
        }
    }

    private void applyCellStat(Dimension dim, int col, int row){
        for(int i = 0; i < stateImages.length; i++){
            if(compareScans(current, dim.width, dim.height, dim.width+cellDist, dim.height+cellDist, stateImages[i]) != null){
                grid[col][row].number = i+1;
                return;
            }
        }
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
    
    //Overriding method for starting at dimension (x, y) of current Buffered Image
    public Dimension compareScans(BufferedImage current, int x, int y, BufferedImage icon){
        Color tempColor;
        Color tempIconColor;
        double tempDist;
        boolean matches;
        for(; x < current.getWidth(); x++){
            for(; y < current.getHeight(); y++){
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

    //Overriding method for starting at dimension (x, y) and ending at dimension (endX, endY) of current Buffered Image
    public Dimension compareScans(BufferedImage current, int x, int y, int endX, int endY, BufferedImage icon){
        Color tempColor;
        Color tempIconColor;
        double tempDist;
        boolean matches;
        for(; x < endX; x++){
            for(; y < endY; y++){
                matches = true;
                for(int iconX = 0; iconX < icon.getWidth(); iconX++){
                    for(int iconY = 0; iconY < icon.getHeight(); iconY++){
                        if(x+iconX < endX && y+iconY < endY) {  //making sure not out of bounds
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
        MinesweeperBot bot = new MinesweeperBot();
        bot.start();
    }
}
