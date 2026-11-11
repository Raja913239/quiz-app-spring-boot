package com.telusko.quizapp.service;


import com.telusko.quizapp.Entity.Question;
import com.telusko.quizapp.Entity.QuestionWapper;
import com.telusko.quizapp.Entity.Quiz;
import com.telusko.quizapp.Entity.Response;
import com.telusko.quizapp.dao.QuestionDao;
import com.telusko.quizapp.dao.QuizDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class QuizService {

    @Autowired
    QuizDao quizDao;

    @Autowired
    QuestionDao questionDao;

    public ResponseEntity<String> creatQuiz(String category, int numQ, String title) {

        List<Question> questions = questionDao.findRandomQuestionsByCategory(category, numQ);

        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setQuestions(questions);
        quizDao.save(quiz);

        return new ResponseEntity<>("Your code is working", HttpStatus.CREATED);

    }

    public ResponseEntity<List<QuestionWapper>> getQuizQuestions(Integer id) {

        Optional<Quiz> quiz = quizDao.findById(id);
        List<Question> questionFromDB = quiz.get().getQuestions();
        List<QuestionWapper> questionForUser = new ArrayList<>(); // Empty Array List
        for(Question q : questionFromDB) {
            QuestionWapper qw = new QuestionWapper(q.getId(),q.getQuestionTitle(),q.getOption1(),q.getOption2(),q.getOption3(),q.getOption4());
            questionForUser.add(qw);
        }

        return new ResponseEntity<>(questionForUser,HttpStatus.OK);
    }

    public ResponseEntity<Integer> calculateResult(Integer id, List<Response> response) {
        Quiz quiz = quizDao.findById(id).get();
        List<Question> questions = quiz.getQuestions();

        Map<Integer, String> rightAnswers = new HashMap<>();
        for (Question q : questions) {
            rightAnswers.put(q.getId(), q.getRightAnswer());
        }

        int right = 0;
        for (Response r : response) {
            String correct = rightAnswers.get(r.getId());
            if (correct != null && correct.equals(r.getResponse())) {
                right++;
            }
        }
        return new ResponseEntity<>(right, HttpStatus.OK);
    }
}
