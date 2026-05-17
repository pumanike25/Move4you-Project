package com.example.maps4u;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChatMessageModelTest {

    @Test
    public void testDefaultMessageIsNotEncrypted() {
        // When creating an old or simple message, it shouldn't be encrypted by default
        ChatMessage message = new ChatMessage("sender123", "receiver456", "Hello!", 1620000000L, "text");

        assertFalse("The standard message must not have the encrypted flag set", message.isEncrypted());
        assertFalse("The standard message must not be a system message", message.isSystemMessage());
        assertEquals("The text should be 'Hello!'", "Hello!", message.getMessageText());
    }

    @Test
    public void testEncryptedMessageFlags() {
        // Simulate creating an E2EE message
        ChatMessage secureMessage = new ChatMessage();
        secureMessage.setEncrypted(true);
        secureMessage.setTextForSender("Encrypted_Text_For_Me");
        secureMessage.setTextForReceiver("Encrypted_Text_For_You");

        assertTrue("The message must be marked as encrypted", secureMessage.isEncrypted());
        assertNull("The raw text must be null or empty", secureMessage.getMessageText());
        assertEquals("Encrypted_Text_For_You", secureMessage.getTextForReceiver());
    }

    @Test
    public void testMeetupStatusUpdates() {
        // Test the logic for meetups
        ChatMessage meetupMessage = new ChatMessage();
        meetupMessage.setMessageType("meetup");
        meetupMessage.setMeetupStatus("PENDING");

        assertEquals("The initial status must be PENDING", "PENDING", meetupMessage.getMeetupStatus());

        meetupMessage.setMeetupStatus("ACCEPTED");
        assertEquals("The status must be updatable to ACCEPTED", "ACCEPTED", meetupMessage.getMeetupStatus());
    }
}