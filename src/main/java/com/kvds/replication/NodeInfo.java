package com.kvds.replication;

import java.util.Objects;

/**
 * Represents information about a node in the cluster.
 */
public class NodeInfo {
    
    private final String nodeId;
    private final String host;
    private final int port;
    private NodeStatus status;
    private long lastHeartbeat;
    
    public enum NodeStatus {
        ACTIVE,
        INACTIVE,
        FAILED
    }
    
    public NodeInfo(String nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.status = NodeStatus.ACTIVE;
        this.lastHeartbeat = System.currentTimeMillis();
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public NodeStatus getStatus() {
        return status;
    }
    
    public void setStatus(NodeStatus status) {
        this.status = status;
    }
    
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
    
    public String getAddress() {
        return host + ":" + port;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return Objects.equals(nodeId, nodeInfo.nodeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }
    
    @Override
    public String toString() {
        return "NodeInfo{" +
                "nodeId='" + nodeId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", status=" + status +
                '}';
    }
}
