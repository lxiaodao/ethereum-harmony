/**
 * 
 */
package com.ethercamp.harmony.service.contracts;

import static org.ethereum.crypto.HashUtil.sha3;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.facade.Ethereum;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ethercamp.harmony.jsonrpc.TypeConverter;

/**
 * @author yang
 *
 */
@Component("contractHandler")
public class ContractHandler {
	
	
	 public static final Logger LOG = LoggerFactory.getLogger(ContractHandler.class);
	 protected final byte[] senderPrivateKey = sha3("flow".getBytes());
	 //flow -> cow
	 
	    // sender address is derived from the private key
	    protected final byte[] senderAddress = ECKey.fromPrivate(senderPrivateKey).getAddress();
	    @Autowired
		 SolidityCompiler compiler;
		 @Autowired
		 protected Ethereum ethereum;

	    private Map<ByteArrayWrapper, TransactionReceipt> txWaiters =
	            Collections.synchronizedMap(new HashMap<ByteArrayWrapper, TransactionReceipt>());

	   
	    public void initContracts() throws Exception {
	        ethereum.addListener(new EthereumListenerAdapter() {
	            // when block arrives look for our included transactions
	            @Override
	            public void onBlock(Block block, List<TransactionReceipt> receipts) {
	            	ContractHandler.this.onBlock(block, receipts);
	            }
	        });

	        LOG.info("Compiling contract...");
	        SolidityCompiler.Result result = compiler.compileSrc(this.loadContractFile("contracts/ri.sol"), true, true,
	                SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
	        if (result.isFailed()) {
	            throw new RuntimeException("Contract compilation failed:\n" + result.errors);
	        }
	        CompilationResult res = CompilationResult.parse(result.output);
	        if (res.getContracts().isEmpty()) {
	            throw new RuntimeException("Compilation failed, no contracts returned:\n" + result.errors);
	        }
	        CompilationResult.ContractMetadata metadata = res.getContracts().iterator().next();
	        if (metadata.bin == null || metadata.bin.isEmpty()) {
	            throw new RuntimeException("Compilation failed, no binary returned:\n" + result.errors);
	        }

	        LOG.info("Sending contract to net and waiting for inclusion");
	        
	        //-> cow  0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826
	        //byte[] toaddress=ByteUtil.hexStringToBytes(fromHex("0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"));
	        byte[] toaddress=TypeConverter.StringHexToByteArray("0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826");
	        TransactionReceipt receipt = sendTxAndWait(toaddress, Hex.decode(metadata.bin));

	        if (!receipt.isSuccessful()) {
	        	LOG.error("Some troubles creating a contract: " + receipt.getError());
	            return;
	        }

	        byte[] contractAddress = receipt.getTransaction().getContractAddress();
	        LOG.info("Contract created: " + Hex.toHexString(contractAddress));

	       /* LOG.info("Calling the contract function 'inc'");
	        CallTransaction.Contract contract = new CallTransaction.Contract(metadata.abi);
	        CallTransaction.Function inc = contract.getByName("inc");
	        byte[] functionCallBytes = inc.encode(777);
	        TransactionReceipt receipt1 = sendTxAndWait(contractAddress, functionCallBytes);
	        if (!receipt1.isSuccessful()) {
	        	LOG.error("Some troubles invoking the contract: " + receipt.getError());
	            return;
	        }
	        LOG.info("Contract modified!");

	        ProgramResult r = ethereum.callConstantFunction(Hex.toHexString(contractAddress),
	                contract.getByName("get"));
	        Object[] ret = contract.getByName("get").decodeResult(r.getHReturn());
	        LOG.info("Current contract data member value: " + ret[0]);*/
	    }

	    protected TransactionReceipt sendTxAndWait(byte[] receiveAddress, byte[] data) throws InterruptedException, ExecutionException {
	        BigInteger nonce = ethereum.getRepository().getNonce(senderAddress);
	        
	        LOG.info(senderAddress+"------the nonce is------"+nonce);
	        
	        
	        long currentGas=ethereum.getGasPrice();
	        LOG.info("------the current gasprice is------"+currentGas);
	        
	        Transaction tx = new Transaction(
	                ByteUtil.bigIntegerToBytes(nonce),
	                ByteUtil.longToBytesNoLeadZeroes(currentGas),
	                ByteUtil.longToBytesNoLeadZeroes(3_000_000),
	                receiveAddress,
	                ByteUtil.longToBytesNoLeadZeroes(1),
	                data,
	                ethereum.getChainIdForNextBlock());
	        tx.sign(ECKey.fromPrivate(senderPrivateKey));
	        LOG.info("<=== Sending transaction: " + tx);
	        
	        Future<Transaction> future=ethereum.submitTransaction(tx);

	        //return waitForTx(tx.getHash());
	        return waitForTx(future.get().getHash());
	    }

	    private void onBlock(Block block, List<TransactionReceipt> receipts) {
	        for (TransactionReceipt receipt : receipts) {
	            ByteArrayWrapper txHashW = new ByteArrayWrapper(receipt.getTransaction().getHash());
	            if (txWaiters.containsKey(txHashW)) {
	                txWaiters.put(txHashW, receipt);
	                synchronized (this) {
	                    notifyAll();
	                }
	            }
	        }
	    }

	    protected TransactionReceipt waitForTx(byte[] txHash) throws InterruptedException {
	        ByteArrayWrapper txHashW = new ByteArrayWrapper(txHash);
	        txWaiters.put(txHashW, null);
	        long startBlock = ethereum.getBlockchain().getBestBlock().getNumber();
	        while(true) {
	            TransactionReceipt receipt = txWaiters.get(txHashW);
	            if (receipt != null) {
	                return receipt;
	            } else {
	            	long curBlock = ethereum.getBlockchain().getBestBlock().getNumber();
	               /* 
	                if (curBlock > startBlock + 16) {
	                    throw new RuntimeException("The transaction was not included during last 16 blocks: " + txHashW.toString().substring(0,8));
	                } else {
	                    LOG.info("Waiting for block with transaction 0x" + txHashW.toString().substring(0,8) +
	                            " included (" + (curBlock - startBlock) + " blocks received so far) ...");
	                }*/
	            	  LOG.info("Waiting for block with transaction 0x" + txHashW.toString().substring(0,8) +
	                            " included (" + (curBlock - startBlock) + " blocks received so far) ...");
	            }
	            synchronized (this) {
	                wait(20000);
	            }
	        }
	    }
	 
	  private File loadContractFile(String path) throws IOException {
		  //getClass().getClassLoader().getResourceAsStream(path)
		  
	        return new File(getClass().getClassLoader().getResource(path).getFile());
	    }


}
