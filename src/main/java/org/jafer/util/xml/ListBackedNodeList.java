package org.jafer.util.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ListBackedNodeList implements NodeList {
    private final List<Node> nodes;

    public ListBackedNodeList() {
        this(new ArrayList<Node>());
    }
    public ListBackedNodeList(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void add(Node node) {
        nodes.add(node);
    }

    public void addAll(NodeList list){
        int len = list.getLength();
        for (int i=0;i<len;i++) nodes.add(list.item(i));
    }

    public int getLength() {
        return nodes.size();
    }

    public Node item(int arg0) {
        return nodes.get(arg0);
    }

}
