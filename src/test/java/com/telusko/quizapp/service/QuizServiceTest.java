package com.telusko.quizapp.service;

import com.telusko.quizapp.dao.QuizDao;
import com.telusko.quizapp.dao.QuestionDao;
import com.telusko.quizapp.Entity.Response;
import com.telusko.quizapp.Entity.Question;
import com.telusko.quizapp.Entity.Quiz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.telusko.quizapp.Entity.QuestionWrapper;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    QuizDao quizDao;            // fake DAO – no database

    @Mock
    QuestionDao questionDao;

    @InjectMocks
    QuizService quizService;    // real service, with the fakes plugged in

    @BeforeEach
    void setUp() {
        Question q1 = new Question();
        q1.setId(2);
        q1.setRightAnswer("127");

        Question q2 = new Question();
        q2.setId(12);
        q2.setRightAnswer("int");

        Quiz quiz = new Quiz();
        quiz.setId(1);
        quiz.setQuestions(List.of(q1, q2));

        // "When the service asks for quiz 1, return this fake quiz"
        lenient().when(quizDao.findById(1)).thenReturn(Optional.of(quiz));
    }

    @Test
    void allCorrect_returnsFullScore() {
        List<Response> answers = List.of(
                new Response(2, "127"),
                new Response(12, "int"));

        ResponseEntity<Integer> result = quizService.calculateResult(1, answers);

        assertEquals(2, result.getBody());
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void anyOrder_stillCorrect() {
        List<Response> answers = List.of(
                new Response(12, "int"),     // reversed order
                new Response(2, "127"));

        assertEquals(2, quizService.calculateResult(1, answers).getBody());
    }

    @Test
    void wrongAnswer_notCounted() {
        List<Response> answers = List.of(
                new Response(2, "128"),      // wrong
                new Response(12, "int"));

        assertEquals(1, quizService.calculateResult(1, answers).getBody());
    }
    @Test
    void getQuizQuestions_returnsQuestionsWithoutAnswers() {
        ResponseEntity<List<QuestionWrapper>> result = quizService.getQuizQuestions(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertEquals(2, result.getBody().get(0).getId());
        assertEquals(12, result.getBody().get(1).getId());
    }

    @Test
    void creatQuiz_savesQuizWithRandomQuestions() {
        Question q = new Question();
        q.setId(5);
        when(questionDao.findRandomQuestionsByCategory("Java", 1)).thenReturn(List.of(q));

        ResponseEntity<String> result = quizService.creatQuiz("Java", 1, "JQuiz");

        assertEquals(HttpStatus.CREATED, result.getStatusCode());

        ArgumentCaptor<Quiz> saved = ArgumentCaptor.forClass(Quiz.class);
        verify(quizDao).save(saved.capture());          // check what was saved
        assertEquals("JQuiz", saved.getValue().getTitle());
        assertEquals(1, saved.getValue().getQuestions().size());
    }
}