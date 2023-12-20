package co.harborbytes.booklion.user;

import co.harborbytes.booklion.budget.Budget;
import co.harborbytes.booklion.budget.BudgetDTO;
import org.mapstruct.*;
@Mapper(componentModel = "spring")

public interface UserMapper {
        User dtoToUser (UserDTO dto);
        UserDTO userToDto(User user);
        void updateUserFromDto(UserDTO dto, @MappingTarget User user);
}
