package com.octo.exceptions;

public class CompteNonExistantException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CompteNonExistantException() {
  }

  public CompteNonExistantException(String message) {
    super(message);
  }
}
