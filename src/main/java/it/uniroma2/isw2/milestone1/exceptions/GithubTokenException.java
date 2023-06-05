package it.uniroma2.isw2.milestone1.exceptions;

public class GithubTokenException extends Exception {

	private static final long serialVersionUID = 1L;

	public GithubTokenException() {
		super("Token Github necessario per le REST API non trovato");
	}
	
}
