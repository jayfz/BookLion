package co.harborbytes.booklion.user;

import co.harborbytes.booklion.account.Account;
import co.harborbytes.booklion.account.AccountRepository;
import co.harborbytes.booklion.account.AccountType;
import co.harborbytes.booklion.budget.Budget;
import co.harborbytes.booklion.budget.BudgetRepository;
import co.harborbytes.booklion.transaction.Transaction;
import co.harborbytes.booklion.transaction.TransactionLine;
import co.harborbytes.booklion.transaction.TransactionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PopulateDatabase {

    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private PasswordEncoder passwordEncoder;
    private BudgetRepository budgetRepository;

    @Autowired
    public PopulateDatabase(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository, PasswordEncoder passwordEncoder, BudgetRepository budgetRepository) {

        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.budgetRepository = budgetRepository;

    }

    @PostConstruct
    public void init() throws FileNotFoundException {
        User user = new User();
        user.setRole(Role.USER);
        user.setFirstName("Carlitos");
        user.setLastName("Baccencio");
        user.setEmail("carlos.bacca@gmail.com");
        user.setPassword(passwordEncoder.encode("123"));
        userRepository.save(user);

        addAccounts(user);
        addTransactions(user);
        addBudgets(user);

    }

    public void addAccounts(User user) throws FileNotFoundException {
        FileReader reader = new FileReader("/home/titan/Downloads/accounts.csv");
        Scanner scanner = new Scanner(reader);

        Map<String, BigDecimal> balance = new HashMap<>();

        while (scanner.hasNextLine()){
            String line = scanner.nextLine();
            String[] parts = line.split(",");

            Account account = new Account();
            account.setNumber(parts[0]);
            account.setName(parts[1]);
            accountRepository.save(account);

            if(parts[2].equals("0") || parts[2].equals("0.00"))
                continue;

            balance.put(account.getNumber(), new BigDecimal(parts[2]));
        }

        final Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCreatedAt(Instant.parse("2023-12-31T23:59:59Z"));
        final List<TransactionLine> lines = new ArrayList<>();

        accountRepository.findAll().stream().filter(account -> account.getNumber().matches("^[12]\\d+$")).forEach((account -> {


            if(balance.get(account.getNumber()) == null)
                return;

            TransactionLine tline = new TransactionLine();
            tline.setAccount(account);
            tline.setTransaction(transaction);

            if (account.getAccountType() == AccountType.ASSETS) {
                tline.setDebitAmount(balance.get(account.getNumber()));
                tline.setCreditAmount(new BigDecimal("0.00"));
            } else {
                tline.setCreditAmount(balance.get(account.getNumber()));
                tline.setDebitAmount(new BigDecimal("0.00"));
            }

            lines.add(tline);

        }));



        TransactionLine tline = new TransactionLine();
        Account account = accountRepository.findByNumber("300").get();
        tline.setAccount(account);
        tline.setDebitAmount(new BigDecimal("0.00"));

        tline.setCreditAmount(balance.get("300"));
        tline.setTransaction(transaction);
        lines.add(tline);

        transaction.setLines(lines);
        transaction.setDescription("Initial invested capital");
        transactionRepository.save(transaction);
        scanner.close();
    }

    public void addBudgets(User user) {
        //besides budgets, we can also have funds...
        Budget budget = new Budget();
        Account familyAndFriendsExpensesAccount = accountRepository.findByNumber("513").get();
        budget.setAccount(familyAndFriendsExpensesAccount);
        budget.setDescription("F&F expenses monitoring");
        budget.setAmount(new BigDecimal("250000.00"));
        budget.setUser(user);
        budgetRepository.save(budget);

    }

    public void addTransactions(User user) throws FileNotFoundException {
        /* second part */

        FileReader reader = new FileReader("/home/titan/Downloads/transactions.csv");
        Scanner scanner = new Scanner(reader);


        Transaction transaction = new Transaction();
        List<TransactionLine> lines = new ArrayList<>();

        while (scanner.hasNextLine()){
            String line = scanner.nextLine();
            String[] parts = line.split(",");

            if (parts.length == 0  || parts[0].equals("")) {
                continue;
            }

            boolean isDateRow = parts[0].matches("\\d+-\\d+-\\d+");
            if (isDateRow) {
                transaction.setCreatedAt(Instant.parse(String.format("%sT00:00:00-05:00", parts[0])));
            }

            boolean isAmountRow = Arrays.stream(parts).allMatch(p -> p.matches("\\d+(\\.\\d+)?"));

            if (isAmountRow) {
                TransactionLine tline = new TransactionLine();
                Account account = accountRepository.findByNumber(parts[0]).get();
                tline.setAccount(account);
                tline.setDebitAmount(new BigDecimal(parts[1]));
                tline.setCreditAmount(new BigDecimal(parts[2]));
                tline.setTransaction(transaction);
                lines.add(tline);
            }

            boolean isDescription = !isAmountRow && !isDateRow;
            if (isDescription) {
                transaction.setDescription(parts[0]);
                transaction.setLines(lines);
                transaction.setUser(user);
                transactionRepository.save(transaction);
                transaction = new Transaction();
                lines = new ArrayList<>();

            }
        }

        scanner.close();
    }
}


