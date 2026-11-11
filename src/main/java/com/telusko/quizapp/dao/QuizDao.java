package com.telusko.quizapp.dao;

import com.telusko.quizapp.Entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;


public interface QuizDao extends JpaRepository<Quiz, Integer> {
}
