package co.harborbytes.booklion.user;

import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final String userCreatedSuccesfully = "User registered succesfully";
    private final AuthenticationManager authenticationManager;
    private final GoogleIdTokenVerificationService googleIdTokenVerificationService;

    @Autowired
    public AuthenticationService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil, AuthenticationManager authenticationManager, GoogleIdTokenVerificationService googleIdTokenVerificationService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.authenticationManager = authenticationManager;
        this.googleIdTokenVerificationService = googleIdTokenVerificationService;

    }

    @Transactional
    public String register(UserDTO userDTO) {

        userRepository.findByEmail(userDTO.getEmail()).ifPresent((foundUser) -> {
            throw new RuntimeException("User already exists, please log in instead");
        });

        User user = userMapper.dtoToUser(userDTO);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);

        return userCreatedSuccesfully;
    }

    public TokenDTO login(LoginDTO loginDTO) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDTO.getEmail(),
                loginDTO.getPassword()
        ));

        User user = userRepository.findByEmail(loginDTO.getEmail()).orElseThrow();
        String token = jwtTokenUtil.generateToken(user);
        return new TokenDTO(token);
    }

    @Transactional
    public TokenDTO googleLogin(TokenDTO googleIdToken){
        User tokenUser = this.googleIdTokenVerificationService.verify(googleIdToken.getToken());
        if(userRepository.findByEmail(tokenUser.getEmail()).isEmpty()){
            userRepository.save(tokenUser);
        }
        String token = jwtTokenUtil.generateToken(tokenUser);
        return new TokenDTO(token);
    }



}
