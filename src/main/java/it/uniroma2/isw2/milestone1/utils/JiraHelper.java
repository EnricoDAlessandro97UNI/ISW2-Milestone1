package it.uniroma2.isw2.milestone1.utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.uniroma2.isw2.milestone1.entities.Bug;
import it.uniroma2.isw2.milestone1.entities.Version;

public class JiraHelper {

	private static final Logger LOGGER = Logger.getLogger(JiraHelper.class.getName());
	
	private String projectName;
	
	public JiraHelper(String projectName) {
		this.projectName = projectName;
	}
	
	public List<Version> retrieveVersions() throws IOException {
		
		List<Version> versions = new ArrayList<>();
		int versionId = 1;
		
		final String URL = "https://issues.apache.org/jira/rest/api/2/project/" + this.projectName;
		
		JsonArray jsonVersions = RestHelper.getJSONObject(URL).get("versions").getAsJsonArray();
		for (JsonElement jsonElement : jsonVersions) {
			JsonObject jsonVersion = jsonElement.getAsJsonObject();
			if (jsonVersion.get("releaseDate") == null) 
				continue;
			
			LocalDateTime date = LocalDate.parse(jsonVersion.get("releaseDate").getAsString()).atStartOfDay();
			String name = jsonVersion.get("name").getAsString();
			
			Version version = new Version(name, date);
			versions.add(version);
		}
		
		Comparator<Version> comparator = new Comparator<Version>() {
			public int compare(Version v1, Version v2) {
				return v1.getReleaseDate().compareTo(v2.getReleaseDate());
			}
		};
		Collections.sort(versions, comparator);
		
		for (Version v : versions) {
			v.setId(versionId++);
			if (v != null)
				LOGGER.log(Level.INFO, v.toString());
		}
		
		return versions;
	}
	
	public List<Bug> retrieveBugs(List<Version> versions) throws IOException {
		
		final int MAX_RESULTS = 1000;
		
		int visited = 0;
		int total = 0;
		
		List<Bug> bugs = new ArrayList<Bug>();
		
		do {
			JsonObject response = RestHelper.getJSONObject(this.getBugsURL(visited, MAX_RESULTS));
			JsonArray jsonIssues = response.get("issues").getAsJsonArray();
			total = response.get("total").getAsInt();
			
			for (JsonElement jsonElement : jsonIssues) {
				JsonObject jsonIssue = jsonElement.getAsJsonObject();
				Bug bug = Bug.fromJsonObject(jsonIssue, versions);
				if (bug != null)
					bugs.add(bug);
			}
			
			visited += jsonIssues.size();
			
		} while(visited < total);
		
		/* Ordinamento dei bug per OV */
		bugs.sort(Comparator.comparing(b -> b.getOv().getReleaseDate()));
		
		return bugs;		
	}
	
	private String getBugsURL(int startIndex, int maxResults) {
		return new StringBuilder("https://issues.apache.org/jira/rest/api/2/search?jql=")
				.append("project=").append(this.projectName)
				.append("%20AND%20issueType=Bug%20AND%20resolution=Fixed%20AND%20status%20in%20(Resolved,Closed)&fields=fixVersions,versions,created")
				.append("&startAt=").append(startIndex)
				.append("&maxResults=").append(maxResults)
				.toString();
	}
	
}
