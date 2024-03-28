package com.xncoder.advanceprotection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class Popup extends PopupWindow {

    Button onBtn;
    public Popup(Context context, boolean success, String msg) {
        super(context);
        View contentView = LayoutInflater.from(context).inflate(R.layout.activity_popup, null);
        setContentView(contentView);
        setWidth(contentView.getResources().getDisplayMetrics().widthPixels - 100);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        setFocusable(true);

        ImageView icon = (ImageView) contentView.findViewById(R.id.popup_icon);
        TextView popupMsg = (TextView) contentView.findViewById(R.id.popup_msg);
        if(success) {
            icon.setImageResource(R.drawable.ic_baseline_done_24);
            popupMsg.setText(msg);
        } else {
            icon.setImageResource(R.drawable.ic_baseline_close_24);
            popupMsg.setText(msg);
        }
        onBtn = (Button) contentView.findViewById(R.id.popup_ok);
        onBtn.setOnClickListener(view -> {
            dismiss();
        });
    }
}
