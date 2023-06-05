package it.uniroma2.isw2.milestone1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.uniroma2.isw2.milestone1.exceptions.GithubOwnerException;
import it.uniroma2.isw2.milestone1.exceptions.GithubTokenException;
import it.uniroma2.isw2.milestone1.exceptions.RateLimitExceededGithubAPIException;

public class Main {
	
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	
//	private static final String PROJECT_NAME = "SYNCOPE";    // Progetti sotto test
	private static final String PROJECT_NAME = "BOOKKEEPER"; 
	private static final String OUTPUTS_FOLDER = "outputs/"; // Directory dei risultati
	
	private static void initOutputsFolder() throws IOException {
		Path path = Paths.get(OUTPUTS_FOLDER);
		if (!Files.exists(path)) // Se non esiste, crea la cartella dei risultati
			Files.createDirectory(path);
		
		// Controlla l'esistenza dei file di output
		Path[] filesPaths = {
				Paths.get(String.format("%s%s_metrics.csv", OUTPUTS_FOLDER, PROJECT_NAME)), 
				Paths.get(String.format("%s%s_metrics.arff", OUTPUTS_FOLDER, PROJECT_NAME))
		};
		
		for (Path filePath : filesPaths) {
			if (Files.exists(filePath)) // Se il file specifico già esiste lo cancella e lo ricrea
				Files.delete(filePath);
			Files.createFile(filePath);
		}
	}

	public static void main(String[] args) {
		
		try {
			initOutputsFolder();
			DatasetBuilder datasetBuilder = new DatasetBuilder(PROJECT_NAME, OUTPUTS_FOLDER);
			datasetBuilder.build();
		} catch (GithubTokenException e) {
			LOGGER.log(Level.SEVERE, String.format("Errore nella costruzione del dataset: %s", e.getMessage()));
			e.printStackTrace();
		} catch (GithubOwnerException e) {
			LOGGER.log(Level.SEVERE, String.format("Errore nella costruzione del dataset: %s", e.getMessage()));
			e.printStackTrace();	
		} catch (RateLimitExceededGithubAPIException e) {
			LOGGER.log(Level.SEVERE, String.format("Errore nella costruzione del dataset: %s", e.getMessage()));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, String.format("Errore inizializzazione dei file di output: %s", e.getMessage()));
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, String.format("Errore generico: %s", e.getMessage()));
		} 
		
	}
	
}
