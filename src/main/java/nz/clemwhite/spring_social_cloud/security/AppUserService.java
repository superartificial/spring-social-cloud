package nz.clemwhite.spring_social_cloud.security;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import nz.clemwhite.spring_social_cloud.security.data.AuthorityEntity;
import nz.clemwhite.spring_social_cloud.security.data.AuthorityEntityRespository;
import nz.clemwhite.spring_social_cloud.security.data.UserEntity;
import nz.clemwhite.spring_social_cloud.security.data.UserEntityRespository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
//@Value can no longer use this lombok annotation because it makes the class final
@Data
@RequiredArgsConstructor
public class AppUserService implements UserDetailsManager {
    private final PasswordEncoder passwordEncoder;
//    private final Map<String,AppUser> users = new HashMap<String,AppUser>();
    private final UserEntityRespository userEntityRespository;
    private final AuthorityEntityRespository authorityEntityRespository;

    private final Executor executor; // so we can create a user on another thread after they have signed in

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userEntityRespository
                .findById(username)
                .map(ue -> AppUser
                        .builder()
                        .username(ue.getUsername())
                        .password(ue.getPassword())
                        .name(ue.getName())
                        .imageUrl(ue.getImageUrl())
                        .provider(ue.getProvider())
                        .authorities(ue
                                .getUserAuthorities()
                                .stream()
                                .map(a -> new SimpleGrantedAuthority(a.getAuthority().getName()))
                                .toList()
                                )
                        .build()

                )
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User %s not found",username)));
    }

    @Transactional
    OAuth2UserService<OidcUserRequest, OidcUser> oidcLoginHandler() {
        return userRequest -> {
            LoginProvider provider = getProvider(userRequest);
            OidcUserService delegate = new OidcUserService();
            OidcUser oidcUser = delegate.loadUser(userRequest);
            AppUser appUser = AppUser
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
            saveOauth2User(appUser);
            return appUser;
        };
    }

    private static LoginProvider getProvider(OidcUserRequest userRequest) {
        return LoginProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());
    }

    @Transactional
    OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2LoginHandler() {
        return userRequest -> {
            LoginProvider provider = getProvider(userRequest);
            DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);
            AppUser appUser = AppUser
                    .builder()
                    .provider(provider)
                    .username(oAuth2User.getAttribute("login"))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .name(oAuth2User.getAttribute("login"))
                    .imageUrl(oAuth2User.getAttribute("avatar_url"))
                    .attributes(oAuth2User.getAttributes())
                    .authorities(oAuth2User.getAuthorities())
                    .build();

            saveOauth2User(appUser);

            return appUser;
        };
    }

    private void saveOauth2User(AppUser appUser) {
        CompletableFuture.runAsync(() -> createUser(appUser),executor);
    }

    private LoginProvider getProvider(OAuth2UserRequest userRequest) {
        return LoginProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());
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

    @Transactional
    private void createUser(AppUser user) {
        UserEntity userEntity = saveUserIfNotExists(user);
        List<AuthorityEntity> authorities = user
                .authorities
                .stream()
                .map(a -> saveAuthorityIfNotExists(a.getAuthority(), user.getProvider()))
                .toList();
        userEntity.mergeAuthorities(authorities);
        userEntityRespository.save(userEntity);
    }

    @Transactional
    private AuthorityEntity saveAuthorityIfNotExists(String authority, LoginProvider provider) {
        return authorityEntityRespository
                .findByName(authority)
                .orElseGet(() -> authorityEntityRespository
                        .save(
                                new AuthorityEntity(authority,provider)
                        )
                );
    }

    @Transactional
    private UserEntity saveUserIfNotExists(AppUser user) {
        return userEntityRespository
                .findById(user.getUsername())
                .orElseGet(
                        () -> userEntityRespository
                                .save(
                                        new UserEntity(
                                                user.getUsername(),
                                                user.getPassword(),
                                                user.getEmail(),
                                                user.getName(),
                                                user.getImageUrl(),
                                                user.getProvider()
                                        )
                                )

                );
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
    @Transactional
    public void deleteUser(String username) {
        if(userExists(username)) {
            userEntityRespository.deleteById(username);
        }
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AppUser currentUser = (AppUser)authentication.getPrincipal();
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            throw new IllegalArgumentException("Old password is not correct");
        }
//        users.get(currentUser.getUsername()).setPassword(passwordEncoder.encode(newPassword));
        userEntityRespository
            .findById((currentUser.getUsername()))
            .ifPresent(ue -> ue.setPassword(passwordEncoder.encode(newPassword)));

    }

    @Transactional
    @Override
    public boolean userExists(String username) {
        return userEntityRespository.existsById(username);
    }
}
