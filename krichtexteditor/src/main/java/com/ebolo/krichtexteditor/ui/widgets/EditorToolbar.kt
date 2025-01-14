package com.ebolo.krichtexteditor.ui.widgets

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import com.bitbucket.eventbus.EventBus
import com.ebolo.krichtexteditor.R
import com.ebolo.krichtexteditor.RichEditor
import com.ebolo.krichtexteditor.ui.enums.EditorButton

class EditorToolbar(private val editor: RichEditor, private val buttonsLayout: List<EditorButton>) {
    var linkButtonAction: (() -> Unit)? = null
    var imageButtonAction: (() -> Unit)? = null

    private lateinit var buttons: Map<EditorButton, ImageView>

    var buttonActivatedColor: Int = Color.CYAN
    var buttonDeactivatedColor: Int = Color.GRAY

    fun createToolbar(container: LinearLayout) {
        fun createButton(actionType: EditorButton): ImageView {
            val buttonLayout = LayoutInflater.from(container.context).inflate(
                R.layout.editor_toolbar_button,
                container,
                false
            )

            val imageView = buttonLayout.findViewById<AppCompatImageView>(R.id.image_view)
            imageView.apply {
                setImageResource(EditorButton.actionButtonDrawables[actionType]!!)
                setColorFilter(buttonDeactivatedColor)
                setOnClickListener {
                    when (actionType) {
                        EditorButton.IMAGE -> imageButtonAction?.invoke()

                        EditorButton.LINK -> linkButtonAction?.invoke()

                        else -> editor.command(actionType)
                    }
                }
            }

            container.addView(buttonLayout)

            return imageView
        }
        buttons = buttonsLayout.associateWith { createButton(it) }
    }

    fun setupListeners(context: Context) {
        val eventBus = EventBus.getInstance()
        buttonsLayout.forEach { buttonId ->
            eventBus.on("style", "style_$buttonId") {
                val state = it as Boolean
                Handler(context.mainLooper).post {
                    buttons[buttonId]?.setColorFilter(
                        when {
                            state -> buttonActivatedColor
                            else -> buttonDeactivatedColor
                        }
                    )
                }
            }
        }
    }
}