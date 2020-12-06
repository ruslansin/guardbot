package com.github.snqlby.guardbot.repository;

import com.github.snqlby.guardbot.database.model.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParameterRepository extends JpaRepository<Parameter, Long> {

	Optional<Parameter> findParameterByNameAndChatId(String name, Long chatId);

	List<Parameter> findParametersByChatId(Long chatId);
}
