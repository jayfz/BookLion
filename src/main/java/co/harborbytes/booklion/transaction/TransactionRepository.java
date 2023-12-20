package co.harborbytes.booklion.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    Page<Transaction> findTransactionsByUserId(Long userId, Pageable pageable);
    Optional<Transaction> findTransactionByIdAndUserId(Long id, Long userId);
    void deleteAllByUserId(Long id);
    void deleteTransactionByIdAndUserId(Long id, Long userId);
}

