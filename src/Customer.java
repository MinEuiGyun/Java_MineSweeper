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
    private Set<String> purchasedColors; 
    private int winStreak;

    // Constructor
    // Initializes a new Customer object with the given name, customerId, and password.
    // Generates a salt and hashes the password.
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
    // Initializes a new Customer object with the given name, customerId, passwordHash, and salt.
    // Used for deserialization.
    public Customer(String name, String customerId, String passwordHash, String salt) {
        this.name = name;
        this.customerId = customerId;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.mileage = 0;
        this.mileageRecords = new ArrayList<>();
        this.purchasedColors = new HashSet<>();
    }

    // generateSalt
    // Generates a random salt for password hashing.
    // Returns the salt as a Base64 encoded string.
    private String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    // hashPassword
    // Hashes the given password with the provided salt using SHA-256.
    // Returns the hashed password as a Base64 encoded string.
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

    // getMileageRecords
    // Returns the list of mileage records.
    public List<MileageRecord> getMileageRecords() {
        return mileageRecords;
    }

    // getPasswordHash
    // Returns the hashed password.
    public String getPasswordHash() {
        return passwordHash;
    }

    // getSalt
    // Returns the salt used for password hashing.
    public String getSalt() {
        return salt;
    }

    // addMileage
    // Adds the specified amount of mileage and records the transaction with a description.
    public void addMileage(int amount, String description) {
        mileage += amount;
        mileageRecords.add(new MileageRecord(amount, description, true));
    }

    // deductMileage
    // Deducts the specified amount of mileage if available and records the transaction with a description.
    // Returns true if the deduction was successful, false otherwise.
    public boolean deductMileage(int amount, String description) {
        if (mileage >= amount) {
            mileage -= amount;
            mileageRecords.add(new MileageRecord(amount, description, false));
            return true;
        }
        return false;
    }

    // addPurchasedColor
    // Adds the specified color to the set of purchased colors.
    public void addPurchasedColor(String colorName) {
        purchasedColors.add(colorName);
    }

    // hasColorPurchased
    // Checks if the specified color has been purchased.
    // Returns true if the color is in the set of purchased colors, false otherwise.
    public boolean hasColorPurchased(String colorName) {
        return purchasedColors.contains(colorName);
    }

    // getPurchasedColors
    // Returns a copy of the set of purchased colors.
    public Set<String> getPurchasedColors() {
        return new HashSet<>(purchasedColors);
    }

    // serialize
    // Serializes the Customer object to a string representation.
    // Includes customer information, mileage records, purchased colors, and win streak.
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

    // deserialize
    // Deserializes a string representation of a Customer object.
    // Returns the Customer object.
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

    // getCustomerId
    // Returns the customer ID.
    public String getCustomerId() {
        return customerId;
    }

    // getName
    // Returns the name of the customer.
    public String getName() {
        return name;
    }

    // getMileage
    // Returns the total mileage of the customer.
    public int getMileage() {
        return mileage;
    }

    // checkPassword
    // Checks if the provided password matches the stored hashed password.
    // Returns true if the passwords match, false otherwise.
    public boolean checkPassword(String password) {
        String hashedPassword = hashPassword(password, salt);
        return passwordHash.equals(hashedPassword);
    }

    // getWinStreak
    // Returns the current win streak of the customer.
    public int getWinStreak() {
        return winStreak;
    }

    // setWinStreak
    // Sets the win streak of the customer to the specified value.
    public void setWinStreak(int winStreak) {
        this.winStreak = winStreak;
    }
}