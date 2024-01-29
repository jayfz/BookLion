package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.annotations.NamedNativeQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;



@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    Page<Transaction> findTransactionsByUserId(Long userId, Pageable pageable);
    List<Transaction> findAllTransactionsByUserIdAndCreatedAtAfter(Long userId, Instant date);
    Optional<Transaction> findTransactionByIdAndUserId(Long id, Long userId);
    void deleteAllByUserId(Long id);
    void deleteTransactionByIdAndUserId(Long id, Long userId);


    @Query("SELECT new co.harborbytes.booklion.transaction.AccountTransactionLedger (t.createdAt, t.description, a.accountType, tl.debitAmount, tl.creditAmount, t.id) " +
            "FROM TransactionLine tl INNER JOIN tl.account a INNER JOIN tl.transaction t where t.user.id  = :userId AND a.number = :accountNumber")
    List<AccountTransactionLedger> findTransactionsByUserIdAndAccountNumber(@Param("userId")Long userId, @Param("accountNumber") String accountNumber);

    @Query("SELECT new co.harborbytes.booklion.transaction.AccountTransactionLedger (t.createdAt, t.description, a.accountType, tl.debitAmount, tl.creditAmount, t.id) " +
            "FROM TransactionLine tl INNER JOIN tl.account a INNER JOIN tl.transaction t where t.user.id  = :userId AND a.number = :accountNumber AND t.createdAt BETWEEN :startDate AND :endDate")
    List<AccountTransactionLedger> findTransactionsByUserIdAndAccountNumberBetweenDates(@Param("userId")Long userId, @Param("accountNumber") String accountNumber, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

//    @Query("SELECT new co.harborbytes.booklion.transaction.BalanceParts(a.name, a.accountType, SUM(tl.debitAmount), SUM(tl.creditAmount))  FROM TransactionLine tl RIGHT JOIN  tl.account a INNER JOIN tl.transaction t WHERE t.user.id = :userId AND a.accountType IN :accountTypes AND t.createdAt BETWEEN :startDate AND :endDate OR t.createdAt = NULL GROUP BY a.name, a.accountType")
//    List<BalanceParts> queryAccountSummary(@Param("userId") Long userId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate, @Param("accountTypes")List<AccountType> accountTypes);

//    @Query("SELECT new co.harborbytes.booklion.transaction.BalanceParts(a.name, a.accountType, SUM(tl.debitAmount), SUM(tl.creditAmount)) FROM TransactionLine tl RIGHT JOIN tl.account a LEFT JOIN tl.transaction t WHERE (t.user.id = :userId OR t.user.id IS NULL) AND (a.accountType IN :accountTypes) GROUP BY a.name, a.accountType")
//
    @Query("SELECT new co.harborbytes.booklion.transaction.BalanceParts(a.name, a.accountType, " +
            "(SELECT COALESCE(SUM(tl.debitAmount), 0) FROM TransactionLine tl JOIN tl.transaction t WHERE (tl.account.id = a.id) AND (t.user.id = u.id)), " +
            "(SELECT COALESCE(SUM(tl.creditAmount), 0) FROM TransactionLine tl JOIN tl.transaction t WHERE (tl.account.id = a.id) AND (t.user.id = u.id)) )" +
            "FROM Account a, User u WHERE u.id = :userId and a.accountType IN :accountTypes " +
            "ORDER BY a.number ASC")

//    @Query(value = "SELECT (a.name, a.type," +
//            "(select coalesce(SUM(tl2.debit_amount), 0) from transaction_line tl2 inner join \"transaction\" t on t.id = tl2.transaction_id  where tl2.account_id = a.id and t.user_id = u.id), " +
//            "(select coalesce (SUM(tl2.credit_amount), 0) from transaction_line tl2 inner join \"transaction\" t on t.id = tl2.transaction_id  where tl2.account_id = a.id and t.user_id = u.id) )" +
//            "from account a cross join users u where u.id= :userId and a.type in :accountTypes", nativeQuery = true)
    List<BalanceParts> queryAccountSummary(@Param("userId") Long userId, @Param("accountTypes")List<AccountType> accountTypes);






}
