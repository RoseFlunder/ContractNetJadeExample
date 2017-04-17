package de.uni.oldenburg.intelligent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AdminAgent extends Agent {

	private static final long serialVersionUID = 1L;
	
	public static final String LOCAL_NAME = "admin";
	
	public static final String START_MSG = "START";
	
	public static final String LOGIN_MSG = "LOGIN";
	public static final String LOGIN_RESPONSE_AUCTIONEER = "AUCTIONEER";
	public static final String LOGIN_RESPONSE_PARTICIPANT = "PARTICIPANT";
	
	
	private AID firstAuctioneer = null;
	private int agentsLoggedIn = 0;

	@Override
	protected void setup() {
		System.out.println("Admin-agent " + getAID().getName() + " is ready.");
		
		addBehaviour(new ReceiveMessagesBehaviour());
	}
	
	
	private class ReceiveMessagesBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage msg = receive();
			
			if (msg != null) {
				System.out.println(getAID() + " received message from" + msg.getSender().getLocalName()
						+ " with content: " + msg.getContent());
				//check for login messages from the three participants
				if (msg.getPerformative() == ACLMessage.INFORM && LOGIN_MSG.equals(msg.getContent())) {
					if (firstAuctioneer == null)
						firstAuctioneer = msg.getSender();
					++agentsLoggedIn;
				} 
				//check for user message to start the system
				else  if (msg.getPerformative() == ACLMessage.REQUEST && START_MSG.equals(msg.getContent())) {
					ACLMessage reply = msg.createReply();
					//start the system if all three participants are logged in
					if (agentsLoggedIn == 3) {
						ACLMessage startSystemMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						startSystemMsg.addReceiver(firstAuctioneer);
						send(startSystemMsg);
					}
					//send an error if the system is not ready yet
					else {
						System.out.println(getAID().getName() + ": Sending refuse message");
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("Not ready yet");
					}
					send(reply);
				}
			} else {
				block();
			}
		}
	}

	@Override
	protected void takeDown() {
		System.out.println("Admin-agent stopped");
	}

}
