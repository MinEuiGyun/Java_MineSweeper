import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MileageRecord {
    private LocalDateTime date;
    private final int amount;
    private final String description;
    private final boolean isCredit;

    public MileageRecord(int amount, String description, boolean isCredit) {
        this.date = LocalDateTime.now();
        this.amount = amount;
        this.description = description;
        this.isCredit = isCredit;
    }

    public String getDate() {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public int getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCredit() {
        return isCredit;
    }

    // Used for serialization
    // 마일리지 기록을 문자열로 변환
    // 날짜, 금액, 설명, 적립/차감 여부를 포함한 문자열 반환
    public String serialize() {
        return String.format("%s,%d,%s,%b", getDate(), amount, description, isCredit);
    }

    // Used for deserialization
    // 문자열에서 마일리지 기록을 복원
    // 저장된 문자열로부터 MileageRecord 객체 생성
    public static MileageRecord deserialize(String data) {
        String[] parts = data.split(",");
        LocalDateTime date = LocalDateTime.parse(parts[0], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        int amount = Integer.parseInt(parts[1]);
        String description = parts[2];
        boolean isCredit = Boolean.parseBoolean(parts[3]);
        MileageRecord record = new MileageRecord(amount, description, isCredit);
        record.date = date; // Use the deserialized date
        return record;
    }
}