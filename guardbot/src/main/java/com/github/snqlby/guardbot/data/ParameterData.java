package com.github.snqlby.guardbot.data;

import lombok.Getter;

@Getter
public enum ParameterData {
	MODULE_CAPTCHA_ENABLED("module.captcha.enabled", Boolean.class),
	MODULE_DELETE_JOIN_MESSAGE_ENABLED("module.delete_join_message.enabled", Boolean.class),
	MODULE_DELETE_FORWARD_MESSAGE_ENABLED("module.delete_forward_message.enabled", Boolean.class);

	private final String name;

	private final Class<?> type;

	ParameterData(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}

	public static ParameterData findByName(String name) {
		for (ParameterData value : values()) {
			if (value.getName().equals(name)) {
				return value;
			}
		}
		return null;
	}
}
