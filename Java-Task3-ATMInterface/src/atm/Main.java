package atm;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Entry point: creates the bank, seeds demo accounts, and starts the ATM.
 *
 * Demo accounts:
 *   User ID  PIN   Holder          Opening balance
 *   1001     1234  Mohammad Dirani 5,000.00
 *   1002     4321  Sara Haddad     2,500.00
 *   1003     1111  Ali Khalil        750.00
 */
public class Main {

    public static void main(String[] args) {
        Bank bank = new Bank("Oasis Bank");
        bank.addAccount(new Account("1001", "Mohammad Dirani", "1234", new BigDecimal("5000.00")));
        bank.addAccount(new Account("1002", "Sara Haddad", "4321", new BigDecimal("2500.00")));
        bank.addAccount(new Account("1003", "Ali Khalil", "1111", new BigDecimal("750.00")));

        try (Scanner scanner = new Scanner(System.in)) {
            new ATM(bank, scanner).start();
        }
    }
}
