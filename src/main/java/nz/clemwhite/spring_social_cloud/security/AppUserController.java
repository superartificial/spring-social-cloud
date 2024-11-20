package nz.clemwhite.spring_social_cloud.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import nz.clemwhite.spring_social_cloud.fomanticUI.Toast;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

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
    ) { }

    record ChangePasswordForm(
        @NotBlank
        String oldPassword,
        @NotBlank
        String password,
        @NotBlank
        String passwordConfirm
    ){
        @AssertTrue(message = "passwords should match")
        public boolean isSamePassword() { // needs to start with "is" and match samePassword error reference in the html
            return Objects.nonNull(password)
                    &&
                    Objects.nonNull(passwordConfirm)
                    &&
                    password.equals(passwordConfirm);
        }
    }

    @ModelAttribute
    AppUser appUser(@AuthenticationPrincipal AppUser appUser) {
        return appUser;
    }

    @GetMapping
    String showUserInfo(@ModelAttribute ChangePasswordForm changePasswordForm) {
        return "app-user/user";
    }

    @GetMapping("/sign-up")
    String showSignUp(@ModelAttribute SignUpForm signUpForm) {
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

    @PatchMapping("/password")
    String changePassword(@Valid ChangePasswordForm changePasswordForm, Errors errors, RedirectAttributes attributes, Model model) {
        if(errors.hasErrors()) {
            model.addAttribute("tab","pass");
            return "app-user/user";
        }
        try {
            appUserService.changePassword(changePasswordForm.oldPassword, changePasswordForm.password);
            attributes.addFlashAttribute("toast",Toast.success("Password","Password changed"));
            return "redirect:/user";
        } catch (Exception e) {
            attributes.addFlashAttribute("tab","pass");
            attributes.addFlashAttribute("toast",Toast.error("Password",e.getMessage()));
            return "redirect:/user";
        }
    }

}
