package de.uni.oldenburg.intelligent.contract;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class StartSystem {

	public static void main(String[] args) {
		Runtime runtime = Runtime.instance();
		Profile p = new ProfileImpl();
		AgentContainer container = runtime.createMainContainer(p);
		
		try {
			container.createNewAgent("rma", "jade.tools.rma.rma", null).start();
			container.createNewAgent("admin", AdminAgent.class.getName(), null).start();
			
			for (String agentName : ContractAgent.AGENT_NAMES) {
				container.createNewAgent(agentName, ContractAgent.class.getName(), null).start();
			}
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

}
