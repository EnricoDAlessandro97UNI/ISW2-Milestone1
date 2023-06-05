package it.uniroma2.isw2.milestone1.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Bug {

	private static final String RELEASE_DATE = "releaseDate";
	
	private String key;
	private Version fv;
	private Version ov;
	private Version iv;
	private Set<String> infectedFiles; 
	
	private Bug() {
		this.infectedFiles = new HashSet<>();
	}

	public String getKey() {
		return key;
	}

	public Version getFv() {
		return fv;
	}

	public void setFv(Version fv) {
		this.fv = fv;
	}

	public Version getOv() {
		return ov;
	}

	public void setOv(Version ov) {
		this.ov = ov;
	}

	public Version getIv() {
		return iv;
	}

	public void setIv(List<Bug> bugs, List<Version> versions) {
		double sumP = 0.0;
		int counter = 0;

		for (Bug b : bugs) {
			if (b.getFv().getReleaseDate().isBefore(this.ov.getReleaseDate())) {
				sumP += b.getProportion(versions);
				counter++;
			}
		}

		int p = (counter == 0) ? 1 : (int)Math.ceil(sumP/counter);
		int fvIndex = versions.indexOf(this.fv);
		int ovIndex = versions.indexOf(this.ov);
		int ivIndex = fvIndex - (fvIndex - ovIndex)*p;

		this.iv = versions.get(Math.max(ivIndex, 0));
	}
	
	private double getProportion(List<Version> versions) {
		int fvIndex = versions.indexOf(this.fv);
		int ovIndex = versions.indexOf(this.ov);
		int ivIndex = versions.indexOf(this.iv);

		return ((double)(fvIndex-ivIndex)/Math.max(fvIndex - ovIndex, 1));
	}

	public Set<String> getInfectedFiles() {
		return infectedFiles;
	}

	public void setInfectedFiles(Set<String> infectedFiles) {
		this.infectedFiles = infectedFiles;
	}
	
	public static Bug fromJsonObject(JsonObject json, List<Version> versions) {
		
		Bug bug = new Bug();
		
		/* Scarta i bug che non hanno un key associata (example_key: BOOKKEEPER-1105) */
		if (json.get("key") == null)
			return null;
		bug.key = json.get("key").getAsString();
				
		/* Preleva i campi del bug */
		JsonObject fields = json.get("fields").getAsJsonObject();
		Version v;
		
		/* Scarta i bug che non hanno una fixed version associata */
		if (fields.get("fixVersions") == null)
			return null;	
		if ((v = extractFixVersion(fields.get("fixVersions").getAsJsonArray(), versions)) == null)
			return null;
		bug.fv = v;
			
		/* Scarta i bug che non hanno una OV */
		if (fields.get("created") == null)
			return null;
		if ((v = extractOpenVersion(fields.get("created").getAsString(), versions)) == null)
			return null;
		bug.ov = v;
		
		/* Se OV > FV, allora scarta il bug */
		if (bug.fv.getReleaseDate().isBefore(bug.ov.getReleaseDate()))
			return null;
		
		/* Setta IV */
		bug.iv = extractInjectedVersion(fields.get("versions").getAsJsonArray(), versions);
		
		return bug; 
	}
	
	private static Version extractFixVersion(JsonArray jsonFixVersions, List<Version> versions) {
		
		String fixVersion = null;
		LocalDateTime fixDate = null;
				
		for (JsonElement element : jsonFixVersions) {
			JsonObject jsonFixVersion = element.getAsJsonObject();
			if (missingFields(jsonFixVersion))
				continue;
						
			String version = jsonFixVersion.get("name").getAsString();
			LocalDateTime date = LocalDate.parse(jsonFixVersion.get(RELEASE_DATE).getAsString()).atStartOfDay();
			
			if (fixDate == null || date.isAfter(fixDate)) {
				fixVersion = version;
				fixDate = date;
			}
		}
				
		for (Version v : versions) 
			/* In JIRA molti bug hanno come suffisso il nome della versione */
			if (fixVersion != null && v.getName().startsWith(fixVersion))
				return v;
		
		return null;
	}
	
	private static boolean missingFields(JsonObject jsonFixVersion) {		
		return jsonFixVersion.get("name") == null || jsonFixVersion.get("name").getAsString() == null || jsonFixVersion.get(RELEASE_DATE) == null || jsonFixVersion.get(RELEASE_DATE).getAsString() == null;
	}

	private static Version extractOpenVersion(String openDate, List<Version> versions) {
		for (Version v : versions)
			if (v.getReleaseDate().isAfter(LocalDate.parse(openDate.substring(0, 10)).atStartOfDay()))
				return v;
		return null;
	}
	
	private static Version extractInjectedVersion(JsonArray jsonAvs, List<Version> versions) {		
		for (JsonElement jsonElement : jsonAvs) {
			if (jsonElement.getAsJsonObject().get("name") != null) {
				String versionName = jsonElement.getAsJsonObject().get("name").getAsString();
				for (Version v : versions)
					if (v.getName().equals(versionName))
						return v;
			}
		}
		return null;
	}
	
	public void addInfectedFile(String filename) {
		this.infectedFiles.add(filename);
	}
	
	public boolean infects(String filename) {
		return this.infectedFiles.contains(filename);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(this.key)
				.append(" [FV: ").append(this.fv.getName())
				.append(", OV: ").append(this.ov.getName());
				
		if (this.iv != null)
			return sb.append(", IV:").append(this.iv.getName()).append("]").toString();
		else
			return sb.append("]").toString();
	}

	public boolean belongsTo(Version currentVersion) {
		if (this.iv == null) {
			return false;
		}
		else {
			return (!this.iv.getReleaseDate().isAfter(currentVersion.getReleaseDate()) && this.fv.getReleaseDate().isAfter(currentVersion.getReleaseDate()));
		}
	}
	
}
