package com.bq.robotic.androidino;

import android.os.Bundle;

public interface DialogListener {

    /**
     * Called when a dialog completes.
     *
     * Executed by the thread that initiated the dialog.
     *
     * @param values
     *            Key-value string pairs extracted from the response.
     */
    public void onComplete(Bundle values);

    /**
     * Called when a dialog is canceled by the user.
     *
     * Executed by the thread that initiated the dialog.
     *
     */
    public void onCancel();
}
