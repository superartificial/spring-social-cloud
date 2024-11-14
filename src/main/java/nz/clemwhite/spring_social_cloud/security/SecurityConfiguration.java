package nz.clemwhite.spring_social_cloud.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@Log4j2
public class SecurityConfiguration {

    @Bean
    @Order(0)
    SecurityFilterChain resources(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/images/**", "/**.css", "/**.js")
                .authorizeHttpRequests(c -> c.anyRequest().permitAll())
                .securityContext(c -> c.disable())
                .sessionManagement(c -> c.disable())
                .requestCache(c -> c.disable())
                .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppUserService appUserService) throws Exception {
        return http
//                .formLogin(withDefaults())
                .formLogin(c -> c.loginPage("/login")
                    .loginProcessingUrl("/authenticate")
                        .usernameParameter("user")
                        .passwordParameter("pass")
                        .defaultSuccessUrl("/user")
                )
                .logout(c -> c.logoutSuccessUrl("/?logout"))
                .oauth2Login(oc -> oc
                        .loginPage("/login")
                        .defaultSuccessUrl("/user")
                        .userInfoEndpoint(ui -> ui.userService(appUserService.oauth2LoginHandler())
                        .oidcUserService(appUserService.oidcLoginHandler())))
                .authorizeHttpRequests(c -> c
                        .requestMatchers("/", "/login").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    UserDetailsService inMemoryUsers() {
//        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
//        var bob = new User("bob", passwordEncoder().encode("1234"), Collections.emptyList());
//        var bill = User.builder().username("bill").password(passwordEncoder().encode("321")).roles("USER").authorities("read").build();
//        users.createUser(bob);
//        users.createUser(bill);
//        return users;
//    }

    @Bean
    ApplicationListener<AuthenticationSuccessEvent> successLogger() {
        return event -> {
            log.info("success: {}", event.getAuthentication());
        };
    }
}
