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

    // getMileageManager 메소드
    // 메소드 기능1: mileageManager 객체 반환
    public CustomerMileageManager getMileageManager() {
        return mileageManager;
    }

    // saveUsers 메소드
    // 메소드 기능1: 사용자 데이터 저장
    private synchronized void saveUsers() {
        // Save users via CustomerMileageManager
        mileageManager.saveData();
    }

    // validateInput 메소드
    // 메소드 기능1: 입력값 검증
    // 메소드 기능2: 유효한 사용자명 및 비밀번호 형식 확인
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

    // generateSalt 메소드
    // 메소드 기능1: 새로운 솔트 생성
    private String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // hashPassword 메소드
    // 메소드 기능1: 비밀번호 해싱
    // 메소드 기능2: 솔트와 결합하여 해시 생성
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

    // register 메소드
    // 메소드 기능1: 사용자 등록
    // 메소드 기능2: 입력값 검증 및 중복 사용자 확인
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

    // login 메소드
    // 메소드 기능1: 사용자 로그인
    // 메소드 기능2: 입력값 검증 및 인증
    public boolean login(String username, String password) {
        if (!validateInput(username, password)) {
            return false;
        }
        return mileageManager.authenticateCustomer(username, password);
    }
}