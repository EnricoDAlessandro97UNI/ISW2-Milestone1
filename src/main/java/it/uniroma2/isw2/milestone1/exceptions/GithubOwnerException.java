package it.uniroma2.isw2.milestone1.exceptions;

public class GithubOwnerException extends Exception {

	private static final long serialVersionUID = 1L;

	public GithubOwnerException() {
		super("Proprietario del repository Github necessario per le REST API non trovato");
	}
	
}
