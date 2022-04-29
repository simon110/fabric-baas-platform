package com.anhui.fabricbaasweb.configuration;

import com.anhui.fabricbaasweb.bean.AntMatchers;
import com.anhui.fabricbaasweb.filter.JwtFilter;
import com.anhui.fabricbaasweb.filter.DosFilter;
import com.anhui.fabricbaasweb.service.JwtUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PropertySource("classpath:fabricbaasweb.properties")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Autowired
    private JwtFilter jwtFilter;
    @Autowired
    private DosFilter dosFilter;
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private AntMatchers antMatchers;


    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().cors()
                .and().authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(dosFilter, JwtFilter.class)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        List<String> list = new ArrayList<>();
        Collections.addAll(list,
                "/favicon.ico",
                "/swagger-ui/*",
                "/swagger-resources/**",
                "/v3/api-docs");
        Collections.addAll(list, antMatchers.get());
        String[] antMatchers = new String[list.size()];
        list.toArray(antMatchers);
        web.ignoring().antMatchers(antMatchers);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(jwtUserDetailsService);
    }
}
