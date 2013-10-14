package be.spiker.model;

import java.io.Serializable;


public class DataUser implements Serializable{

	Long id;
	String name;
	String screename;
	String roles;

	public DataUser(Long id, String name, String screename, String roles) {
		this.id = id;
		this.name = name;
		this.screename = screename;
		this.roles = roles;
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

	public String getScreename() {
		return screename;
	}

	public void setScreename(String screename) {
		this.screename = screename;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

}
