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
        secType(SecType.OPT.name());
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
        if (this.symbol().equals(other.symbol()) &&
                // The Day section has no significance
                this.lastTradeDateOrContractMonth().regionMatches(0, other.lastTradeDateOrContractMonth(), 0, 6) &&
                this.right().equals(other.right())&&
                this.secType().equals(other.secType())) {
            
            // fix it up
            this.localSymbol(other.localSymbol());
            this.lastTradeDateOrContractMonth(other.lastTradeDateOrContractMonth()); 
            return true;
        }
        
        return false;
    }

    @Override 
    public String toString() {
        StringBuilder sb = new StringBuilder();

        add( sb, "conid", conid());
        add( sb, "symbol", symbol());
        add( sb, "secType", secType());
        add( sb, "lastTradeDateOrContractMonth", this.lastTradeDateOrContractMonth());
        add( sb, "strike", strike());
        add( sb, "right", right());
//        add( sb, "multiplier", m_multiplier);
        add( sb, "exchange", exchange());
        add( sb, "currency", currency());
//        add( sb, "localSymbol", m_localSymbol);
//        add( sb, "tradingClass", m_tradingClass);
//        add( sb, "primaryExch", m_primaryExch);
//        add( sb, "secIdType", m_secIdType);
//        add( sb, "secId", m_secId);

        return sb.toString();
    }

}
