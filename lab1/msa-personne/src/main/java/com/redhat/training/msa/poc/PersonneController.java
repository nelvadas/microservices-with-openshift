package com.redhat.training.msa.poc;

import org.springframework.web.bind.annotation.RestController;

import com.redhat.training.msa.poc.data.IPersonneDataRepository;
import com.redhat.training.msa.poc.model.Personne;

import java.util.Collection;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
@RequestMapping("/Personne")

public class PersonneController {
	
	@Autowired
	private IPersonneDataRepository repository;

	
	public PersonneController(){		
	}
	
    /**
     * Find person by reference
     * @param ref
     * @return
     */
    @RequestMapping( path="/{ref}",
    		         method = RequestMethod.GET,
    		         produces = {MediaType.APPLICATION_JSON_VALUE} 
    		        )
    public Personne findByRef(@PathVariable(value = "ref") String ref) {
    	 Personne tiers= repository.findByRef(ref);
    	 //custom injection of data.
    	 System.out.println("findByRef:"+tiers);
    	 return tiers;
    }
 
    
    /**
     * List all person from the repository
     * @return
     */
    @RequestMapping("/")
    public Collection<Personne> findAll() {
        return repository.findAll(); 
    }
 
    /**
     * Add a new person to the collection
     * @param personne
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<Personne> create(@RequestBody Personne personne) {
    	Personne outPersonne = repository.insert(personne);
    	return new ResponseEntity<Personne>(outPersonne, HttpStatus.OK);
    }
    
    
}
