package com.example.maps4u;

import com.google.firebase.firestore.Exclude;

public class ChatMessage {
    private String id;
    private String senderId;
    private String receiverId;
    private String messageText;
    private long timestamp;
    private String messageType;
    private String meetupName;
    private String meetupDescription;
    private double meetupLat;
    private double meetupLng;
    private String meetupAddress;
    private long meetupTime;
    private String meetupStatus;
    private String textForSender;
    private String textForReceiver;
    private boolean isEncrypted;
    private boolean isSystemMessage;

    public ChatMessage() {}

    public ChatMessage(String senderId, String receiverId, String messageText, long timestamp, String messageType) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.messageType = messageType;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getMeetupName() { return meetupName; }
    public void setMeetupName(String meetupName) { this.meetupName = meetupName; }

    public String getMeetupDescription() { return meetupDescription; }
    public void setMeetupDescription(String meetupDescription) { this.meetupDescription = meetupDescription; }

    public double getMeetupLat() { return meetupLat; }
    public void setMeetupLat(double meetupLat) { this.meetupLat = meetupLat; }

    public double getMeetupLng() { return meetupLng; }
    public void setMeetupLng(double meetupLng) { this.meetupLng = meetupLng; }

    public String getMeetupAddress() { return meetupAddress; }
    public void setMeetupAddress(String meetupAddress) { this.meetupAddress = meetupAddress; }

    public long getMeetupTime() { return meetupTime; }
    public void setMeetupTime(long meetupTime) { this.meetupTime = meetupTime; }

    public String getMeetupStatus() { return meetupStatus; }
    public void setMeetupStatus(String meetupStatus) { this.meetupStatus = meetupStatus; }

    public String getTextForSender() { return textForSender; }
    public void setTextForSender(String textForSender) { this.textForSender = textForSender; }

    public String getTextForReceiver() { return textForReceiver; }
    public void setTextForReceiver(String textForReceiver) { this.textForReceiver = textForReceiver; }

    public boolean isEncrypted() { return isEncrypted; }
    public void setEncrypted(boolean encrypted) { this.isEncrypted = encrypted; }

    public boolean isSystemMessage() { return isSystemMessage; }
    public void setSystemMessage(boolean systemMessage) { this.isSystemMessage = systemMessage; }
}