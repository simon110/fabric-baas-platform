package com.anhui.fabricbaasttp.service.impl;

import com.anhui.fabricbaasttp.entity.UserEntity;
import com.anhui.fabricbaasttp.repository.UserRepo;
import com.anhui.fabricbaasweb.service.JwtUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class JwtUserDetailsServiceImpl implements JwtUserDetailsService {
    @Autowired
    private UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserEntity> userOptional = userRepo.findById(username);
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            List<GrantedAuthority> authorities = new ArrayList<>();
            user.getAuthorities().forEach(authorityName -> authorities.add(new SimpleGrantedAuthority(authorityName)));
            return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
        }
        return null;
    }
}
