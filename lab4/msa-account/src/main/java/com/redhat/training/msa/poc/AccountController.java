package com.redhat.training.msa.poc;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.redhat.training.msa.poc.data.IAccountDataRepository;
import com.redhat.training.msa.poc.model.Account;

@RestController
@RequestMapping("/Account")

public class AccountController {
	
	@Autowired
	private IAccountDataRepository repository;

	
	public AccountController(){		
	}
	
    /**
     * Find person account list 
     * @param owner
     * @return List of account for this specific user
     */
    @RequestMapping( path="/{owner}",
    		         method = RequestMethod.GET,
    		         produces = {MediaType.APPLICATION_JSON_VALUE} 
    		        )
    public List<Account> findByRef(@PathVariable(value = "owner") String owner) {
    	List<Account> accounts= repository.findByOwner(owner);
    	 System.out.println("findByOwner:"+accounts);
    	 return accounts;
    }
 
    
    /**
     * List all accounts from the repository
     * @return
     */
    @RequestMapping("/")
    public Collection<Account> findAll() {
        return repository.findAll(); 
    }
 
    /**
     * Add a new Account
     * @param Account object
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<Account> create(@RequestBody Account account) {
    	Account outAccount = repository.insert(account);
    	return new ResponseEntity<Account>(outAccount, HttpStatus.OK);
    }
    
    
}
