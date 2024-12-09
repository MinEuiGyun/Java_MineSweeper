import javax.swing.JButton;
import java.awt.Color;
import java.util.Map;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.BorderFactory;
import java.awt.Font;

public class Cell extends JButton {
    private static final long serialVersionUID = 1L;
    private final int row;
    private final int col;
    private boolean isMine;
    private boolean isFlagged;
    private boolean isRevealed;
    private int adjacentMines;
    private static final Color REVEALED_COLOR = new Color(211, 211, 211); // 밝은 회색 (revealed)
    private static final Map<Integer, Color> NUMBER_COLORS = GameResources.NUMBER_COLORS;
    private static final Color UNREVEALED_COLOR = Color.WHITE; // 흰색 (unrevealed)
    private static final Color MINE_COLOR = Color.RED; // 폭탄 배경색
    private Color currentColor = UNREVEALED_COLOR; // 초기 색상을 흰색으로 변경
    private static final Font EMOJI_FONT = new Font("Noto Color Emoji", Font.PLAIN, 16);
    private static final Font NUMBER_FONT = new Font("맑은 고딕", Font.BOLD, 14);
    private static final Color HOVER_COLOR = new Color(230, 230, 230);

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.isMine = false;
        this.isFlagged = false;
        this.isRevealed = false;
        this.adjacentMines = 0;
        
        setMargin(new Insets(0, 0, 0, 0));
        setFocusPainted(false);
        setBorderPainted(true);
        setBorder(BorderFactory.createRaisedBevelBorder());
        setBackground(UNREVEALED_COLOR); // 초기 배경색을 흰색으로 설정
        
        int size = 30; // Ensure cells are square
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setFont(EMOJI_FONT); // Use predefined emoji font

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!isRevealed && !isFlagged) {
                    setBackground(HOVER_COLOR);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!isRevealed && !isFlagged) {
                    setBackground(currentColor);
                }
            }
        });
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setMine(boolean isMine) {
        this.isMine = isMine;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public void setFlagged(boolean flagged) {
        isFlagged = flagged;
        setText(isFlagged ? "🚩" : "");
    }

    //_______toggleFlag_______
    // 셀의 깃발 상태를 전환
    // 깃발이 없으면 설치하고, 있으면 제거
    public void toggleFlag() {
        if (!isRevealed) {
            isFlagged = !isFlagged;
            setText(isFlagged ? "🚩" : "");
            setFont(EMOJI_FONT);
        }
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    public void setRevealed(boolean revealed) {
        isRevealed = revealed;
        if (isRevealed) {
            setBackground(isMine ? MINE_COLOR : REVEALED_COLOR); // 지뢰가 아닌 경우 모두 회색으로
            if (isMine) {
                setFont(EMOJI_FONT);
                setText("💣");
            } else if (adjacentMines > 0) {
                setFont(NUMBER_FONT);
                setText(String.valueOf(adjacentMines));
                setForeground(NUMBER_COLORS.get(adjacentMines)); // 숫자별 다른 색상
            }
            setBorder(BorderFactory.createLineBorder(REVEALED_COLOR, 1)); // 테두리도 회색으로 설정
            setBorderPainted(false); // 테두리 제거
        }
    }

    //_______reveal_______
    // 셀의 내용을 화면에 표시
    // 지뢰인 경우 빨간 배경과 폭탄 이모지, 아닌 경우 주변 지뢰 개수 표시
    public void reveal(Color customColor) {
        if (!isRevealed && !isFlagged) {
            isRevealed = true;
            if (isMine) {
                setBackground(MINE_COLOR); // 지뢰만 빨간색
            } else {
                setBackground(REVEALED_COLOR); // 숫자와 빈 칸 모두 회색으로 통일
            }
            
            if (isMine) {
                setFont(EMOJI_FONT);
                setText("💣");
                setForeground(Color.BLACK);
            } else {
                setFont(NUMBER_FONT);
                if (adjacentMines > 0) {
                    setText(String.valueOf(adjacentMines));
                    setForeground(NUMBER_COLORS.getOrDefault(adjacentMines, Color.BLACK));
                } else {
                    setText("");
                }
            }
            setBorder(BorderFactory.createLineBorder(REVEALED_COLOR, 1)); // 테두리도 회색으로 설정
            setOpaque(true);
        }
    }

    public int getAdjacentMines() {
        return adjacentMines;
    }

    public void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }

    //_______setCellColor_______
    // 셀의 배경색을 변경
    // 셀이 공개되지 않은 상태에서만 색상 변경 가능
    public void setCellColor(Color color) {
        if (!isRevealed) {
            currentColor = color;
            setBackground(currentColor);
        }
    }

    // For game over state, reveal mine without red background
    public void revealMine() {
        if (isMine && !isFlagged) {
            setBackground(MINE_COLOR); // 게임 오버시 모든 지뢰를 빨간색으로 표시
            setText("💣");
            setForeground(Color.BLACK);
        }
    }

    // For wrong flag indicator
    public void markWrongFlag() {
        if (isFlagged && !isMine) {
            setText("❌");
            setForeground(Color.RED);
        }
    }
}
