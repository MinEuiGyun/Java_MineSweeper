import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Customer {
    private String name;
    private String customerId;
    private String passwordHash;
    private String salt;
    private int mileage;
    private List<MileageRecord> mileageRecords;
    private Set<String> purchasedColors;  // 구매한 색상 저장
    private int winStreak;

    public Customer(String name, String customerId, String password) {
        this.name = name;
        this.customerId = customerId;
        this.salt = generateSalt();
        this.passwordHash = hashPassword(password, salt);
        this.mileage = 0;
        this.mileageRecords = new ArrayList<>();
        this.purchasedColors = new HashSet<>();
        this.winStreak = 0;
    }

    // Constructor for serialization
    public Customer(String name, String customerId, String passwordHash, String salt) {
        this.name = name;
        this.customerId = customerId;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.mileage = 0;
        this.mileageRecords = new ArrayList<>();
        this.purchasedColors = new HashSet<>();
    }

    //_______generateSalt_______
    // 비밀번호 암호화용 임의의 솔트값 생성
    // Base64로 인코딩된 16바이트 솔트 문자열 반환
    // 보안을 위한 임의의 솔트값을 생성
    // Base64로 인코딩된 16바이트 솔트 문자열을 반환
    private String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    //_______hashPassword_______
    // 비밀번호를 솔트와 함께 해시처리
    // SHA-256으로 암호화된 비밀번호 문자열 반환
    // 비밀번호와 솔트를 결합하여 SHA-256 해시 생성
    // 해시된 비밀번호를 Base64로 인코딩하여 반환
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashed = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found", e);
        }
    }

    //_______getMileageRecords_______
    // 고객의 마일리지 기록 목록을 반환
    // 모든 마일리지 적립 및 사용 내역이 포함된 리스트 반환
    public List<MileageRecord> getMileageRecords() {
        return mileageRecords;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    //_______addMileage_______
    // 마일리지 적립 처리
    // 마일리지 증가 및 적립 기록 추가
    // 고객의 마일리지를 증가시키고 기록을 추가
    // 증가된 마일리지 금액과 설명을 기록에 저장
    public void addMileage(int amount, String description) {
        mileage += amount;
        mileageRecords.add(new MileageRecord(amount, description, true));
    }

    //_______deductMileage_______
    // 마일리지 차감 처리
    // 잔액 확인 후 차감 및 사용 기록 추가
    // 고객의 마일리지를 차감하고 기록을 추가
    // 차감 가능한 경우 true, 잔액 부족시 false 반환
    public boolean deductMileage(int amount, String description) {
        if (mileage >= amount) {
            mileage -= amount;
            mileageRecords.add(new MileageRecord(amount, description, false));
            return true;
        }
        return false;
    }

    public void addPurchasedColor(String colorName) {
        purchasedColors.add(colorName);
    }

    public boolean hasColorPurchased(String colorName) {
        return purchasedColors.contains(colorName);
    }

    public Set<String> getPurchasedColors() {
        return new HashSet<>(purchasedColors);
    }

    //_______serialize_______
    // 고객 정보를 문자열로 변환
    // 모든 고객 데이터를 포함한 문자열 형식으로 반환
    // 고객 정보를 문자열로 직렬화
    // 모든 고객 데이터를 포함한 형식화된 문자열 반환
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("======================================\n");
        sb.append("고객번호: ").append(customerId)
          .append(" || 이름: ").append(name)
          .append(" || 비밀번호: ").append(passwordHash)  // Changed from 비밀번호해시
          .append(" || 솔트: ").append(salt)
          .append(" || 총 마일리지 : ").append(mileage).append("\n");
        sb.append("---------------------------------------------------------------------\n");
        sb.append("마일리지 적립\n");
        for (MileageRecord record : mileageRecords) {
            if (record.isCredit()) {
                sb.append(record.serialize()).append("\n");
            }
        }
        sb.append("---------------------------------------------------------------------\n");
        sb.append("마일리지 소비\n");
        for (MileageRecord record : mileageRecords) {
            if (!record.isCredit()) {
                sb.append(record.serialize()).append("\n");
            }
        }
        sb.append("---------------------------------------------------------------------\n");
        sb.append("구매한 색상\n");
        for (String color : purchasedColors) {
            sb.append(color).append("\n");
        }
        sb.append("연승 기록: ").append(winStreak).append("\n");
        sb.append("======================================\n");
        return sb.toString();
    }

    //_______deserialize_______
    // 문자열에서 고객 정보를 복원
    // 저장된 문자열로부터 Customer 객체 생성
    // 문자열에서 고객 정보를 역직렬화
    // 직렬화된 문자열로부터 Customer 객체 생성하여 반환
    public static Customer deserialize(String data) {
        String[] parts = data.split("\n");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid data format: " + data);
        }
        String[] customerInfo = parts[1].split(" \\|\\| ");
        if (customerInfo.length != 5) {
            throw new IllegalArgumentException("Invalid customer info format: " + parts[1]);
        }
        String customerId = customerInfo[0].split(": ")[1];
        String name = customerInfo[1].split(": ")[1];
        String passwordHash = customerInfo[2].split(": ")[1];  // Now matches 비밀번호
        String salt = customerInfo[3].split(": ")[1];
        int mileage = Integer.parseInt(customerInfo[4].split(": ")[1]);
        Customer customer = new Customer(name, customerId, passwordHash, salt);
        customer.mileage = mileage;
        boolean readingColors = false;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.startsWith("구매한 색상")) {
                readingColors = true;
            } else if (part.startsWith("연승 기록:")) {
                readingColors = false;
                customer.winStreak = Integer.parseInt(part.split(": ")[1]);
            } else if (readingColors && !part.isEmpty() && 
                      !part.startsWith("=") && !part.startsWith("-")) {
                customer.purchasedColors.add(part);
            } else if (!readingColors && !part.isEmpty() && 
                      !part.startsWith("=") && !part.startsWith("-") &&
                      !part.startsWith("마일리지")) {
                try {
                    customer.mileageRecords.add(MileageRecord.deserialize(part));
                } catch (Exception e) {
                    // Skip invalid records
                }
            }
        }
        return customer;
    }

    // Add missing getter methods
    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public int getMileage() {
        return mileage;
    }

    //_______checkPassword_______
    // 비밀번호 일치 여부 확인
    // 입력된 비밀번호와 저장된 해시값 비교
    // 입력된 비밀번호의 유효성을 검사
    // 비밀번호가 일치하면 true, 불일치시 false 반환
    public boolean checkPassword(String password) {
        String hashedPassword = hashPassword(password, salt);
        return passwordHash.equals(hashedPassword);
    }

    public int getWinStreak() {
        return winStreak;
    }

    public void setWinStreak(int winStreak) {
        this.winStreak = winStreak;
    }
}