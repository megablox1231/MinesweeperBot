import lc.kra.system.keyboard.GlobalKeyboardHook;
        import lc.kra.system.keyboard.event.GlobalKeyAdapter;
        import lc.kra.system.keyboard.event.GlobalKeyEvent;

        import javax.imageio.ImageIO;
        import java.awt.*;
        import java.awt.event.InputEvent;
        import java.awt.image.BufferedImage;
        import java.awt.image.DataBufferInt;
        import java.io.IOException;
        import java.util.Map;

public class MinesweeperBot {

    private final int LENGTH = 30;
    private final int HEIGHT = 16;
    private final boolean autoRestart = true;   //true if we want to auto-restart game every time we fail

    private int cellDist;
    private int startRow, startCol; //the starting coords for cellTrail; def (0,0)
    private boolean noZero;     //when true, cellTrail can't move to empty cells
    private boolean prevAction; //when true, cell(s) were revealed on the last cellTrail loop
    private boolean stopGuess;  //when true, guess() will stop running
    private Robot myRobot;
    private final Rectangle screen;
    private Dimension originDim;
    private Dimension faceDim;
    private Cell[][] grid;

    private BufferedImage[] stateImages;
    private int[] curPixels;
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
        noZero = false;
        prevAction = true;
        stopGuess = false;
        startRow = startCol = 0;
    }

    private class Cell {
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
        BufferedImage temp;
        Graphics2D graphics;
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
            temp = ImageIO.read(getClass().getResource("frown.png"));
            frown = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = frown.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("smile.png"));
            smile = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = smile.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("sunglasses.png"));
            sunglasses = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = sunglasses.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("unopened.png"));
            unopened = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = unopened.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor0.png"));
            neighbor0 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor0.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor1.png"));
            neighbor1 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor1.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor2.png"));
            neighbor2 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor2.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor3.png"));
            neighbor3 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor3.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor4.png"));
            neighbor4 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor4.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor5.png"));
            neighbor5 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor5.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor6.png"));
            neighbor6 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor6.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor7.png"));
            neighbor7 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor7.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
            temp = ImageIO.read(getClass().getResource("neighbor8.png"));
            neighbor8 = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_RGB);
            graphics = neighbor8.createGraphics();
            graphics.drawImage(temp, 0, 0, null);
            graphics.dispose();
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

    public void start() {
        screenCap();
        originDim = containsImage(curPixels, unopened);
        faceDim = containsImage(curPixels, smile);
        if (originDim == null || faceDim == null) {
            throw new NullPointerException("Minesweeper window obscured");
        }
        myRobot.mouseMove(originDim.width, originDim.height);

        //ensures focus is on minesweeper app
        myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        Dimension tempDim = containsImage(curPixels, originDim.width, originDim.height + 1, unopened);
        if (tempDim == null) {
            throw new NullPointerException("Minesweeper window obscured");
        }
        myRobot.mouseMove(tempDim.width, tempDim.height);

        cellDist = tempDim.height - originDim.height;

        do {
            restart();
            myRobot.mouseMove(originDim.width, originDim.height);

            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            screenCap();

            applyCellStat(0, 0);
            while (!failCheck() && !winCheck()) {
                //we haven't won yet
                while (prevAction) {
                    prevAction = false;
                    cellTrail(startRow, startCol);
                    screenCap();
                    noZero = false;
                    resetVisited();
                }
                if (!winCheck()) {
                    guess(startRow, startCol);
                    if (!prevAction) {
                        //all that's left is to find stranded cells
                        islandSearch();
                        screenCap();
                        prevAction = true;
                    }
                    stopGuess = false;
                    noZero = false;
                    resetVisited();
                }
            }
            prevAction = true;
            for (int row = 0; row < grid.length; row++) {
                for (int col = 0; col < grid[0].length; col++) {
                    grid[row][col] = new Cell();
                }
            }
        } while (!winCheck() && autoRestart);
    }

    //Looks for cell trail and once found, runs down it in all 8 directions.
    //Can flag surrounding cells, chain open, and do pattern flagging and opening (1-1 and 1-2; see wiki)
    private void cellTrail(int row, int col) {
        //checking if out of bounds, previously visited or not trailing
        if (row >= HEIGHT || row < 0 || col >= LENGTH || col < 0 || grid[row][col].visited) {
            return;
        }

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
            cellTrail(row - 1, col - 1);
            cellTrail(row - 1, col);
            cellTrail(row - 1, col + 1);
            cellTrail(row, col - 1);
            cellTrail(row, col + 1);
            cellTrail(row + 1, col - 1);
            cellTrail(row + 1, col);
            cellTrail(row + 1, col + 1);
            return;
        }

        int unopened = 0;   //surrounding cells that are unopened
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
            //flag all surrounding, unrevealed cells
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
                        myRobot.mouseMove(originDim.width + (cellDist * c), originDim.height + (cellDist * r));
                        myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                        myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                        grid[r][c].isFlagged = true;
                        grid[r][c].isRevealed = true;
                        flags++;
                        unopened--;
                        prevAction = true;
                    }
                }
            }
        }
        if (flags == grid[row][col].number && unopened > 0) {
            //chain open surrounding cells
            myRobot.mouseMove(originDim.width + (cellDist * col), originDim.height + (cellDist * row));
            myRobot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
            myRobot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
            prevAction = true;
            screenCap();
        }
        //pattern flagging and opening (1-1 and 1-2)
        else if (grid[row][col].number - flags == 1) {
            int effNum;
            //cell above
            if (row - 1 > 0) {
                effNum = calcEffNum(row - 1, col);
                if (effNum == 1 && (row == HEIGHT - 1 || ((col == 0 || grid[row + 1][col - 1].isRevealed) && grid[row + 1][col].isRevealed && (col == LENGTH - 1 || grid[row + 1][col + 1].isRevealed)))) {
                    if (col > 0) {
                        applyCellStat(col - 1, row - 2);
                        if (!grid[row - 2][col - 1].isRevealed) {
                            // opening top left cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 1)), originDim.height + (cellDist * (row - 2)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            screenCap();
                        }
                    }
                    if (col < LENGTH - 1) {
                        applyCellStat(col + 1, row - 2);
                        if (!grid[row - 2][col + 1].isRevealed) {
                            //opening top right cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 1)), originDim.height + (cellDist * (row - 2)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            screenCap();
                        }
                    }
                } else if (effNum == 2) {
                    if (col > 0) {
                        applyCellStat(col - 1, row - 2);
                        if (!grid[row - 2][col - 1].isRevealed && grid[row - 2][col].isRevealed && (col == LENGTH - 1 || grid[row - 2][col + 1].isRevealed)) {
                            //flagging top left cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 1)), originDim.height + (cellDist * (row - 2)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row - 2][col - 1].isRevealed = true;
                            grid[row - 2][col - 1].isFlagged = true;
                            prevAction = true;
                        }
                    }
                    if (col < LENGTH - 1) {
                        applyCellStat(col + 1, row - 2);
                        if (!grid[row - 2][col + 1].isRevealed && grid[row - 2][col].isRevealed && (col == 0 || grid[row - 2][col - 1].isRevealed)) {
                            //flagging top right cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 1)), originDim.height + (cellDist * (row - 2)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row - 2][col + 1].isRevealed = true;
                            grid[row - 2][col + 1].isFlagged = true;
                            prevAction = true;
                        }
                    }
                }
            }

            //cell below
            if (row + 2 < HEIGHT) {
                effNum = calcEffNum(row + 1, col);
                if (effNum == 1 && (row == 0 || ((col == 0 || grid[row - 1][col - 1].isRevealed) && grid[row - 1][col].isRevealed && (col == LENGTH - 1 || grid[row - 1][col + 1].isRevealed)))) {
                    if (col > 0) {
                        applyCellStat(col - 1, row + 2);
                        if (!grid[row + 2][col - 1].isRevealed) {
                            // opening bottom left cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 1)), originDim.height + (cellDist * (row + 2)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            screenCap();
                        }
                    }
                    if (col < LENGTH - 1) {
                        applyCellStat(col + 1, row + 2);
                        if (!grid[row + 2][col + 1].isRevealed) {
                            //opening bottom right cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 1)), originDim.height + (cellDist * (row + 2)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            screenCap();
                        }
                    }
                } else if (effNum == 2) {
                    if (col > 0) {
                        applyCellStat(col - 1, row + 2);
                        if (!grid[row + 2][col - 1].isRevealed && grid[row + 2][col].isRevealed && (col == LENGTH - 1 || grid[row + 2][col + 1].isRevealed)) {
                            //flagging bottom left cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 1)), originDim.height + (cellDist * (row + 2)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row + 2][col - 1].isRevealed = true;
                            grid[row + 2][col - 1].isFlagged = true;
                            prevAction = true;
                        }
                    }
                    if (col < LENGTH - 1) {
                        applyCellStat(col + 1, row + 2);
                        if (!grid[row + 2][col + 1].isRevealed && grid[row + 2][col].isRevealed && (col == 0 || grid[row + 2][col - 1].isRevealed)) {
                            //flagging bottom right cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 1)), originDim.height + (cellDist * (row + 2)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row + 2][col + 1].isRevealed = true;
                            grid[row + 2][col + 1].isFlagged = true;
                            prevAction = true;
                        }
                    }
                }
            }

            //left cell
            if (col - 1 > 0) {
                effNum = calcEffNum(row, col - 1);
                if (effNum == 1 && (col == LENGTH - 1 || ((row == 0 || grid[row - 1][col + 1].isRevealed) && grid[row][col + 1].isRevealed && (row == HEIGHT - 1 || grid[row + 1][col + 1].isRevealed)))) {
                    if (row > 0) {
                        applyCellStat(col - 2, row - 1);
                        if (!grid[row - 1][col - 2].isRevealed) {
                            // opening top left cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 2)), originDim.height + (cellDist * (row - 1)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            screenCap();
                        }
                    }
                    if (row < HEIGHT - 1) {
                        applyCellStat(col - 2, row + 1);
                        if (!grid[row + 1][col - 2].isRevealed) {
                            //opening bottom left cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 2)), originDim.height + (cellDist * (row + 1)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            screenCap();
                        }
                    }
                } else if (effNum == 2) {
                    if (row > 0) {
                        applyCellStat(col - 2, row - 1);
                        if (!grid[row - 1][col - 2].isRevealed && grid[row][col - 2].isRevealed && (row == HEIGHT - 1 || grid[row + 1][col - 2].isRevealed)) {
                            //flagging top left cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 2)), originDim.height + (cellDist * (row - 1)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row - 1][col - 2].isRevealed = true;
                            grid[row - 1][col - 2].isFlagged = true;
                            prevAction = true;
                        }
                    }
                    if (row < HEIGHT - 1) {
                        applyCellStat(col - 2, row + 1);
                        if (!grid[row + 1][col - 2].isRevealed && grid[row][col - 2].isRevealed && (row == 0 || grid[row - 1][col - 2].isRevealed)) {
                            //flagging bottom left cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col - 2)), originDim.height + (cellDist * (row + 1)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row + 1][col - 2].isRevealed = true;
                            grid[row + 1][col - 2].isFlagged = true;
                            prevAction = true;
                        }
                    }
                }
            }

            //right cell
            if (col + 2 < LENGTH) {
                effNum = calcEffNum(row, col + 1);
                if (effNum == 1 && (col == 0 || ((row == 0 || grid[row - 1][col - 1].isRevealed) && grid[row][col - 1].isRevealed && (row == HEIGHT - 1 || grid[row + 1][col - 1].isRevealed)))) {
                    if (row > 0) {
                        applyCellStat(col + 2, row - 1);
                        if (!grid[row - 1][col + 2].isRevealed) {
                            // opening top right cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 2)), originDim.height + (cellDist * (row - 1)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            screenCap();
                        }
                    }
                    if (row < HEIGHT - 1) {
                        applyCellStat(col + 2, row + 1);
                        if (!grid[row + 1][col + 2].isRevealed) {
                            //opening bottom right cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 2)), originDim.height + (cellDist * (row + 1)));
                            myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            prevAction = true;
                            screenCap();
                        }
                    }
                } else if (effNum == 2) {
                    if (row > 0) {
                        applyCellStat(col + 2, row - 1);
                        if (!grid[row - 1][col + 2].isRevealed && grid[row][col + 2].isRevealed && (row == HEIGHT - 1 || grid[row + 1][col + 2].isRevealed)) {
                            //flagging top right cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 2)), originDim.height + (cellDist * (row - 1)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row - 1][col + 2].isRevealed = true;
                            grid[row - 1][col + 2].isFlagged = true;
                            prevAction = true;
                        }
                    }
                    if (row < HEIGHT - 1) {
                        applyCellStat(col + 2, row + 1);
                        if (!grid[row + 1][col + 2].isRevealed && grid[row][col + 2].isRevealed && (row == 0 || grid[row - 1][col + 2].isRevealed)) {
                            //flagging bottom right cell
                            myRobot.mouseMove(originDim.width + (cellDist * (col + 2)), originDim.height + (cellDist * (row + 1)));
                            myRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            myRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            grid[row + 1][col + 2].isRevealed = true;
                            grid[row + 1][col + 2].isFlagged = true;
                            prevAction = true;
                        }
                    }
                }
            }
        }

        noZero = true;
        cellTrail(row - 1, col - 1);
        cellTrail(row - 1, col);
        cellTrail(row - 1, col + 1);
        cellTrail(row, col - 1);
        cellTrail(row, col + 1);
        cellTrail(row + 1, col - 1);
        cellTrail(row + 1, col);
        cellTrail(row + 1, col + 1);
    }

    //Simple version of cellTrail used to find and click on the first unrevealed cell
    //that is along the cell trail
    private void guess(int row, int col) {
        //checking if out of bounds, previously visited or not trailing
        if (row >= HEIGHT || row < 0 || col >= LENGTH || col < 0 || grid[row][col].visited || stopGuess) {
            return;
        }

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
            guess(row - 1, col - 1);
            guess(row - 1, col);
            guess(row - 1, col + 1);
            guess(row, col - 1);
            guess(row, col + 1);
            guess(row + 1, col - 1);
            guess(row + 1, col);
            guess(row + 1, col + 1);
            return;
        }

        //examine surrounding cells and click on the first unrevealed one
        for (int r = row - 1; r <= row + 1; r++) {
            if (r >= HEIGHT || r < 0) {
                continue;
            }
            for (int c = col - 1; c <= col + 1; c++) {
                if ((c == col && r == row) || c >= LENGTH || c < 0) {
                    continue;
                }
                if (!grid[r][c].isRevealed) {
                    //click unrevealed cell
                    myRobot.mouseMove(originDim.width + (cellDist * (c)), originDim.height + (cellDist * (r)));
                    myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    stopGuess = true;
                    prevAction = true;
                    return;
                }
            }
        }

        noZero = true;
        guess(row - 1, col - 1);
        guess(row - 1, col);
        guess(row - 1, col + 1);
        guess(row, col - 1);
        guess(row, col + 1);
        guess(row + 1, col - 1);
        guess(row + 1, col);
        guess(row + 1, col + 1);
    }

    //looks for and clicks on the first unrevealed cell on the grid
    //used when no unrevealed cells on cell trail
    private void islandSearch() {
        int tempRow = 0, tempCol = 0;
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < LENGTH; c++) {
                if (!grid[r][c].isRevealed) {
                    applyCellStat(c, r);
                    if (grid[r][c].number == -1) {
                        tempRow = r;
                        tempCol = c;
                    } else if (grid[r][c].number != 0) {
                        startRow = r;
                        startCol = c;
                        return;
                    }
                }
            }
        }
        startRow = tempRow;
        startCol = tempCol;
        //click unrevealed cell
        myRobot.mouseMove(originDim.width + (cellDist * (startCol)), originDim.height + (cellDist * (startRow)));
        myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private void applyCellStat(int col, int row) {
        Dimension dim = new Dimension(originDim.width + (col * cellDist), originDim.height + (row * cellDist));
        for (int i = 0; i < stateImages.length; i++) {
            if (compareImages(curPixels, dim.width, dim.height, stateImages[i])) {
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

    //Returns true if we get a game over
    private boolean failCheck() {
        return compareImages(curPixels, faceDim.width, faceDim.height, frown);
    }

    //Returns true if we won
    private boolean winCheck() {
        return compareImages(curPixels, faceDim.width, faceDim.height, sunglasses);
    }

    private void restart() {
        //+10 because button doesn't register click on edge
        myRobot.mouseMove(faceDim.width + 10, faceDim.height + 10);
        myRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        myRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }


    //screen capture (BufferedImage) is converted to data buffer and alpha is removed here to keep performance high
    private void screenCap() {
        BufferedImage temp = myRobot.createScreenCapture(screen);
        curPixels = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

        //alpha removal
        for (int i = 0; i < curPixels.length; i++) {
            curPixels[i] &= 0xFFFFFF;
        }
    }

    /*
    scans data buffer curPixels looking for a set of pixel colors that matches the icon.
    iconX/iconY -- coordinates for the icon
    x/y -- coordinates for the screen capture
        @return
            the dimensions of the 1st pixel (x,y) of the icon on the curPixels screen
            NULL if nothing matches
     */
    private Dimension containsImage(int[] curPixels, BufferedImage icon) {
        final int[] iconPixels = ((DataBufferInt) icon.getRaster().getDataBuffer()).getData();

        int width = screen.width;
        int height = screen.height;
        int iconHeight = icon.getHeight();
        int iconWidth = icon.getWidth();
        boolean matches;


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                matches = true;
                for (int iconY = 0; iconY < iconHeight; iconY++) {
                    for (int iconX = 0; iconX < iconWidth; iconX++) {
                        if (x + iconX >= width || y + iconY >= height || curPixels[(y + iconY) * width + x + iconX] != iconPixels[iconY * iconWidth + iconX]) {
                            matches = false;
                            break;
                        }
                    }
                    if (!matches) { //have to break out of both icon dimension loops
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

    //Overloading method for starting at dimension (x, y) of curPixels data buffer
    private Dimension containsImage(int[] curPixels, int xStart, int yStart, BufferedImage icon) {
        final int[] iconPixels = ((DataBufferInt) icon.getRaster().getDataBuffer()).getData();

        int width = screen.width;
        int height = screen.height;
        int iconHeight = icon.getHeight();
        int iconWidth = icon.getWidth();
        boolean matches;


        for (int y = yStart; y < height; y++) {
            for (int x = xStart; x < width; x++) {
                matches = true;
                for (int iconY = 0; iconY < iconHeight; iconY++) {
                    for (int iconX = 0; iconX < iconWidth; iconX++) {
                        if (x + iconX >= width || y + iconY >= height || curPixels[(y + iconY) * width + x + iconX] != iconPixels[iconY * iconWidth + iconX]) {
                            matches = false;
                            break;
                        }
                    }
                    if (!matches) { //have to break out of both icon dimension loops
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

    //Overloading method for starting at dimension (x, y) and ending at dimension (endX, endY) of curPixels data buffer
    private Dimension containsImage(int[] curPixels, int startX, int startY, int endX, int endY, BufferedImage icon) {
        final int[] iconPixels = ((DataBufferInt) icon.getRaster().getDataBuffer()).getData();

        int width = screen.width;
        int height = screen.height;
        int iconHeight = icon.getHeight();
        int iconWidth = icon.getWidth();
        boolean matches;


        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                matches = true;
                for (int iconY = 0; iconY < iconHeight; iconY++) {
                    for (int iconX = 0; iconX < iconWidth; iconX++) {
                        if (x + iconX >= endX || y + iconY >= endY || curPixels[(y + iconY) * width + x + iconX] != iconPixels[iconY * iconWidth + iconX]) {
                            matches = false;
                            break;
                        }
                    }
                    if (!matches) { //have to break out of both icon dimension loops
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

    //Checks if curPixels, starting at dimension (x, y) is icon
    private boolean compareImages(int[] curPixels, int startX, int startY, BufferedImage icon) {
        final int[] iconPixels = ((DataBufferInt) icon.getRaster().getDataBuffer()).getData();

        int width = screen.width;
        int height = screen.height;
        int iconHeight = icon.getHeight();
        int iconWidth = icon.getWidth();

        if (startY + iconHeight > height || startX + iconWidth > width) {
            return false;
        }

        for (int y = 0; y < iconHeight; y++) {
            for (int x = 0; x < iconWidth; x++) {
                if (curPixels[(startY + y) * width + x + startX] != iconPixels[y * iconWidth + x]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] cheese) {
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
            }
        });
        MinesweeperBot bot = new MinesweeperBot();
        bot.start();
        keyboardHook.shutdownHook();
    }
}