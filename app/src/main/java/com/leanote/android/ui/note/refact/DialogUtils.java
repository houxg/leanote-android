package com.leanote.android.ui.note.refact;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.leanote.android.R;

public class DialogUtils {

    public static void editLink(Context context, String title, String link, @NonNull final ChangedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_link, null);
        final EditText urlEdit = (EditText) view.findViewById(R.id.linkURL);
        final EditText titleEdit = (EditText) view.findViewById(R.id.linkText);
        titleEdit.setText(title);
        urlEdit.setText(link);
        new AlertDialog.Builder(context)
                .setTitle("Edit link")
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        listener.onChanged(titleEdit.getText().toString(), urlEdit.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    public interface ChangedListener {
        void onChanged(String title, String link);
    }
}
