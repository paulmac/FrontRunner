/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.contracts;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

public class OptContract extends Contract {
    public OptContract(String symbol, String lastTradeDateOrContractMonth, double strike, String right) {
        this(symbol, "SMART", lastTradeDateOrContractMonth, strike, right);
    }

    public OptContract(String symbol, String exchange, String lastTradeDateOrContractMonth, double strike, String right) {
        symbol(symbol);
        secType(SecType.OPT.name());
        exchange("SMART");
        currency("USD");
        lastTradeDateOrContractMonth(lastTradeDateOrContractMonth);
        strike(strike);
        right(right);
    }
    
    public OptContract(String symbol) {
        this.symbol(symbol);
        this.exchange("SMART");
        currency("USD");
    }

    
        @Override
    public boolean equals(Object obj) {
    
    	if (this == obj) {
    		return true;
    	}

    	if (obj == null || !(obj instanceof Contract)) {
    		return false;
    	}

        Contract other = (Contract)obj;
        
        // Light comparison
        return this.symbol().equals(other.symbol()) && 
                this.lastTradeDateOrContractMonth().equals(other.lastTradeDateOrContractMonth())&&
                this.right().equals(other.right())&&
                this.secType().equals(other.secType());
    }

}
