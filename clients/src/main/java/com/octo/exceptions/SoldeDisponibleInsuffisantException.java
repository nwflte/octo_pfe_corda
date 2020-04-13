package com.octo.exceptions;

public class SoldeDisponibleInsuffisantException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SoldeDisponibleInsuffisantException() {
  }

  public SoldeDisponibleInsuffisantException(String message) {
    super(message);
  }
}
