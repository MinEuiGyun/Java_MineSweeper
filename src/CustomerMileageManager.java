import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class CustomerMileageManager {
    private static CustomerMileageManager instance;
    private final Map<String, Customer> mileageMap;
    private final String dataFilePath;
    private final ReentrantLock lock = new ReentrantLock();

    // getInstance
    // Returns the singleton instance of CustomerMileageManager
    // Initializes the instance if it is null
    public static synchronized CustomerMileageManager getInstance() {
        if (instance == null) {
            instance = new CustomerMileageManager(GameResources.USER_DATA_FILE);
        }
        return instance;
    }

    // CustomerMileageManager
    // Constructor to initialize dataFilePath and load mileage data
    protected CustomerMileageManager(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.mileageMap = loadData();
    }

    // registerCustomer
    // Registers a new customer if the customerId is not already in use
    // Saves the updated mileage data to the file
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

    // authenticateCustomer
    // Authenticates a customer using customerId and password
    // Returns true if authentication is successful
    public boolean authenticateCustomer(String customerId, String password) {
        Customer customer = mileageMap.get(customerId);
        if (customer != null) {
            return customer.checkPassword(password);
        }
        return false;
    }

    // addMileage
    // Adds mileage to a customer's account
    // Saves the updated mileage data to the file
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

    // useMileage
    // Deducts mileage from a customer's account if sufficient mileage is available
    // Saves the updated mileage data to the file
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

    // getCustomer
    // Retrieves a customer object by customerId
    public Customer getCustomer(String customerId) {
        return mileageMap.get(customerId);
    }

    // getAllCustomers
    // Returns a map of all customers
    public Map<String, Customer> getAllCustomers() {
        return mileageMap;
    }

    // saveData
    // Saves the mileage data of all customers to the file
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

    // loadData
    // Loads mileage data from the file and returns a map of customers
    // Creates a new file if it does not exist
    private Map<String, Customer> loadData() {
        Map<String, Customer> map = new HashMap<>();
        File file = new File(dataFilePath);
        
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs(); 
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
