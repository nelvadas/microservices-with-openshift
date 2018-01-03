package com.redhat.training.msa.poc.model;

import java.io.Serializable;
import java.util.Date;



public class Account implements Serializable {
 
private static final long serialVersionUID = 1L;

 private String id;
 
 //Account Owner references   Person.id in msa-personne microservice
 private String owner;
 public String getOwner() {
	return owner;
}
public void setOwner(String owner) {
	this.owner = owner;
}
private String accountType;
 private Date creationDate;
 private Double balance;
 private String status;
 

@Override
public String toString() {
	return "Account [id=" + id + ", personRef=" + owner + ", accountType=" + accountType + ", creationDate="
			+ creationDate + ", balance=" + balance + ", status=" + status + "]";
}
public String getStatus() {
	return status;
}
public void setStatus(String status) {
	this.status = status;
}
public String getId() {
	return id;
}
public void setId(String id) {
	this.id = id;
}

public String getAccountType() {
	return accountType;
}
public void setAccountType(String accountType) {
	this.accountType = accountType;
}
public Date getCreationDate() {
	return creationDate;
}
public void setCreationDate(Date creationDate) {
	this.creationDate = creationDate;
}
public Double getBalance() {
	return balance;
}
public void setBalance(Double balance) {
	this.balance = balance;
}
public static long getSerialversionuid() {
	return serialVersionUID;
}
 
 

}
