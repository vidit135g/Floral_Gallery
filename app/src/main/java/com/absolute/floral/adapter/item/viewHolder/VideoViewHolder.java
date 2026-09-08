package com.absolute.floral.adapter.item.viewHolder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.absolute.floral.R;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.ui.ItemActivity;
import com.absolute.floral.util.ItemViewUtil;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;

/** Inline video — an ExoPlayer surface with the default controls, created on first tap. */
public class VideoViewHolder extends ViewHolder {

    private ExoPlayer player;
    private boolean prepared;

    public VideoViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View inflateView(ViewGroup container) {
        ViewGroup v = super.inflateVideoView(container);
        final ImageView thumb = itemView.findViewById(R.id.image);
        ItemViewUtil.bindTransitionView(thumb, albumItem);

        thumb.setOnClickListener(view -> startInline());
        View badge = itemView.findViewById(R.id.play_badge);
        if (badge != null) badge.setOnClickListener(view -> startInline());
        return v;
    }

    private void startInline() {
        if (itemView == null) return;
        StyledPlayerView pv = itemView.findViewById(R.id.player_view);
        final ImageView thumb = itemView.findViewById(R.id.image);
        final View badge = itemView.findViewById(R.id.play_badge);
        if (pv == null) return;

        if (player == null) {
            player = new ExoPlayer.Builder(itemView.getContext()).build();
            player.setMediaItem(MediaItem.fromUri(albumItem.getUri(itemView.getContext())));
            player.setRepeatMode(
                    com.absolute.floral.data.Settings.getInstance(itemView.getContext()).loopVideos()
                            ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
            player.prepare();
            player.addListener(new Player.Listener() {
                @Override public void onRenderedFirstFrame() {
                    if (thumb != null) thumb.setVisibility(View.GONE);
                }
            });
            pv.setPlayer(player);
            prepared = true;
        }
        pv.setVisibility(View.VISIBLE);
        if (badge != null) badge.setVisibility(View.GONE);
        player.setPlayWhenReady(true);
        try { ((ItemActivity) itemView.getContext()).onVideoInline(); } catch (Throwable ignored) {}
    }

    public void pausePlayback() {
        if (player != null) player.setPlayWhenReady(false);
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
            prepared = false;
        }
    }

    @Override
    public boolean isAtRest() {
        return true;
    }

    @Override
    public void onSharedElementEnter() {
        // thumbnail is already the shared element; nothing extra needed
    }

    @Override
    public void onSharedElementExit(final ItemActivity.Callback callback) {
        pausePlayback();
        callback.done();
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
