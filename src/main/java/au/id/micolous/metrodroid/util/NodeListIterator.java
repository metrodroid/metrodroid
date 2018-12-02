package au.id.micolous.metrodroid.util;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class NodeListIterator implements Iterator<Node> {
    private int mPosition = 0;
    private NodeList mNodeList;

    public NodeListIterator(NodeList nodeList) {
        mNodeList = nodeList;
    }

    @Override
    public boolean hasNext() {
        return mPosition < mNodeList.getLength();
    }

    @Override
    public Node next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return mNodeList.item(++mPosition);
    }
}
