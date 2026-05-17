package com.example.maps4u;

import java.util.ArrayList;
import java.util.List;

public class PlaceComment {
    private String id;
    private String userId;
    private String userName;
    private String userImageUrl;
    private String text;
    private long timestamp;
    private List<String> helpful;
    private List<String> unhelpful;
    private List<CommentReply> replies;

    public PlaceComment() {
        helpful = new ArrayList<>();
        unhelpful = new ArrayList<>();
        replies = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserImageUrl() { return userImageUrl; }
    public void setUserImageUrl(String userImageUrl) { this.userImageUrl = userImageUrl; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<String> getHelpful() { return helpful; }
    public void setHelpful(List<String> helpful) { this.helpful = helpful; }

    public List<String> getUnhelpful() { return unhelpful; }
    public void setUnhelpful(List<String> unhelpful) { this.unhelpful = unhelpful; }

    public List<CommentReply> getReplies() { return replies; }
    public void setReplies(List<CommentReply> replies) { this.replies = replies; }
}