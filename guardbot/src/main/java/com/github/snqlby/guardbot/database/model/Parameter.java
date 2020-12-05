package com.github.snqlby.guardbot.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "guard_parameters")
@Data
public class Parameter implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "guard_parameters_seq_gen")
	@SequenceGenerator(name = "guard_parameters_seq_gen", sequenceName = "guard_parameters_id_seq")
	private Long id;

	private Long chatId;

	private String name;

	@NotNull
	private Class<?> type;

	private String value;
}
