package nz.clemwhite.spring_social_cloud.security;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

// @Service
//@Value
@RequiredArgsConstructor
public class UserCreatorService {

    private final PasswordEncoder passwordEncoder;
    private final AppUserService appUserService;

    // @PostConstruct
    @Transactional
    private void createHardcodedUsers() {
        var bill = AppUser
                .builder()
                .username("bill")
                .password(passwordEncoder.encode("321"))
                .authorities(List.of(new SimpleGrantedAuthority("read")))
                .build();
        var bob = AppUser
                .builder()
                .username("bob")
                .password(passwordEncoder.encode("1234"))
                .authorities(List.of(new SimpleGrantedAuthority("read")))
                .build();

        appUserService.createUser(bob);
        appUserService.createUser(bill);

    }

}
