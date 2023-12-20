package co.harborbytes.booklion.budget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

//    @Query("SELECT b from Budget b WHERE b.user.id = ?1")
    Page<Budget> findAllByUserId(Long userId, Pageable pageable);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
