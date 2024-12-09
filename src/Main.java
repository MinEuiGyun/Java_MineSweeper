import javax.swing.UIManager;
import java.awt.Color;

public class Main {
    public static void main(String[] args) {
        // Set UIManager properties to ensure background color is not overridden
        UIManager.put("Button.background", Color.LIGHT_GRAY);
        UIManager.put("Button.opaque", true);

        MinesweeperGame game = new MinesweeperGame();
        game.start();
    }
}
