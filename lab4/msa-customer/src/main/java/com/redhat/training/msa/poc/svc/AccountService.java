package com.redhat.training.msa.poc.svc;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.redhat.training.msa.poc.model.Account;

@Service
public class AccountService {

	@Value( "${account.svc.url}" )
	private String accountSvcBaseUrl;
	
	/**
     * Call the remote Account Microservie to retreive a customer account
     * @param id: customerId
     * @return User Account list ( main call)
     */
    @HystrixCommand(fallbackMethod="getCustomerAccountListDegrade")
    public List<Account> getCustomerAccountList(String id){
    	System.out.println("account url="+accountSvcBaseUrl);
    	RestTemplate restTemplate = new RestTemplate();
    	URI accountUri = URI.create(String.format("%s/%s", accountSvcBaseUrl,id));
    	Account[] userAccounts= restTemplate.getForObject(accountUri, Account[].class);
    	return (List<Account>)Arrays.asList(userAccounts);
    }
    
    
    
	/**
	 * Fallback method to use to retreive user accounts when the remote account service is not available
	 * @param id: customerId
	 * @return User Account list ( fallback)
	 */
	public List<Account> getCustomerAccountListDegrade(String id){
    	 Account userDefaultAccount = new Account();
    	 userDefaultAccount.setAccountType("CompteCourant");
    	 userDefaultAccount.setOwner(id);
    	 userDefaultAccount.setStatus("*****ServiceDégradé****");
    	 userDefaultAccount.setBalance(1.00);
    	 return (List<Account>)Arrays.asList(userDefaultAccount);
    	 
    }
	
	

	
}
