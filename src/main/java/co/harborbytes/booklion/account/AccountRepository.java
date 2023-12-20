package co.harborbytes.booklion.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    //@Modifying where needed

    Optional<Account> findByNumber(String number);
    Page<Account> findAllById(Long id, Pageable page);
}
