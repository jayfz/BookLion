package co.harborbytes.booklion.user;

import co.harborbytes.booklion.apiresponsewrapper.ApiResponseSuccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final AuthenticationService authenticationService;

    @Autowired
    public UserController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseSuccess<String> register (@RequestBody @Validated UserDTO user){
        return new ApiResponseSuccess<>(authenticationService.register(user));
    }

    @PostMapping("/auth/login")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseSuccess<TokenDTO> login (@RequestBody LoginDTO loginDTO){
        return new ApiResponseSuccess<>(authenticationService.login(loginDTO));

    }

}
