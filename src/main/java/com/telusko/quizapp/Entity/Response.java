package com.telusko.quizapp.Entity;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
//@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private Integer id;
    private String response;
}
