package com.medina.juanantonio.watcher.features.dialog

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.databinding.FragmentIdleDialogBinding
import com.medina.juanantonio.watcher.shared.utils.autoCleared

class IdleDialogFragment : androidx.fragment.app.DialogFragment() {

    companion object {
        private const val TITLE_KEY = "TITLE_KEY"
        private var onClickListener: (IdleDialogButton) -> Unit = {}

        fun getInstance(
            title: String,
            onClickListener: (IdleDialogButton) -> Unit = {}
        ): IdleDialogFragment {
            this.onClickListener = onClickListener

            return IdleDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(TITLE_KEY, title)
                }
            }
        }
    }

    private var binding: FragmentIdleDialogBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIdleDialogBinding.inflate(inflater, container, false)
        dialog?.window?.decorView?.setBackgroundColor(Color.TRANSPARENT)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(TITLE_KEY)
        binding.textViewPromptTitle.text = getString(R.string.are_you_still_watching_title, title)
        binding.buttonAskAgainLater.setOnClickListener {
            onClickListener(IdleDialogButton.ASK_AGAIN)
            dismiss()
        }
        binding.buttonPlayWithoutAskingAgain.setOnClickListener {
            onClickListener(IdleDialogButton.PLAY_WITHOUT_ASKING)
            dismiss()
        }
        binding.buttonImDone.setOnClickListener {
            onClickListener(IdleDialogButton.DONE)
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onClickListener(IdleDialogButton.ASK_AGAIN)
        onClickListener = {}
    }
}

enum class IdleDialogButton {
    ASK_AGAIN,
    PLAY_WITHOUT_ASKING,
    DONE
}