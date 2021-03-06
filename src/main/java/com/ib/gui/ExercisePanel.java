/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.gui;

import com.ib.client.Contract;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;

import com.ib.client.Types.ExerciseType;
import com.ib.client.Types.SecType;
import com.ib.controller.ApiController.IAccountHandler;
import com.ib.controller.Position;

import com.ib.gui.AccountInfoPanel.PortfolioModel;
import com.ib.util.HtmlButton;
import com.ib.util.NewTabbedPanel.INewTab;
import com.ib.util.TCombo;
import com.ib.util.UpperField;
import com.ib.util.VerticalPanel;
import com.ib.util.VerticalPanel.HorzPanel;


public class ExercisePanel extends HorzPanel implements INewTab, IAccountHandler {
	private DefaultListModel<String> m_acctList = new DefaultListModel<>();
	private JList<String> m_accounts = new JList<>( m_acctList);
	private String m_selAcct = "";
	private PortfolioModel m_portfolioModel = new PortfolioModel();
	private JTable m_portTable = new JTable( m_portfolioModel);
	
	ExercisePanel() {
		JScrollPane acctsScroll = new JScrollPane( m_accounts);
		acctsScroll.setBorder( new TitledBorder( "Select account"));
		
		JScrollPane portScroll = new JScrollPane( m_portTable);
		portScroll.setBorder( new TitledBorder( "Select long option position"));
		
		setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
		add( acctsScroll);
		add( portScroll);
		add( new ExPanel() );

		m_accounts.addListSelectionListener(e -> onAcctChanged());
	}
	
	private void onAcctChanged() {
		int i = m_accounts.getSelectedIndex();
		if (i != -1) {
			String selAcct = m_acctList.get( i);
			if (!selAcct.equals( m_selAcct) ) {
				m_selAcct = selAcct;
				m_portfolioModel.clear();
				FrontRunnerTws.INSTANCE.twsController().reqAccountUpdates(true, m_selAcct, this);
			}
		}
	}

	class ExPanel extends VerticalPanel {
		TCombo<ExerciseType> m_combo = new TCombo<>( ExerciseType.values() );
		UpperField m_qty = new UpperField( "1");
		JCheckBox m_override = new JCheckBox();
		
		ExPanel() {
			HtmlButton but = new HtmlButton( "Go") {
				protected void actionPerformed() {
					onExercise();
				}
			};
			
			m_combo.removeItem( ExerciseType.None);
			
			add( "Action", m_combo);
			add( "Quantity", m_qty);
			add( "Override", m_override);
			add( but);
		}

		void onExercise() {
			String account = m_accounts.getSelectedValue();
			int i = m_portTable.getSelectedRow();
			if (i != -1 && account != null) {
				Position position = m_portfolioModel.getPosition( i);
				FrontRunnerTws.INSTANCE.twsController().exerciseOption(account, position.contract(), m_combo.getSelectedItem(), m_qty.getInt(), m_override.isSelected() );
			}
		}
	}

	/** Show long option positions only. */
	@Override public void updatePortfolio(Position position) {
		if (position.account().equals( m_selAcct) && position.contract().secType() == SecType.OPT) {		
			m_portfolioModel.update( position);
		}
	}
        		
	/** Called when the tab is first visited. */
	@Override public void activated() {
		for (String account : FrontRunnerTws.INSTANCE.accountList() ) {
			m_acctList.addElement( account);
		}
	}
	
	/** Called when the tab is closed by clicking the X. */
	@Override public void closed() {
	}

	@Override public void accountValue(String account, String key, String value, String currency) {
	}

	@Override public void accountTime(String timeStamp) {
	}

	@Override public void accountDownloadEnd(String account) {
	}
}
