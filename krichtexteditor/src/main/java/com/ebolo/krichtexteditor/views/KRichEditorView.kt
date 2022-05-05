package com.ebolo.krichtexteditor.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.*
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.bitbucket.eventbus.EventBus
import com.ebolo.krichtexteditor.R
import com.ebolo.krichtexteditor.RichEditor
import com.ebolo.krichtexteditor.ui.actionImageViewStyle
import com.ebolo.krichtexteditor.ui.widgets.ColorPaletteView
import com.ebolo.krichtexteditor.ui.widgets.EditorButton
import com.ebolo.krichtexteditor.ui.widgets.EditorToolbar
import com.ebolo.krichtexteditor.ui.widgets.TextEditorWebView
import com.ebolo.krichtexteditor.utils.rgbToHex
import com.github.salomonbrys.kotson.fromJson
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.sdk27.coroutines.onClick
import ru.whalemare.sheetmenu.SheetMenu

class KRichEditorView : FrameLayout {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    private val eventBus by lazy { EventBus.getInstance() }
    private val menuFormatButtons = mutableMapOf<EditorButton, ImageView>()
    private val menuFormatHeadingBlocks = mutableMapOf<EditorButton, View>()

    private lateinit var fontSizeTextView: TextView
    private lateinit var textColorPalette: ColorPaletteView
    private lateinit var highlightColorPalette: ColorPaletteView

    val editor = RichEditor()

    private val webView: TextEditorWebView by lazy { findViewById(R.id.web_view) }
    private val toolbar: LinearLayout by lazy { findViewById(R.id.toolbar) }
    private val menuButton: ImageView by lazy { findViewById(R.id.menu_button) }
    private val toolsContainer: LinearLayout by lazy {
        findViewById(R.id.tools_container)
    }
    private val editorMenuScrollView: NestedScrollView by lazy {
        findViewById(R.id.editor_menu_scroll_view)
    }
    private val editorMenuContainer: FrameLayout by lazy {
        findViewById(R.id.editor_menu_container)
    }

    private lateinit var editorToolbar: EditorToolbar

    // Customizable settings
    private var placeHolder = "Start writing..."
    private var imageButtonAction: (() -> Unit)? = null
    private var showToolbar = true
    private var showMenuButton = true
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

    @StyleRes
    private var dialogStyle =
        com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog

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
        showMenuButton = options.showMenuButton

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

        editorMenuScrollView.verticalLayout { //TODO migrate to XML layout
            verticalLayout {
                backgroundColorResource = R.color.gray_100

                // First box: font size, alignment, basic text format
                linearLayout {
                    backgroundColorResource = R.color.white
                    padding = dip(16)
                    weightSum = 10f

                    // Font size box
                    verticalLayout {
                        gravity = Gravity.CENTER
                        backgroundResource = R.drawable.btn_white_round_rectangle

                        textView(R.string.font_size) {
                            textSize = 10f
                            gravity = Gravity.CENTER
                        }

                        fontSizeTextView = textView("normal") {
                            textSize = 18f
                            textColorResource = R.color.light_blue_500
                            gravity = Gravity.CENTER

                            onClick { _ ->
                                //val menu = PopupMenu(ui.ctx, this@textView)
                                SheetMenu().apply {
                                    titleId = R.string.font_sizes_title
                                    menu = R.menu.font_sizes_menu
                                    showIcons = false // true, by default

                                    click = MenuItem.OnMenuItemClickListener {
                                        onMenuButtonClicked(
                                            EditorButton.SIZE, when (it.itemId) {
                                                R.id.font_size_small -> "small"
                                                R.id.font_size_large -> "large"
                                                R.id.font_size_huge -> "huge"
                                                else -> ""
                                            }
                                        )
                                        true
                                    }
                                }.show(context)
                            }
                        }.lparams { topMargin = dip(8) }

                    }.lparams(width = dip(0), height = dip(100)) { weight = 3f }

                    verticalLayout {
                        gravity = Gravity.CENTER

                        // Justify(alignment) buttonsLayout
                        linearLayout {
                            backgroundResource = R.drawable.round_rectangle_white
                            gravity = Gravity.CENTER
                            setPadding(16, dip(6), 16, dip(6))

                            fun justifyButton(
                                type: EditorButton,
                                drawable: Int,
                                neighbor: Boolean = false
                            ) = menuFormatButtons.put(type, imageView(drawable) {
                                padding = dip(8)
                                backgroundResource = R.drawable.btn_white_material

                                onClick { onMenuButtonClicked(type) }
                            }.lparams {
                                if (neighbor) marginStart = dip(16)
                            }.apply { actionImageViewStyle() })

                            justifyButton(
                                EditorButton.JUSTIFY_LEFT,
                                R.drawable.ic_format_align_left
                            )
                            justifyButton(
                                EditorButton.JUSTIFY_CENTER,
                                R.drawable.ic_format_align_center,
                                true
                            )
                            justifyButton(
                                EditorButton.JUSTIFY_RIGHT,
                                R.drawable.ic_format_align_right,
                                true
                            )
                            justifyButton(
                                EditorButton.JUSTIFY_FULL,
                                R.drawable.ic_format_align_justify,
                                true
                            )

                        }.lparams(width = matchParent, height = dip(46))

                        // Basic formats: bold, italic, underline, strike
                        linearLayout {
                            backgroundResource = R.drawable.round_rectangle_white
                            gravity = Gravity.CENTER
                            setPadding(dip(16), dip(6), dip(16), dip(6))

                            fun formatButton(
                                type: EditorButton,
                                drawable: Int
                            ) = menuFormatButtons.put(type, imageView(drawable) {
                                padding = dip(8)
                                backgroundResource = R.drawable.btn_white_material

                                onClick { onMenuButtonClicked(type) }
                            }
                                .lparams { weight = 1f }
                                .apply { actionImageViewStyle() })

                            formatButton(EditorButton.BOLD, R.drawable.ic_format_bold)
                            formatButton(EditorButton.ITALIC, R.drawable.ic_format_italic)
                            formatButton(
                                EditorButton.UNDERLINE,
                                R.drawable.ic_format_underlined
                            )
                            formatButton(
                                EditorButton.STRIKETHROUGH,
                                R.drawable.ic_format_strikethrough
                            )

                        }.lparams(width = matchParent, height = dip(46)) {
                            topMargin = dip(8)
                        }

                    }.lparams(width = dip(0), height = dip(100)) {
                        marginStart = dip(8)
                        weight = 7f
                    }

                }.lparams(width = matchParent, height = wrapContent)

                // Second box: text color and highlight
                verticalLayout {
                    backgroundColorResource = R.color.white
                    padding = dip(16)

                    textView(R.string.font_color) {
                        textSize = 10f
                    }.lparams(width = matchParent)

                    linearLayout {
                        gravity = Gravity.CENTER
                        backgroundResource = R.drawable.round_rectangle_white

                        textColorPalette = ankoView(::ColorPaletteView, 0) {
                            onColorChange {
                                onMenuButtonClicked(
                                    EditorButton.FORE_COLOR,
                                    this.selectedColor
                                )
                            }
                        }.lparams(width = matchParent, height = wrapContent)

                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(8)
                    }

                    textView(R.string.font_highlight_color) {
                        textSize = 10f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(16)
                    }

                    linearLayout {
                        gravity = Gravity.CENTER

                        highlightColorPalette = ankoView(::ColorPaletteView, 0) {
                            backgroundResource = R.drawable.round_rectangle_white
                            gravity = Gravity.CENTER

                            onColorChange {
                                onMenuButtonClicked(
                                    EditorButton.BACK_COLOR,
                                    this.selectedColor
                                )
                            }
                        }.lparams(width = wrapContent, height = wrapContent) {
                            weight = 1f
                        }

                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(8)
                    }

                }.lparams(width = matchParent, height = wrapContent) {
                    topMargin = dip(8)
                }

                // Third box: headings
                horizontalScrollView {
                    linearLayout {
                        padding = dip(16)
                        backgroundColorResource = R.color.white

                        fun headingBlock(
                            type: EditorButton,
                            previewText: Pair<String, Float>,
                            text: Int,
                            neighbor: Boolean = false
                        ) = menuFormatHeadingBlocks.put(type, verticalLayout {
                            backgroundResource = R.drawable.round_rectangle_white
                            gravity = Gravity.CENTER
                            setPadding(0, 0, 0, dip(8))

                            onClick { onMenuButtonClicked(type) }

                            textView(previewText.first) {
                                maxLines = 1
                                gravity = Gravity.CENTER
                                textSize = previewText.second
                            }.lparams(width = wrapContent, height = dip(32))

                            view {
                                backgroundColor = 0xe0e0e0.opaque
                            }.lparams(width = matchParent, height = dip(0.5f)) {
                                bottomMargin = dip(4)
                            }

                            textView(text) {
                                textSize = 10f
                                gravity = Gravity.CENTER
                            }

                        }.lparams(width = dip(80), height = matchParent) {
                            if (neighbor) marginStart = dip(8)
                        })

                        headingBlock(
                            EditorButton.NORMAL, previewText = "AaBbCcDd" to 10f,
                            text = R.string.font_style_normal
                        )

                        headingBlock(
                            EditorButton.H1, previewText = "AaBb" to 18f,
                            text = R.string.font_style_heading_1, neighbor = true
                        )

                        headingBlock(
                            EditorButton.H2, previewText = "AaBbC" to 14f,
                            text = R.string.font_style_heading_2, neighbor = true
                        )

                        headingBlock(
                            EditorButton.H3, previewText = "AaBbCcD" to 12f,
                            text = R.string.font_style_heading_3, neighbor = true
                        )

                        headingBlock(
                            EditorButton.H4, previewText = "AaBbCcDd" to 12f,
                            text = R.string.heading_4, neighbor = true
                        )

                        headingBlock(
                            EditorButton.H5, previewText = "AaBbCcDd" to 12f,
                            text = R.string.heading_5, neighbor = true
                        )

                        headingBlock(
                            EditorButton.H6, previewText = "AaBbCcDd" to 12f,
                            text = R.string.heading_6, neighbor = true
                        )

                    }.lparams { topMargin = dip(8) }

                }.lparams(width = matchParent, height = wrapContent)

                /**
                 * Inner function:  additionalFormatBox
                 * Description:     Create a box with 4 buttonsLayout divided into two
                 *                  smaller ones.
                 * Param pattern:   a pair mapping ActionType Int to Drawable Res Id
                 * @param item1 first button
                 * @param item2 second button
                 * @param item3 third button
                 * @param item4 fourth button
                 */
                fun additionalFormatBox(
                    item1: Pair<EditorButton, Int>,
                    item2: Pair<EditorButton, Int>,
                    item3: Pair<EditorButton, Int>,
                    item4: Pair<EditorButton, Int>
                ) = linearLayout {
                    backgroundColorResource = R.color.white
                    padding = dip(16)

                    fun innerBox(
                        item1: Pair<EditorButton, Int>,
                        item2: Pair<EditorButton, Int>,
                        isSecond: Boolean = false
                    ) = linearLayout {
                        backgroundResource = R.drawable.round_rectangle_white
                        gravity = Gravity.CENTER
                        setPadding(0, dip(8), 0, dip(8))

                        fun formatButton(
                            item: Pair<EditorButton, Int>,
                            isSecond: Boolean = false
                        ) =
                            menuFormatButtons.put(
                                item.first,
                                imageView(item.second) {
                                    backgroundResource = R.drawable.btn_white_material
                                    padding = dip(10)

                                    onClick { onMenuButtonClicked(item.first) }
                                }
                                    .lparams { if (isSecond) marginStart = dip(32) }
                                    .apply { actionImageViewStyle() }
                            )

                        formatButton(item1)
                        formatButton(item2, true)
                    }.lparams(width = wrapContent, height = wrapContent) {
                        weight = 1f
                        if (isSecond) marginStart = dip(8)
                    }

                    innerBox(item1, item2)
                    innerBox(item3, item4, true)

                }.lparams(width = matchParent, height = wrapContent) {
                    topMargin = dip(8)
                }

                additionalFormatBox(
                    item1 = EditorButton.SUBSCRIPT to R.drawable.ic_format_subscript,
                    item2 = EditorButton.SUPERSCRIPT to R.drawable.ic_format_superscript,
                    item3 = EditorButton.BLOCK_QUOTE to R.drawable.ic_format_quote,
                    item4 = EditorButton.BLOCK_CODE to R.drawable.ic_code_block
                )

                additionalFormatBox(
                    item1 = EditorButton.INDENT to R.drawable.ic_format_indent_increase,
                    item2 = EditorButton.OUTDENT to R.drawable.ic_format_indent_decrease,
                    item3 = EditorButton.UNORDERED to R.drawable.ic_format_list_bulleted,
                    item4 = EditorButton.ORDERED to R.drawable.ic_format_list_numbered
                )

                // Sixth box: insert buttonsLayout - image, link, table, code
                verticalLayout {
                    backgroundColorResource = R.color.white
                    padding = dip(16)
                    isBaselineAligned = false

                    textView(R.string.font_insert) {
                        textSize = 10f
                    }.lparams(width = matchParent)

                    linearLayout {
                        backgroundResource = R.drawable.round_rectangle_white
                        gravity = Gravity.CENTER
                        padding = dip(8)

                        fun insertButton(
                            type: EditorButton,
                            drawable: Int
                        ) =
                            themedImageView(
                                drawable,
                                R.style.ActionImageView
                            ) {
                                this.id = id
                                backgroundResource = R.drawable.btn_white_material
                                padding = dip(8)

                                onClick { onMenuButtonClicked(type) }
                            }.lparams { weight = 1f }.apply { actionImageViewStyle() }

                        insertButton(EditorButton.CHECK, R.drawable.ic_format_list_check)
                        insertButton(EditorButton.IMAGE, R.drawable.ic_insert_photo)
                        menuFormatButtons[EditorButton.LINK] =
                            insertButton(EditorButton.LINK, R.drawable.ic_insert_link)
                        insertButton(EditorButton.CODE_VIEW, R.drawable.ic_code_review)

                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(8)
                    }

                }.lparams(width = matchParent, height = wrapContent) {
                    topMargin = dip(8)
                }
            }.lparams(width = matchParent, height = wrapContent)
        }

        if (!showMenuButton) {
            menuButton.visibility = View.GONE
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

                            MaterialAlertDialogBuilder(context, dialogStyle).also {
                                it.setView(inflatedView)
                                it.setPositiveButton(R.string.ok) { _, _ ->
                                    val urlValue = addressInput?.text.toString()
                                    if (urlValue.startsWith("http://", true)
                                        || urlValue.startsWith("https://", true)
                                    ) {
                                        hideMenu()
                                        editor.command(EditorButton.LINK, urlValue)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.link_missing_protocol,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                it.setNegativeButton(R.string.cancel) { _, _ -> }
                            }.show()
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

    // Preparation
    private fun hideMenu() {
        menuButton.setColorFilter(buttonDeactivatedColor)
        editorMenuContainer.visibility = View.GONE
    }

    private fun showMenu() {
        menuButton.setColorFilter(buttonActivatedColor)
        editorMenuContainer.visibility = View.VISIBLE
        editor.refreshStyle()
    }

    private fun setupListeners(context: Context) {
        if (showToolbar) {
            // Setup ui handlers for editor menu
            eventBus.on("style", "style_${EditorButton.SIZE}") {
                context.runOnUiThread { fontSizeTextView.text = (it as String) }
            }

            eventBus.on("style", "style_${EditorButton.FORE_COLOR}") {
                val selectedColor = rgbToHex(it as String)
                if (selectedColor != null)
                    context.runOnUiThread { textColorPalette.selectedColor = selectedColor }
            }

            eventBus.on("style", "style_${EditorButton.BACK_COLOR}") {
                val selectedColor = rgbToHex(it as String)
                if (selectedColor != null)
                    context.runOnUiThread { highlightColorPalette.selectedColor = selectedColor }
            }

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
                    context.runOnUiThread {
                        menuFormatHeadingBlocks[style]?.backgroundResource = when {
                            state -> R.drawable.round_rectangle_blue
                            else -> R.drawable.round_rectangle_white
                        }
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
                    context.runOnUiThread {
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

            menuButton.setOnClickListener {
                when (editorMenuContainer.visibility) {
                    View.VISIBLE -> hideMenu()
                    else -> showMenu()
                }
            }
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
    val showMenuButton: Boolean = true,
    val readOnly: Boolean = false
)