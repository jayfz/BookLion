package co.harborbytes.booklion.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {

    @NotNull
    @Email
    private String email;

    @NotEmpty
    private String password;
}
