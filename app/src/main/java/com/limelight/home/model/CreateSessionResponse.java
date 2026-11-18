package com.limelight.home.model;

import com.google.gson.annotations.SerializedName;

public class CreateSessionResponse {

    private int status;
    private boolean success;
    private String message;
    
    // In some cases like 401, the server might return "error" instead of "message"
    private String error;

    private Data data;

    public static class Data {
        private String status;
        private Host host;
        private Queue queue;

        public static class Host {
            private int id;
            private String ip;
            private String hostname;
            private int port;
            private String status;

            public int getId() { return id; }
            public String getIp() { return ip; }
            public String getHostname() { return hostname; }
            public int getPort() { return port; }
            public String getStatus() { return status; }
        }

        public static class Queue {
            private boolean queued;
            private int position;
            private int total;
            @SerializedName("estimatedWaitSeconds")
            private int estimatedWaitSeconds;

            public boolean isQueued() { return queued; }
            public int getPosition() { return position; }
            public int getTotal() { return total; }
            public int getEstimatedWaitSeconds() { return estimatedWaitSeconds; }
        }

        public String getStatus() { return status; }
        public Host getHost() { return host; }
        public Queue getQueue() { return queue; }
    }

    public int getStatus() { return status; }
    public boolean isSuccess() { return success; }
    
    public String getMessage() { 
        if (message != null) return message;
        return error;
    }

    public String getDataStatus() {
        return data != null ? data.getStatus() : null;
    }

    public Data.Queue getQueue() {
        return data != null ? data.getQueue() : null;
    }

    public String getIp() {
        if (data != null && data.getHost() != null) {
            return data.getHost().getIp();
        }
        return null;
    }

    public int getPort() {
        if (data != null && data.getHost() != null) {
            return data.getHost().getPort();
        }
        return 0;
    }
}
