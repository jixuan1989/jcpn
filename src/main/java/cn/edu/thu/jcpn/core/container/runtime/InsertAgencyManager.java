package cn.edu.thu.jcpn.core.container.runtime;

import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeIndividualCPN;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leven on 2017/2/18.
 */
public class InsertAgencyManager {

    private static InsertAgencyManager instance;

    private RuntimeFoldingCPN foldingCPN;
    private Map<INode, Map<Integer, InsertAgency>> nodeCidAgencies;

    public static InsertAgencyManager getInstance() {
        return instance;
    }

    private InsertAgencyManager(RuntimeFoldingCPN foldingCPN) {
        this.foldingCPN = foldingCPN;
        this.nodeCidAgencies = new HashMap<>();
    }

    public static void init(RuntimeFoldingCPN foldingCPN) {
        if (null == instance) {
            synchronized (InsertAgencyManager.class) {
                if (null == instance) {
                    instance = new InsertAgencyManager(foldingCPN);
                }
            }
        }
    }

    public void addTokens(INode to, int cid, List<IToken> tokens) {
        Map<Integer, InsertAgency> cidAgencies = nodeCidAgencies.computeIfAbsent(to, obj -> new HashMap<>());

        InsertAgency agency = cidAgencies.get(cid);
        if (null == agency) {
            RuntimeIndividualCPN toCPN = foldingCPN.getIndividualCPN(to);
            if (null == toCPN) return;

            IRuntimeContainer toContainer = toCPN.getContainer(cid);
            if (null == toContainer) return;

            agency = new InsertAgency(toContainer);
            cidAgencies.put(toContainer.getId(), agency);
        }

        synchronized (agency) {
            agency.accept(tokens);
        }
    }

    public void runAgencyEvents() {
        nodeCidAgencies.forEach((node, cidAgencies) -> cidAgencies.forEach((cid, agency) -> agency.shift()));
    }

    public void runAgencyEvents(INode node) {
        nodeCidAgencies.getOrDefault(node, new HashMap<>()).forEach((cid, agency) -> agency.shift());
    }
}
