import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class CustomerMileageManager {
    private static CustomerMileageManager instance;
    private final Map<String, Customer> mileageMap;
    private final String dataFilePath;
    private final ReentrantLock lock = new ReentrantLock();

    //_______싱글톤_인스턴스_획득_______
    //CustomerMileageManager의 유일한 인스턴스를 반환
    //처음 호출 시 새로운 인스턴스 생성
    public static synchronized CustomerMileageManager getInstance() {
        if (instance == null) {
            instance = new CustomerMileageManager(GameResources.USER_DATA_FILE);
        }
        return instance;
    }

    protected CustomerMileageManager(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.mileageMap = loadData();
    }

    //_______고객_등록_______
    //새로운 고객을 시스템에 등록
    //중복 ID 체크 및 데이터 저장 수행
    public boolean registerCustomer(String name, String customerId, String password) {
        lock.lock();
        try {
            if (mileageMap.containsKey(customerId)) {
                System.out.println(customerId + "은(는) 이미 등록된 고객입니다.");
                return false;
            }
            Customer newCustomer = new Customer(name, customerId, password);
            mileageMap.put(customerId, newCustomer);
            saveData();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean authenticateCustomer(String customerId, String password) {
        Customer customer = mileageMap.get(customerId);
        if (customer != null) {
            return customer.checkPassword(password);
        }
        return false;
    }

    public void addMileage(String customerId, int mileage, String description) {
        lock.lock();
        try {
            Customer customer = mileageMap.get(customerId);
            if (customer != null) {
                customer.addMileage(mileage, description);
                saveData();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean useMileage(String customerId, int mileage, String description) {
        lock.lock();
        try {
            Customer customer = mileageMap.get(customerId);
            if (customer != null && customer.deductMileage(mileage, description)) {
                saveData();
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public Customer getCustomer(String customerId) {
        return mileageMap.get(customerId);
    }

    public Map<String, Customer> getAllCustomers() {
        return mileageMap;
    }

    //_______데이터_저장_______
    //현재 메모리의 고객 데이터를 파일에 저장
    //스레드 안전성을 위해 잠금 사용
    public void saveData() {
        lock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath))) {
            for (Customer customer : mileageMap.values()) {
                writer.write(customer.serialize());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private Map<String, Customer> loadData() {
        Map<String, Customer> map = new HashMap<>();
        File file = new File(dataFilePath);
        
        // 파일이 없으면 디렉토리 생성 후 빈 파일 생성
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs(); // resources 디렉토리 생성
                file.createNewFile();
                return map;
            } catch (IOException e) {
                System.out.println("Error creating file: " + e.getMessage());
                return map;
            }
        }

        lock.lock();
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFilePath))) {
            StringBuilder data = new StringBuilder();
            String line;
            boolean isReadingCustomer = false;

            while ((line = reader.readLine()) != null) {
                if (line.equals("======================================")) {
                    if (isReadingCustomer && data.length() > 0) {
                        try {
                            Customer customer = Customer.deserialize(data.toString());
                            map.put(customer.getCustomerId(), customer);
                        } catch (IllegalArgumentException e) {
                            System.out.println("Skipping invalid customer data: " + e.getMessage());
                        }
                        data.setLength(0);
                    }
                    isReadingCustomer = true;
                    data.append(line).append("\n");
                } else if (isReadingCustomer) {
                    data.append(line).append("\n");
                }
            }

            // 마지막 고객 데이터 처리
            if (isReadingCustomer && data.length() > 0) {
                try {
                    Customer customer = Customer.deserialize(data.toString());
                    map.put(customer.getCustomerId(), customer);
                } catch (IllegalArgumentException e) {
                    System.out.println("Skipping invalid customer data: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        } finally {
            lock.unlock();
        }
        return map;
    }
}
