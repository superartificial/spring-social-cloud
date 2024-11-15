package nz.clemwhite.spring_social_cloud.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import nz.clemwhite.spring_social_cloud.fomanticUI.Toast;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("user")
@Log4j2
@Value
public class AppUserController {

    AppUserService appUserService;

    record SignUpForm(
            @NotBlank
            String username,
            @Size(min = 4, message = "password must be at least {min} characters")
            String password
    ) {}

    @ModelAttribute
    AppUser appUser(@AuthenticationPrincipal AppUser appUser) {
        return appUser;
    }

    @GetMapping
    String showUserInfo() {
        return "app-user/user";
    }

    @GetMapping("/sign-up")
    String showSignUp(@ModelAttribute SignUpForm signUpForm) {
        System.out.println("going to sign up page I hope");
        return "app-user/signup";
    }

    @PostMapping("/sign-up")
    String signUp(@Valid SignUpForm signUpForm, Errors errors, RedirectAttributes attributes) {
        log.info("Posted signup: {}",signUpForm);
        if(errors.hasErrors()) {
            return "app-user/signup";
        }
        try {
            appUserService.createUser(signUpForm.username, signUpForm.password);
            attributes.addFlashAttribute("toast", Toast.success("User sign up", "User " + signUpForm.username + " was created"));
            return "redirect:/login";
        } catch (Exception e) {
            attributes.addFlashAttribute("toast", Toast.error("User sign up", e.getMessage()));
            return "redirect:/user/sign-up";
        }
    }
}
