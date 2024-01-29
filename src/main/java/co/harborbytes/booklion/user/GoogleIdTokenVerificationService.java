package co.harborbytes.booklion.user;

import co.harborbytes.booklion.exception.DomainEntityNotFoundException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleIdTokenVerificationService {

    private String CLIENT_ID;

    @Autowired
    public GoogleIdTokenVerificationService(@Value("${google-client-id}") String CLIENT_ID) {
        this.CLIENT_ID = CLIENT_ID;
    }

    public User verify(String token) throws DomainEntityNotFoundException {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(CLIENT_ID))
                    .build();
            GoogleIdToken idToken = verifier.verify(token);
            Payload payload = idToken.getPayload();
            boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());

            User user = new User();
            user.setRole(Role.USER);
            user.setEmail(payload.getEmail());
            user.setFirstName((String) payload.get("name"));
            user.setLastName((String) payload.get("family_name"));
            user.setPassword(payload.getSubject());
            return user;

        } catch (Exception ex) {
            throw new DomainEntityNotFoundException(this.getClass().getSimpleName(), "google-id-token", token);
        }
    }
}
