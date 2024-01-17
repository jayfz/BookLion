package co.harborbytes.booklion.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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

}
