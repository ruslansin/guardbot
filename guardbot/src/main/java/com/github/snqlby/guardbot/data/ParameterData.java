package com.github.snqlby.guardbot.data;

import lombok.Getter;

@Getter
public enum ParameterData {
	MODULE_CAPTCHA_ENABLED("module.captcha.enabled", Boolean.class, "Капча при входе активна. Значения: <true/false>"),
	MODULE_DELETE_JOIN_MESSAGE_ENABLED("module.delete_join_message.enabled", Boolean.class, "Удалять сообщения о входе. Значения: <true/false>"),
	MODULE_DELETE_FORWARD_MESSAGE_ENABLED("module.delete_forward_message.enabled", Boolean.class, "Удалять ли перадресованные сообщения. Значения: <true/false>"),
	MODULE_DELETE_FORWARD_MESSAGE_FILTER("module.delete_forward_message.filter", String[].class, "Исключения для переадресованных сообщений. Значения: <a,b,c>");

	private final String name;

	private final Class<?> type;

	private final String description;

	ParameterData(String name, Class<?> type, String description) {
		this.name = name;
		this.type = type;
		this.description = description;
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
