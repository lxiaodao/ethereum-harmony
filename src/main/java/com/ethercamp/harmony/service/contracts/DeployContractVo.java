/**
 * 
 */
package com.ethercamp.harmony.service.contracts;

import com.ethercamp.harmony.service.ContractsService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yang
 *
 */
@Data
@NoArgsConstructor

public class DeployContractVo {
	
	private String txhash;
	private String code;
	
	public DeployContractVo(String txhash, String code) {
		super();
		this.txhash = txhash;
		this.code = code;
	}
	

}
