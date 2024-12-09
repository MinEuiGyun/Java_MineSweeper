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
    private Color cellColor = Color.LIGHT_GRAY; 

    // GameBoard 생성자 // 게임 보드 초기화 // 지뢰 배치 및 인접 지뢰 계산
    public GameBoard(int rows, int cols, int mines, Runnable winCallback, Runnable gameOverCallback) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new Cell[rows][cols];
        this.winCallback = winCallback;
        this.gameOverCallback = gameOverCallback;
        this.gameOver = false;

        int maxDim = Math.max(rows, cols);
        setLayout(new GridLayout(maxDim, maxDim, 1, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        setBackground(Color.GRAY); // Change background color to gray

        int cellSize = 30;

        int boardSize = cellSize * maxDim;
        setPreferredSize(new Dimension(boardSize, boardSize));

        initializeCells(maxDim, cellSize);
        placeMines(mines);
        calculateAdjacentMines();
    }

    // initializeCells // 셀 초기화 // 셀 클릭 이벤트 리스너 추가
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

    // handleRightClick // 우클릭 처리 // 깃발 토글
    private void handleRightClick(Cell cell) {
        if (gameOver) return; 
        cell.toggleFlag();
    }

    // handleCellClick // 셀 클릭 처리 // 첫 클릭 시 지뢰 재배치 및 최적화
    private void handleCellClick(Cell cell) {
        if (gameOver || cell.isFlagged()) return; 

        if (firstClick) {
            firstClick = false;
            if (cell.isMine()) {
                relocateMine(cell);
            }
            optimizeMineLayout(cell);
        }

        if (cell.isMine()) {
            cell.reveal(cellColor); 
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

    // relocateMine // 지뢰 재배치 // 인접 지뢰 수 재계산
    private void relocateMine(Cell cell) {
        cell.setMine(false);
        placeMines(1);
        calculateAdjacentMines(); 
    }

    // optimizeMineLayout // 지뢰 배치 최적화 // 50-50 상황 해결
    private void optimizeMineLayout(Cell cell) {
        int row = cell.getRow();
        int col = cell.getCol();
        
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                int nRow = row + dRow;
                int nCol = col + dCol;
                if (isValidCell(nRow, nCol) && cells[nRow][nCol].isMine()) {
                    relocateMine(cells[nRow][nCol]);
                }
            }
        }
        
        checkAndFixFiftyFiftySituations();
    }

    // checkAndFixFiftyFiftySituations // 50-50 상황 확인 및 해결 // 지뢰 재배치
    private void checkAndFixFiftyFiftySituations() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (isFiftyFiftySituation(row, col)) {
                    fixFiftyFiftySituation(row, col);
                }
            }
        }
    }

    // isFiftyFiftySituation // 50-50 상황인지 확인 // 인접 셀의 지뢰 및 미확인 셀 수 계산
    private boolean isFiftyFiftySituation(int row, int col) {
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

    // fixFiftyFiftySituation // 50-50 상황 해결 // 인접 지뢰 재배치
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

    // isValidCell // 유효한 셀인지 확인 // 행과 열 범위 내인지 확인
    private boolean isValidCell(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    // revealAdjacentCells // 인접 셀 공개 // 인접 지뢰가 없는 경우 재귀적으로 공개
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

    // revealAllMines // 모든 지뢰 공개 // 게임 오버 시 호출
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

    // checkWinCondition // 승리 조건 확인 // 모든 지뢰 외 셀이 공개되었는지 확인
    private void checkWinCondition() {
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                if (!cell.isMine() && !cell.isRevealed()) return;
            }
        }
        gameOver = true;
        winCallback.run();
    }

    // placeMines // 지뢰 배치 // 무작위 위치에 지뢰 배치
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

    // calculateAdjacentMines // 인접 지뢰 수 계산 // 각 셀의 인접 지뢰 수 설정
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

    // countAdjacentMines // 인접 지뢰 수 세기 // 인접 셀의 지뢰 수 계산
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

    // setCellColor // 셀 색상 설정 // 공개되지 않은 셀의 색상 변경
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

    // setFirstClick // 첫 클릭 여부 설정 // 첫 클릭 여부 변경
    public void setFirstClick(boolean value) {
        this.firstClick = value;
    }

    // isFirstClick // 첫 클릭 여부 확인 // 첫 클릭 여부 반환
    public boolean isFirstClick() {
        return firstClick;
    }

    // setGameOver // 게임 오버 여부 설정 // 게임 오버 여부 변경
    public void setGameOver(boolean value) {
        this.gameOver = value;
    }

    // saveBoardState // 보드 상태 저장 // 셀 상태를 파일에 저장
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

    // loadBoardState // 보드 상태 로드 // 파일에서 셀 상태를 불러옴
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
    // setEnabled // 보드 활성화/비활성화 // 모든 셀의 활성화/비활성화 설정
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                cell.setEnabled(enabled);
            }
        }
    }
}