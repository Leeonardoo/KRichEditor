package com.ebolo.krichtexteditor.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bitbucket.eventbus.EventBus
import com.ebolo.krichtexteditor.R
import com.ebolo.krichtexteditor.RichEditor
import com.ebolo.krichtexteditor.ui.enums.EditorButton
import com.ebolo.krichtexteditor.ui.view.TextEditorWebView
import com.ebolo.krichtexteditor.ui.widgets.EditorToolbar
import com.github.salomonbrys.kotson.fromJson
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson

class KRichEditorView : FrameLayout {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    private val eventBus by lazy { EventBus.getInstance() }
    private val menuFormatButtons = mutableMapOf<EditorButton, ImageView>()
    private val menuFormatHeadingBlocks = mutableMapOf<EditorButton, View>()

    val editor = RichEditor()

    private val webView: TextEditorWebView by lazy { findViewById(R.id.web_view) }
    private val toolbar: LinearLayout by lazy { findViewById(R.id.toolbar) }
    private val toolsContainer: LinearLayout by lazy {
        findViewById(R.id.tools_container)
    }

    private lateinit var editorToolbar: EditorToolbar

    // Customizable settings
    private var placeHolder = "Start writing..."
    private var imageButtonAction: (() -> Unit)? = null
    private var showToolbar = true
    private var readOnly = false

    // Default buttons layout
    private var allowedButtons = listOf(
        EditorButton.UNDO,
        EditorButton.REDO,
        EditorButton.IMAGE,
        EditorButton.LINK,
        EditorButton.BOLD,
        EditorButton.ITALIC,
        EditorButton.UNDERLINE,
        EditorButton.SUBSCRIPT,
        EditorButton.SUPERSCRIPT,
        EditorButton.STRIKETHROUGH,
        EditorButton.JUSTIFY_LEFT,
        EditorButton.JUSTIFY_CENTER,
        EditorButton.JUSTIFY_RIGHT,
        EditorButton.JUSTIFY_FULL,
        EditorButton.ORDERED,
        EditorButton.UNORDERED,
        EditorButton.CHECK,
        EditorButton.NORMAL,
        EditorButton.H1,
        EditorButton.H2,
        EditorButton.H3,
        EditorButton.H4,
        EditorButton.H5,
        EditorButton.H6,
        EditorButton.INDENT,
        EditorButton.OUTDENT,
        EditorButton.BLOCK_QUOTE,
        EditorButton.BLOCK_CODE,
        EditorButton.CODE_VIEW
    )

    private var buttonActivatedColor: Int = ContextCompat.getColor(context, R.color.colorAccent)
    private var buttonDeactivatedColor: Int = ContextCompat.getColor(context, R.color.tintColor)

    private var onInitialized: (() -> Unit)? = null

    fun initView(options: Options) {
        //WebView.setWebContentsDebuggingEnabled(true)
        onInitialized = options.onInitialized
        placeHolder = options.placeHolder
        imageButtonAction = options.onClickImageButton
        allowedButtons = options.allowedButtons
        buttonActivatedColor = options.buttonActivatedColor
        buttonDeactivatedColor = options.buttonDeactivatedColor
        showToolbar = options.showToolbar
        readOnly = options.readOnly

        inflate(context, R.layout.view_krich_editor, this)
        setupWebView()

        if (!showToolbar || readOnly) {
            toolbar.visibility = View.GONE
        } else if (showToolbar) {
            editorToolbar = EditorToolbar(editor, allowedButtons).apply {
                if (EditorButton.LINK in allowedButtons)
                    linkButtonAction = { onMenuButtonClicked(EditorButton.LINK) }
                if (EditorButton.IMAGE in allowedButtons)
                    imageButtonAction = { onMenuButtonClicked(EditorButton.IMAGE) }

                buttonActivatedColor = this@KRichEditorView.buttonActivatedColor
                buttonDeactivatedColor = this@KRichEditorView.buttonDeactivatedColor
            }
            editorToolbar.createToolbar(toolsContainer)
        }

        setupListeners(context)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.initWebView {
            it.webViewClient = WebViewClient()
            it.webChromeClient = WebChromeClient()

            it.settings.javaScriptEnabled = true
            it.settings.domStorageEnabled = true

            isFocusable = true
            isFocusableInTouchMode = true

            editor.apply {
                mWebView = it
                placeHolder = this@KRichEditorView.placeHolder
                onInitialized = {
                    this@KRichEditorView.onInitialized?.invoke()

                    if (readOnly) editor.disable()
                }
            }
            it.addJavascriptInterface(editor, "KRichEditor")

            it.loadUrl("file:///android_asset/richEditor.html")
        }
    }

    /**
     * Function:    onMenuButtonClicked
     * Description: Declare sets of actions of formatting buttonsLayout
     * @param type type of action defined in EditorButton class
     * @param param param of action if necessary
     * @see EditorButton
     */
    private fun onMenuButtonClicked(
        type: EditorButton,
        param: String? = null
    ) {
        when (type) {
            EditorButton.SIZE -> editor.command(EditorButton.SIZE, param!!)
            EditorButton.FORE_COLOR -> editor.command(EditorButton.FORE_COLOR, param!!)
            EditorButton.BACK_COLOR -> editor.command(EditorButton.BACK_COLOR, param!!)
            EditorButton.IMAGE -> when (imageButtonAction) {
                null -> Toast
                    .makeText(context, "Image handler not implemented!", Toast.LENGTH_LONG)
                    .show()
                else -> imageButtonAction?.invoke()
            }
            EditorButton.LINK -> {
                editor.getSelection { value ->
                    val selection = Gson().fromJson<Map<String, Int>>(value)
                    if (selection["length"]!! > 0) {
                        if (!editor.selectingLink()) {
                            // Setup dialog view
                            val inflatedView = LayoutInflater.from(context).inflate(
                                R.layout.address_input_dialog,
                                parent as ViewGroup?,
                                false
                            )

                            val addressInput: TextInputEditText? = inflatedView.findViewById(
                                R.id.address_input
                            )

//                            MaterialAlertDialogBuilder(context, dialogStyle).also {
//                                it.setView(inflatedView)
//                                it.setPositiveButton(android.R.string.ok) { _, _ ->
//                                    val urlValue = addressInput?.text.toString()
//                                    if (urlValue.startsWith("http://", true)
//                                        || urlValue.startsWith("https://", true)
//                                    ) {
//                                        hideMenu()
//                                        editor.command(EditorButton.LINK, urlValue)
//                                    } else {
//                                        Toast.makeText(
//                                            context,
//                                            R.string.link_missing_protocol,
//                                            Toast.LENGTH_LONG
//                                        ).show()
//                                    }
//                                }
//                                it.setNegativeButton(android.R.string.cancel) { _, _ -> }
//                            }.show()
                        } else {
                            editor.command(EditorButton.LINK, "")
                        }
                    } else {
                        Snackbar.make(
                            rootView,
                            R.string.link_empty_warning,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
            else -> editor.command(type)
        }
    }

    private fun setupListeners(context: Context) {
        if (showToolbar) {
            listOf(
                EditorButton.NORMAL,
                EditorButton.H1,
                EditorButton.H2,
                EditorButton.H3,
                EditorButton.H4,
                EditorButton.H5,
                EditorButton.H6
            ).forEach { style ->
                eventBus.on("style", "style_$style") {
                    val state = it as Boolean
                    Handler(context.mainLooper).post {
                        val background =
                            if (state) R.drawable.round_rectangle_blue else R.drawable.round_rectangle_white

                        menuFormatHeadingBlocks[style]?.setBackgroundResource(background)
                    }
                }
            }

            listOf(
                EditorButton.BOLD,
                EditorButton.ITALIC,
                EditorButton.UNDERLINE,
                EditorButton.STRIKETHROUGH,
                EditorButton.JUSTIFY_CENTER,
                EditorButton.JUSTIFY_FULL,
                EditorButton.JUSTIFY_LEFT,
                EditorButton.JUSTIFY_RIGHT,
                EditorButton.SUBSCRIPT,
                EditorButton.SUPERSCRIPT,
                EditorButton.CODE_VIEW,
                EditorButton.BLOCK_CODE,
                EditorButton.BLOCK_QUOTE,
                EditorButton.LINK
            ).forEach { style ->
                eventBus.on("style", "style_$style") {
                    val state = it as Boolean
                    Handler(context.mainLooper).post {
                        menuFormatButtons[style]?.setColorFilter(
                            when {
                                state -> buttonActivatedColor
                                else -> buttonDeactivatedColor
                            }
                        )
                    }
                }
            }

            editorToolbar.setupListeners(context)
        }
    } // Else do nothing as this is not necessary

    fun removeListeners() {
        if (showToolbar) eventBus.unsubscribe("style")
    }
}

/**
 * Class serve as a container of options to be transmitted to the editor to set it up
 */
data class Options(
    val placeHolder: String,
    val onClickImageButton: () -> Unit = {},
    val onClickAddUrl: () -> Unit = {},
    val allowedButtons: List<EditorButton> = listOf(
        EditorButton.UNDO,
        EditorButton.REDO,
        EditorButton.IMAGE,
        EditorButton.LINK,
        EditorButton.BOLD,
        EditorButton.ITALIC,
        EditorButton.UNDERLINE,
        EditorButton.SUBSCRIPT,
        EditorButton.SUPERSCRIPT,
        EditorButton.STRIKETHROUGH,
        EditorButton.JUSTIFY_LEFT,
        EditorButton.JUSTIFY_CENTER,
        EditorButton.JUSTIFY_RIGHT,
        EditorButton.JUSTIFY_FULL,
        EditorButton.ORDERED,
        EditorButton.UNORDERED,
        EditorButton.NORMAL,
        EditorButton.H1,
        EditorButton.H2,
        EditorButton.H3,
        EditorButton.H4,
        EditorButton.H5,
        EditorButton.H6,
        EditorButton.INDENT,
        EditorButton.OUTDENT,
        EditorButton.BLOCK_QUOTE,
        EditorButton.BLOCK_CODE,
        EditorButton.CODE_VIEW
    ),
    val buttonActivatedColor: Int = Color.CYAN,
    val buttonDeactivatedColor: Int = Color.GRAY,
    val onInitialized: () -> Unit = {},
    val showToolbar: Boolean = true,
    val readOnly: Boolean = false
)