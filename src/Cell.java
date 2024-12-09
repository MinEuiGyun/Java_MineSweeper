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
    private static final Color REVEALED_COLOR = new Color(211, 211, 211);
    private static final Map<Integer, Color> NUMBER_COLORS = GameResources.NUMBER_COLORS;
    private static final Color UNREVEALED_COLOR = Color.WHITE; 
    private static final Color MINE_COLOR = Color.RED; 
    private Color currentColor = UNREVEALED_COLOR; 
    private static final Font EMOJI_FONT = new Font("Noto Color Emoji", Font.PLAIN, 16);
    private static final Font NUMBER_FONT = new Font("맑은 고딕", Font.BOLD, 14);
    private static final Color HOVER_COLOR = new Color(230, 230, 230);

    // Cell constructor
    // Initializes the cell with default properties and sets up the UI
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
        setBackground(UNREVEALED_COLOR); 
        
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

    // getRow method
    // Returns the row index of the cell
    public int getRow() {
        return row;
    }

    // getCol method
    // Returns the column index of the cell
    public int getCol() {
        return col;
    }

    // isMine method
    // Checks if the cell contains a mine
    public boolean isMine() {
        return isMine;
    }

    // setMine method
    // Sets the mine status of the cell
    public void setMine(boolean isMine) {
        this.isMine = isMine;
    }

    // isFlagged method
    // Checks if the cell is flagged
    public boolean isFlagged() {
        return isFlagged;
    }

    // setFlagged method
    // Flags or unflags the cell and updates the UI
    public void setFlagged(boolean flagged) {
        isFlagged = flagged;
        setText(isFlagged ? "🚩" : "");
    }

    // toggleFlag method
    // Toggles the flagged status of the cell and updates the UI
    public void toggleFlag() {
        if (!isRevealed) {
            isFlagged = !isFlagged;
            setText(isFlagged ? "🚩" : "");
            setFont(EMOJI_FONT);
        }
    }

    // isRevealed method
    // Checks if the cell is revealed
    public boolean isRevealed() {
        return isRevealed;
    }

    // setRevealed method
    // Reveals the cell and updates the UI based on its content
    public void setRevealed(boolean revealed) {
        isRevealed = revealed;
        if (isRevealed) {
            setBackground(isMine ? MINE_COLOR : REVEALED_COLOR); 
            if (isMine) {
                setFont(EMOJI_FONT);
                setText("💣");
            } else if (adjacentMines > 0) {
                setFont(NUMBER_FONT);
                setText(String.valueOf(adjacentMines));
                setForeground(NUMBER_COLORS.get(adjacentMines)); 
            }
            setBorder(BorderFactory.createLineBorder(REVEALED_COLOR, 1)); 
            setBorderPainted(false); 
        }
    }

    // reveal method
    // Reveals the cell with a custom color and updates the UI based on its content
    public void reveal(Color customColor) {
        if (!isRevealed && !isFlagged) {
            isRevealed = true;
            if (isMine) {
                setBackground(MINE_COLOR); 
            } else {
                setBackground(REVEALED_COLOR); 
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
            setBorder(BorderFactory.createLineBorder(REVEALED_COLOR, 1)); 
            setOpaque(true);
        }
    }

    // getAdjacentMines method
    // Returns the number of adjacent mines
    public int getAdjacentMines() {
        return adjacentMines;
    }

    // setAdjacentMines method
    // Sets the number of adjacent mines
    public void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }

    // setCellColor method
    // Sets the background color of the cell if it is not revealed
    public void setCellColor(Color color) {
        if (!isRevealed) {
            currentColor = color;
            setBackground(currentColor);
        }
    }

    // revealMine method
    // Reveals the mine in the cell if it is not flagged
    public void revealMine() {
        if (isMine && !isFlagged) {
            setBackground(MINE_COLOR); 
            setText("💣");
            setForeground(Color.BLACK);
        }
    }

    // markWrongFlag method
    // Marks the cell with a wrong flag indication if it is flagged but not a mine
    public void markWrongFlag() {
        if (isFlagged && !isMine) {
            setText("❌");
            setForeground(Color.RED);
        }
    }
}
