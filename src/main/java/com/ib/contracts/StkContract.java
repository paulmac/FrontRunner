/* Copyright (C) 2017 Paulmac Software. All rights reserved. 
*/

package com.ib.contracts;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

public class StkContract extends Contract {
    public StkContract(String symbol) {
        symbol(symbol);
        secType(SecType.STK.name());
        exchange("SMART");
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
        if (this.secType().equals(other.secType()) && this.symbol().equals(other.symbol())) {
            this.localSymbol(other.localSymbol());
            return true;
        }
        
        return false;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();

        add( sb, "conid", conid());
        add( sb, "symbol", symbol());
        add( sb, "secType", secType());
//        add( sb, "lastTradeDateOrContractMonth", this.lastTradeDateOrContractMonth());
//        add( sb, "strike", strike());
//        add( sb, "right", right());
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
