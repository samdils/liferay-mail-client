package be.spiker.model;

import java.io.Serializable;

public class Template implements Serializable{

	private Long id;
	private String name;
	private String title;
	private String content;
	private String sender;

	public Template(Long id, String name, String title, String content, String sender) {
		this.id = id;
		this.name = name;
		this.title = title;
		this.content = content;
		this.sender = sender;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

}
