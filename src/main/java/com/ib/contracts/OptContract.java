/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.contracts;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
    

public class OptContract extends Contract {

   final static Logger logger = LoggerFactory.getLogger(OptContract.class);
    
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
        
        buildLs();    
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
        add( sb, "right", right());
        add( sb, "strike", strike());
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

    @Override 
    public String localSymbol() { 
        if (StringUtils.isEmpty(super.localSymbol()))
            buildLs();
        return super.localSymbol();
    }

    private void buildLs() {
        
        StringBuilder sb = new StringBuilder("      00000000"); // format of a Opt Contract LocalSymbol

        sb.replace(0, symbol().length(), symbol());
        sb.insert(6, this.lastTradeDateOrContractMonth().substring(2));
        sb.insert(12, right().getApiString());
//        String s = new Double(strike()).toString();
//        if (s.contains("."))
        double s = strike();
        String i = new Integer((int)s).toString(); // Math.abs(strike());
        sb.replace(18 - i.length(), 18, i); //  insert(18 - i.length(), i);
        double c = s - ((int)s);
        if (c > 0) { // there is a fractional part
            String cents = (new Double(c).toString()).substring(2);
            sb.replace(18, 18 + cents.length(), cents);                
        }
        
        logger.info("Calculated localSymbol {}", sb.toString());
        super.localSymbol(sb.toString());
    }
}
