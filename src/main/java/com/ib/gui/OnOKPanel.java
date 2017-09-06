package com.ib.gui;

import com.ib.client.OrderCondition;

import com.ib.util.VerticalPanel;

public abstract class OnOKPanel extends VerticalPanel {
	public abstract OrderCondition onOK();
}