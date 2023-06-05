package it.uniroma2.isw2.milestone1.entities;

public class Diff {

	private String filename;
	private int addedLines;
	private int deletedLines;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getAddedLines() {
		return addedLines;
	}

	public void setAddedLines(int addedLines) {
		this.addedLines = addedLines;
	}

	public int getDeletedLines() {
		return deletedLines;
	}

	public void setDeletedLines(int deletedLines) {
		this.deletedLines = deletedLines;
	}
	
}
