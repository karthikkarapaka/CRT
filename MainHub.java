import java.util.Scanner;

public class MainHub {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        mainMenuLoop:
        while (true) {
            System.out.println("\n============== MAIN HUB LAUNCHER ==============");
            System.out.println("1. Launch Data Structures Calculator");
            System.out.println("2. Launch Console CRUD Application");
            System.out.println("3. Launch JavaFX GUI CRUD Application");
            System.out.println("4. Exit");
            System.out.println("=============================================");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine();

            switch (choice.trim()) {
                case "1":
                    System.out.println("\n>>> Launching Data Structures Calculator...\n");
                    // UPDATED: Calls your new class name
                    DataStructureCalculatorApp.main(new String[0]);
                    System.out.println("\n<<< Calculator has finished. Returning to main hub.");
                    break;
                case "2":
                    System.out.println("\n>>> Launching Console CRUD Application...\n");
                    // UPDATED: Calls your new class name
                    CrudConsoleApp.main(new String[0]);
                    System.out.println("\n<<< Console CRUD App has finished. Returning to main hub.");
                    break;
                case "3":
                    System.out.println("\n>>> Launching JavaFX GUI Application...\n");
                    // UPDATED: Calls your new class name
                    CrudGuiApp.main(new String[0]);
                    System.out.println("\n<<< JavaFX GUI window has been closed. Returning to main hub.");
                    break;
                case "4":
                    System.out.println("👋 Exiting Main Hub. Goodbye!");
                    break mainMenuLoop;
                default:
                    System.out.println("❌ Invalid choice. Please enter a number between 1 and 4.");
                    break;
            }
        }
        scanner.close();
    }
}