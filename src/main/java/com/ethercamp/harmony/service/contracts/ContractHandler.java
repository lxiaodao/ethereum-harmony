/**
 * 
 */
package com.ethercamp.harmony.service.contracts;

import static org.ethereum.crypto.HashUtil.sha3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.facade.Ethereum;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.program.ProgramResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ethercamp.harmony.jsonrpc.JsonRpc;
import com.ethercamp.harmony.jsonrpc.TransactionReceiptDTO;
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
		 
		 
		 
		 String contract =
		            "contract Sample {" +
		            "  int i;" +
		            "  function inc(int n) {" +
		            "    i = i + n;" +
		            "  }" +
		            "  function get() returns (int) {" +
		            "    return i;" +
		            "  }" +
		            "}";


	    private Map<ByteArrayWrapper, TransactionReceipt> txWaiters =
	            Collections.synchronizedMap(new HashMap<ByteArrayWrapper, TransactionReceipt>());

	    @Autowired
	       JsonRpc jsonRpc;
	    
	    
	    public void initContracts(String name) throws Exception {
	      
	    	 if("ri".equals(name)) {
	    		 
	    	 }else {
	    		 LOG.info("------loadRpcContract------");
	    		 loadRpcContract(name);
	    	 }
	    }
	    
	    /**
	     * 加载编译和发布智能合约
	     * @param name
	     * @return 
	     * @throws Exception 
	     */
	    public DeployContractVo loadRpcContract(String name) throws Exception {
	    	 String passphrase = "123";
	    	  String cowAcct ="0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826";
		     jsonRpc.personal_unlockAccount(cowAcct, passphrase, "");
		     
		     //1.编译智能合约
		     com.ethercamp.harmony.jsonrpc.JsonRpc.CompilationResult rpcResult=jsonRpc.eth_compileSolidity(contract);
		    
		     
		    
		     JsonRpc.CallArguments ca = new JsonRpc.CallArguments();
	            ca.from = cowAcct;
	            ca.data = rpcResult.code;
	            //2.计算需要的gas
		     String hxgas=jsonRpc.eth_estimateGas(ca);
		     
	            ca.from = cowAcct;
	            ca.gas= hxgas;
	            ca.data = rpcResult.code;
	            //3.部署合约
		     String txhash=this.jsonRpc.eth_sendTransaction(ca);
		     TransactionReceiptDTO receipt=jsonRpc.eth_getTransactionReceipt(txhash);
		     LOG.debug("------return the contract address-------"+receipt!=null?receipt.getContractAddress():null);	     
		     
		   	return new DeployContractVo(txhash,rpcResult.code);	     
		}
	    
	    public String excuteTheMethodOfContract(String method,Object args) throws Exception {
	    	
	    	 String passphrase = "123";
	    	  String cowAcct ="0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826";
		     jsonRpc.personal_unlockAccount(cowAcct, passphrase, "");
		     
		     if(method==null||"".equals(method)) {
		    	 method="inc";
		     }
		     
		     //编译智能合约
		     //com.ethercamp.harmony.jsonrpc.JsonRpc.CompilationResult compila=jsonRpc.eth_compileSolidity(contract);
		    
	   
	    	//执行代码
	   
	        JsonRpc.CallArguments ca = new JsonRpc.CallArguments();
            ca.from = cowAcct;
            ca.data = TypeConverter.toJsonHex(CallTransaction.Function.fromSignature("inc","int").encodeArguments(88));
            ca.to="0xda7ce79725418f4f6e13bf5f520c89cec5f6a974";
            
            String txhash=this.jsonRpc.eth_sendTransaction(ca);
            LOG.info("------the inc method excute hash------"+txhash);
            
                     
          
            JsonRpc.CallArguments ca_get = new JsonRpc.CallArguments();
            ca_get.from = cowAcct;
            ca_get.data = TypeConverter.toJsonHex(CallTransaction.Function.fromSignature("get").encode());
            ca_get.to="0xda7ce79725418f4f6e13bf5f520c89cec5f6a974";
            
                     
            
            String resultof=jsonRpc.eth_call(ca_get, "pending");
            LOG.info("------return result------"+resultof);
	    	
	    	return resultof;
	    }
	    
	    JsonRpc.CallArguments createCall(String contractAddress, String functionName) {
            JsonRpc.CallArguments result = new JsonRpc.CallArguments();
            result.to = contractAddress;
            result.data = TypeConverter.toJsonHex(CallTransaction.Function.fromSignature(functionName).encode());
            return result;
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
	    
	    public CompilationResult.ContractMetadata getContractData(String name) throws IOException {
	    	LOG.info("Compiling contract...");
	        
	        SolidityCompiler.Result result = null;
	        if("ri".equals(name)) {
	        	result = compiler.compileSrc(this.loadContractFile("contracts/ri.sol"), true, true,
		                SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
	        }else {
	        	result = compiler.compileSrc(contract.getBytes(), true, true,
		                SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
	        	LOG.info("------load sample contract------");
	        }
	        
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
	        return metadata;
	    }
	    
	    public void loadRawContract(String name) throws IOException, InterruptedException, ExecutionException {
	    	
	    	  ethereum.addListener(new EthereumListenerAdapter() {
		            // when block arrives look for our included transactions
		            @Override
		            public void onBlock(Block block, List<TransactionReceipt> receipts) {
		            	ContractHandler.this.onBlock(block, receipts);
		            }
		        });

		        LOG.info("Compiling contract...");
		        
		        SolidityCompiler.Result result = null;
		        if("ri".equals(name)) {
		        	result = compiler.compileSrc(this.loadContractFile("contracts/ri.sol"), true, true,
			                SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
		        }else {
		        	result = compiler.compileSrc(contract.getBytes(), true, true,
			                SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
		        	LOG.info("------load sample contract------");
		        }
		        
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

		        LOG.info("Calling the contract function 'inc'");
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
		        LOG.info("Current contract data member value: " + ret[0]);
	    	
	    }

	    protected TransactionReceipt sendTxAndWait(byte[] receiveAddress, byte[] data) throws InterruptedException, ExecutionException {
	        BigInteger nonce = ethereum.getRepository().getNonce(senderAddress);
	        
	        LOG.info(senderAddress+"------the nonce is------"+nonce);
	        
	        
	        long currentGas=ethereum.getGasPrice();
	        LOG.info("------the current gasprice is------"+currentGas);
	        
	        Transaction tx = new Transaction(
	                ByteUtil.bigIntegerToBytes(nonce),
	                ByteUtil.longToBytesNoLeadZeroes(currentGas),
	                ByteUtil.longToBytesNoLeadZeroes(700_000_000_000L),
	                receiveAddress,
	                ByteUtil.longToBytesNoLeadZeroes(0),
	                data);
	        
	        //,ethereum.getChainIdForNextBlock()
	        tx.sign(ECKey.fromPrivate(senderPrivateKey));
	        LOG.info("<=== Sending transaction: " + tx);
	        
	        Future<Transaction> future=ethereum.submitTransaction(tx);

	        //return waitForTx(tx.getHash());
	        return waitForTx(future.get().getHash());
	    }

	    private void onBlock(Block block, List<TransactionReceipt> receipts) {
	    	
	        for (TransactionReceipt receipt : receipts) {
	            ByteArrayWrapper txHashW = new ByteArrayWrapper(receipt.getTransaction().getHash());
	            LOG.info("---------onblock2 method,hansh byte of block------"+txHashW.getData());
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
	        LOG.info("---------waitForTx2 method,hansh byte of block------"+txHashW.getData());
	        txWaiters.put(txHashW, null);
	        long startBlock = ethereum.getBlockchain().getBestBlock().getNumber();
	        while(true) {
	            TransactionReceipt receipt = txWaiters.get(txHashW);
	            if (receipt != null) {
	                return receipt;
	            } else {
	            	long curBlock = ethereum.getBlockchain().getBestBlock().getNumber();
	            
	                if (curBlock > startBlock + 16) {
	                    throw new RuntimeException("The transaction was not included during last 16 blocks: " + txHashW.toString().substring(0,8));
	                } else {
	                    LOG.info("Waiting for block with transaction 0x" + txHashW.toString().substring(0,8) +
	                            " included (" + (curBlock - startBlock) + " blocks received so far) ...");
	                }
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
