package com.example.demo.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserState {
    private FormStep step = FormStep.NONE;
    private String name;
    private String email;
}
