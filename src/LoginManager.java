import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class LoginManager {
    private static final Logger LOGGER = Logger.getLogger(LoginManager.class.getName());
    private static final int SALT_LENGTH = 16;
    private final CustomerMileageManager mileageManager;
    private Map<String, Customer> users;

    public LoginManager() {
        this.mileageManager = CustomerMileageManager.getInstance();
        this.users = mileageManager.getAllCustomers();
    }

    public CustomerMileageManager getMileageManager() {
        return mileageManager;
    }

    //_______USER_DATA_MANAGEMENT_______
    // Handles file-based user data storage
    // Uses BufferedReader/BufferedWriter for file operations
    private synchronized void saveUsers() {
        // Save users via CustomerMileageManager
        mileageManager.saveData();
    }

    //_______USER_VALIDATION_______
    // Validates username and password format
    // Uses regex for input validation
    private boolean validateInput(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        // Username: 4-20자의 영문, 숫자, 언더스코어만 허용
        if (!username.matches("^[a-zA-Z0-9_]{4,20}$")) {
            return false;
        }
        // Password: 최소 6자, 영문/숫자/특수문자 중 2종류 이상 조합
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{6,}$")) {
            return false;
        }
        return true;
    }

    //_______SECURITY_OPERATIONS_______
    // Implements password hashing using SHA-256
    // Uses SecureRandom for salt generation
    private String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Hashing algorithm not found", e);
            throw new RuntimeException("Security error", e);
        }
    }

    public boolean register(String username, String password) {
        // 입력값 검증
        if (!validateInput(username, password)) {
            LOGGER.log(Level.INFO, "Invalid input for registration: " + username);
            return false;
        }
        
        // 중복 사용자 확인
        if (users.containsKey(username)) {
            LOGGER.log(Level.INFO, "Username already exists: " + username);
            return false;
        }
        
        try {
            // Generate salt and hash password
            String salt = generateSalt();
            String passwordHash = hashPassword(password, salt);

            // Create new customer with hashed password and salt
            Customer newCustomer = new Customer(username, username, passwordHash, salt);
            users.put(username, newCustomer);

            // Save data
            saveUsers();
            LOGGER.log(Level.INFO, "Successfully registered user: " + username);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register user: " + username, e);
            return false;
        }
    }

    public boolean login(String username, String password) {
        if (!validateInput(username, password)) {
            return false;
        }
        return mileageManager.authenticateCustomer(username, password);
    }

    //_______generateSalt_______
    // 비밀번호 암호화를 위한 솔트값 생성
    // 16바이트 길이의 무작위 솔트값 생성 및 반환

    //_______hashPassword_______
    // 비밀번호와 솔트를 결합하여 해시 생성
    // SHA-256 알고리즘으로 암호화된 비밀번호 문자열 반환

    //_______validateInput_______
    // 사용자명과 비밀번호의 유효성 검사
    // 입력값이 정해진 형식에 맞는지 확인하고 결과 반환

    //_______register_______
    // 새로운 사용자 등록 처리
    // 유효성 검사 후 사용자 정보 저장 및 결과 반환

    //_______login_______
    // 사용자 로그인 인증 처리
    // 입력된 정보 검증 후 로그인 성공 여부 반환
}