package com.damir00109;

import java.util.ArrayList;

public class RadioChannel {
	private int channelNum;
	private ArrayList<RadioSender> senders;
	private ArrayList<RadioListener> listeners;

	public RadioChannel(int id) {
		this.channelNum = id;
	}

	public RadioSender newSender() {
		return new RadioSender(this);
	}
	public RadioListener newListener() {
		return new RadioListener(this);
	}
}
