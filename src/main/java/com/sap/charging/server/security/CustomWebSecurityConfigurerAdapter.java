package com.sap.charging.server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class CustomWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    // @Autowired private CustomBasicAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    	// Example of Basic authentication, if needed:
    	// auth
    	// .inMemoryAuthentication()
        //  .withUser("user1")
        //  .password(passwordEncoder().encode("BE4kpTZHkCpMMVj38zpj"))
    	//  .authorities("ROLE_USER");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	// Example of Basic authentication, if needed:
        // http
    	//   .authorizeRequests()
    	//   .antMatchers("/actuator/*")
    	//   .permitAll()
    	//   .anyRequest()
    	//   .authenticated()
    	//   .and()
    	//   .httpBasic() 
    	//   .authenticationEntryPoint(authenticationEntryPoint)
    	//   .and()
    	//   .csrf().disable();
          
    	//  http.addFilterAfter(new CustomFilter(), BasicAuthenticationFilter.class);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}