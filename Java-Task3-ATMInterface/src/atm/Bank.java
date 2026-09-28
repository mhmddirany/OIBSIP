package atm;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds all accounts and performs the business rules for each operation.
 * The ATM class only handles input/output and delegates the logic here.
 */
public class Bank {

    private final String name;
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public Bank(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void addAccount(Account account) {
        accounts.put(account.getUserId(), account);
    }

    public Account findAccount(String userId) {
        return accounts.get(userId);
    }

    /** Returns the account if the ID exists and the PIN matches, otherwise null. */
    public Account authenticate(String userId, String pin) {
        Account account = accounts.get(userId);
        if (account != null && account.checkPin(pin)) {
            return account;
        }
        return null;
    }

    public void deposit(Account account, BigDecimal amount) {
        requirePositive(amount);
        account.deposit(amount, Transaction.Type.DEPOSIT, "Cash deposit");
    }

    public void withdraw(Account account, BigDecimal amount) {
        requirePositive(amount);
        if (!account.hasSufficientFunds(amount)) {
            throw new IllegalStateException("Insufficient Funds");
        }
        account.withdraw(amount, Transaction.Type.WITHDRAW, "Cash withdrawal");
    }

    public void transfer(Account from, String toUserId, BigDecimal amount) {
        requirePositive(amount);
        Account to = accounts.get(toUserId);
        if (to == null) {
            throw new IllegalArgumentException("Recipient account '" + toUserId + "' not found");
        }
        if (to == from) {
            throw new IllegalArgumentException("You cannot transfer to your own account");
        }
        if (!from.hasSufficientFunds(amount)) {
            throw new IllegalStateException("Insufficient Funds");
        }
        from.withdraw(amount, Transaction.Type.TRANSFER_OUT, "To " + to.getUserId());
        to.deposit(amount, Transaction.Type.TRANSFER_IN, "From " + from.getUserId());
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
