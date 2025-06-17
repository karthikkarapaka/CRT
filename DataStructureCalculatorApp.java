import java.util.Scanner;

public class DataStructureCalculatorApp {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        mainMenuLoop:
        while (true) {
            System.out.println("\n========= DATA STRUCTURES CALCULATOR MENU =========");
            System.out.println("1. Run Calculator (using custom LinkedList)");
            System.out.println("2. Run Calculator (using standard ArrayList)");
            System.out.println("3. Run Calculator (using a Queue)");
            System.out.println("4. Exit to Main Hub");
            System.out.println("================================================");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine();

            switch (choice.trim()) {
                case "1":
                    // Calls the original helper class name
                    LinkedListCalculator.runCalculator(sc);
                    break;
                case "2":
                    ArrayListCalculator.runCalculator(sc);
                    break;
                case "3":
                    QueueCalculator.runCalculator(sc);
                    break;
                case "4":
                    System.out.println("👋 Exiting Data Structures Calculator.");
                    break mainMenuLoop;
                default:
                    System.out.println("❌ Invalid choice. Please enter a number between 1 and 4.");
                    break;
            }
        }
    }
}