package it.uniroma2.isw2.milestone1.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import it.uniroma2.isw2.milestone1.entities.Commit;
import it.uniroma2.isw2.milestone1.entities.Diff;
import it.uniroma2.isw2.milestone1.exceptions.GithubOwnerException;
import it.uniroma2.isw2.milestone1.exceptions.GithubTokenException;
import it.uniroma2.isw2.milestone1.exceptions.RateLimitExceededGithubAPIException;

public class GitHelper {

	private static final String REPO_OWNER_PATH = "owner.dat"; // proprietario del repository github
	private static final String TOKEN_GITHUB_PATH = "token.key"; // token github 
	private static final String CACHE_COMMIT_LIST = ".cache/commit-list/%s/%s.json"; // percorso dei file di cache per gli hash dei commit
	private static final String CACHE_COMMIT_INFO = ".cache/commit-info/%s/%s.json"; // percorso dei file contenenti le informazioni dei commit
	private static final String REMOTE_COMMIT_LIST = "https://api.github.com/repos/%s/%s/commits?per_page=100&page=%d"; // API per l'ottenimento degli hash dei commit
	private static final String REMOTE_COMMIT_INFO = "https://api.github.com/repos/%s/%s/commits/%s"; // API per l'ottenimento dell'informazioni di un commit dato il suo hash

	private String token;
	private String repoOwner;
	private String projectName;
	
	public GitHelper(String projectName) throws GithubTokenException, GithubOwnerException {
		this.projectName = projectName.toLowerCase();
		readCredentials(); // Recupera le credenziali dalla cartella locale per l'utilizzo delle API di Github
	}

	private void readCredentials() throws GithubTokenException, GithubOwnerException {
		readOwnerGithubFromLocalFile();
		readTokenGithubFromLocalFile();
	}
	
	private void readOwnerGithubFromLocalFile() throws GithubOwnerException {
		try (BufferedReader reader = new BufferedReader(new FileReader(REPO_OWNER_PATH))) {
				this.repoOwner = reader.readLine();
		} catch (IOException e) {
			throw new GithubOwnerException();
		}
	}
	
	private void readTokenGithubFromLocalFile() throws GithubTokenException {
		try (BufferedReader reader = new BufferedReader(new FileReader(TOKEN_GITHUB_PATH))) {
			this.token = reader.readLine();
		} catch (IOException e) {
			throw new GithubTokenException();
		}
	}

	/* Recupera tutti i commits fino ad una data specificata */
	public List<Commit> retrieveCommits(LocalDateTime targetDate) throws IOException, RateLimitExceededGithubAPIException {
		List<Commit> commits = new ArrayList<>();
		Iterator<String> shas = retrieveCommitsSHA().iterator();
		boolean targetDateReached = false;

		while(shas.hasNext() && !targetDateReached) {
			Commit c = retrieveCommitBySHA(shas.next());
			if (c.getDate().isAfter(targetDate))
				targetDateReached = true;
			else
				commits.add(c);
		}

		return commits;
	}
	
	/* Recupera tutti gli SHA dei commit */
	private List<String> retrieveCommitsSHA() throws IOException {
		
		List<String> commits = new ArrayList<>();
		
		int index = 1;
		int results = 0;
		do {
			String cache = String.format(CACHE_COMMIT_LIST, this.projectName, index);
			String remote = String.format(REMOTE_COMMIT_LIST, this.repoOwner, this.projectName, index);
						
			JsonArray jsonCommits = RestHelper.getJSONArray(remote, this.token, cache);
			results = jsonCommits.size();
			
			jsonCommits.forEach(element -> {
				JsonObject jsonCommit = element.getAsJsonObject();
				String sha = jsonCommit.get("sha").getAsString();
				commits.add(0, sha);
			});
			index++;
			
		} while(results > 0);

		return commits;
	}
	
	/* Recupera un commit in base al suo SHA */
	private Commit retrieveCommitBySHA(String sha) throws IOException, RateLimitExceededGithubAPIException {
		
		String cache = String.format(CACHE_COMMIT_INFO, this.projectName, sha);
		String remote = String.format(REMOTE_COMMIT_INFO, this.repoOwner, this.projectName, sha);
				
		// Recupera il commit in base allo SHA specificato e prende le informazioni relative al commit e all'autore del commit stesso
		JsonObject jsonResponse = RestHelper.getJSONObject(remote, this.token, cache);
		JsonObject jsonCommit = jsonResponse.get("commit").getAsJsonObject();
		JsonObject jsonAuthor = jsonCommit.get("author").getAsJsonObject();
		
		Commit c = new Commit();
		c.setSha(sha);
		c.setAuthor(jsonAuthor.getAsJsonObject().get("name").getAsString());
		c.setDate(jsonAuthor.get("date").getAsString());
		c.setMessage(jsonCommit.get("message").getAsString());
		
		// Recupera i file toccati dal commit
		JsonArray jsonDiffs = jsonResponse.get("files").getAsJsonArray();
		// Per ogni file, se è un file java, recupera il nome del file, le righe aggiunte e le righe rimosse e lo aggiunte alla lista dei file modificati dal commit
		jsonDiffs.forEach(element -> {
			JsonObject jsonDiff = element.getAsJsonObject();
			Diff d = new Diff();
			String filename = jsonDiff.get("filename").getAsString();
			
			if (filename.endsWith(".java")) {
				d.setFilename(filename);
				d.setAddedLines(jsonDiff.get("additions").getAsInt());
				d.setDeletedLines(jsonDiff.get("deletions").getAsInt());
				c.addDiff(d);	
			}
		});
		
		return c;
	}
	
}
