package com.ethercamp.harmony.service.contracts;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContractLoad {
	
	
	 public static String loadContractContent(String path) {
		  
		  InputStream is= ContractLoad.class.getClassLoader().getResourceAsStream(path);
		  try
	        {
	            StringBuffer build = new StringBuffer();	          
	            int index = 0;
				for (;;) {
					byte[] content = new byte[4096];
					 
					int res = is.read(content,0,content.length);// 从流中读取固定长度的byte[]
					if (res == -1) {
						break;
					}

												
					build.append(new String(content,"UTF-8"));
					index = index + res;

				}
	            return build.toString();
	        }
	        catch ( IOException e)
	        {
	           throw new RuntimeException(e);
	        } 
	      
	   
	  }
	 
	 public static void main(String[] arr) {
		 
		String contract_ri= ContractLoad.loadContractContent("contracts/ri.sol");
		System.out.println(contract_ri);
	 }

}
