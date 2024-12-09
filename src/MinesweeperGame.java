import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.io.File;
import java.util.Map;

public class MinesweeperGame {
    private static final Logger LOGGER = Logger.getLogger(MinesweeperGame.class.getName());
    private LoginManager loginManager;
    private AudioPlayer audioPlayer;
    private CustomerMileageManager mileageManager;
    private String playerName;
    private String difficulty;
    private int rows;
    private int cols;
    private int mines;
    private JLabel timerLabel;
    private final AtomicInteger elapsedTime;
    private Timer timer;
    private JFrame frame;
    private final ExecutorService executorService;
    private GameBoard gameBoard;
    private static final Map<String, Color> AVAILABLE_COLORS = GameResources.CELL_COLORS;
    private static final int COLOR_PRICE = 100;
    private boolean firstClick;
    private Color cellColor = Color.LIGHT_GRAY;
    private int winStreak = 0;
    private JPanel mainPanel;
    private JPanel gameBoardPanel;
    private boolean gameOver;  // Add this field
    private static final Font DEFAULT_FONT = new Font("맑은 고딕", Font.PLAIN, 14);
    private static final Font EMOJI_FONT = new Font("Noto Color Emoji", Font.PLAIN, 16);
    private static final Color BUTTON_COLOR = new Color(63, 81, 181);
    private static final Color HOVER_COLOR = new Color(92, 107, 192);
    private JPanel infoPanel;
    private JButton smileButton;

    public MinesweeperGame() {
        executorService = Executors.newSingleThreadExecutor();
        elapsedTime = new AtomicInteger(0);
        loginManager = new LoginManager();
        audioPlayer = new AudioPlayer();
        mileageManager = loginManager.getMileageManager();
        gameOver = false;  // Initialize gameOver
    }

    public void start() {
        try {
            // Look and Feel 설정
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            frame = new JFrame("지뢰찾기");
            frame.setBackground(new Color(240, 240, 240));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Ensure the application exits on close
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    cleanup();
                    System.exit(0);
                }
            });

            if (!showLoginDialog()) {
                return;
            }

            // 사용자 이름 입력
            playerName = JOptionPane.showInputDialog(frame, "이름을 입력하세요:", "사용자 이름", JOptionPane.QUESTION_MESSAGE);
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "플레이어";
            }

            // 난이도 선택
            String[] options = {"쉬움", "보통", "어려움"};
            difficulty = (String) JOptionPane.showInputDialog(frame,
                    "난이도 선택:",
                    "난이도 선택",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (difficulty == null) difficulty = "쉬움";

            // 난이도 설정
            switch (difficulty) {
                case "쉬움":
                    rows = 8; cols = 8; mines = 10;
                    break;
                case "보통":
                    rows = 16; cols = 16; mines = 40;
                    break;
                case "어려움":
                    rows = 16; cols = 30; mines = 99;
                    break;
            }

            // Ensure the game board is square
            if (rows != cols) {
                cols = rows;
            }

            // UI 구성 순서 변경
            mainPanel = new JPanel(new BorderLayout());
            
            // 상단 정보 패널
            initializeInfoPanel();

            // 게임 보드 초기화
            gameBoard = new GameBoard(rows, cols, mines, this::onWin, this::onGameOver);
            gameBoard.setCellColor(cellColor);
            
            // 게임 보드 패널 설정
            gameBoardPanel = new JPanel(new BorderLayout());
            gameBoardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(Color.GRAY, 1)
            ));
            gameBoardPanel.add(gameBoard);
            mainPanel.add(gameBoardPanel, BorderLayout.CENTER);

            // 하단 버튼 패널 초기화
            initializeButtonPanel();

            // 프레임에 메인 패널 추가
            frame.add(mainPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            playBackgroundMusic();
            startTimer(); // 게임 시작과 함께 타이머 시작
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start game", e);
            showError("게임 시작 실패", "게임을 시작할 수 없습니다: " + e.getMessage());
        }
    }

    private void initializeInfoPanel() {
        infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(new Color(250, 250, 250));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 15, 5, 15);
        gbc.fill = GridBagConstraints.BOTH;
        
        // Timer with different fonts for emoji and text
        gbc.gridx = 0;
        timerLabel = new JLabel();
        updateTimerLabel(0);
        infoPanel.add(timerLabel, gbc);
        
        // Smile Button
        gbc.gridx = 1;
        smileButton = new JButton("🙂");
        smileButton.setFont(EMOJI_FONT);
        smileButton.setPreferredSize(new Dimension(50, 50));
        smileButton.setBorderPainted(true);
        smileButton.setFocusPainted(false);
        smileButton.setContentAreaFilled(true); // Enable content fill
        smileButton.setBackground(Color.LIGHT_GRAY);
        smileButton.setOpaque(true);
        smileButton.setBorder(BorderFactory.createRaisedBevelBorder());
        smileButton.addActionListener(e -> restartGame());
        smileButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                smileButton.setBackground(HOVER_COLOR);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                smileButton.setBackground(Color.LIGHT_GRAY);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                smileButton.setBorder(BorderFactory.createLoweredBevelBorder());
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                smileButton.setBorder(BorderFactory.createRaisedBevelBorder());
            }
        });
        infoPanel.add(smileButton, gbc);
        
        // Difficulty
        gbc.gridx = 2;
        JLabel difficultyLabel = new JLabel("난이도: " + difficulty);
        difficultyLabel.setFont(DEFAULT_FONT);
        infoPanel.add(difficultyLabel, gbc);
        
        mainPanel.add(infoPanel, BorderLayout.NORTH);
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setBackground(new Color(250, 250, 250));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JButton mileageButton = createStyledButton("마일리지 조회", BUTTON_COLOR);
        mileageButton.addActionListener(e -> showMileageDialog());
        
        JButton customizeButton = createStyledButton("색상 커스터마이징", BUTTON_COLOR);
        customizeButton.addActionListener(e -> customizeCellColor());
        
        buttonPanel.add(mileageButton);
        buttonPanel.add(customizeButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(DEFAULT_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(150, 35));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(HOVER_COLOR);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });
        
        return button;
    }

    private boolean showLoginDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);

        // 로고 이미지 추가
        try {
            URL imageUrl = MinesweeperGame.class.getResource("/mine.jpg");
            if (imageUrl == null) {
                // Fallback to file system if resource not found in classpath
                File imageFile = new File("resources/mine.jpg");
                if (imageFile.exists()) {
                    imageUrl = imageFile.toURI().toURL();
                }
            }
            
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                Image scaledImage = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                panel.add(logoLabel, gbc);
            } else {
                LOGGER.warning("Logo image not found in resources");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load logo image", e);
            // 이미지 로딩 실패해도 게임 진행 가능
        }

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField usernameField = new JTextField(15);
        usernameField.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        JPasswordField passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        
        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel usernameLabel = new JLabel("사용자 이름:");
        usernameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        panel.add(usernameLabel, gbc);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel passwordLabel = new JLabel("비밀번호:");
        passwordLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        panel.add(passwordLabel, gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        int result = JOptionPane.showOptionDialog(frame, panel, "로그인",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new String[]{"로그인", "회원가입", "취소"}, "로그인");

        if (result == 2) { // 취소 버튼 클릭 시
            cleanup(); 
            System.exit(0);
            return false;
        } else if (result == 0) { // 로그인
            return processLogin(usernameField.getText(), new String(passwordField.getPassword()));
        } else if (result == 1) { // 회원가입
            return showRegisterDialog();
        }
        return false;
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    updateTimerLabel(elapsedTime.incrementAndGet());
                });
            }
        }, 0, 1000);
    }

    private boolean processLogin(String username, String password) {
        if (loginManager.login(username, password)) {
            playerName = username;
            return true;
        }
        JOptionPane.showMessageDialog(frame, 
            "아이디 또는 비밀번호가 잘못되었습니다.",
            "로그인 실패", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private boolean showRegisterDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);

        try {
            URL imageUrl = MinesweeperGame.class.getResource("/mine.jpg");
            if (imageUrl == null) {
                File imageFile = new File("resources/mine.jpg");
                if (imageFile.exists()) {
                    imageUrl = imageFile.toURI().toURL();
                }
            }
            
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                Image scaledImage = originalIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                panel.add(logoLabel, gbc);
            } else {
                LOGGER.warning("Logo image not found in resources");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load logo image", e);
        }

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField usernameField = new JTextField(15);
        usernameField.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        JPasswordField passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        JPasswordField confirmField = new JPasswordField(15);
        confirmField.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        
        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel usernameLabel = new JLabel("사용자 이름:");
        usernameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        panel.add(usernameLabel, gbc);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel passwordLabel = new JLabel("비밀번호:");
        passwordLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        panel.add(passwordLabel, gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);
        
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel confirmLabel = new JLabel("비밀번호 확인:");
        confirmLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // Set font
        panel.add(confirmLabel, gbc);
        gbc.gridx = 1;
        panel.add(confirmField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JLabel guidelines = new JLabel("<html><body style='text-align: left;'>"
            + "사용자명: 4-20자의 영문, 숫자, 언더스코어만 허용<br>"
            + "비밀번호: 최소 6자, 영문/숫자/특수문자 중 2종류 이상 조합" 
            + "</body></html>");
        guidelines.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
        panel.add(guidelines, gbc);

        if (JOptionPane.showConfirmDialog(frame, panel, "회원가입",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            return processRegistration(usernameField.getText(),
                                    new String(passwordField.getPassword()),
                                    new String(confirmField.getPassword()));
        }
        return false;
    }

    private boolean processRegistration(String username, String password, String confirm) {
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(frame, 
                "비밀번호가 일치하지 않습니다.",
                "회원가입 실패", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (loginManager.register(username, password)) {
            playerName = username;
            return true;
        }
        JOptionPane.showMessageDialog(frame, 
            "회원가입에 실패했습니다.\n" +
            "아이디는 4자 이상이어야 합니다.\n" +
            "비밀번호는 6자 이상의 영문과 숫자 조합이어야 합니다.",
            "회원가입 실패", 
            JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private void cleanup() {
        if (timer != null) {
            timer.cancel();
        }
        if (audioPlayer != null) {
            audioPlayer.close();
        }
        executorService.shutdown();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void showAlert(String title, String message) {
        JOptionPane.showMessageDialog(
            frame,
            String.valueOf(message),
            String.valueOf(title),
            title.contains("축하") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
        );
    }

    // 파일 저장 메소드 수정
    private void saveGameResult(String result) {
        try (FileWriter writer = new FileWriter(GameResources.GAME_RESULTS_FILE, true)) {
            writer.write(result + "\n");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save game result", e);
        }
    }

    private void restartGame() {
        int choice = JOptionPane.showConfirmDialog(frame, 
            "정말 게임을 다시 시작하시겠습니까?", 
            "게임 재시작", 
            JOptionPane.YES_NO_OPTION);
            
        if (choice == JOptionPane.YES_OPTION) {
            if (gameOver || JOptionPane.showConfirmDialog(frame,
                "진행 중인 게임이 있습니다. 정말 재시작하시겠습니까?",
                "재시작 확인",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                
                cleanup();
                elapsedTime.set(0);
                gameOver = false;
                firstClick = true;

                gameBoardPanel.removeAll();
                
                gameBoard = new GameBoard(rows, cols, mines, this::onWin, this::onGameOver);
                gameBoard.setFirstClick(firstClick);  // firstClick 상태 전달
                gameBoard.setCellColor(cellColor);
                
                gameBoardPanel.add(gameBoard, BorderLayout.CENTER);
                updateTimerLabel(0);
                
                startTimer();
                playBackgroundMusic();
                
                gameBoardPanel.revalidate();
                gameBoardPanel.repaint();
                
                smileButton.setText("🙂");
                LOGGER.info("Game successfully restarted");
                showAlert("재시작", "게임이 재시작되었습니다.");
            }
        }
    }

    private void customizeCellColor() {
        if (!gameOver && JOptionPane.showConfirmDialog(frame,
                "게임 진행 중에 색상을 변경하시겠습니까?",
                "색상 변경 확인",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        Customer customer = mileageManager.getCustomer(playerName);
        if (customer == null) {
            showAlert("오류", "고객 정보를 찾을 수 없습니다.");
            return;
        }

        // 메인 패널 생성
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 상단 헤더 추가 (제목, 마일리지 정보)
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("색상 선택", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        JLabel mileageLabel = new JLabel("보유 마일리지: " + customer.getMileage(), SwingConstants.RIGHT);
        mileageLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(mileageLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 색상 선택을 위한 메인 컨테이너 패널
        JPanel colorContainerPanel = new JPanel(new BorderLayout());
        colorContainerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 색상 선택 패널을 FlowLayout으로 변경하여 중앙 정렬
        
        // 보유 색상 섹션 패널
        JPanel ownedSection = new JPanel(new BorderLayout());
        JLabel ownedLabel = new JLabel("보유 중인 색상", SwingConstants.CENTER);
        ownedLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        ownedLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JPanel ownedColors = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        // 보유한 색상 표시
        for (Map.Entry<String, Color> entry : AVAILABLE_COLORS.entrySet()) {
            String colorName = entry.getKey();
            if (colorName.equals("기본") || customer.hasColorPurchased(colorName)) {
                addColorPreviewPanel(ownedColors, colorName, entry.getValue(), customer, true);
            }
        }
        
        ownedSection.add(ownedLabel, BorderLayout.NORTH);
        ownedSection.add(ownedColors, BorderLayout.CENTER);
        
        // 구분선 추가
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // 구매 가능한 색상 섹션 패널
        JPanel purchasableSection = new JPanel(new BorderLayout());
        JLabel purchasableLabel = new JLabel("구매 가능한 색상", SwingConstants.CENTER);
        purchasableLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        purchasableLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JPanel purchasableColors = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        // 구매 가능한 색상 표시
        for (Map.Entry<String, Color> entry : AVAILABLE_COLORS.entrySet()) {
            String colorName = entry.getKey();
            if (!colorName.equals("기본") && !customer.hasColorPurchased(colorName)) {
                addColorPreviewPanel(purchasableColors, colorName, entry.getValue(), customer, false);
            }
        }
        
        purchasableSection.add(purchasableLabel, BorderLayout.NORTH);
        purchasableSection.add(purchasableColors, BorderLayout.CENTER);
        
        // 섹션들을 수직으로 배치
        JPanel sectionsPanel = new JPanel();
        sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));
        sectionsPanel.add(ownedSection);
        sectionsPanel.add(separator);
        sectionsPanel.add(purchasableSection);
        
        colorContainerPanel.add(sectionsPanel, BorderLayout.CENTER);

        // 스크롤 패널에 컨테이너 추가
        JScrollPane scrollPane = new JScrollPane(colorContainerPanel);
        scrollPane.setPreferredSize(new Dimension(400, 400));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 다이얼로그 생성 및 표시
        JDialog dialog = new JDialog(frame, "색상 커스터마이징", true);
        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void addColorPreviewPanel(JPanel colorPanel, String colorName, Color color, Customer customer, boolean isOwned) {
        JPanel colorPreviewPanel = new JPanel(new BorderLayout(5, 5));
        colorPreviewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(cellColor.equals(color) ? Color.YELLOW : Color.GRAY, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        colorPreviewPanel.setPreferredSize(new Dimension(100, 80));
        
        // 색상 미리보기 영역
        JPanel previewArea = new JPanel();
        previewArea.setBackground(color);
        previewArea.setPreferredSize(new Dimension(60, 40));
        JPanel previewContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        previewContainer.add(previewArea);
        
        // 색상 이름 및 상태 표시
        JLabel nameLabel = new JLabel(colorName + (isOwned ? "" : " (" + COLOR_PRICE + "M)"));
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setFont(new Font("맑은 고딕", cellColor.equals(color) ? Font.BOLD : Font.PLAIN, 12));
        if (cellColor.equals(color)) {
            nameLabel.setForeground(Color.BLUE);
        }
        
        colorPreviewPanel.add(previewContainer, BorderLayout.CENTER);
        colorPreviewPanel.add(nameLabel, BorderLayout.SOUTH);
        
        // 마우스 이벤트 처리
        colorPreviewPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                colorPreviewPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLUE, 2),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
                ));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                colorPreviewPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(cellColor.equals(color) ? Color.YELLOW : Color.GRAY, 1),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
            }
            
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (isOwned) {
                    cellColor = color;
                    gameBoard.setCellColor(color);
                    ((Window) colorPreviewPanel.getTopLevelAncestor()).dispose();
                    showAlert("색상 변경", "색상이 변경되었습니다.");
                } else {
                    if (customer.getMileage() >= COLOR_PRICE) {
                        int choice = JOptionPane.showConfirmDialog(frame,
                            String.format("%s 색상을 %d 마일리지로 구매하시겠습니까?", colorName, COLOR_PRICE),
                            "색상 구매", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            customer.deductMileage(COLOR_PRICE, "색상 구매: " + colorName);
                            customer.addPurchasedColor(colorName);
                            cellColor = color;
                            gameBoard.setCellColor(color);
                            mileageManager.saveData();
                            ((Window) colorPreviewPanel.getTopLevelAncestor()).dispose();
                            showAlert("구매 완료", "색상 구매가 완료되었습니다.");
                        }
                    } else {
                        showAlert("마일리지 부족", "마일리지가 부족합니다.");
                    }
                }
            }
        });
        
        colorPanel.add(colorPreviewPanel);
    }

    private void showMileageDialog() {
        Customer customer = mileageManager.getCustomer(playerName);
        if (customer == null) {
            showError("오류", "고객 정보를 찾을 수 없습니다.");
            return;
        }

        StringBuilder mileageInfo = new StringBuilder();
        mileageInfo.append("고객명: ").append(customer.getName()).append("\n");
        mileageInfo.append("마일리지: ").append(customer.getMileage()).append("\n\n");
        mileageInfo.append("마일리지 내역:\n");

        for (MileageRecord record : customer.getMileageRecords()) {
            mileageInfo.append(record.getDate()).append(" - ")
                       .append(record.isCredit() ? "+" : "-")
                       .append(record.getAmount()).append(" - ")
                       .append(record.getDescription()).append("\n");
        }

        JTextArea textArea = new JTextArea(mileageInfo.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JOptionPane.showMessageDialog(frame, scrollPane, "마일리지 조회", JOptionPane.INFORMATION_MESSAGE);
    }

    // 배경음악 재생 부분 수정
    private void playBackgroundMusic() {
        try {
            audioPlayer.play(GameResources.BACKGROUND_MUSIC_FILE);
        } catch (AudioPlayerException e) {
            LOGGER.log(Level.WARNING, "배경음악을 재생할 수 없습니다: " + e.getMessage());
        }
    }

    private void updateTimerLabel(int time) {
        String emojiPart = "<html><font face='Noto Color Emoji'>⏱️</font>";
        String textPart = "<font face='맑은 고딕'> 시간: " + time + "</font></html>";
        timerLabel.setText(emojiPart + textPart);
    }

    private void onWin() {
        audioPlayer.stop();
        if (timer != null) {
            timer.cancel();
        }
        gameOver = true;
        String result = playerName + "님이 " + elapsedTime.get() + "초 만에 승리! 난이도: " + difficulty;
        saveGameResult(result);
        winStreak++;
        int bonus = calculateWinBonus();
        mileageManager.addMileage(playerName, bonus, 
            String.format("게임 승리 (난이도: %s, %d연승)", difficulty, winStreak));
        gameBoard.setEnabled(false); 
        gameBoard.setGameOver(true); // GameBoard에 게임 종료 상태 전달
        smileButton.setText("😎");
        showAlert("축하합니다!", result);
    }

    private void onGameOver() {
        audioPlayer.stop();
        if (timer != null) {
            timer.cancel();
        }
        gameOver = true;
        String result = playerName + "님, 아쉽네요! 플레이 시간: " + elapsedTime.get() + "초";
        saveGameResult(result);
        winStreak = 0;
        int penalty = calculateLossPenalty();
        mileageManager.useMileage(playerName, penalty, 
            String.format("게임 패배 (난이도: %s)", difficulty));
        gameBoard.setEnabled(false);
        gameBoard.setGameOver(true); // GameBoard에 게임 종료 상태 전달
        smileButton.setText("😲");
        showAlert("게임 오버", result);
    }

    private int calculateWinBonus() {
        int baseBonus = switch(difficulty) {
            case "쉬움" -> 50;
            case "보통" -> 100;
            case "어려움" -> 200;
            default -> 50;
        };
        return baseBonus + (winStreak * 10); // 연승 보너스
    }

    private int calculateLossPenalty() {
        return switch(difficulty) {
            case "쉬움" -> 10;
            case "보통" -> 20;
            case "어려움" -> 30;
            default -> 10;
        };
    }
}