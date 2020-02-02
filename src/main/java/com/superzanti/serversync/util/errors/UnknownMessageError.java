package com.superzanti.serversync.util.errors;

import com.superzanti.serversync.util.enums.EErrorType;

public class UnknownMessageError extends MessageError {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5925745070127909014L;

	public UnknownMessageError() {
		super("", EErrorType.MESSAGE_UNKNOWN);
		this.message = "从服务器收到未知信息";
	}
	
	public UnknownMessageError(Object o) {
		super("", EErrorType.MESSAGE_UNKNOWN);
		this.message = "从服务器收到未知信息: " + o;
	}
}
