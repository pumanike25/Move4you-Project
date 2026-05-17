package com.example.maps4u;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PlaceCommentsAdapter extends RecyclerView.Adapter<PlaceCommentsAdapter.CommentViewHolder> {

    private Context context;
    private List<PlaceComment> commentsList;
    private String currentUserId;
    private OnCommentActionListener actionListener;

    // interface for sending back the info to home  activity
    public interface OnCommentActionListener {
        void onHelpfulClicked(PlaceComment comment, boolean isCurrentlyHelpful);
        void onUnhelpfulClicked(PlaceComment comment, boolean isCurrentlyUnhelpful);
        void onReplyClicked(PlaceComment comment);
    }

    public PlaceCommentsAdapter(Context context, List<PlaceComment> commentsList, String currentUserId, OnCommentActionListener actionListener) {
        this.context = context;
        this.commentsList = commentsList;
        this.currentUserId = currentUserId;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        PlaceComment comment = commentsList.get(position);

        holder.tvUserName.setText(comment.getUserName());
        holder.tvText.setText(comment.getText());

        String timeAgo = (String) DateUtils.getRelativeTimeSpanString(comment.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        holder.tvDate.setText(timeAgo);

        if (comment.getUserImageUrl() != null && !comment.getUserImageUrl().isEmpty()) {
            Glide.with(context).load(comment.getUserImageUrl()).into(holder.imgUser);
        } else {
            holder.imgUser.setImageResource(R.drawable.ic_profile_placeholder);
        }

        boolean isHelpful = comment.getHelpful().contains(currentUserId);
        boolean isUnhelpful = comment.getUnhelpful().contains(currentUserId);

        holder.tvHelpfulCount.setText(String.valueOf(comment.getHelpful().size()));
        holder.tvUnhelpfulCount.setText(String.valueOf(comment.getUnhelpful().size()));

        // Cchange colour if the user already voted
        holder.icHelpful.setColorFilter(isHelpful ? context.getColor(R.color.primary) : context.getColor(android.R.color.darker_gray));
        holder.tvHelpfulCount.setTextColor(isHelpful ? context.getColor(R.color.primary) : context.getColor(android.R.color.darker_gray));

        holder.icUnhelpful.setColorFilter(isUnhelpful ? context.getColor(R.color.design_default_color_error) : context.getColor(android.R.color.darker_gray));
        holder.tvUnhelpfulCount.setTextColor(isUnhelpful ? context.getColor(R.color.design_default_color_error) : context.getColor(android.R.color.darker_gray));

        // Click Listeners
        holder.btnHelpful.setOnClickListener(v -> actionListener.onHelpfulClicked(comment, isHelpful));
        holder.btnUnhelpful.setOnClickListener(v -> actionListener.onUnhelpfulClicked(comment, isUnhelpful));
        holder.btnReply.setOnClickListener(v -> actionListener.onReplyClicked(comment));

        // dynamic insert of comms
        holder.repliesContainer.removeAllViews();

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            holder.repliesContainer.setVisibility(View.VISIBLE);

            for (CommentReply reply : comment.getReplies()) {
                View replyView = LayoutInflater.from(context).inflate(R.layout.item_comment_reply, holder.repliesContainer, false);

                ImageView imgReplyUser = replyView.findViewById(R.id.imgReplyUser);
                TextView tvReplyUserName = replyView.findViewById(R.id.tvReplyUserName);
                TextView tvReplyTime = replyView.findViewById(R.id.tvReplyTime);
                TextView tvReplyText = replyView.findViewById(R.id.tvReplyText);

                tvReplyUserName.setText(reply.getUserName());
                tvReplyText.setText(reply.getText());

                String replyTimeAgo = (String) DateUtils.getRelativeTimeSpanString(reply.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                tvReplyTime.setText(replyTimeAgo);

                if (reply.getUserImageUrl() != null && !reply.getUserImageUrl().isEmpty()) {
                    Glide.with(context).load(reply.getUserImageUrl()).into(imgReplyUser);
                } else {
                    imgReplyUser.setImageResource(R.drawable.ic_profile_placeholder);
                }

                holder.repliesContainer.addView(replyView);
            }
        } else {
            holder.repliesContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUser, icHelpful, icUnhelpful;
        TextView tvUserName, tvDate, tvText, tvHelpfulCount, tvUnhelpfulCount, btnReply;
        LinearLayout btnHelpful, btnUnhelpful, repliesContainer;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUser = itemView.findViewById(R.id.imgCommentUser);
            tvUserName = itemView.findViewById(R.id.tvCommentUserName);
            tvDate = itemView.findViewById(R.id.tvCommentDate);
            tvText = itemView.findViewById(R.id.tvCommentText);

            btnHelpful = itemView.findViewById(R.id.btnHelpful);
            icHelpful = itemView.findViewById(R.id.icHelpful);
            tvHelpfulCount = itemView.findViewById(R.id.tvHelpfulCount);

            btnUnhelpful = itemView.findViewById(R.id.btnUnhelpful);
            icUnhelpful = itemView.findViewById(R.id.icUnhelpful);
            tvUnhelpfulCount = itemView.findViewById(R.id.tvUnhelpfulCount);

            btnReply = itemView.findViewById(R.id.btnReply);
            repliesContainer = itemView.findViewById(R.id.repliesContainer);
        }
    }
}