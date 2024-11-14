package nz.clemwhite.spring_social_cloud.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("user")
public class AppUserController {

    @ModelAttribute
    AppUser appUser(@AuthenticationPrincipal AppUser appUser) {
        return appUser;
    }

    @GetMapping
    String showUserInfo() {
        return "app-user/user";
    }

}
