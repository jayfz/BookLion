package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.exception.DomainEntityNotFoundException;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
import co.harborbytes.booklion.exception.TransactionValidationException;
import co.harborbytes.booklion.user.User;
import co.harborbytes.booklion.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final TransactionMapper mapper;
    private final UserRepository userRepo;
    private final Validator validator;
    private final EntityManager entityManager;
    public TransactionService(TransactionRepository transactionRepo, TransactionMapper mapper, UserRepository userRepo, Validator validator, EntityManager entityManager) {
        this.transactionRepo = transactionRepo;
        this.mapper = mapper;
        this.userRepo = userRepo;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    @Transactional
    public TransactionDTO createTransaction(TransactionDTO dto, Long userId){

        User user = userRepo
                .findById(userId).orElseThrow(() -> new DomainEntityNotFoundException(User.class.getSimpleName(), "id", userId.toString()));

        Transaction transaction = mapper.dtoToTransaction(dto);
        transaction.setId(null);
        transaction.setUser(user);
        transaction.setCreatedAt(Instant.now());
        transaction.getLines().forEach(line -> line.setTransaction(transaction));

        if(!transaction.isValid())
            throw new TransactionValidationException("Transaction is invalid. Please check that the accounts in the transaction lines are unique," +
                    "that debits and credits are not both zero simultaneously," +
                    "that they are not both set simultaneously," +
                    " and that the transaction is balanced");

        transactionRepo.save(transaction);
        entityManager.refresh(transaction);
        return mapper.transactionToDto(transaction);
    }


    public Page<TransactionDTO> getTransactionsByUserId(Long userId, Pageable pageable){
        return transactionRepo
                .findTransactionsByUserId(userId, pageable)
                .map(mapper::transactionToDto);
    }

    public TransactionDTO getTransactionByIdAndUserId(Long id, Long userId){

        Transaction transaction =  transactionRepo
                .findTransactionByIdAndUserId(id, userId).orElseThrow(() -> new DomainEntityNotFoundException(Transaction.class.getSimpleName(), "id", id.toString()));

        return mapper.transactionToDto(transaction);
    }

    public TransactionDTO getTransactionById(Long id){
        Transaction transactionToFind = transactionRepo
                .findById(id).orElseThrow(() -> new DomainEntityNotFoundException(Transaction.class.getSimpleName(), "id", id.toString()));

        return  mapper.transactionToDto(transactionToFind);
    }

    @Transactional
    public TransactionDTO updateTransactionDescription(Long id, Map<String, Object> incompleteTransaction){


        Transaction transactionToUpdate = transactionRepo
                .findById(id).orElseThrow(() -> new DomainEntityNotFoundException(Transaction.class.getSimpleName(), "id", id.toString()));

        if(!incompleteTransaction.keySet().contains("description"))
            return mapper.transactionToDto(transactionToUpdate);

        Object description = incompleteTransaction.get("description");
        if(description == null)
            transactionToUpdate.setDescription(null);
        else
            transactionToUpdate.setDescription(description.toString());

        BindingResult result = new BeanPropertyBindingResult(transactionToUpdate, "transactionToUpdate");

        validator.validate(transactionToUpdate, result);

        if(result.hasErrors()){
            throw new DomainEntityValidationException(result);
        }

        return mapper.transactionToDto(transactionToUpdate);
    }

    @Transactional
    public void deleteTransactionsByUserId(Long id){
        transactionRepo.deleteAllByUserId(id);
    }

    @Transactional
    public void deleteTransactionByIdAndUserId(Long id, Long userId){
        transactionRepo.deleteTransactionByIdAndUserId(id, userId);
    }
}
