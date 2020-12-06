package com.github.snqlby.guardbot.data;

import lombok.Getter;

@Getter
public enum ParameterData {
	MODULE_CAPTCHA_ENABLED("module.captcha.enabled", Boolean.class, "Captcha is active on join. Args: <true/false>"),
	MODULE_DELETE_JOIN_MESSAGE_ENABLED("module.delete_join_message.enabled", Boolean.class, "Remote join messages. Args: <true/false>"),
	MODULE_DELETE_FORWARD_MESSAGE_ENABLED("module.delete_forward_message.enabled", Boolean.class, "Remove forwarded messages. Args: <true/false>"),
	MODULE_DELETE_FORWARD_MESSAGE_FILTER("module.delete_forward_message.filter", String[].class, "Allow forwarded messages with keywords: <a,b,c>");

	private final String name;

	private final Class<?> type;

	private final String description;

	private final boolean visible;

	ParameterData(String name, Class<?> type, String description) {
		this.name = name;
		this.type = type;
		this.description = description;
		this.visible = true;
	}

	ParameterData(String name, Class<?> type, String description, boolean visible) {
		this.name = name;
		this.type = type;
		this.description = description;
		this.visible = visible;
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
