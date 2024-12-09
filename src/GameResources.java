import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameResources {
    private GameResources() {}

    public static final String RESOURCES_PATH = "./resources/";
    public static final String USER_DATA_FILE = RESOURCES_PATH + "user_mileage_data.txt";
    public static final String GAME_RESULTS_FILE = RESOURCES_PATH + "game_results.txt";
    public static final String BACKGROUND_MUSIC_FILE = RESOURCES_PATH + "background_music.wav";
    public static final String LOGO_FILE = RESOURCES_PATH + "mine.jpg";

    public static final Color EMPTY_SPACE_COLOR = new Color(211, 211, 211); // Light Gray

    public static final Map<String, Color> CELL_COLORS = new LinkedHashMap<>();
    static {
        CELL_COLORS.put("기본", new Color(220, 220, 220));
        CELL_COLORS.put("하늘색", new Color(176, 226, 255));
        CELL_COLORS.put("연보라", new Color(230, 230, 250));
        CELL_COLORS.put("민트", new Color(200, 255, 200));
        CELL_COLORS.put("파스텔 핑크", new Color(255, 200, 200));
    }

    public static final Map<Integer, Color> NUMBER_COLORS = new LinkedHashMap<>();
    static {
        NUMBER_COLORS.put(1, new Color(25, 118, 210));
        NUMBER_COLORS.put(2, new Color(56, 142, 60));
        NUMBER_COLORS.put(3, new Color(211, 47, 47));
        NUMBER_COLORS.put(4, new Color(123, 31, 162));
        NUMBER_COLORS.put(5, new Color(255, 143, 0));
        NUMBER_COLORS.put(6, new Color(0, 151, 167));
        NUMBER_COLORS.put(7, new Color(66, 66, 66));
        NUMBER_COLORS.put(8, new Color(38, 50, 56));
    }
}