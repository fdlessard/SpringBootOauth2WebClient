package io.fdlessard.codebites.oauth2.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Slf4j
@Configuration
//@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${spring.security.user.name}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        log.info("SecurityConfig.configure()");

        http.authorizeRequests()
                //   .antMatchers("/actuator/health").permitAll()
                .antMatchers("/WebClientMessage").authenticated()
                .and()
                .httpBasic()
                //.disable()
        ;
        http.csrf().disable();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

        log.info("SecurityConfig.configure(AuthenticationManagerBuilder)");

        auth.inMemoryAuthentication()
                .withUser(username)
                .password("{noop}" + password)
                .roles("USER");
    }
}