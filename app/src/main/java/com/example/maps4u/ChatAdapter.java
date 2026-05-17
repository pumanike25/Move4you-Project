package com.example.maps4u;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    private static final int MSG_TYPE_MEETUP = 2;
    private static final int MSG_TYPE_SYSTEM = 3;

    private Context context;
    private List<ChatMessage> chatList;
    private String currentUserId;
    private OnMeetupActionListener meetupActionListener;

    // interface for sending the decision, for meetup, back to ChatActivity
    public interface OnMeetupActionListener {
        void onAccept(ChatMessage meetupMsg);
        void onDecline(ChatMessage meetupMsg);
    }

    public ChatAdapter(Context context, List<ChatMessage> chatList, String currentUserId, OnMeetupActionListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.currentUserId = currentUserId;
        this.meetupActionListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_SYSTEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_system, parent, false);
            return new SystemMessageHolder(view);
        } else if (viewType == MSG_TYPE_MEETUP) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_meetup, parent, false);
            return new MeetupMessageHolder(view);
        } else if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatList.get(position);

        if (holder.getItemViewType() == MSG_TYPE_SYSTEM) {
            SystemMessageHolder systemHolder = (SystemMessageHolder) holder;
            systemHolder.tvMessage.setText(message.getMessageText());

        } else if (holder.getItemViewType() == MSG_TYPE_MEETUP) {
            MeetupMessageHolder meetupHolder = (MeetupMessageHolder) holder;

            meetupHolder.tvName.setText(message.getMeetupName());

            if (message.getMeetupDescription() != null && !message.getMeetupDescription().trim().isEmpty()) {
                meetupHolder.tvDesc.setVisibility(View.VISIBLE);
                meetupHolder.tvDesc.setText(message.getMeetupDescription());
            } else {
                meetupHolder.tvDesc.setVisibility(View.GONE);
            }

            meetupHolder.tvAddress.setText("Address: " + message.getMeetupAddress());

            SimpleDateFormat sdfMeetup = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
            String scheduledTime = sdfMeetup.format(new Date(message.getMeetupTime()));
            meetupHolder.tvTime.setText("Scheduled for: " + scheduledTime);

            meetupHolder.tvStatus.setText("Status: " + message.getMeetupStatus());

            // status coloring depending on the condition
            if ("ACCEPTED".equals(message.getMeetupStatus())) {
                meetupHolder.tvStatus.setTextColor(context.getColor(android.R.color.holo_green_dark));
                meetupHolder.llActions.setVisibility(View.GONE); // hide buttons if already accepted
            } else if ("DECLINED".equals(message.getMeetupStatus())) {
                meetupHolder.tvStatus.setTextColor(context.getColor(android.R.color.holo_red_dark));
                meetupHolder.llActions.setVisibility(View.GONE);
            } else {
                meetupHolder.tvStatus.setTextColor(context.getColor(android.R.color.darker_gray));

                // showing the buttons depending if the user is invtited or inviting
                if (!message.getSenderId().equals(currentUserId)) {
                    meetupHolder.llActions.setVisibility(View.VISIBLE);
                } else {
                    meetupHolder.llActions.setVisibility(View.GONE);
                }
            }

            meetupHolder.btnAccept.setOnClickListener(v -> meetupActionListener.onAccept(message));
            meetupHolder.btnDecline.setOnClickListener(v -> meetupActionListener.onDecline(message));

        } else {
            // text msg logic
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String time = sdf.format(new Date(message.getTimestamp()));

            String finalMessageText = message.getMessageText(); // Fallback for old mssg, uncrypted

            if (message.isEncrypted()) {
                if (message.getSenderId().equals(currentUserId)) {
                    // if i sent the mssg, use crypted text for me
                    finalMessageText = EncryptionHelper.decryptMessage(message.getTextForSender());
                } else {
                    // if he sent the mssg, use crypted text with my public key
                    finalMessageText = EncryptionHelper.decryptMessage(message.getTextForReceiver());
                }
            }

            if (holder.getItemViewType() == MSG_TYPE_RIGHT) {
                SentMessageHolder sentHolder = (SentMessageHolder) holder;
                sentHolder.tvMessage.setText(finalMessageText); // use decrypted text
                sentHolder.tvTime.setText(time);
            } else {
                ReceivedMessageHolder receivedHolder = (ReceivedMessageHolder) holder;
                receivedHolder.tvMessage.setText(finalMessageText); // use decrypted text
                receivedHolder.tvTime.setText(time);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = chatList.get(position);

        if (msg.isSystemMessage()) {
            return MSG_TYPE_SYSTEM;
        } else if ("meetup".equals(msg.getMessageType())) {
            return MSG_TYPE_MEETUP;
        } else if (msg.getSenderId().equals(currentUserId)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    // --- VIEW HOLDERS ---
    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        public SentMessageHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvSentMessage);
            tvTime = itemView.findViewById(R.id.tvSentTime);
        }
    }

    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        public ReceivedMessageHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvTime = itemView.findViewById(R.id.tvReceivedTime);
        }
    }

    // View Holder for Meetup
    static class MeetupMessageHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvAddress, tvTime, tvStatus;
        LinearLayout llActions;
        Button btnAccept, btnDecline;

        public MeetupMessageHolder(@NonNull View itemView) {
            super(itemView);
            tvAddress = itemView.findViewById(R.id.tvMeetupAddress);
            tvTime = itemView.findViewById(R.id.tvMeetupTime);
            tvStatus = itemView.findViewById(R.id.tvMeetupStatus);
            llActions = itemView.findViewById(R.id.llMeetupActions);
            btnAccept = itemView.findViewById(R.id.btnAcceptMeetup);
            btnDecline = itemView.findViewById(R.id.btnDeclineMeetup);
            tvName = itemView.findViewById(R.id.tvMeetupName);
            tvDesc = itemView.findViewById(R.id.tvMeetupDesc);
        }
    }

    static class SystemMessageHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        public SystemMessageHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.text_system_message);
        }
    }
}