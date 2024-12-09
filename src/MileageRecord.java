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

    // 메소드 이름: getDate
    // 메소드 기능1: 날짜를 문자열로 반환
    // 메소드 기능2: "yyyy-MM-dd HH:mm:ss" 형식으로 포맷
    public String getDate() {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // 메소드 이름: getAmount
    // 메소드 기능1: 금액을 반환
    // 메소드 기능2: 없음
    public int getAmount() {
        return amount;
    }

    // 메소드 이름: getDescription
    // 메소드 기능1: 설명을 반환
    // 메소드 기능2: 없음
    public String getDescription() {
        return description;
    }

    // 메소드 이름: isCredit
    // 메소드 기능1: 크레딧 여부를 반환
    // 메소드 기능2: 없음
    public boolean isCredit() {
        return isCredit;
    }

    // 메소드 이름: serialize
    // 메소드 기능1: 객체를 문자열로 직렬화
    // 메소드 기능2: "날짜,금액,설명,크레딧여부" 형식으로 반환
    public String serialize() {
        return String.format("%s,%d,%s,%b", getDate(), amount, description, isCredit);
    }

    // 메소드 이름: deserialize
    // 메소드 기능1: 문자열을 객체로 역직렬화
    // 메소드 기능2: 문자열을 분리하여 객체 생성
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