package com.example.maps4u;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private Context context;
    private List<User> friendsList;

    public ChatListAdapter(Context context, List<User> friendsList) {
        this.context = context;
        this.friendsList = friendsList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_user, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        User friend = friendsList.get(position);

        holder.tvName.setText(friend.getUsername());

        if (friend.getImageUrl() != null && !friend.getImageUrl().isEmpty()) {
            Glide.with(context).load(friend.getImageUrl()).into(holder.imgUser);
        } else {
            holder.imgUser.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // when clicking on a friend, open the chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("TARGET_USER_ID", friend.getUid());
            intent.putExtra("TARGET_USER_NAME", friend.getUsername());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUser;
        TextView tvName;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUser = itemView.findViewById(R.id.imgChatUser);
            tvName = itemView.findViewById(R.id.tvChatUserName);
        }
    }
}