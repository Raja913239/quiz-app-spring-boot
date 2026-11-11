package com.telusko.quizapp.controller;

import com.telusko.quizapp.service.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QuizService quizService;

    @Test
    void submitQuiz_returnsScore() throws Exception {
        when(quizService.calculateResult(eq(1), anyList()))
                .thenReturn(new ResponseEntity<>(2, HttpStatus.OK));

        mockMvc.perform(post("/quiz/submit/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":2,\"response\":\"127\"},{\"id\":12,\"response\":\"int\"}]"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }
}