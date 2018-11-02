package uk.ac.mas.dare;

import java.util.ArrayList;

// The comms channel is simply a shared space (similar to that used by
// the inTray within the producer/consumer problem.  We assume a maximum
// number of messages that can be inserted or retrieved by each agent.
//
// The aim should be that whenever a message is sent from one agent to
// another, then each message is tagged with a "to" field as well as a
// "from" field.  We could either create JSON strings and place these into
// comms channel, or (better) we create a new comms object that can be passed
// into this channel and shared.
public class CommsChannel {
	
	private ArrayList<CommsObject> messages;
	private final int maxNumMessages = 1;
	
	public CommsChannel() {
		messages = new ArrayList<CommsObject>();
	}
	
	// The first two sets of methods will attempt to either send or receive
	// messages on the comms channel, or will go into a wait loop until the
	// comms channel is free (to send) or has a message (to receive)
	
	public synchronized void send(CommsObject co) {

		while (this.messages.size() == maxNumMessages) {
			// System.out.println("CommsChannel full.  Agent sender must now wait until the current message has been received.");
			try {
				wait();
			} catch (InterruptedException e) {};
		}
		this.messages.add(co);
		// System.out.println("A message has successfully been sent (send)...");
		notifyAll();
	}
	
	public synchronized CommsObject receive() {

		CommsObject co = null;
		while (this.messages.size() == 0) {
			// System.out.println("Comms Channel empty - agent must wait...");
			try {
				wait();
			} catch (InterruptedException e) {};
		}
		co = this.messages.get(0);
		this.messages.remove(0);
		// System.out.println("A message has been removed from the channel");
		notifyAll();
		return co;
	}

	// The second two sets of methods ...

	public synchronized CommsObject receiveFor(String recipient) {

		CommsObject co = null;
		
		while ((this.messages.size() == 0) ||
				(this.messages.get(0).getRecipientAgent() == null) ||
				(!this.messages.get(0).getRecipientAgent().equals(recipient))){
			// System.out.println("Message for "+recipient+" not in Comms Channel - agent must wait...");
			try {
				wait();
			} catch (InterruptedException e) {};
		}
		co = this.messages.get(0);
		this.messages.remove(0);
		// System.out.println("A message has been removed from the channel");
		notifyAll();
		return co;
	}

	public synchronized CommsObject receiveIfNotSender(String sender) {

		CommsObject co = null;
		while ((this.messages.size() == 0) ||
				(this.messages.get(0).getSendingAgent().equals(sender))){
			// System.out.println("No Message or msg from "+sender+" in Comms Channel - agent must wait...");
			try {
				wait();
			} catch (InterruptedException e) {};
		}
		co = this.messages.get(0);
		this.messages.remove(0);
		// System.out.println("A message has been removed from the channel");
		notifyAll();
		return co;
	}

	// The third two sets of methods will attempt to either send or receive
	// messages on the comms channel, or will fail, returning some indication
	// to the caller

	public synchronized boolean sendMessageIfPossibe(CommsObject co) {

		boolean success = false;
		
		if (this.messages.size() < maxNumMessages) {
			// System.out.println("Number of messages prior..."+this.messages.size());
			this.messages.add(co);
			// System.out.println("Number of messages post..."+this.messages.size());

			success = true;
			// System.out.println("A message has successfully been sent (sendMessageIfPossible)...");

		}
		notifyAll();
		return success;
	}

	// This is a variant of receive, which checks to see if a message is in
	// the comms channel, and if not, returns null rather than waiting.
	public synchronized CommsObject getAvailableMessage() {

		CommsObject co = null;
		if (this.messages.size() != 0) {
			co = this.messages.get(0);
		}
		this.messages.remove(0);
		notifyAll();
		return co;
	}


}
