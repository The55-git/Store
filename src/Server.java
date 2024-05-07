
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private static final String USERS_FILENAME = "users.txt";
    private static final String PRODUCTS_FILENAME = "products.txt";
    private final Object usersLock;
    private  double minPrice = 500.0;

    private Map<Product, Integer> cart;

    private ServerSocket server;
    private List<Product> products;

    public Server() {
        initAdmins();
        usersLock = new Object();
        initProducts();
        cart = new HashMap<>();
    }


    public void initAdmins() {
        if (new File(USERS_FILENAME).exists())
            return;
        List<User> users = new ArrayList<>();
        users.add(new Admin("admin", "12345"));
        users.add(new Customer("custom", "12345"));
        saveUsers(users);
    }

    public void start() {
        try {
            System.out.println("Server listening.");
            server = new ServerSocket(8080);

            while (true) {
                Socket client = server.accept();

                Thread clientThread = new Thread(() -> {
                    System.out.println("Accepted client.");
                    Scanner sc = null;
                    PrintStream out = null;

                    try {
                        sc = new Scanner(client.getInputStream());
                        out = new PrintStream(client.getOutputStream());
                        userMenu(sc, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (sc != null)
                                sc.close();
                            if (out != null)
                                out.close();
                            client.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @SuppressWarnings("unchecked")
    public List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(USERS_FILENAME))) {
            users = (List<User>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {

            e.printStackTrace();
        }
        return users;
    }


    public void saveUsers(List<User> users) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(USERS_FILENAME))) {
            out.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerUser(String userName, String password, UserType userType) throws CredentialsException {
        User user = UserFactory.createUser(userName, password, userType);
        synchronized (usersLock) {
            List<User> users = loadUsers();
            users.add(user);
            saveUsers(users);
        }
    }

    private User login(String userName, String password) {
        synchronized (usersLock) {
            for (User user : loadUsers()) {
                if (Objects.equals(user.getUserName(), userName) && Objects.equals(user.getPassword(), password))
                    return user;
            }
            return null;
        }
    }

    private void userMenu(Scanner sc, PrintStream out) {
        while (true) {
            out.println("Login? Y/N");
            String login = sc.nextLine();

            if (!login.equalsIgnoreCase("Y")) {
                out.println("Goodbye.");
                return;
            }

            out.println("Enter username:");
            String userName = sc.nextLine().trim();

            out.println("Enter password:");
            String password = sc.nextLine().trim();

            User user = login(userName, password);

            if (user == null) {
                out.println("Error: Invalid login.");
                continue;
            }

            switch (user.getUserType()) {
                case ADMIN:
                    adminMenu(sc, out, (Admin) user);
                    break;
                case CUSTOMER:
                    customerMenu(sc, out, (Customer) user);
                    break;
                case EMPLOYEE:
                    employeeMenu(sc, out, (Employee) user);
                    break;
            }
        }
    }


    private void adminMenu(Scanner sc, PrintStream out, Admin admin) {
        out.println("Logged in as admin.");

        out.println("Enter user type to create: (ADMIN | CUSTOMER | EMPLOYEE");
        try {
            UserType userType = UserType.valueOf(sc.nextLine().toUpperCase());

            out.println("Enter username:");
            String userName = sc.nextLine();

            out.println("Enter password:");
            String password = sc.nextLine();

            registerUser(userName, password, userType);

            out.println("Success.");
        } catch (IllegalArgumentException e) {
            out.println("Error: Invalid user type.");
        } catch (CredentialsException e) {
            out.println(e.getMessage());
        }
    }


    private void initProducts() {
        File productsFile = new File(PRODUCTS_FILENAME);
        if (productsFile.exists()) {
            products = loadProducts();
        } else {

            products = new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Product> loadProducts() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(PRODUCTS_FILENAME))) {
            return (List<Product>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();

            return new ArrayList<>();
        }
    }

    private void saveProducts() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(PRODUCTS_FILENAME))) {
            out.writeObject(products);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addProduct(Product product) {
        if (product.getPrice() < minPrice) {
            System.out.println("Error: Product price cannot be less than " + minPrice);
            return;
        }
        products.add(product);
        saveProducts();
    }


    public void editProduct(String productName, Product newProductDetails) {
        for (Product product : products) {
            if (product.getName().equalsIgnoreCase(productName)) {
                product.setName(newProductDetails.getName());
                product.setPrice(newProductDetails.getPrice());
                if (newProductDetails.getPrice() < minPrice) {
                    System.out.println("Error: Product price cannot be less than the minimum price requirement (" + minPrice + ")");
                    return;
                }
                product.setQuantity(newProductDetails.getQuantity());
                saveProducts();
                return;

            }
        }
        System.out.println("Product not found: " + productName);
    }

    public void deleteProduct(Integer productId) {
        boolean productFound = false;
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getId() == (productId)) {
                products.remove(i);
                productFound = true;
                updateIdsAfterDeletion(i + 1);
                break;
            }
        }
        if (!productFound) {
            System.out.println("Product not found with ID: " + productId);
        } else {
            saveProducts();
        }
    }

    private void updateIdsAfterDeletion(int startIndex) {
        for (int i = startIndex; i < products.size(); i++) {
            Product product = products.get(i);
            product.setId(product.getId() - 1);
        }
    }

    public void viewProducts() {
        for (Product product : products) {
            System.out.println(product.toString());
        }
    }

    public int checkProductAvailability(String productName) {
        for (Product product : products) {
            if (product.getName().equalsIgnoreCase(productName)) {
                return product.getQuantity();
            }
        }
        return 0;
    }

    public void reduceMinPriceRequirement(double newMinPrice, Employee employee) {
        if (employee == null || employee.getUserType() != UserType.EMPLOYEE) {
            System.out.println("Error: Only employees can reduce the minimum price requirement.");
            return;
        }


        if (newMinPrice >= 0) {
            minPrice = newMinPrice;
            System.out.println("Minimum price requirement reduced to: " + minPrice);
        } else {
            System.out.println("Error: Invalid price. Minimum price requirement must be a non-negative value.");
        }
    }

    private void customerMenu(Scanner sc, PrintStream out, Customer customer) {
        while (true) {
            out.println("Welcome, " + customer.getUserName() + "!");
            out.println("1. View Products");
            out.println("2. Check Product Availability");
            out.println("3. View Cart");
            out.println("4. Checkout");
            out.println("5. Exit");
            out.println("Choose an option:");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    viewProducts();
                    sendProductsToClient(out);
                    break;
                case 2:
                    out.println("Enter product name:");
                    String productName = sc.nextLine();
                    int availability = checkProductAvailability(productName);
                    out.println("Availability of " + productName + ": " + availability);
                    break;
                case 3:
                    out.println("Goodbye, " + customer.getUserName() + "!");
                    return;
                case 4:
                    viewCart(out);
                    break;
                case 5:
                    processPurchase(customer);
                    out.println("Checkout successful!");
                    break;

                default:
                    out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void employeeMenu(Scanner sc, PrintStream out, Employee employee) {
        while (true) {
            out.println("1. Add Product");
            out.println("2. Edit Product");
            out.println("3. Delete Product");
            out.println("4. View Products");
            out.println("5. Update minimum price");
            out.println("6. Exit");
            out.println("Choose an option:");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    out.println("Enter product name:");
                    String name = sc.nextLine();
                    out.println("Enter product price:");
                    double price = sc.nextDouble();
                    out.println("Enter product quantity:");
                    int quantity = sc.nextInt();
                    addProduct(new Product(name, price, quantity));
                    break;
                case 2:
                    out.println("Enter product name to edit:");
                    String productName = sc.nextLine();
                    out.println("Enter new product name:");
                    String newName = sc.nextLine();
                    out.println("Enter new product price:");
                    double newPrice = sc.nextDouble();
                    if(newPrice<minPrice){
                        System.out.println("Product price, cant be below min price");
                        break;
                    }

                    out.println("Enter new product quantity:");
                    int newQuantity = sc.nextInt();
                    editProduct(productName, new Product(newName, newPrice, newQuantity));
                    break;
                case 3:
                    out.println("Enter product name to delete:");
                    String productToDelete = sc.nextLine();
                    deleteProduct(Integer.valueOf(productToDelete));
                    break;
                case 4:
                    viewProducts();
                    sendProductsToClient(out);
                    break;
                case 5:
                    out.println("Enter the new minimum price:");
                    double newMinPrice = sc.nextDouble();
                    reduceMinPriceRequirement(newMinPrice, employee);
                    break;
                case 6:
                    out.println("Goodbye, " + employee.getUserName() + "!");
                    return;
                default:
                    out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void sendProductsToClient(PrintStream out) {

        int productId = 1;
        for (Product product : products) {
            out.println(productId + ". " + product.getName() + " - Price: " + product.getPrice() + ", Quantity: " + product.getQuantity());
            productId++;
        }

        out.println();
    }

    public synchronized void addToCart(Product product, int quantity) {
        cart.put(product, cart.getOrDefault(product, 0) + quantity);
    }

    public synchronized void removeFromCart(Product product, int quantity) {
        int currentQuantity = cart.getOrDefault(product, 0);
        int newQuantity = currentQuantity - quantity;
        if (newQuantity <= 0) {
            cart.remove(product);
        } else {
            cart.put(product, newQuantity);
        }
    }

    public synchronized void processPurchase(Customer customer) {

        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            int availableQuantity = checkProductAvailability(product.getName());
            if (quantity > availableQuantity) {
                System.out.println("Error: Not enough quantity available for product " + product.getName());
                continue;
            }

            product.setQuantity(availableQuantity - quantity);
            saveProducts();
            System.out.println("Purchase successful: " + quantity + " units of " + product.getName());
        }

        cart.clear();
    }

    private void viewCart(PrintStream out) {
        if (cart.isEmpty()) {
            out.println("Your cart is empty.");
        } else {
            out.println("Your Cart:");
            for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();
                out.println(quantity + " units of " + product.getName() + " - Price: " + product.getPrice());
            }
        }

    }
}