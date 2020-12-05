package com.github.snqlby.guardbot.service;

import com.github.snqlby.guardbot.data.ParameterData;
import com.github.snqlby.guardbot.database.model.Parameter;
import com.github.snqlby.guardbot.repository.ParameterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@Slf4j
public class ParameterService {

	private final ParameterRepository parameterRepository;

	public static String MODULE_CAPTCHA_ENABLED = "module.captcha.enabled";

	public ParameterService(ParameterRepository parameterRepository) {
		this.parameterRepository = parameterRepository;
	}

	public <T> T findParameterOrDefault(ParameterData parameter, Long chatId, T defaultParameter) {
		Optional<Parameter> parameterOptional = parameterRepository.findParameterByNameAndChatId(parameter.getName(), chatId);
		if (parameterOptional.isPresent()) {
			Parameter parameterEntity = parameterOptional.get();
			if (!parameter.getType().equals(parameterEntity.getType())) {
				log.error("Incorrect type: {} for parameter: {}. Expected: {}",
						parameterEntity.getType(),
						parameter.getName(),
						parameter.getType());
				throw new IllegalStateException("Incorrect type");
			}
			if (Boolean.class.equals(parameter.getType())) {
				return (T) Boolean.valueOf(parameterEntity.getValue());
			} else if (String.class.equals(parameter.getType())) {
				return (T) parameterEntity.getValue();
			} else if (String[].class.equals(parameter.getType())) {
				return (T) parameterEntity.getValue().split(",");
			}
			log.error("Unknown type: {} for parameter: {}", parameterEntity.getType(), parameter.getName());
			throw new IllegalStateException("Can't process type");
		} else {
			return defaultParameter;
		}
	}

	public Parameter createOrUpdate(ParameterData parameter, Long chatId, String value) {
		Optional<Parameter> parameterOptional = parameterRepository.findParameterByNameAndChatId(parameter.getName(), chatId);

		Parameter entityParameter = parameterOptional.orElseGet(Parameter::new);
		entityParameter.setChatId(chatId);
		entityParameter.setName(parameter.getName());
		entityParameter.setType(parameter.getType());
		entityParameter.setValue(value);

		parameterRepository.save(entityParameter);
		return entityParameter;
	}
}
