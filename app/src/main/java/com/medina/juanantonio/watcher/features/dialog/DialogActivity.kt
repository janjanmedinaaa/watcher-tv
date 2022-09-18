package com.medina.juanantonio.watcher.features.dialog

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment

class DialogActivity : FragmentActivity() {

    companion object {
        const val DIALOG_TITLE = "DialogTitle"
        const val DIALOG_DESCRIPTION = "DialogDescription"
        const val DIALOG_POSITIVE_BUTTON = "DialogPositiveButton"
        const val DIALOG_NEGATIVE_BUTTON = "DialogNegativeButton"

        @JvmStatic
        fun getIntent(
            context: Context,
            title: String,
            description: String = "",
            positiveButton: String = context.getString(android.R.string.ok),
            negativeButton: String = context.getString(android.R.string.cancel)
        ): Intent {
            return Intent(context, DialogActivity::class.java).apply {
                putExtra(DIALOG_TITLE, title)
                putExtra(DIALOG_DESCRIPTION, description)
                putExtra(DIALOG_POSITIVE_BUTTON, positiveButton)
                putExtra(DIALOG_NEGATIVE_BUTTON, negativeButton)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#21272A")))

        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(
                this,
                DialogFragment(),
                android.R.id.content
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}