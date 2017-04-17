package de.uni.oldenburg.intelligent;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

public class ContractAgent extends Agent {

	private static final long serialVersionUID = 1L;
	
	private static final String[] AGENT_NAMES = {"Alice", "Bob", "Charlie"};
	private static final AID AID_ADMIN = new AID(AdminAgent.LOCAL_NAME, AID.ISLOCALNAME);
	
	public static final String CALL_MSG_TEMPLATE = "CALL:%d:%d:%d";
	public static final String OFFER_MSG_TEMPLATE = "OFFER:%d:%d";

	private Random r = new Random();

	private int callId = 0;
	private Set<AID> otherAgents = null;
	private ACLMessage firstOffer = null;

	@Override
	protected void setup() {
		System.out.println("Contract-agent " + getAID().getName() + " is ready.");
		
		otherAgents = new HashSet<>();
		for (String name : AGENT_NAMES) {
			if (!name.equals(getAID().getLocalName())) {
				otherAgents.add(new AID(name, AID.ISLOCALNAME));
			}
		}

		addBehaviour(new LoginBehaviour());
		addBehaviour(new ReceiveMessageBehaviour());
	}

	private class LoginBehaviour extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(AID_ADMIN);
			msg.setContent(AdminAgent.LOGIN_MSG);
			send(msg);
		}
	}

	private class ReceiveMessageBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage msg = receive();

			if (msg != null) {
				System.out.println(getAID().getLocalName() + " received message from " + msg.getSender().getLocalName()
						+ " with content: " + msg.getContent());

				// Participant received call from auctioneer
				if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent() != null
						&& msg.getContent().startsWith("CALL")) {
					addBehaviour(new MakeOfferBehaviour(msg));
				}
				// Auctioneer received offer from participant
				else if (msg.getPerformative() == ACLMessage.PROPOSE) {
					if (firstOffer == null)
						firstOffer = msg;
					else {
						addBehaviour(new DecideBehaviour(firstOffer, msg));
						firstOffer = null;
					}
				} 
				// Participant received answer from auctioneer
				else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					addBehaviour(new CallForTenders(ContractAgent.this, 1000L));
				}
			} else {
				block();
			}
		}

	}

	private class DecideBehaviour extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;
		private ACLMessage offer1;
		private ACLMessage offer2;

		public DecideBehaviour(ACLMessage offer1, ACLMessage offer2) {
			this.offer1 = offer1;
			this.offer2 = offer2;
		}

		@Override
		public void action() {
			int date1 = Integer.parseInt(offer1.getContent().split(":")[2]);
			int date2 = Integer.parseInt(offer2.getContent().split(":")[2]);

			ACLMessage reply1 = offer1.createReply();
			ACLMessage reply2 = offer2.createReply();
			
			if (date1 < date2) {
				reply1.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				reply1.setContent("WON");
				reply2.setPerformative(ACLMessage.REJECT_PROPOSAL);
				reply2.setContent("LOST");
				System.out.println("Offer from " + offer1.getSender().getLocalName() + " is better.");
			} else {
				reply1.setPerformative(ACLMessage.REJECT_PROPOSAL);
				reply1.setContent("LOST");
				reply2.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				reply2.setContent("WON");
				System.out.println("Offer from " + offer2.getSender().getLocalName() + " is better.");
			}
			
			send(reply1);
			send(reply2);
		}

	}

	private class MakeOfferBehaviour extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;
		private ACLMessage call;

		public MakeOfferBehaviour(ACLMessage call) {
			this.call = call;
		}

		@Override
		public void action() {
			int min = 0, max = 0;
			String[] tokens = call.getContent().split(":");
			for (int i = 1; i < tokens.length; i++) {
				String s = tokens[i];

				if (i == 1) {
					callId = Integer.parseInt(s);
				} else if (i == 2) {
					min = Integer.parseInt(s);
				} else if (i == 3) {
					max = Integer.parseInt(s);
				}
			}

			int offer = min + r.nextInt(max - min);

			ACLMessage msg = call.createReply();
			msg.setPerformative(ACLMessage.PROPOSE);
			msg.setContent(String.format(OFFER_MSG_TEMPLATE, callId, offer));
			send(msg);
		}

	}

	private class CallForTenders extends WakerBehaviour {
		
		private static final long serialVersionUID = 1L;

		public CallForTenders(Agent a, long timeout) {
			super(a, timeout);
		}

		@Override
		public void onWake() {
			System.out.println("--------------");
			System.out.println(getAID().getLocalName() + " is the new auctioneer!");
			int start = r.nextInt(100);
			int end = start + 1 + r.nextInt(100);

			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setContent(String.format(CALL_MSG_TEMPLATE, callId++, start, end));
			for (AID receiver : otherAgents)
				msg.addReceiver(receiver);
			send(msg);

		}

	}

	@Override
	protected void takeDown() {
		System.out.println("Contract agent stopped");
	}

}
