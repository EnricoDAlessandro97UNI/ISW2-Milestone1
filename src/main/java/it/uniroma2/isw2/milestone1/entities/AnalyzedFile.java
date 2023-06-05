package it.uniroma2.isw2.milestone1.entities;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalyzedFile {

	private LocalDateTime birth;
	private String name;
	private int size;
	private int additions;
	private int deletions;
	private int maxChurn;
	private	int maxAdditions;
	private int updateTimes;
	
	private Set<Commit> commits;
	
	List<Metrics> evaluatedMetrics;
	
	public AnalyzedFile(String name, LocalDateTime birth) {
		this.evaluatedMetrics = new ArrayList<>();
		this.name = name;
		this.birth = birth;
		this.size = 0;
		this.resetMetrics();
	}
	
	private void resetMetrics() {
		this.commits = new HashSet<>();
		this.additions = 0;
		this.deletions = 0;
		this.maxChurn=0;
		this.maxAdditions = 0;
		this.updateTimes = 0;
	}
	
	public int getSize() {
		return size;
	}
	
	public String getName() {
		return name;
	}
	
	public LocalDateTime getBirth() {
		return birth;
	}
	
	public void updateChurn(int additions, int deletions) {
		if (additions > this.maxAdditions)
			this.maxAdditions = additions;
		
		this.updateTimes++;
		this.additions += additions;
		this.deletions += deletions;
		
		int churn = Math.abs(additions - deletions);
		if (churn > this.maxChurn)
			this.maxChurn = churn;
		this.size += churn;
	}
	
	public void insertCommit(Commit commit) {
		commits.add(commit);
	}
	
	public void computeMetrics(Version currentVersion, LocalDateTime currentDate, List<Bug> bugs) {
		if (this.size > 0) {
			Metrics m = new Metrics();	
			
			int churn = Math.abs(this.additions - this.deletions);
			m.setVersion(currentVersion);
			m.setName(this.name);
			m.setSize(this.size);
			m.setNumberOfRevisions(this.commits.size());
			m.setNumberOfAuthors(this.numberOfAuthors());
			m.setLOCAdded(this.additions);
			m.setMaxLOCAdded(this.maxAdditions);
			m.setAverageLOCAdded((this.updateTimes == 0) ? 0 : (double)this.additions/this.updateTimes);
			m.setChurn(churn);
			m.setMaxChurn(this.maxChurn);
			m.setAverageChurn((this.updateTimes == 0) ? 0 : ((double)churn/this.updateTimes));
			m.setChangeSetSize(this.chgSetSize());
			m.setAge((currentDate == null) ? 0 : ChronoUnit.WEEKS.between(this.birth, currentDate));
			m.setBuggyness(this.isBuggy(currentVersion, bugs));
			
			this.evaluatedMetrics.add(m);
			this.resetMetrics();
		}
	}
	
	public List<Metrics> getComputedMetrics() {
		return this.evaluatedMetrics;
	}
	
	private boolean isBuggy(Version currentVersion, List<Bug> bugs) {
		for (Bug b : bugs) {
			if (b.infects(this.name) && b.belongsTo(currentVersion)) {
				return true;
			}
		}
		return false;
	}
	
	private int numberOfAuthors() {
		Set<String> authors = new HashSet<>();
		this.commits.forEach(c->authors.add(c.getAuthor()));
		return authors.size();
	}
	
	private int chgSetSize() {
		Set<String> files = new HashSet<>();
		this.commits.forEach(c-> {
			List<Diff> fileChanged = c.getDiffs();
			fileChanged.forEach(d-> files.add(d.getFilename()));
		});
		return files.size();
	}
	
}
