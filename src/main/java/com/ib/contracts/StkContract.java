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
        return this.secType().equals(other.secType()) && this.symbol().equals(other.symbol());//                if (contract.equals(pos.contract()))
    }

}
