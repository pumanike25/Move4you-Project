package com.example.maps4u;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;
import java.util.Locale;

public class DiscoverPlacesAdapter extends RecyclerView.Adapter<DiscoverPlacesAdapter.PlaceViewHolder> {

    private Context context;
    private List<DiscoverPlace> placeList;
    private OnPlaceClickListener listener;

    // interface for clicking
    public interface OnPlaceClickListener {
        void onPlaceClick(DiscoverPlace place);
    }

    public DiscoverPlacesAdapter(Context context, List<DiscoverPlace> placeList, OnPlaceClickListener listener) {
        this.context = context;
        this.placeList = placeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_discover_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        DiscoverPlace place = placeList.get(position);

        holder.tvName.setText(place.getName());
        holder.tvRating.setText(String.valueOf(place.getRating()));

        holder.tvAddress.setText(place.getAddress());

        String distanceStr = String.format(Locale.getDefault(), "%.1f km", place.getDistanceInMeters() / 1000f);
        holder.tvDistance.setText(distanceStr);

        holder.tvAddress.setText(distanceStr + " • " + place.getAddress());

        if (place.getPhotoUrl() != null && !place.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(place.getPhotoUrl())
                    .transform(new CenterCrop(), new RoundedCorners(24))
                    .into(holder.imgPlace);
        } else {
            holder.imgPlace.setImageResource(R.drawable.ic_transport);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaceClick(place);
            }
        });
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlace;
        TextView tvName, tvAddress, tvRating, tvDistance;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlace = itemView.findViewById(R.id.imgPlace);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvAddress = itemView.findViewById(R.id.tvPlaceAddress);
            tvRating = itemView.findViewById(R.id.tvPlaceRating);
            tvDistance = itemView.findViewById(R.id.tvPlaceDistance);
        }
    }
}