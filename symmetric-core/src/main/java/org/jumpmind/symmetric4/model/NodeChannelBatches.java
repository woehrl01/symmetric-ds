package org.jumpmind.symmetric4.model;

import java.io.Serializable;
import java.util.List;

public class NodeChannelBatches implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Node node;
    private Channel channel;
    private List<OutgoingBatch> batches;
    
    public NodeChannelBatches() {
    }
    
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
    
    public Channel getChannel() {
        return channel;
    }
    
    public void setNode(Node node) {
        this.node = node;
    }
    
    public Node getNode() {
        return node;
    }
    
    public void setBatches(List<OutgoingBatch> batches) {
        this.batches = batches;
    }
    
    public List<OutgoingBatch> getBatches() {
        return batches;
    }

}
