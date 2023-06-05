package it.uniroma2.isw2.milestone1.entities;

import java.time.LocalDateTime;

public class Version {

	private int id;
	private String name;
	private LocalDateTime releaseDate;
	
	public Version(String name, LocalDateTime releaseDate) {
		this.name = name;
		this.releaseDate = releaseDate;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public LocalDateTime getReleaseDate() {
		return releaseDate;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return new StringBuilder("VersionId ").append(this.id).append(" --> ").append(this.name).append(" (").append(this.releaseDate).append(")").toString();
	}
	
}
