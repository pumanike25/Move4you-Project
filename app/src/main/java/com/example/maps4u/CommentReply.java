package com.example.maps4u;

public class CommentReply {
    private String userId;
    private String userName;
    private String userImageUrl;
    private String text;
    private long timestamp;

    public CommentReply() {}

    public CommentReply(String userId, String userName, String userImageUrl, String text, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.userImageUrl = userImageUrl;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserImageUrl() { return userImageUrl; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}