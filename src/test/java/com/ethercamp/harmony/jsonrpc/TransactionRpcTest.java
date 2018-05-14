/*
 * Copyright 2015, 2016 Ether.Camp Inc. (US)
 * This file is part of Ethereum Harmony.
 *
 * Ethereum Harmony is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ethereum Harmony is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ethereum Harmony.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ethercamp.harmony.jsonrpc;

import static com.ethercamp.harmony.jsonrpc.TypeConverter.HexToLong;
import static com.ethercamp.harmony.jsonrpc.TypeConverter.StringHexToByteArray;
import static java.math.BigInteger.valueOf;
import static java.util.Arrays.stream;
import static org.ethereum.crypto.HashUtil.sha3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.DbSource;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.facade.EthereumImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import com.ethercamp.harmony.jsonrpc.JsonRpcTest.TestRunner;
import com.ethercamp.harmony.keystore.FileSystemKeystore;
import com.typesafe.config.ConfigFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Yang 2018-5-7
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TransactionRpcTest {
	
	@Autowired
    private Ethereum ethereum;
	
	   @Autowired
       JsonRpc jsonRpc;
	
	
	@Test
	public void test_getGas() {
		log.info("------TransactionRpcTest------"+ethereum.getGasPrice());
		
	}
	
	@Test
	public void test_aa() throws Exception {
		   String passphrase = "123";
	    	/* String passphrase = "123";
	            ECKey newKey = ECKey.fromPrivate(sha3("aa".getBytes()));
	            String keydata = Hex.toHexString(newKey.getPrivKeyBytes());
	            String cowAcct = jsonRpc.personal_importRawKey(keydata, passphrase);*/
	            
	            String cowAcct ="0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826";
	            jsonRpc.personal_unlockAccount(cowAcct, passphrase, "");
	            //log.info("------cowAcct------"+cowAcct);
	            
	            
	            String pendingTxFilterId = jsonRpc.eth_newPendingTransactionFilter();
	            Object[] changes = jsonRpc.eth_getFilterChanges(pendingTxFilterId);
	            log.info("------changes number------"+changes.length);
	            
	            // from cow to aa     	 
	    	   
	            JsonRpc.CallArguments ca = new JsonRpc.CallArguments();
	            ca.from = cowAcct;
	            ca.to = "0x8670e9d7dc4cc00f157cde5a3cd8afe53b97a567"; //aa 
	            ca.gas = "0x300000";
	            ca.gasPrice = "0x10000000000";
	            ca.value = "0x7777";
	            ca.data = "0x";
	            long sGas = TypeConverter.StringHexToBigInteger(jsonRpc.eth_estimateGas(ca)).longValue();
	            log.info("------the balance is------"+jsonRpc.eth_getLastBalance(cowAcct));
	            log.info("------sGas------"+sGas);
	            String txHash1 = jsonRpc.eth_sendTransactionArgs(cowAcct, "0x8670e9d7dc4cc00f157cde5a3cd8afe53b97a567", "0x300000",
	                    "0x10000000000", "0x7777", "0x", "0x00");
	            log.info("Tx hash: " + txHash1);
	            assertTrue(TypeConverter.StringHexToBigInteger(txHash1).compareTo(BigInteger.ZERO) > 0);

	           
	            JsonRpc.BlockResult blockResult = jsonRpc.eth_getBlockByNumber("pending", true);
	           
	            log.info("------blockResult------"+blockResult);
	            assertEquals(txHash1, ((com.ethercamp.harmony.jsonrpc.TransactionResultDTO) blockResult.transactions[0]).hash);
	            
	            log.info("------test_getBalanceof aa is------"+jsonRpc.eth_getLastBalance("0x8670e9d7dc4cc00f157cde5a3cd8afe53b97a567"));
	            
	            
	            String block_hash=mineBlock();
	            
	            JsonRpc.BlockResult blockResult1 = jsonRpc.eth_getBlockByHash(block_hash, true);
	            
	            assertEquals(block_hash,blockResult1.hash);
	            
	            log.info("------test_getBalanceof aa is------"+jsonRpc.eth_getLastBalance("0x8670e9d7dc4cc00f157cde5a3cd8afe53b97a567"));
	            
	    }
	    @Test
	    public void test_getBalanceof() throws Exception {
	    	
	    	
	    	  //log.info("------mine block hash ------"+mineBlock());
	    	  log.info("------the balance is------"+jsonRpc.eth_getLastBalance("0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826"));
	    	  log.info("------test_getBalanceof aa is ------"+jsonRpc.eth_getLastBalance("0x8670e9d7dc4cc00f157cde5a3cd8afe53b97a567"));
	    }
	    private String mineBlock() throws InterruptedException {
	            String blockFilterId = jsonRpc.eth_newBlockFilter();
	            jsonRpc.miner_start();
	            int cnt = 0;
	            String hash1;
	            while (true) {
	                Object[] blocks = jsonRpc.eth_getFilterChanges(blockFilterId);
	                cnt += blocks.length;
	                if (cnt > 0) {
	                    hash1 = (String) blocks[0];
	                    break;
	                }
	                Thread.sleep(100);
	            }
	            jsonRpc.miner_stop();
	            Thread.sleep(100);
	            Object[] blocks = jsonRpc.eth_getFilterChanges(blockFilterId);
	            cnt += blocks.length;
	            System.out.println(cnt + " blocks mined");
	            boolean b = jsonRpc.eth_uninstallFilter(blockFilterId);
	            assertTrue(b);
	            return hash1;
	        }
    
  
} 


