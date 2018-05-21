/**
 * 
 */
package com.ethercamp.harmony.keystore;

import static org.ethereum.crypto.HashUtil.sha3;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yang
 *
 */

public class HashAddressTest {
	
	
	  @Test
	 public void test1() {

	        byte[] result = HashUtil.sha3("horse".getBytes());

	        assertEquals("c87f65ff3f271bf5dc8643484f66b200109caffe4bf98c4cb393dc35740b28c0",
	                Hex.toHexString(result));

	        result = HashUtil.sha3("cow".getBytes());

	        assertEquals("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4",
	                Hex.toHexString(result));
	    }

	    @Test
	    public void test3() {
	    	
	        BigInteger privKey = new BigInteger("cd244b3015703ddf545595da06ada5516628c5feadbf49dc66049c4b370cc5d8", 16);
	        byte[] addr = ECKey.fromPrivate(privKey).getAddress();
	        assertEquals("89b44e4d3c81ede05d0f5de8d1a68f754d73d997", Hex.toHexString(addr));
	    }
	    
	    @Test
	    public void test_four_peer() {
	    	 
	    	   ECKey cowKey = ECKey.fromPrivate(sha3("cow".getBytes()));
	           byte[] addr =cowKey.getAddress();
	           System.out.println("------cowprivate------"+cowKey.getPrivKey().toString());
	           assertEquals("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826", Hex.toHexString(addr).toUpperCase());
	    	
	    }
	    
	 
	

}
