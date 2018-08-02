/**
 * 
 */
package com.ethercamp.harmony.service.contracts;

import java.math.BigInteger;

import javax.annotation.PostConstruct;

import org.ethereum.core.Block;
import org.ethereum.core.CallTransaction.Invocation;
import org.ethereum.facade.Ethereum;
import org.ethereum.core.PendingStateImpl;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.listener.EthereumListener.PendingTransactionState;
import org.ethereum.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.ToString;

/**
 * @author yang
 *
 */
@ToString
class RISentEvent{
	//event Sent(address from, address to, uint amount);
	String from;
	String to;
	BigInteger amount;
	
	public RISentEvent(String from, String to, BigInteger amount) {
		super();
		this.from = from;
		this.to = to;
		this.amount = amount;
	}
	
	
	
}


public class RIContractEventlistener extends EventListener<RISentEvent> {
	
	public static final Logger LOG = LoggerFactory.getLogger(RIContractEventlistener.class);
	
	
	
	public RIContractEventlistener(PendingStateImpl pendingState) {
		super(pendingState);
		
	}
	
    public RIContractEventlistener(PendingStateImpl pendingState, String contractABI, byte[] contractAddress) {
		  super(pendingState);
		  this.initContractAddress(contractABI, contractAddress);
	}

	@Override
	protected RISentEvent onEvent(Invocation event, Block block, TransactionReceipt receipt, int txCount,
			PendingTransactionState state) {
		 if ("Sent".equals(event.function.name)) {
			 
			 String address = Hex.toHexString((byte[]) event.args[0]);
			 LOG.info("------onEvent address from------"+address);
			 String addressTo = Hex.toHexString((byte[]) event.args[1]);
			 LOG.info("------onEvent address to------"+addressTo);
			
			 LOG.info("------onEvent amount------"+ event.args[2]);
		 
		 }
		return null;
	}

	@Override
	protected boolean pendingTransactionUpdated(EventListener<RISentEvent>.PendingEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void pendingTransactionsUpdated() {
		// TODO Auto-generated method stub
		
	}



}
