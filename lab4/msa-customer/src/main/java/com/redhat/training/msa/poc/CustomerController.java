package com.redhat.training.msa.poc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.redhat.training.msa.poc.model.Account;
import com.redhat.training.msa.poc.model.Customer;
import com.redhat.training.msa.poc.model.Personne;
import com.redhat.training.msa.poc.svc.AccountService;
import com.redhat.training.msa.poc.svc.PersonneService;

@RestController
@EnableCircuitBreaker
@RequestMapping("/Customer")

public class CustomerController {
	
	
	
	@Autowired
	private AccountService accountService;
	
	@Autowired
	private PersonneService personneService;
	
	
	
	
	public CustomerController(){		
	}
	
    /**
     * Fetch customer details and aggregate them.
     * @param ref
     * @return
     */
    @RequestMapping( path="/{custId}",
    		         method = RequestMethod.GET,
    		         produces = {MediaType.APPLICATION_JSON_VALUE} 
    		        )
    public Customer getCustomer(@PathVariable(value = "custId") String customerId) {
    	 Customer customer= new Customer();
    	
    	 
    	//Step 1: Get the customer Identity
    	 Personne identity =personneService.getPersonneNominal(customerId);
    	 customer.setIdentity(identity);
    	  
    	//Step 2: Get the customer Accounts
    	 List<Account> accounts = accountService.getCustomerAccountList(customerId);
    	 for (Account account : accounts) {
    		 System.out.println("" +account);
		 }
    	 
    	 //Step3: Populate User accounts
    	 customer.setAccounts(accounts);
    	 return customer;
    }
    
    
    
	
    

    
    
}
