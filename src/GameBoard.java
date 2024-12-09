import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class GameBoard extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Font EMOJI_FONT = new Font("Noto Color Emoji", Font.PLAIN, 16);
    private static final Font NUMBER_FONT = new Font("맑은 고딕", Font.BOLD, 14);
    private final int rows;
    private final int cols;
    private final Cell[][] cells;
    private final Runnable winCallback;
    private final Runnable gameOverCallback;
    private boolean gameOver;
    private boolean firstClick = true;
    private Color cellColor = Color.LIGHT_GRAY; // 기본 셀 색상
    // NUMBER_COLORS 필드 제거 - Cell 클래스에서 직접 GameResources.NUMBER_COLORS 사용

    //_______게임보드_생성자_______
    //게임보드의 크기, 지뢰 수, 승패 콜백을 설정
    //화면 크기에 맞춰 셀 크기를 자동 조절
    public GameBoard(int rows, int cols, int mines, Runnable winCallback, Runnable gameOverCallback) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new Cell[rows][cols];
        this.winCallback = winCallback;
        this.gameOverCallback = gameOverCallback;
        this.gameOver = false;

        // Use maximum dimension to ensure square board
        int maxDim = Math.max(rows, cols);
        setLayout(new GridLayout(maxDim, maxDim, 1, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        setBackground(Color.GRAY); // Change background color to gray

        // Use fixed cell size instead of calculating from screen resolution
        int cellSize = 30;

        // Set the board size to be square
        int boardSize = cellSize * maxDim;
        setPreferredSize(new Dimension(boardSize, boardSize));

        initializeCells(maxDim, cellSize);
        placeMines(mines);
        calculateAdjacentMines();
    }

    //_______셀_초기화_______
    //지정된 크기로 셀을 생성하고 초기화
    //각 셀에 대한 이벤트 리스너 설정
    private void initializeCells(int maxDim, int cellSize) {
        for (int row = 0; row < maxDim; row++) {
            for (int col = 0; col < maxDim; col++) {
                Cell cell;
                if (row < rows && col < cols) {
                    cell = new Cell(row, col);
                    cells[row][col] = cell;
                } else {
                    // Create dummy cells for padding
                    cell = new Cell(row, col);
                    cell.setEnabled(false);
                    cell.setBackground(Color.DARK_GRAY);
                }
                
                // Set cell size and style
                cell.setPreferredSize(new Dimension(cellSize, cellSize));
                cell.setMinimumSize(new Dimension(cellSize, cellSize));
                cell.setMaximumSize(new Dimension(cellSize, cellSize));
                cell.setFont(NUMBER_FONT); // Ensure font supports text
                
                if (row < rows && col < cols) {
                    cell.addActionListener(e -> handleCellClick((Cell)e.getSource()));
                    cell.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                handleRightClick((Cell)e.getSource());
                            }
                        }
                    });
                }
                
                add(cell);
            }
        }
    }

    //_______우클릭_처리_______
    //셀에 깃발을 설정하거나 해제
    //게임 종료 상태에서는 동작하지 않음
    private void handleRightClick(Cell cell) {
        if (gameOver) return; 
        cell.toggleFlag();
    }

    //_______CELL_CLICK_HANDLING_______
    // Processes left and right mouse clicks
    // Implements game logic for cell revelation
    private void handleCellClick(Cell cell) {
        if (gameOver || cell.isFlagged()) return; // 게임 종료 또는 플래그된 셀은 무시

        if (firstClick) {
            firstClick = false;
            // 첫 클릭이 지뢰인 경우 지뢰를 다른 위치로 이동
            if (cell.isMine()) {
                relocateMine(cell);
            }
            // 첫 클릭 주변의 지뢰 배치도 조정하여 50-50 상황 방지
            optimizeMineLayout(cell);
        }

        if (cell.isMine()) {
            cell.reveal(cellColor); // This will show red background for the clicked mine
            revealAllMines();
            gameOver = true;
            gameOverCallback.run();
        } else {
            cell.reveal(cellColor);
            if (cell.getAdjacentMines() == 0) {
                revealAdjacentCells(cell);
            }
            checkWinCondition();
        }
    }

    private void relocateMine(Cell cell) {
        cell.setMine(false);
        placeMines(1);
        calculateAdjacentMines(); // 인접 지뢰 수 재계산
    }

    private void optimizeMineLayout(Cell cell) {
        int row = cell.getRow();
        int col = cell.getCol();
        
        // 첫 클릭 주변 3x3 영역에서 지뢰 제거
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                int nRow = row + dRow;
                int nCol = col + dCol;
                if (isValidCell(nRow, nCol) && cells[nRow][nCol].isMine()) {
                    relocateMine(cells[nRow][nCol]);
                }
            }
        }
        
        // 50-50 상황 감지 및 수정
        checkAndFixFiftyFiftySituations();
    }

    private void checkAndFixFiftyFiftySituations() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (isFiftyFiftySituation(row, col)) {
                    fixFiftyFiftySituation(row, col);
                }
            }
        }
    }

    private boolean isFiftyFiftySituation(int row, int col) {
        // 두 개의 인접한 셀만 남아있고, 그 중 하나가 반드시 지뢰인 상황 감지
        if (!isValidCell(row, col) || cells[row][col].isMine()) {
            return false;
        }
        
        int unknownCells = 0;
        int mineCount = 0;
        
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                if (dRow == 0 && dCol == 0) continue;
                int nRow = row + dRow;
                int nCol = col + dCol;
                if (isValidCell(nRow, nCol)) {
                    if (!cells[nRow][nCol].isRevealed()) {
                        unknownCells++;
                    }
                    if (cells[nRow][nCol].isMine()) {
                        mineCount++;
                    }
                }
            }
        }
        
        return unknownCells == 2 && mineCount == 1;
    }

    private void fixFiftyFiftySituation(int row, int col) {
        // 50-50 상황을 해결하기 위해 지뢰 재배치
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                int nRow = row + dRow;
                int nCol = col + dCol;
                if (isValidCell(nRow, nCol) && cells[nRow][nCol].isMine()) {
                    relocateMine(cells[nRow][nCol]);
                    return;
                }
            }
        }
    }

    private boolean isValidCell(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private void revealAdjacentCells(Cell cell) {
        int row = cell.getRow();
        int col = cell.getCol();
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                int nRow = row + dRow;
                int nCol = col + dCol;
                if (nRow >= 0 && nRow < cells.length &&
                    nCol >= 0 && nCol < cells[0].length) {
                    Cell adjacentCell = cells[nRow][nCol];
                    if (!adjacentCell.isRevealed() && !adjacentCell.isFlagged()) {
                        adjacentCell.reveal(GameResources.EMPTY_SPACE_COLOR);
                        if (adjacentCell.getAdjacentMines() == 0) {
                            revealAdjacentCells(adjacentCell);
                        }
                    }
                }
            }
        }
    }

    private void revealAllMines() {
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                if (cell.isMine()) {
                    cell.setFont(EMOJI_FONT);
                    cell.setText("💣");
                    cell.setBackground(Color.RED);
                    cell.setBorderPainted(true);
                    cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                } else if (cell.isFlagged()) {
                    cell.setFont(EMOJI_FONT);
                    cell.setText("❌");
                    cell.setForeground(Color.RED);
                    cell.setBorderPainted(true);
                    cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                }
            }
        }
    }

    private void checkWinCondition() {
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                if (!cell.isMine() && !cell.isRevealed()) return;
            }
        }
        gameOver = true;
        winCallback.run();
    }

    //_______MINE_PLACEMENT_______
    // Randomly distributes mines on the board
    // Ensures first click safety
    private void placeMines(int mines) {
        int placed = 0;
        while (placed < mines) {
            int row = (int) (Math.random() * cells.length);
            int col = (int) (Math.random() * cells[0].length);

            if (!cells[row][col].isMine()) {
                cells[row][col].setMine(true);
                placed++;
            }
        }
    }

    private void calculateAdjacentMines() {
        for (int row = 0; row < cells.length; row++) {
            for (int col = 0; col < cells[0].length; col++) {
                if (!cells[row][col].isMine()) {
                    int count = countAdjacentMines(row, col);
                    cells[row][col].setAdjacentMines(count);
                }
            }
        }
    }

    private int countAdjacentMines(int row, int col) {
        int count = 0;
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                int nRow = row + dRow;
                int nCol = col + dCol;
                if (nRow >= 0 && nRow < cells.length && 
                    nCol >= 0 && nCol < cells[0].length &&
                    cells[nRow][nCol].isMine()) {
                    count++;
                }
            }
        }
        return count;
    }

    public void setCellColor(Color color) {
        this.cellColor = color;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (!cells[row][col].isRevealed()) {
                    cells[row][col].setCellColor(color);
                }
            }
        }
    }

    public void setFirstClick(boolean value) {
        this.firstClick = value;
    }
    
    public boolean isFirstClick() {
        return firstClick;
    }

    public void setGameOver(boolean value) {
        this.gameOver = value;
    }

    // Save the current board state to a writer
    public void saveBoardState(BufferedWriter writer) throws IOException {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Cell cell = cells[row][col];
                writer.write(cell.isMine() + " ");
                writer.write(cell.isFlagged() + " ");
                writer.write(cell.isRevealed() + " ");
                writer.write(cell.getAdjacentMines() + "\n");
            }
        }
    }

    // Load the board state from a reader
    public void loadBoardState(BufferedReader reader) throws IOException {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Cell cell = cells[row][col];
                String[] cellInfo = reader.readLine().split(" ");
                boolean isMine = Boolean.parseBoolean(cellInfo[0]);
                boolean isFlagged = Boolean.parseBoolean(cellInfo[1]);
                boolean isRevealed = Boolean.parseBoolean(cellInfo[2]);
                int adjacentMines = Integer.parseInt(cellInfo[3]);
                
                cell.setMine(isMine);
                cell.setAdjacentMines(adjacentMines);
                if (isFlagged) {
                    cell.setFlagged(true);
                }
                if (isRevealed) {
                    cell.reveal(cellColor);
                } else {
                    cell.setCellColor(cellColor);
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                cell.setEnabled(enabled);
            }
        }
    }
}