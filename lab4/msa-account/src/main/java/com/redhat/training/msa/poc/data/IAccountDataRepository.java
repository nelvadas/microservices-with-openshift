package com.redhat.training.msa.poc.data;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.redhat.training.msa.poc.model.Account;

public interface IAccountDataRepository extends MongoRepository<Account, String> {
	
	public List<Account> findByOwner(String owner);
	public List<Account> findAll();
	

}
