package co.harborbytes.booklion.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 2, max = 128)
    @Column(name = "first_name", length = 128)
    private String firstName;

    @NotNull
    @Size(min = 2, max = 128)
    @Column(name = "last_name", length = 128)
    private String lastName;

    @NotEmpty
    private String password;

    @NotNull
    @Email
    private String email;

    @NotNull
    @Column(name = "role")
    @Enumerated(EnumType.ORDINAL)
    private Role role;

    @NotNull
    @Column(name = "is_account_non_expired")
    private boolean isAccountNonExpired = true;

    @NotNull
    @Column(name = "is_account_non_locked")
    private boolean isAccountNonLocked = true;

    @NotNull
    @Column(name = "is_credentials_non_expired")
    private boolean isCredentialsNonExpired = true;

    @NotNull
    @Column(name = "is_enabled")
    private boolean isEnabled = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isAccountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isAccountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isCredentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}
