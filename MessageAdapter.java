package com.xiehe.ai.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.xiehe.ai.R;
import com.xiehe.ai.models.Message;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_USER_MESSAGE = 1;
    private static final int TYPE_AI_MESSAGE = 2;
    private static final int TYPE_IMAGE_MESSAGE = 3;
    
    private List<Message> messageList;
    
    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }
    
    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if ("user".equals(message.getRole())) {
            return message.getImageData() != null ? TYPE_IMAGE_MESSAGE : TYPE_USER_MESSAGE;
        } else {
            return TYPE_AI_MESSAGE;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case TYPE_USER_MESSAGE:
                return new UserMessageViewHolder(
                    inflater.inflate(R.layout.item_user_message, parent, false)
                );
            case TYPE_AI_MESSAGE:
                return new AIMessageViewHolder(
                    inflater.inflate(R.layout.item_ai_message, parent, false)
                );
            case TYPE_IMAGE_MESSAGE:
                return new ImageMessageViewHolder(
                    inflater.inflate(R.layout.item_image_message, parent, false)
                );
            default:
                return new AIMessageViewHolder(
                    inflater.inflate(R.layout.item_ai_message, parent, false)
                );
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        
        switch (holder.getItemViewType()) {
            case TYPE_USER_MESSAGE:
                ((UserMessageViewHolder) holder).bind(message);
                break;
            case TYPE_AI_MESSAGE:
                ((AIMessageViewHolder) holder).bind(message);
                break;
            case TYPE_IMAGE_MESSAGE:
                ((ImageMessageViewHolder) holder).bind(message);
                break;
        }
    }
    
    @Override
    public int getItemCount() {
        return messageList.size();
    }
    
    // 用户消息ViewHolder
    public static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textMessage;
        
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_user_message);
        }
        
        public void bind(Message message) {
            textMessage.setText(message.getText());
        }
    }
    
    // AI消息ViewHolder
    public static class AIMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textMessage;
        private ImageView avatar;
        
        public AIMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_ai_message);
            avatar = itemView.findViewById(R.id.image_ai_avatar);
        }
        
        public void bind(Message message) {
            textMessage.setText(message.getText());
            
            // 设置AI头像
            Glide.with(itemView.getContext())
                .load(R.drawable.ic_ai_avatar)
                .circleCrop()
                .into(avatar);
        }
    }
    
    // 图像消息ViewHolder
    public static class ImageMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textMessage;
        private ImageView imageMessage;
        
        public ImageMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_image_message);
            imageMessage = itemView.findViewById(R.id.image_message);
        }
        
        public void bind(Message message) {
            textMessage.setText(message.getText());
            
            // 加载图像
            if (message.getImageData() != null) {
                try {
                    byte[] imageBytes = android.util.Base64.decode(message.getImageData(), android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    imageMessage.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    imageMessage.setImageResource(R.drawable.ic_image_error);
                }
            }
        }
    }
}