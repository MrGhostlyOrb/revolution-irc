package io.mrarm.irc.drawer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class DrawerMenuListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DRAWER_HEADER = 0;
    private static final int TYPE_SERVER_HEADER = 1;
    private static final int TYPE_CHANNEL = 2;
    private static final int TYPE_SEPARATOR = 3;
    private static final int TYPE_MENU_ITEM = 4;

    private List<ServerConnectionInfo> mServers;
    private ArrayList<DrawerMenuItem> mMenuItems = new ArrayList<>();
    private TreeMap<Integer, ServerConnectionInfo> mItemIndexToServerMap = new TreeMap<>();
    private int mCurrentItemCount;

    public DrawerMenuListAdapter(List<ServerConnectionInfo> servers) {
        mServers = servers;
        notifyServerListChanged();
        for (ServerConnectionInfo connectionInfo : servers) {
            connectionInfo.addOnChannelListChangeListener((ServerConnectionInfo connection,
                                                           List<String> newChannels) -> {
                notifyServerListChanged();
            });
        }
    }

    public void addMenuItem(DrawerMenuItem item) {
        this.mMenuItems.add(item);
    }

    public ArrayList<DrawerMenuItem> getMenuItems() {
        return mMenuItems;
    }

    private void updateItemIndexToServerMap() {
        int currentIndex = 1;
        mItemIndexToServerMap.clear();
        for (ServerConnectionInfo info : mServers) {
            mItemIndexToServerMap.put(currentIndex, info);
            if (info.isExpandedInDrawer() && info.getChannels() != null)
                currentIndex += info.getChannels().size();
            currentIndex += 2;
        }
        mCurrentItemCount = currentIndex;
    }

    public void notifyServerListChanged() {
        updateItemIndexToServerMap();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_DRAWER_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_header, viewGroup, false);
            return new SimpleViewHolder(view);
        } else if (viewType == TYPE_SEPARATOR) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_separator, viewGroup, false);
            return new SimpleViewHolder(view);
        } else if (viewType == TYPE_SERVER_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_server_item, viewGroup, false);
            return new ServerHeaderHolder(this, view);
        } else if (viewType == TYPE_CHANNEL) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_channel_item, viewGroup, false);
            return new ChannelHolder(view);
        } else if (viewType == TYPE_MENU_ITEM) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_menu_item, viewGroup, false);
            return new MenuItemHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        if (viewType == TYPE_SERVER_HEADER || viewType == TYPE_CHANNEL) {
            Map.Entry<Integer, ServerConnectionInfo> entry =
                    mItemIndexToServerMap.floorEntry(position);
            if (viewType == TYPE_SERVER_HEADER)
                ((ServerHeaderHolder) holder).bind(entry.getValue());
            else
                ((ChannelHolder) holder).bind(entry.getValue(), position - entry.getKey() - 1);
        } else if (viewType == TYPE_MENU_ITEM) {
            ((MenuItemHolder) holder).bind(mMenuItems.get(position - mCurrentItemCount));
        }
    }

    @Override
    public int getItemCount() {
        return mCurrentItemCount + mMenuItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_DRAWER_HEADER;
        if (position >= mCurrentItemCount)
            return TYPE_MENU_ITEM;
        Map.Entry<Integer, ServerConnectionInfo> entry = mItemIndexToServerMap.floorEntry(position);
        if (entry == null || entry.getKey() == position)
            return TYPE_SERVER_HEADER;
        if (position == entry.getKey() + (entry.getValue().isExpandedInDrawer() &&
                entry.getValue().getChannels() != null ?
                entry.getValue().getChannels().size() : 0) + 1)
            return TYPE_SEPARATOR;
        return TYPE_CHANNEL;
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {

        public SimpleViewHolder(View v) {
            super(v);
        }

    }

    public static class ServerHeaderHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private DrawerMenuListAdapter mAdapter;
        private ServerConnectionInfo mServerInfo;
        private TextView mServerName;
        private ImageView mExpandIcon;

        public ServerHeaderHolder(DrawerMenuListAdapter adapter, View v) {
            super(v);
            mAdapter = adapter;
            mServerName = (TextView) v.findViewById(R.id.server_name);
            mExpandIcon = (ImageView) v.findViewById(R.id.server_expand_icon);
            v.findViewById(R.id.server_entry).setOnClickListener(this);
        }

        public void bind(ServerConnectionInfo info) {
            mServerInfo = info;
            mServerName.setText(info.getName());
            mExpandIcon.setRotation(mServerInfo.isExpandedInDrawer() ? 180.f : 0.f);
        }

        @Override
        public void onClick(View v) {
            mServerInfo.setExpandedInDrawer(!mServerInfo.isExpandedInDrawer());
            mAdapter.updateItemIndexToServerMap();
            for (Map.Entry<Integer, ServerConnectionInfo> entry :
                    mAdapter.mItemIndexToServerMap.entrySet()) {
                if (entry.getValue() == mServerInfo && mServerInfo.getChannels() != null) {
                    if (mServerInfo.isExpandedInDrawer())
                        mAdapter.notifyItemRangeInserted(entry.getKey() + 1,
                                mServerInfo.getChannels().size());
                    else
                        mAdapter.notifyItemRangeRemoved(entry.getKey() + 1,
                                mServerInfo.getChannels().size());
                    break;
                }
            }

            mExpandIcon.setRotation(mServerInfo.isExpandedInDrawer() ? 180.f : 0.f);
            int toDegrees = (mServerInfo.isExpandedInDrawer() ? 0 : 360);
            RotateAnimation rotate = new RotateAnimation(180, toDegrees,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(250);
            rotate.setInterpolator(new AccelerateDecelerateInterpolator());
            mExpandIcon.startAnimation(rotate);
        }

    }

    public static class ChannelHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private TextView mName;

        public ChannelHolder(View v) {
            super(v);
            mName = (TextView) v.findViewById(R.id.channel_name);
            v.findViewById(R.id.channel_entry).setOnClickListener(this);
        }

        public void bind(ServerConnectionInfo info, int channelIndex) {
            mName.setText(info.getChannels().get(channelIndex));
        }

        @Override
        public void onClick(View v) {
            //
        }

    }

    public static class MenuItemHolder extends RecyclerView.ViewHolder {

        private View mEntry;
        private TextView mName;
        private ImageView mIcon;

        public MenuItemHolder(View v) {
            super(v);
            mName = (TextView) v.findViewById(R.id.item_name);
            mIcon = (ImageView) v.findViewById(R.id.item_icon);
            mEntry = v.findViewById(R.id.item_entry);
        }

        public void bind(DrawerMenuItem item) {
            mName.setText(item.getName());
            mIcon.setImageDrawable(item.getIcon());
            mEntry.setOnClickListener(item.mListener);
        }

    }

}