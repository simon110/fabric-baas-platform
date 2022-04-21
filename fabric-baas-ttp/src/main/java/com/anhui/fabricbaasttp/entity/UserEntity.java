package com.anhui.fabricbaasttp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {
    @Id
    private String username;

    private String password;

    private List<String> authorities;
}