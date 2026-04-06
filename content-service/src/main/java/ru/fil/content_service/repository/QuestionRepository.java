package ru.fil.content_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.fil.content_service.entity.Question;
import ru.fil.content_service.entity.QuestionDifficulty;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByTopicIdInAndDifficultyIn(List<UUID> topicIds, List<QuestionDifficulty> difficulties);
}
