package com.redhat.training.msa.poc.model;

import java.io.Serializable;
import java.util.List;

public class Customer implements Serializable {
	private static final long serialVersionUID = 1L;
	private Personne identity;
	private List<Account> accounts;
	
	
	public List<Account> getAccounts() {
		return accounts;
	}
	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
	public Personne getIdentity() {
		return identity;
	}
	public void setIdentity(Personne identity) {
		this.identity = identity;
	}
	
	
}
