package com.octo.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;

public class BaseTest {

    static protected final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows")
    )));
    static protected final StartedMockNode a = network.createNode(CordaX500Name.parse("O=BankA,L=New York,C=US"));
    static protected final StartedMockNode bc = network.createNode(CordaX500Name.parse("O=CentralBank,L=New York,C=US"));

}
