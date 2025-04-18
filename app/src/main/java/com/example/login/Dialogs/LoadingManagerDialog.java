package com.example.login.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.login.R;

/**
 * A utility class to manage loading dialogs throughout the application
 */
public class LoadingManagerDialog {

    private static AlertDialog loadingDialog;

    /**
     * Shows a loading circle dialog
     * @param activity The activity where the dialog should be displayed
     * @param message The message to display with the loading circle
     */
    public static void showLoading(Activity activity, String message) {
        // Dismiss any existing dialog
        hideLoading();

        if (activity == null || activity.isFinishing()) {
            return;
        }

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Inflate the loading view
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading_manager, null);
        TextView tvMessage = dialogView.findViewById(R.id.tvLoadingMessage);

        // Set the message
        tvMessage.setText(message);

        // Set the view and make it not cancelable
        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();

        // Set background to be transparent
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Show the dialog
        loadingDialog.show();
    }

    /**
     * Hides the loading circle dialog
     */
    public static void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }
}