import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * A single product with a name and a unit price (in rupees).
 */
class Product {
    private final String name;
    private final int price;

    Product(String name, int price) {
        this.name = name;
        this.price = price;
    }

    String getName() {
        return name;
    }

    int getPrice() {
        return price;
    }
}

/**
 * A named group of products, e.g. "Dairy".
 */
class Category {
    private final String name;
    private final List<Product> products;

    Category(String name, Product... products) {
        this.name = name;
        this.products = Arrays.asList(products);
    }

    String getName() {
        return name;
    }

    List<Product> getProducts() {
        return Collections.unmodifiableList(products);
    }

    Product getProduct(int number) {          // number is 1-based
        return products.get(number - 1);
    }

    int size() {
        return products.size();
    }

    void display() {
        System.out.println(name + ":");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            System.out.printf("   %d. %-12s Rs.%d%n", i + 1, p.getName(), p.getPrice());
        }
    }
}

/**
 * The store catalogue. All products and prices are defined here.
 */
class Store {
    private final List<Category> categories = new ArrayList<>();

    Store() {
        categories.add(new Category("Dairy",
                new Product("Milk", 28),
                new Product("Paneer", 90),
                new Product("Curd", 35)));
        categories.add(new Category("Snacks",
                new Product("Chips", 20),
                new Product("Biscuits", 15),
                new Product("Samosa", 12)));
        categories.add(new Category("Beverages",
                new Product("Coffee", 25),
                new Product("Lemon Soda", 30),
                new Product("Badam Milk", 45)));
    }

    List<Category> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    Category getCategory(int number) {        // number is 1-based
        return categories.get(number - 1);
    }

    int size() {
        return categories.size();
    }

    void displayMenu() {
        System.out.println("\n--- Product Catalogue ---");
        for (int i = 0; i < categories.size(); i++) {
            System.out.print((i + 1) + ". ");
            categories.get(i).display();
        }
    }
}

/**
 * Holds the items the customer has chosen and their quantities.
 * LinkedHashMap keeps items in the order they were added.
 */
class Cart {
    private final Map<Product, Integer> items = new LinkedHashMap<>();

    void add(Product product, int quantity) {
        items.merge(product, quantity, Integer::sum);
    }

    int getTotal() {
        int total = 0;
        for (Map.Entry<Product, Integer> e : items.entrySet()) {
            total += e.getKey().getPrice() * e.getValue();
        }
        return total;
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    void clear() {
        items.clear();
    }

    /** Builds the itemised bill lines; used for both screen and file output. */
    List<String> getBillLines() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Product, Integer> e : items.entrySet()) {
            Product p = e.getKey();
            int qty = e.getValue();
            lines.add(String.format("%-12s x %-3d @ Rs.%-4d = Rs.%d",
                    p.getName(), qty, p.getPrice(), p.getPrice() * qty));
        }
        return lines;
    }

    void display() {
        if (isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }
        System.out.println("\n--- Your Cart ---");
        for (String line : getBillLines()) {
            System.out.println(line);
        }
        System.out.println("Total: Rs." + getTotal());
    }
}

/**
 * Handles all file operations: saving bills and reading them back.
 */
class BillManager {
    private static final String FILE_NAME = "bills.txt";
    private static final String DIVIDER = "========================================";
    private static final String LINE = "----------------------------------------";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /** Appends the cart's bill to the file. Returns true on success. */
    boolean saveBill(Cart cart) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME, true))) {
            writer.println(DIVIDER);
            writer.println("BILL - " + LocalDateTime.now().format(TIME_FORMAT));
            writer.println(LINE);
            for (String line : cart.getBillLines()) {
                writer.println(line);
            }
            writer.println(LINE);
            writer.println("TOTAL: Rs." + cart.getTotal());
            writer.println(DIVIDER);
            return true;
        } catch (IOException e) {
            System.out.println("Error saving bill: " + e.getMessage());
            return false;
        }
    }

    /**
     * Opens bills.txt in Notepad (Windows), TextEdit (Mac) or the default
     * text editor (Linux). Falls back to printing in the console if that fails.
     */
    void openBills() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println("No saved bills found.");
            return;
        }
        try {
            String path = file.getAbsolutePath();
            String os = System.getProperty("os.name").toLowerCase();
            String editor;
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("notepad.exe", path);          // Windows
                editor = "Notepad";
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", "-e", path);           // macOS
                editor = "TextEdit";
            } else {
                pb = new ProcessBuilder("xdg-open", path);             // Linux
                editor = "the default text editor";
            }
            pb.start();
            System.out.println("Opening bills in " + editor + "...");
        } catch (IOException e) {
            System.out.println("Could not open editor, showing bills here instead.");
            displayBills();
        }
    }

    /** Prints every bill stored in the file to the console. */
    void displayBills() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            System.out.println("\n--- Saved Bills ---");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("No saved bills found.");
        }
    }
}

/**
 * Entry point: runs the menu loop and handles user input.
 */
public class Main {
    private final Store store = new Store();
    private final Cart cart = new Cart();
    private final BillManager billManager = new BillManager();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        System.out.println("===== Welcome to the Store =====");
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice: ", 1, 6);
            switch (choice) {
                case 1: store.displayMenu();  break;
                case 2: addItem();            break;
                case 3: cart.display();       break;
                case 4: checkout();           break;
                case 5: billManager.openBills();    break;
                case 6: running = false;      break;
            }
        }
        System.out.println("Thank you for shopping!");
        scanner.close();
    }

    private void printMainMenu() {
        System.out.println("\n1. View Categories");
        System.out.println("2. Add Items");
        System.out.println("3. View Cart & Total");
        System.out.println("4. Checkout & Save Bill");
        System.out.println("5. Open Saved Bills");
        System.out.println("6. Exit");
    }

    private void addItem() {
        List<Category> categories = store.getCategories();
        System.out.println("\nSelect a category:");
        for (int i = 0; i < categories.size(); i++) {
            System.out.println((i + 1) + ". " + categories.get(i).getName());
        }
        Category category = store.getCategory(readInt("Category: ", 1, store.size()));

        category.display();
        Product product = category.getProduct(readInt("Item: ", 1, category.size()));
        int quantity = readInt("Quantity: ", 1, 100);

        cart.add(product, quantity);
        System.out.println(quantity + " x " + product.getName() + " added to cart.");
    }

    private void checkout() {
        if (cart.isEmpty()) {
            System.out.println("Your cart is empty. Nothing to save.");
            return;
        }
        cart.display();
        if (billManager.saveBill(cart)) {
            System.out.println("Bill saved to bills.txt");
            cart.clear();
        }
    }

    /**
     * Reads an integer in [min, max]. Re-prompts on letters or out-of-range
     * numbers instead of crashing.
     */
    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // falls through to the error message below
            }
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }
}