package nz.clemwhite.spring_social_cloud.security;

import jakarta.annotation.PostConstruct;
import lombok.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Value
public class AppUserService implements UserDetailsManager {
    PasswordEncoder passwordEncoder;
    Map<String,AppUser> users = new HashMap<String,AppUser>();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.get(username);
    }

    OAuth2UserService<OidcUserRequest, OidcUser> oidcLoginHandler() {
        return userRequest -> {
            LoginProvider provider = getProvider(userRequest);
            OidcUserService delegate = new OidcUserService();
            OidcUser oidcUser = delegate.loadUser(userRequest);
            return AppUser
                    .builder()
                    .username(oidcUser.getEmail())
                    .name(oidcUser.getFullName())
                    .provider(provider)
                    .email(oidcUser.getEmail())
                    .userId(oidcUser.getName())
                    .imageUrl(oidcUser.getAttribute("picture"))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .attributes(oidcUser.getAttributes())
                    .authorities(oidcUser.getAuthorities())
                    .build();
        };
    }

    private static LoginProvider getProvider(OidcUserRequest userRequest) {
        return LoginProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());
    }

    OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2LoginHandler() {
        return userRequest -> {
            LoginProvider provider = getProvider(userRequest);
            DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);
            return AppUser
                    .builder()
                    .provider(provider)
                    .username(oAuth2User.getAttribute("login"))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .name(oAuth2User.getAttribute("login"))
                    .imageUrl(oAuth2User.getAttribute("avatar_url"))
                    .attributes(oAuth2User.getAttributes())
                    .authorities(oAuth2User.getAuthorities())
                    .build();
        };
    }

    private LoginProvider getProvider(OAuth2UserRequest userRequest) {
        return LoginProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());
    }

    @PostConstruct
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

        createUser(bob);
        createUser(bill);

    }

    public void createUser(String username, String password) {
        createUser(
            User
            .builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .authorities(Collections.emptyList())
            .build());
    }

    private void createUser(AppUser user) {
        users.putIfAbsent(user.getUsername(),user);
    }

    @Override
    public void createUser(UserDetails user) {

        if (userExists(user.getUsername())) {
            throw new IllegalArgumentException(String.format("User %s already exists!", user.getUsername()));
        }

        createUser(AppUser
                .builder()
                .provider(LoginProvider.APP)
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .build());
    }

    @Override
    public void updateUser(UserDetails user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUser(String username) {
        if(userExists(username)) {
            users.remove(username);
        }
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {

    }

    @Override
    public boolean userExists(String username) {
        return users.containsKey(username);
    }
}
