package com.redhat.training.msa.poc.svc;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.redhat.training.msa.poc.model.Personne;

@Service
public class PersonneService {

	@Value( "${personne.svc.url}" )
	private String personneSvcBaseUrl;
	
	/**
     * Call the remote Perssonne (msa-personne) to retreive a Personne Identity
     * @param id: customerId
     * @return Personne  ( main call)
     */
    @HystrixCommand(fallbackMethod="getPersonneDegrade")
    public Personne getPersonneNominal(String id){
    	System.out.println("Personne url="+personneSvcBaseUrl);
    	RestTemplate restTemplate = new RestTemplate();
    	URI personneUri = URI.create(String.format("%s/%s", personneSvcBaseUrl,id));
    	Personne personne= restTemplate.getForObject(personneUri, Personne.class);
    	return personne;
    }
    
    
    
	/**
	 * Fallback method to use to retreive person when the remote person service is not available
	 * @param id: customerId
	 * @return PersonneData ( fallback)
	 */
	public Personne getPersonneDegrade(String id){
		  Personne personne = new Personne();
		  personne.setRef(id);
		  personne.setCustomTag("*****ServicePersonneDégradé****");
    	 return personne;
    	 
    }
	
	

	
}
