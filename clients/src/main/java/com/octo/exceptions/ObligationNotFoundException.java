package com.octo.exceptions;

public class ObligationNotFoundException extends RuntimeException {

    private final String id;

    public ObligationNotFoundException(String id)
    {
        super("Obligation could not be found with id: " + id);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
