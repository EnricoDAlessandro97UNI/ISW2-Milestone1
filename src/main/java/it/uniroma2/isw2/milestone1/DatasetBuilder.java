package it.uniroma2.isw2.milestone1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.uniroma2.isw2.milestone1.entities.AnalyzedFile;
import it.uniroma2.isw2.milestone1.entities.Bug;
import it.uniroma2.isw2.milestone1.entities.Commit;
import it.uniroma2.isw2.milestone1.entities.Diff;
import it.uniroma2.isw2.milestone1.entities.Metrics;
import it.uniroma2.isw2.milestone1.entities.Version;
import it.uniroma2.isw2.milestone1.exceptions.GithubOwnerException;
import it.uniroma2.isw2.milestone1.exceptions.GithubTokenException;
import it.uniroma2.isw2.milestone1.exceptions.RateLimitExceededGithubAPIException;
import it.uniroma2.isw2.milestone1.utils.GitHelper;
import it.uniroma2.isw2.milestone1.utils.JiraHelper;

public class DatasetBuilder {

	private static final Logger LOGGER = Logger.getLogger(DatasetBuilder.class.getName()); 
	
	private static final String OUTPUT_FILE_NAME_FORMAT = "%s%s_metrics.%s";
	
	private String projectName;
	private String outputsFolder;
	
	private GitHelper gitHelper;
	private JiraHelper jiraHelper;
	
	private List<Version> versions;
	private List<Commit> commits;
	private List<Bug> bugs;
	private Map<String, AnalyzedFile> files;
	
	public DatasetBuilder(String projectName, String outputsFolder) throws GithubTokenException, GithubOwnerException {
		this.projectName = projectName;
		this.outputsFolder = outputsFolder;
		this.gitHelper = new GitHelper(this.projectName);
		this.jiraHelper = new JiraHelper(this.projectName);
	}

	public void build() throws IOException, RateLimitExceededGithubAPIException {
		
		/* Recupero delle versioni del progetto da JIRA */		
		LOGGER.log(Level.INFO, "Recupero versioni del progetto da JIRA");
		this.versions = jiraHelper.retrieveVersions();
		LOGGER.log(Level.INFO, "Trovate {0} versioni", this.versions.size());
					
		/* Recupero i bug del progetto da JIRA */
		LOGGER.log(Level.INFO, "Recupero i bug del progetto da JIRA");
		this.bugs = jiraHelper.retrieveBugs(this.versions);
		LOGGER.log(Level.INFO, "Trovati {0} bug", this.bugs.size());
		
		/* Scarto del 50% delle versioni */
		int targetId = (int)Math.ceil(this.versions.size()/2.0);

		/* Recupero i commit del progetto da Github */
		LOGGER.log(Level.INFO, "Recupero commit del progetto da Github");
		this.commits = gitHelper.retrieveCommits(maxDate(this.bugs.get(this.bugs.size()-1).getFv().getReleaseDate(), this.versions.get(targetId).getReleaseDate()));
		LOGGER.log(Level.INFO, "Trovati {0} commit", this.commits.size());

		this.files = new HashMap<>();
		
		/* Applicazione di proportion per il calcolo dell'IV quando mancante */
		LOGGER.log(Level.INFO, "Applicazione di Proportion per il calcolo delle IV mancanti");
		this.proportion();
		
		/* Costruzione dell'insieme dei file toccati da ciascun bug */
		LOGGER.log(Level.INFO, "Costruzione dell'insieme dei file toccati da ciascun bug");
		this.setInfectedFiles();
		
		LOGGER.log(Level.INFO, "Considera soltanto la prima metà delle versioni ({0})", targetId);
		
		/* Creazione del dataset */
		LOGGER.log(Level.INFO, "Creazione del dataset");
		this.createDataset(targetId);
		LOGGER.log(Level.INFO, "Dataset creato correttamente");

		LOGGER.log(Level.INFO, "Creazione dei file csv e arff");
		this.createOutputFile();
		LOGGER.log(Level.INFO, "File csv e arff creati correttamente");

	}
	
	private void proportion() {
		for (Bug b : bugs) {
			if (b.getIv() == null) { // Se IV mancante, allora lo setto
				b.setIv(this.bugs, this.versions);
			}
		}
	}
	
	private void setInfectedFiles() {
		for (Bug b : this.bugs) {
			for (Commit c : commits) {
				if (c.getMessage().contains(b.getKey())) {
					for (Diff d : c.getDiffs()) {
						b.addInfectedFile(d.getFilename());
					}
				}
			}
		}
	}
	
	private void createDataset(int targetVersionIdx) {
		int versionsIdx = 0;
		int commitsIdx = 0;
		while (versionsIdx < targetVersionIdx) {			
			if (this.needSwitchVersion(versionsIdx, commitsIdx)) {
				LOGGER.log(Level.INFO, "Dopo {0} commit passo dalla versione {1} alla {2}", new Object[] { commitsIdx, this.versions.get(versionsIdx), this.versions.get(versionsIdx+1) });
				this.evalStatistics(commitsIdx, versionsIdx++);
			}
			this.applyDiff(commitsIdx++);
		}
	}
	
	private boolean needSwitchVersion(int versionsIdx, int commitsIdx) {
		return this.commits.get(commitsIdx).getDate().isAfter(this.versions.get(versionsIdx+1).getReleaseDate());
	}
	
	private void evalStatistics(int commitsIdx, int versionIdx) {
		
		LocalDateTime date = (commitsIdx > 0) ? commits.get(commitsIdx-1).getDate() : null;

		for (AnalyzedFile f : this.files.values()) 
			f.computeMetrics(this.versions.get(versionIdx), date, this.bugs);
		
		LOGGER.log(Level.INFO, "Valutate le statistiche per {0} entry", this.files.values().size());
	}
	
	private void applyDiff(int commitsIdx) {
		for (Diff d : this.commits.get(commitsIdx).getDiffs()) {
			String filename = d.getFilename();
			
			AnalyzedFile analyzedFile;
			if (this.files.containsKey(filename))
				analyzedFile = this.files.get(filename);
			else
				analyzedFile = new AnalyzedFile(d.getFilename(), commits.get(commitsIdx).getDate());
			
			analyzedFile.updateChurn(d.getAddedLines(), d.getDeletedLines());
			analyzedFile.insertCommit(this.commits.get(commitsIdx));

			this.files.put(filename, analyzedFile);
		}
	}
	
	private void createOutputFile() throws IOException {
		List<Metrics> metrics = new ArrayList<>();
		for (AnalyzedFile f : this.files.values())
			metrics.addAll(f.getComputedMetrics());
		
		/* Ordina le release in base alla data e dopo in base al nome dei file */
		Comparator<Metrics> comparator = Comparator.comparing(m -> m.getVersion().getReleaseDate());
		comparator = comparator.thenComparing(Metrics::getName);
		metrics.sort(comparator);
		
		File csvDataset = new File(String.format(OUTPUT_FILE_NAME_FORMAT, this.outputsFolder, this.projectName, "csv"));
		File arffDataset = new File(String.format(OUTPUT_FILE_NAME_FORMAT, this.outputsFolder, this.projectName, "arff"));

		try (FileWriter csvWriter = new FileWriter(csvDataset, false);
			 FileWriter arffWriter = new FileWriter(arffDataset, false)) {

			csvWriter.append(Metrics.CSV_HEADER);
			arffWriter.append(String.format(Metrics.ARFF_HEADER_FORMAT, this.projectName));

			for (Metrics m : metrics) {
				csvWriter.append(String.format("%s%n", m));
				arffWriter.append(String.format("%s%n", m));
			}
		}
	}
	
	private LocalDateTime maxDate(LocalDateTime d1, LocalDateTime d2) {
		return (d1.isAfter(d2)) ? d1 : d2;
	}
	
}
