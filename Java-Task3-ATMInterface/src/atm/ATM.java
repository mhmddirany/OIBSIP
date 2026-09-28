package atm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * The console user interface of the ATM: login, menu, and prompts.
 * All money rules live in Bank / Account; this class only talks to the user.
 */
public class ATM {

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private final Bank bank;
    private final Scanner in;
    private Account current;

    public ATM(Bank bank, Scanner in) {
        this.bank = bank;
        this.in = in;
    }

    public void start() {
        printBanner();
        if (!login()) {
            System.out.println("\nToo many incorrect attempts. Access denied. Your card has been retained.");
            return;
        }
        System.out.println("\nWelcome, " + current.getHolderName() + "!");
        menuLoop();
    }

    // ---------------------------------------------------------------- login

    private boolean login() {
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String userId = prompt("User ID: ");
            String pin = prompt("PIN: ");
            if (userId == null || pin == null) {
                return false;
            }
            Account account = bank.authenticate(userId.trim(), pin.trim());
            if (account != null) {
                current = account;
                return true;
            }
            int left = MAX_LOGIN_ATTEMPTS - attempt;
            // Same message for wrong ID or wrong PIN, so we don't reveal which one was wrong.
            System.out.println("Invalid User ID or PIN."
                    + (left > 0 ? " Attempts remaining: " + left : ""));
        }
        return false;
    }

    // ----------------------------------------------------------------- menu

    private void menuLoop() {
        while (true) {
            System.out.println();
            System.out.println("========== MAIN MENU ==========");
            System.out.println(" 1. Transaction History");
            System.out.println(" 2. Withdraw");
            System.out.println(" 3. Deposit");
            System.out.println(" 4. Transfer");
            System.out.println(" 5. Quit");
            System.out.println("-------------------------------");
            System.out.printf(" Current balance: %,.2f%n", current.getBalance());
            String choice = prompt("Choose an option (1-5): ");
            if (choice == null) {
                quit();
                return;
            }
            switch (choice.trim()) {
                case "1": showHistory(); break;
                case "2": withdraw(); break;
                case "3": deposit(); break;
                case "4": transfer(); break;
                case "5": quit(); return;
                default: System.out.println("Invalid option. Please enter a number from 1 to 5.");
            }
        }
    }

    private void showHistory() {
        List<Transaction> history = current.getHistory();
        System.out.println("\n------ TRANSACTION HISTORY ------");
        if (history.isEmpty()) {
            System.out.println("No transactions yet in this session.");
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            System.out.printf("%2d. %s%n", i + 1, history.get(i));
        }
        System.out.printf("Total transactions: %d%n", history.size());
    }

    private void withdraw() {
        BigDecimal amount = readAmount("Amount to withdraw: ");
        if (amount == null) return;
        try {
            bank.withdraw(current, amount);
            System.out.printf("Please collect your cash: %,.2f%n", amount);
            System.out.printf("New balance: %,.2f%n", current.getBalance());
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void deposit() {
        BigDecimal amount = readAmount("Amount to deposit: ");
        if (amount == null) return;
        try {
            bank.deposit(current, amount);
            System.out.printf("Deposited %,.2f. New balance: %,.2f%n", amount, current.getBalance());
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void transfer() {
        String toId = prompt("Recipient account ID: ");
        if (toId == null || toId.trim().isEmpty()) {
            System.out.println("Recipient account ID cannot be empty.");
            return;
        }
        Account recipient = bank.findAccount(toId.trim());
        if (recipient == null) {
            System.out.println("Recipient account '" + toId.trim() + "' not found.");
            return;
        }
        BigDecimal amount = readAmount("Amount to transfer: ");
        if (amount == null) return;
        try {
            bank.transfer(current, toId.trim(), amount);
            System.out.printf("Transferred %,.2f to %s (%s).%n",
                    amount, recipient.getHolderName(), recipient.getUserId());
            System.out.printf("New balance: %,.2f%n", current.getBalance());
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void quit() {
        System.out.println("\nThank you for banking with " + bank.getName()
                + ". Goodbye, " + current.getHolderName() + "!");
    }

    // -------------------------------------------------------------- helpers

    /** Reads a positive amount with at most 2 decimals; returns null if invalid. */
    private BigDecimal readAmount(String message) {
        String raw = prompt(message);
        if (raw == null) return null;
        try {
            BigDecimal amount = new BigDecimal(raw.trim());
            if (amount.signum() <= 0) {
                System.out.println("Amount must be greater than zero.");
                return null;
            }
            if (amount.scale() > 2) {
                System.out.println("Amount can have at most 2 decimal places.");
                return null;
            }
            if (amount.compareTo(MAX_AMOUNT) > 0) {
                System.out.printf("Amount cannot exceed %,.2f per transaction.%n", MAX_AMOUNT);
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount. Please enter a number, e.g. 250 or 99.50");
            return null;
        }
    }

    /** Prints a prompt and reads a line; returns null if input has ended. */
    private String prompt(String message) {
        System.out.print(message);
        if (!in.hasNextLine()) {
            System.out.println();
            return null;
        }
        return in.nextLine();
    }

    private void printBanner() {
        System.out.println("===============================");
        System.out.println("   " + bank.getName() + " ATM");
        System.out.println("===============================");
    }
}
