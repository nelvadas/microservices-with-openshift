package com.redhat.training.msa.poc.model;

import java.io.Serializable;
import java.util.Date;

import org.springframework.data.annotation.Id;


public class Personne implements Serializable {
 
private static final long serialVersionUID = 1L;

@Id
 private String ref;
 private String firstName;
 private String lastName;
 private Date birthDate;
 private String customTag;
 
 

public String getRef() {
	return ref;
}
public void setRef(String ref) {
	this.ref = ref;
}
public String getFirstName() {
	return firstName;
}
public void setFirstName(String firstName) {
	this.firstName = firstName;
}
public String getLastName() {
	return lastName;
}
public void setLastName(String lastName) {
	this.lastName = lastName;
}

public Date getBirthDate() {
	return birthDate;
}
public void setBirthDate(Date birthDate) {
	this.birthDate = birthDate;
}
public String getCustomTag() {
	return customTag;
}
public void setCustomTag(String customTag) {
	this.customTag = customTag;
}

@Override
public String toString() {
	return "Personne [ref=" + ref + ", firstName=" + firstName + ", lastName=" + lastName + ", birthDate=" + birthDate
			+ ", customTag=" + customTag + "]";
}
}
