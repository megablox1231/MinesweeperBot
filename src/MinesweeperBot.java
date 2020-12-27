

import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyAdapter;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.Buffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MinesweeperBot {

    private final int LENGTH = 30;
    private final int HEIGHT = 16;
    private final boolean autoRestart = true;   //true if we want to auto-restart game every time we fail

    private int cellDist;
    private boolean trailing;   //when true, cellTrail can continue
    private boolean noZero;     //when true, cellTrail can't move to empty cells
    private boolean prevAction; //when true, cell(s) were revealed on the last cellTrail loop
    private Robot myRobot;
    private Rectangle screen;
    private Dimension originDim;
    private Dimension faceDim;
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
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[0].length; col++) {
                grid[row][col] = new Cell();
            }
        }
        trailing = true;
        noZero = false;
        prevAction = true;
    }

    //TODO: !isRevealed can include unscanned 0 cells
    public class Cell {
        //-1 means not revealed yet
        public int number = -1;
        public boolean isRevealed = false; //we know the contents of this cell (false = unopened and not flagged)
        public boolean isFlagged = false;
        public boolean visited = false;
        public int neighbors = 0;

        @Override
        public String toString() {
            return "Cell{" +
                    "number=" + number +
                    ", isRevealed=" + isRevealed +
                    ", isFlagged=" + isFlagged +
                    ", visited=" + visited +
                    ", neighbors=" + neighbors +
                    '}';
        }
    }

    private void initImages() {
        BufferedImage neighbor0 = null;
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
            neighbor0 = ImageIO.read(getClass().getResource("neighbor0.png"));
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
                neighbor0,
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

    //TODO: add diagonals, search for straggling islands (could also start on one), guessing,
    // don't start over unless totally out of options

    public void start() {
        current = myRobot.createScreenCapture(screen);
        originDim = compareScans(current, unopened);
        faceDim = compareScans(current, smile);
        myRobot.mouseMove(originDim.width, originDim.height);

        //presses out of ide window TODO: Remove after program finished
        myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        Dimension tempDim = compareScans(current, originDim.width, originDim.height + 1, unopened);
        myRobot.mouseMove(tempDim.width, tempDim.height);

        cellDist = tempDim.height - originDim.height;

        do {
            //TODO: find a way to have it so you don't restart on first go :/
            restart();
            myRobot.mouseMove(originDim.width, originDim.height);

            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            current = myRobot.createScreenCapture(screen);

            applyCellStat(0, 0);
            //we haven't won yet
            while (prevAction) {
                prevAction = false;
                cellTrail(0, 0); //pass -1, -1 if no previous cell
                current = myRobot.createScreenCapture(screen);
                trailing = true;
                noZero = false;
                resetVisited();
            }

            prevAction = true;
            for (int row = 0; row < grid.length; row++) {
                for (int col = 0; col < grid[0].length; col++) {
                    grid[row][col] = new Cell();
                }
            }
        } while (!winCheck() && autoRestart);
    }

    private void cellTrail(int row, int col) {
        //checking if out of bounds, previously visited or not trailing
        if (row >= HEIGHT || row < 0 || col >= LENGTH || col < 0 || !trailing || grid[row][col].visited) {
            return;
        }

//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        myRobot.mouseMove(originDim.width + (cellDist * col), originDim.height + (cellDist * row));

        grid[row][col].visited = true;

        if (!grid[row][col].isRevealed) {
            applyCellStat(col, row);
        }
        if (grid[row][col].number == -1) {
            return;
        }
        if (grid[row][col].number == 0) {
            if (noZero) {
                return;
            }
            cellTrail(row - 1, col);
            cellTrail(row, col - 1);
            cellTrail(row, col + 1);
            cellTrail(row + 1, col);
            return;
        }

        int unopened = 0;
        int flags = 0;
        //identify cells around grid[row][col] and count unopened and flagged ones
        for (int r = row - 1; r <= row + 1; r++) {
            if (r >= HEIGHT || r < 0) {
                continue;
            }
            for (int c = col - 1; c <= col + 1; c++) {
                if ((c == col && r == row) || c >= LENGTH || c < 0) {
                    continue;
                }
                if (!grid[r][c].isRevealed) {
                    applyCellStat(c, r);
                }
                if (grid[r][c].number == -1) {
                    if (grid[r][c].isFlagged) {
                        flags++;
                    } else {
                        unopened++;
                    }
                }
            }
        }

        if (unopened == (grid[row][col].number - flags) && unopened > 0) {
            //flag em (iterate over all 8 cells again or keep track of unopened cell locales with new array)
            for (int r = row - 1; r <= row + 1; r++) {
                if (r >= HEIGHT || r < 0) {
                    continue;
                }
                for (int c = col - 1; c <= col + 1; c++) {
                    if ((c == col && r == row) || c >= LENGTH || c < 0) {
                        continue;
                    }
                    if (!grid[r][c].isRevealed) {
                        //flag here
                        System.out.println("we flagging bruh");
                        myRobot.mouseMove(originDim.width + (cellDist * c), originDim.height + (cellDist * r));
                        myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                        myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                        grid[r][c].isFlagged = true;
                        grid[r][c].isRevealed = true;
                        System.out.println("Row: " + r + " Col: " + c);
                        flags++;
                        unopened--;
                        prevAction = true;
                    }
                }
            }
        }
        if (flags == grid[row][col].number && unopened > 0) {
            //chain
            myRobot.mouseMove(originDim.width + (cellDist * col), originDim.height + (cellDist * row));
            myRobot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
            myRobot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
            prevAction = true;
            //return all the way back up stack because cells were opened
            trailing = false;
            return;
        } else if (grid[row][col].number - flags == 1) {
            int effNum = 0;
            //scan cross
            //cell above
            if (row - 1 > 0) {
                effNum = calcEffNum(row - 1, col);
                if(effNum == 1 && (row == HEIGHT - 1 || ((col == 0 || grid[row+1][col-1].isRevealed) && grid[row+1][col].isRevealed && (col == LENGTH - 1 || grid[row+1][col+1].isRevealed)))){
                    if (col > 0) {
                        applyCellStat(col-1, row-2);
                        if(!grid[row-2][col-1].isRevealed) {
                            // opening top left cell
                            System.out.println(row + " and " + col);
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 1)), originDim.height + (cellDist * (row - 2)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            trailing = false;
                            return;
                        }
                    }
                    if (col < LENGTH - 1){
                        applyCellStat(col+1, row-2);
                        if(!grid[row-2][col+1].isRevealed) {
                            //opening top right cell
                            System.out.println(row + " and " + col);
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 1)), originDim.height + (cellDist * (row - 2)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            trailing = false;
                            return;
                        }
                    }
                }
                else if(effNum == 2){
                    if(col > 0){
                        applyCellStat(col-1, row-2);
                        if (!grid[row-2][col-1].isRevealed && grid[row-2][col].isRevealed && (col == LENGTH - 1 || grid[row-2][col+1].isRevealed)) {
                            //flagging top left cell
                            System.out.println(row + " and " + col + " up top left");
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 1)), originDim.height + (cellDist * (row - 2)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row-2][col-1].isRevealed = true;
                            grid[row-2][col-1].isFlagged = true;
                            prevAction = true;
                        }
                    }
                    if(col < LENGTH - 1){
                        applyCellStat(col+1, row-2);
                        if(!grid[row-2][col+1].isRevealed && grid[row-2][col].isRevealed && (col == 0 || grid[row-2][col-1].isRevealed)) {
                            //flagging top right cell
                            System.out.println(row + " and " + col + " up top right");
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 1)), originDim.height + (cellDist * (row - 2)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row-2][col+1].isRevealed = true;
                            grid[row-2][col+1].isFlagged = true;
                            prevAction = true;
                        }
                    }
                }
            }

            //cell below
            if(row + 2 < HEIGHT){
                effNum = calcEffNum(row+1, col);
                if(effNum == 1 && (row == 0 || ((col == 0 || grid[row-1][col-1].isRevealed) && grid[row-1][col].isRevealed && (col == LENGTH - 1 || grid[row-1][col+1].isRevealed)))){
                    if (col > 0) {
                        applyCellStat(col-1, row+2);
                        if(!grid[row+2][col-1].isRevealed) {
                            // opening bottom left cell
                            System.out.println(row + " and " + col);
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 1)), originDim.height + (cellDist * (row + 2)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            trailing = false;
                            return;
                        }
                    }
                    if (col < LENGTH - 1){
                        applyCellStat(col+1, row+2);
                        if(!grid[row+2][col+1].isRevealed) {
                            //opening bottom right cell
                            System.out.println(row + " and " + col);
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 1)), originDim.height + (cellDist * (row + 2)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            trailing = false;
                            return;
                        }
                    }
                }
                else if(effNum == 2){
                    if(col > 0){
                        applyCellStat(col-1, row+2);
                        if(!grid[row+2][col-1].isRevealed && grid[row+2][col].isRevealed && (col == LENGTH - 1 || grid[row+2][col+1].isRevealed)) {
                            //flagging bottom left cell
                            System.out.println(row + " and " + col + " down bottom left");
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 1)), originDim.height + (cellDist * (row + 2)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row+2][col-1].isRevealed = true;
                            grid[row+2][col-1].isFlagged = true;
                            prevAction = true;
                        }
                    }
                    if(col < LENGTH - 1){
                        applyCellStat(col+1, row+2);
                        if(!grid[row+2][col+1].isRevealed && grid[row+2][col].isRevealed && (col == 0 || grid[row+2][col-1].isRevealed)) {
                            //flagging bottom right cell
                            System.out.println(row + " and " + col + " down bottom right");
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 1)), originDim.height + (cellDist * (row + 2)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row+2][col+1].isRevealed = true;
                            grid[row+2][col+1].isFlagged = true;
                            prevAction = true;
                        }
                    }
                }
            }

            //left cell
            if(col - 1 > 0) {
                effNum = calcEffNum(row, col-1);
                if(effNum == 1 && (col == LENGTH - 1 || ((row == 0 || grid[row-1][col+1].isRevealed) && grid[row][col+1].isRevealed && (row == HEIGHT - 1 || grid[row+1][col+1].isRevealed)))){
                    if (row > 0) {
                        applyCellStat(col-2, row-1);
                        if(!grid[row-1][col-2].isRevealed) {
                            // opening top left cell
                            System.out.println(row + " and " + col);
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 2)), originDim.height + (cellDist * (row - 1)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            trailing = false;
                            return;
                        }
                    }
                    if (row < HEIGHT - 1){
                        applyCellStat(col-2, row+1);
                        if(!grid[row+1][col-2].isRevealed) {
                            //opening bottom left cell
                            System.out.println(row + " and " + col);
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 2)), originDim.height + (cellDist * (row + 1)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            trailing = false;
                            return;
                        }
                    }
                }
                else if(effNum == 2){
                    if(row > 0){
                        applyCellStat(col-2, row-1);
                        if(!grid[row-1][col-2].isRevealed && grid[row][col-2].isRevealed && (row == HEIGHT - 1 || grid[row+1][col-2].isRevealed)) {
                            //flagging top left cell
                            System.out.println(row + " and " + col + " left top left");
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 2)), originDim.height + (cellDist * (row - 1)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row-1][col-2].isRevealed = true;
                            grid[row-1][col-2].isFlagged = true;
                            prevAction = true;
                        }
                    }
                    if(row < HEIGHT - 1){
                        applyCellStat(col-2, row+1);
                        if(!grid[row+1][col-2].isRevealed && grid[row][col-2].isRevealed && (row == 0 || grid[row-1][col-2].isRevealed)) {
                            //flagging bottom left cell
                            System.out.println(row + " and " + col + "left bottom left");
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 2)), originDim.height + (cellDist * (row + 1)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row+1][col-2].isRevealed = true;
                            grid[row+1][col-2].isFlagged = true;
                            prevAction = true;
                        }
                    }
                }
            }

            //right cell
            if(col + 2 < LENGTH){
                effNum = calcEffNum(row, col+1);
                if(effNum == 1 && (col == 0 || ((row == 0 || grid[row-1][col-1].isRevealed) && grid[row][col-1].isRevealed && (row == HEIGHT - 1 || grid[row+1][col-1].isRevealed)))){
                    if (row > 0) {
                        applyCellStat(col+2, row-1);
                        if(!grid[row-1][col+2].isRevealed) {
                            // opening top right cell
                            System.out.println(row + " and " + col);
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 2)), originDim.height + (cellDist * (row - 1)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            trailing = false;
                            return;
                        }
                    }
                    if (row < HEIGHT - 1){
                        applyCellStat(col+2, row+1);
                        if(!grid[row+1][col+2].isRevealed) {
                            //opening bottom right cell
                            System.out.println(row + " and " + col);
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 2)), originDim.height + (cellDist * (row + 1)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            trailing = false;
                            return;
                        }
                    }
                }
                else if(effNum == 2){
                    if(row > 0){
                        applyCellStat(col+2, row-1);
                        if(!grid[row-1][col+2].isRevealed && grid[row][col+2].isRevealed && (row == HEIGHT - 1 || grid[row+1][col+2].isRevealed)) {
                            //flagging top right cell
                            System.out.println(row + " and " + col + " right top right");
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 2)), originDim.height + (cellDist * (row - 1)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row-1][col+2].isRevealed = true;
                            grid[row-1][col+2].isFlagged = true;
                            prevAction = true;
                        }
                    }
                    if(row < HEIGHT - 1){
                        applyCellStat(col+2, row+1);
                        if(!grid[row+1][col+2].isRevealed && grid[row][col+2].isRevealed && (row == 0 || grid[row-1][col+2].isRevealed)) {
                            //flagging bottom right cell
                            System.out.println(row + " and " + col + " right bottom right");
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 2)), originDim.height + (cellDist * (row + 1)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row+1][col+2].isRevealed = true;
                            grid[row+1][col+2].isFlagged = true;
                            prevAction = true;
                        }
                    }
                }
            }
        }

        noZero = true;

        cellTrail(row - 1, col);
        cellTrail(row, col - 1);
        cellTrail(row, col + 1);
        cellTrail(row + 1, col);
    }

    private void applyCellStat(int col, int row) {
        Dimension dim = new Dimension(originDim.width + (col * cellDist), originDim.height + (row * cellDist));
        for (int i = 0; i < stateImages.length; i++) {
            if (compareScans(current, dim.width, dim.height, dim.width + cellDist, dim.height + cellDist, stateImages[i]) != null) {
                grid[row][col].number = i;
                grid[row][col].isRevealed = true;
                return;
            }
        }
    }

    //returns the effective number of a cell
    private int calcEffNum(int row, int col) {
        int flags = 0;
        for (int r = row - 1; r <= row + 1; r++) {
            if (r >= HEIGHT || r < 0) {
                continue;
            }
            for (int c = col - 1; c <= col + 1; c++) {
                if ((c == col && r == row) || c >= LENGTH || c < 0) {
                    continue;
                }
                if (grid[r][c].isFlagged) {
                    flags++;
                }
            }
        }
        return grid[row][col].number - flags;
    }

    private void resetVisited() {
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < LENGTH; c++) {
                grid[r][c].visited = false;
            }
        }
    }

    //TODO: maybe find where face is first time then start there always afterwards
    //Returns true if we get a game over
    private boolean failCheck(){
        return compareScans(current, faceDim.width, faceDim.height, faceDim.width+frown.getWidth(), faceDim.height+frown.getHeight(), frown) != null;
    }

    //Returns true if we won
    private boolean winCheck(){
        return compareScans(current, faceDim.width, faceDim.height, faceDim.width+sunglasses.getWidth(), faceDim.height+sunglasses.getHeight(), sunglasses) != null;
    }

    private void restart(){
        //+10 because button doesn't register click on edge
        myRobot.mouseMove(faceDim.width + 10, faceDim.height + 10);
        myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    /*
    scans the screen looking for a set of pixel colors that matches the icon. Uses the rgb space to do this.
    iconX/iconY -- coordinates for the icon
    x/y -- coordinates for the screen capture
        @return
            the dimensions of the 1st pixel (x,y) of the icon on the current screen
            NULL if nothing matches
     */
    public Dimension compareScans(BufferedImage current, BufferedImage icon) {
        Color tempColor;
        Color tempIconColor;
        double tempDist;
        boolean matches;
        for (int x = 0; x < current.getWidth(); x++) {
            for (int y = 0; y < current.getHeight(); y++) {
                matches = true;
                for (int iconX = 0; iconX < icon.getWidth(); iconX++) {
                    for (int iconY = 0; iconY < icon.getHeight(); iconY++) {
                        if (x + iconX < current.getWidth() && y + iconY < current.getHeight()) {  //making sure not out of bounds
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
                        } else {    //not within bounds
                            matches = false;
                            break;
                        }
                    }
                    if (!matches) {   //have to break out of both icon dimension loops
                        break;
                    }
                }
                if (matches) {
                    return new Dimension(x, y);
                }
            }
        }
        return null;
    }

    //Overloading method for starting at dimension (x, y) of current Buffered Image
    public Dimension compareScans(BufferedImage current, int x, int y, BufferedImage icon) {
        Color tempColor;
        Color tempIconColor;
        double tempDist;
        boolean matches;
        for (; x < current.getWidth(); x++) {
            for (; y < current.getHeight(); y++) {
                matches = true;
                for (int iconX = 0; iconX < icon.getWidth(); iconX++) {
                    for (int iconY = 0; iconY < icon.getHeight(); iconY++) {
                        if (x + iconX < current.getWidth() && y + iconY < current.getHeight()) {  //making sure not out of bounds
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
                        } else {    //not within bounds
                            matches = false;
                            break;
                        }
                    }
                    if (!matches) {   //have to break out of both icon dimension loops
                        break;
                    }
                }
                if (matches) {
                    return new Dimension(x, y);
                }
            }
        }
        return null;
    }

    //Overloading method for starting at dimension (x, y) and ending at dimension (endX, endY) of current Buffered Image
    public Dimension compareScans(BufferedImage current, int x, int y, int endX, int endY, BufferedImage icon) {
        Color tempColor;
        Color tempIconColor;
        double tempDist;
        boolean matches;
        for (; x < endX; x++) {
            for (; y < endY; y++) {
                matches = true;
                for (int iconX = 0; iconX < icon.getWidth(); iconX++) {
                    for (int iconY = 0; iconY < icon.getHeight(); iconY++) {
                        if (x + iconX < endX && y + iconY < endY) {  //making sure not out of bounds
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
                        } else {    //not within bounds
                            matches = false;
                            break;
                        }
                    }
                    if (!matches) {   //have to break out of both icon dimension loops
                        break;
                    }
                }
                if (matches) {
                    return new Dimension(x, y);
                }
            }
        }
        return null;
    }

    public static void main(String[] cheese) throws InterruptedException {
        GlobalKeyboardHook keyboardHook = new GlobalKeyboardHook(true);
        for (Map.Entry<Long, String> keyboard : GlobalKeyboardHook.listKeyboards().entrySet()) {
            System.out.format("%d: %s\n", keyboard.getKey(), keyboard.getValue());
        }
        keyboardHook.addKeyListener(new GlobalKeyAdapter() {

            @Override
            public void keyPressed(GlobalKeyEvent event) {
                if (event.getVirtualKeyCode() == GlobalKeyEvent.VK_ESCAPE) {
                    keyboardHook.shutdownHook();
                    System.exit(2);
                }
            }

            @Override
            public void keyReleased(GlobalKeyEvent event) {
                ;
            }
        });

        MinesweeperBot bot = new MinesweeperBot();
        bot.start();
    }
}