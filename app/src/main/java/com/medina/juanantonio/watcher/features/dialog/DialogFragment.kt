package com.medina.juanantonio.watcher.features.dialog


import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction

class DialogFragment : GuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        val dialogTitle =
            activity?.intent?.getStringExtra(DialogActivity.DIALOG_TITLE)
        val dialogDescription =
            activity?.intent?.getStringExtra(DialogActivity.DIALOG_DESCRIPTION)

        return Guidance(
            dialogTitle,
            dialogDescription,
            "",
            null
        )
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val dialogPositiveButton =
            activity?.intent?.getStringExtra(DialogActivity.DIALOG_POSITIVE_BUTTON)
        val dialogNegativeButton =
            activity?.intent?.getStringExtra(DialogActivity.DIALOG_NEGATIVE_BUTTON)

        val positiveAction = GuidedAction.Builder(context)
            .id(ACTION_ID_POSITIVE)
            .title(dialogPositiveButton)
            .build()
        actions.add(positiveAction)
        val negativeAction = GuidedAction.Builder(context)
            .id(ACTION_ID_NEGATIVE)
            .title(dialogNegativeButton)
            .build()
        actions.add(negativeAction)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        requireActivity().run {
            intent.data = Uri.parse("${action.id}")
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    companion object {
        const val ACTION_ID_POSITIVE = 1L
        const val ACTION_ID_NEGATIVE = ACTION_ID_POSITIVE + 1L
    }
}