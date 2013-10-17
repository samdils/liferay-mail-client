package be.spiker.model;

import java.io.Serializable;
import java.util.List;

import com.liferay.portal.model.Role;

public class DataUser implements Serializable {

	private Long id;
	private String name;
	private String screename;
	private String email;
	private List<Role> roles;

	public DataUser() {

	}

	public DataUser(Long id, String name, String screename, String email, List<Role> roles) {
		this.id = id;
		this.name = name;
		this.screename = screename;
		this.roles = roles;
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

}
