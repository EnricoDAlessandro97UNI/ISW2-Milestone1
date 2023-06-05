package it.uniroma2.isw2.milestone1.exceptions;

public class RateLimitExceededGithubAPIException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public RateLimitExceededGithubAPIException() {
		super("Numero di richieste alle Github REST API raggiunto:\n" +
	            "https://docs.github.com/en/developers/apps/building-github-apps/rate-limits-for-github-apps#default-user-to-server-rate-limits-for-githubcom");
	}
	
}
