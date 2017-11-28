package com.redhat.training.msa.poc.data;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.redhat.training.msa.poc.model.Personne;

public interface IPersonneDataRepository extends MongoRepository<Personne, String> {
	
	public Personne findByRef(String ref);
	public List<Personne> findAll();
	

}
