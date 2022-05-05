package com.ebolo.krichtexteditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.Toast
import com.bitbucket.eventbus.EventBus
import com.ebolo.krichtexteditor.ui.enums.EditorButton
import com.ebolo.krichtexteditor.utils.QuillFormat
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * Rich Editor = Rich Editor Action + Rich Editor Callback
 * Created by even.wu on 8/8/17.
 * Ported by ebolo(daothanhduy305) on 21/12/2017
 */
class RichEditor {
    private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }
    private var currentFormat = QuillFormat()
    var html: String? = null

    private val mFontBlockGroup by lazy {
        listOf(
            EditorButton.NORMAL,
            EditorButton.H1,
            EditorButton.H2,
            EditorButton.H3,
            EditorButton.H4,
            EditorButton.H5,
            EditorButton.H6
        )
    }
    private val mTextAlignGroup by lazy {
        mapOf(
            EditorButton.JUSTIFY_LEFT to "",
            EditorButton.JUSTIFY_CENTER to "center",
            EditorButton.JUSTIFY_RIGHT to "right",
            EditorButton.JUSTIFY_FULL to "justify"
        )
    }
    private val mListStyleGroup by lazy {
        mapOf(
            EditorButton.ORDERED to "ordered",
            EditorButton.UNORDERED to "bullet"
        )
    }

    lateinit var placeHolder: String
        // Allow the webview layer to access the placeholder string
        @JavascriptInterface get

    lateinit var mWebView: WebView
    var onInitialized: (() -> Unit)? = null
    var styleUpdatedCallback: ((type: EditorButton, value: Any) -> Unit)? = null
    var onTextChanged: ((String) -> Unit)? = null

    // region Low level function access

    /**
     * Method to allow the lower webview layer to set the html content
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param html String
     */
    @JavascriptInterface
    fun returnHtml(html: String) {
        this.html = html
    }

    /**
     * Method to allow the lower webview layer to be able to set the current style data
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param currentStyle String
     */
    @JavascriptInterface
    fun updateCurrentStyle(currentStyle: String) = try {
        Log.d("FontStyle", currentStyle)
        updateStyle(gson.fromJson(currentStyle))
    } catch (e: Exception) {
        e.printStackTrace()
    }

    /**
     * Method to allow the lower webview layer to be able to log the message to Android logging
     * interface
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param message String
     * @return Int
     */
    @JavascriptInterface
    fun debugJs(message: String) = Log.d("JS", message)

    /**
     * Method to allow the lower webview layer to invoke the code block set to be run on init
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @return Unit?
     */
    @JavascriptInterface
    fun onInitialized() = onInitialized?.invoke()

    @JavascriptInterface
    fun onTextChanged(source: String) {
        if (source == "user" || source == "api") {
            getHtmlContent { html ->
                onTextChanged?.invoke(html)
            }
        }
    }

    // endregion

    // region Inner interfaces

    /**
     * Interface as the callback for Java API on HTML returned
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    interface OnHtmlReturned {
        fun process(html: String)
    }

    /**
     * Interface as the callback for Java API on text returned
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    interface OnTextReturned {
        fun process(text: String)
    }

    /**
     * Interface as the callback for Java API on contents (delta) returned
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    interface OnContentsReturned {
        fun process(contents: String)
    }

    // endregion

    /**
     * Method to refresh the current style
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    fun refreshStyle() = getStyle {
        try {
            updateStyle(gson.fromJson(it))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Private function to update the current style of the editor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param quillFormat QuillFormat
     */
    private fun updateStyle(quillFormat: QuillFormat) {
        // Log.d("FontStyle", gson.toJson(fontStyle))

        if (currentFormat.isBold != quillFormat.isBold) {
            notifyFontStyleChange(EditorButton.BOLD, quillFormat.isBold ?: false)
        }

        if (currentFormat.isItalic != quillFormat.isItalic) {
            notifyFontStyleChange(EditorButton.ITALIC, quillFormat.isItalic ?: false)
        }

        if (currentFormat.isUnderline != quillFormat.isUnderline) {
            notifyFontStyleChange(EditorButton.UNDERLINE, quillFormat.isUnderline ?: false)
        }

        if (currentFormat.isStrike != quillFormat.isStrike) {
            notifyFontStyleChange(EditorButton.STRIKETHROUGH, quillFormat.isStrike ?: false)
        }

        if (currentFormat.isCode != quillFormat.isCode) {
            notifyFontStyleChange(EditorButton.CODE_VIEW, quillFormat.isCode ?: false)
        }

        quillFormat.header = quillFormat.header ?: 0

        if (currentFormat.header != quillFormat.header) {
            mFontBlockGroup.indices.forEach {
                notifyFontStyleChange(mFontBlockGroup[it], (quillFormat.header == it))
            }
        }

        if (currentFormat.script != quillFormat.script) {
            notifyFontStyleChange(EditorButton.SUBSCRIPT, (quillFormat.script == "sub"))
            notifyFontStyleChange(EditorButton.SUPERSCRIPT, (quillFormat.script == "super"))
        }

        quillFormat.align = quillFormat.align ?: ""

        if (currentFormat.align != quillFormat.align) {
            mTextAlignGroup.forEach {
                notifyFontStyleChange(
                    it.key,
                    (quillFormat.align == it.value)
                )
            }
        }

        if (currentFormat.list != quillFormat.list) {
            mListStyleGroup.forEach {
                notifyFontStyleChange(it.key, (quillFormat.list == it.value))
            }
        }

        notifyFontStyleChange(EditorButton.SIZE, quillFormat.size)

        if (currentFormat.link != quillFormat.link)
            notifyFontStyleChange(EditorButton.LINK, !quillFormat.link.isNullOrBlank())

        currentFormat = quillFormat
    }

    /**
     * Private method to notify when the style has been changed
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param type Int of the action button
     * @param value Any data for the action
     */
    private fun notifyFontStyleChange(type: EditorButton, value: Any) {
        when (styleUpdatedCallback) {
            null -> EventBus.getInstance().post("style", "style_$type", value)
            else -> styleUpdatedCallback!!.invoke(type, value)
        }
    }

    // region Js wrapper

    // region General commands

    /**
     * Method to undo the last change
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun undo() = load("javascript:undo()")

    /**
     * Method to redo the last undo
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun redo() = load("javascript:redo()")

    /**
     * Method to make the cursor to focus into view
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param showKeyboard Set to true to implicitly show keyboard after focus
     */
    fun focus(showKeyboard: Boolean = false) = run {
        mWebView.requestFocus()
        load("javascript:focus()")
        if (showKeyboard)
            (mWebView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
                ?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
    }

    /**
     * Method to disable the editor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    fun disable() = load("javascript:disable()")

    /**
     * Method to enable back the editor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    fun enable() = load("javascript:enable()")

    /**
     * Method to change the background color of the editor container
     *
     * @param colorHex New background color preferably in Hex string
     */
    fun setContainerBackgroundColor(colorHex: String) =
        load("javascript:setContainerBackgroundColor('$colorHex')")

    /**
     * Method to change the background color of the editor container
     *
     * @param colorHex New background color preferably in Hex string
     */
    fun setTextColor(colorHex: String) =
        load("javascript:setTextColor('$colorHex')")

    /**
     * Method to change the container text-size. Other formatting
     * options such as Heading will be based on this size
     *
     * @param fontSizePx New fontSize in px
     */
    fun setTextSize(fontSizePx: Float) =
        load("javascript:setTextSize('${fontSizePx}px')")

    // endregion

    // region Font
    /**
     * Method to bold to current text or the next at the cursor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun bold() = load("javascript:bold()")

    /**
     * Method to italic to current text or the next at the cursor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun italic() = load("javascript:italic()")

    /**
     * Method to underline to current text or the next at the cursor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun underline() = load("javascript:underline()")

    /**
     * Method to strike to current text or the next at the cursor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun strikethrough() = load("javascript:strikethrough()")

    /**
     * Method to apply sub/superscript to current text or the next at the cursor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param style String available styles ['sub', 'super', '']
     */
    private fun script(style: String) = load("javascript:script('$style')")

    /**
     * Method to change the background color of current text selection or the next at the cursor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param color String
     */
    private fun backColor(color: String) = load("javascript:background('$color')")

    /**
     * Method to change the foreground color of current text selection or the next at the cursor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param color String
     */
    private fun foreColor(color: String) = load("javascript:color('$color')")

    /**
     * Method to change font size to current text or the next at the cursor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param size String Available font sizes ['small', 'large', 'huge', '']
     */
    private fun fontSize(size: String) = load("javascript:fontSize('$size')")

    // endregion

    // region Paragraph

    /**
     * Method to align the current paragraph
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param style String Available styles ['center', 'right', 'justify', '']
     */
    private fun align(style: String) = load("javascript:align('$style')")

    /**
     * Method to insert an ordered list
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun insertOrderedList() = load("javascript:insertOrderedList()")

    /**
     * Method to insert an un-ordered list
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun insertUnorderedList() = load("javascript:insertUnorderedList()")

    /**
     * Method to insert a check list
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun insertCheckList() = load("javascript:insertCheckList()")

    /**
     * Method to indent the current paragraph
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun indent() = load("javascript:indent()")

    /**
     * Method to outdent the current paragraph
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun outdent() = load("javascript:outdent()")

    /**
     * Method to format the current paragraph into a blockquote
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun formatBlockquote() = load("javascript:formatBlock('blockquote')")

    /**
     * Method to format the current paragraph into a code block
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun formatBlockCode() = load("javascript:formatBlock('pre')")

    /**
     * Method to get the current selection and do (any) actions on the result
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String>? action to do
     */
    fun getSelection(callback: ValueCallback<String>? = null) =
        load("javascript:getSelection()", callback)

    /**
     * Method to get the current style at the cursor and do (any) actions on it
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String>? action to do
     */
    private fun getStyle(callback: ValueCallback<String>? = null) =
        load("javascript:getStyle()", callback)

    /**
     * Private method to get the HTML content and do (any) actions on the result
     * This method is to be utilized by the public ones
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callBack ValueCallback<String> action to do
     */
    // Correcting javascript method name from `getHtmlContent()' to 'getHtml()'
    // private fun getHtmlContent(callBack: ValueCallback<String>) = load("javascript:getHtmlContent()", callBack)
    private fun getHtmlContent(callBack: ValueCallback<String>) =
        load("javascript:getHtml()", callBack)

    /**
     * Method to get the HTML content and do (any) actions on the result
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String> action to do
     */
    fun getHtmlContent(callback: ((html: String) -> Unit)? = null) =
        getHtmlContent(ValueCallback { html ->
            val escapedData = html
                // There a bug? that the returned result has the unicode for < instead of the  char
                // and has double \\. So we are escaping them here
                .replace(oldValue = "\\u003C", newValue = "<")
                .replace(oldValue = "\\\"", newValue = "\"")
            callback?.invoke(
                escapedData.substring(
                    startIndex = 1,
                    endIndex = escapedData.length - 1
                )
            )
        })

    /**
     * Method to allow setting the HTML content to the editor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param htmlContent String
     * @param replaceCurrentContent Boolean set to true replace the whole content, false to concatenate
     */
    fun setHtmlContent(
        htmlContent: String,
        replaceCurrentContent: Boolean = true
    ) = load("javascript:setHtml('$htmlContent', $replaceCurrentContent)")

    /**
     * Method to get the HTML content and do (any) actions on the result - Java version
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String> action to do
     */
    fun getHtmlContent(callback: OnHtmlReturned) = getHtmlContent { callback.process(it) }

    /**
     * Private method to get the HTML content and do (any) actions on the result
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String> action to do
     */
    private fun getText(callback: ValueCallback<String>) = load("javascript:getText()", callback)

    /**
     * Method to get the text content and do (any) actions on the result
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String> action to do
     */
    fun getText(callback: ((text: String) -> Unit)?) = getText(ValueCallback {
        callback?.invoke(it.substring(1, it.length - 1).replace("\\n", "\n"))
    })

    /**
     * Method to get the text content and do (any) actions on the result - Java version
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String> action to do
     */
    fun getText(callback: OnTextReturned) = getText { callback.process(it) }

    /**
     * Private method to get the delta content and do (any) actions on the result
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String> action to do
     */
    private fun getContents(callback: ValueCallback<String>) =
        load("javascript:getContents()", callback)

    /**
     * Method to get the delta content and do (any) actions on the result
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String> action to do
     */
    fun getContents(callback: ((text: String) -> Unit)?) =
        getContents(ValueCallback { callback?.invoke(it) })

    /**
     * Method to get the delta content and do (any) actions on the result - Java version
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param callback ValueCallback<String> action to do
     */
    fun getContents(callback: OnContentsReturned) = getContents { callback.process(it) }

    /**
     * Method to set the delta content to the editor
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param data String delta content as string
     */
    fun setContents(data: String) = load("javascript:setContents($data)")

    /**
     * Method to format the current paragraph to the header styles
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param level Int 1 -> 6
     */
    private fun header(level: Int) = load("javascript:header($level)")

    /**
     * Method to toggle the code view to the current paragraph
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun codeView() = load("javascript:codeView()")

    // endregion

    // region Advanced formatting

    /**
     * Method to allow inserting an image by url to a specific selection index
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param index Int
     * @param url String
     */
    private fun insertImage(index: Int, url: String) =
        load("javascript:insertEmbed($index, 'image', '$url')")

    /**
     * Method to allow inserting an image as base 64 data to a specific selection index
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param index Int
     * @param path String path of the image to be converted to base 64
     */
    private fun insertImageB64(index: Int, path: String) = GlobalScope.launch(Dispatchers.Default) {
        val type = path.split('.').last().uppercase(Locale.getDefault())
        val bitmap = BitmapFactory.decodeFile(path)
        val stream = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
        }
        val encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        withContext(Dispatchers.Main) {
            load("javascript:insertEmbed($index, 'image', 'data:image/${type.lowercase(Locale.getDefault())};base64, $encodedImage')")
        }
    }

    /**
     * Method to allow inserting a hyperlink
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param linkUrl String
     */
    private fun createLink(linkUrl: String) = load("javascript:createLink('$linkUrl')")

    // fun insertTable(colCount: Int, rowCount: Int) = load("javascript:insertTable('${colCount}x$rowCount')")

    /**
     * Method to insert a horizontal line
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     */
    private fun insertHorizontalRule() = load("javascript:insertHorizontalRule()")

    // endregion

    // endregion

    /**
     * Method to evaluate a script and do action on result via the callback
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param trigger String script to be evaluated
     * @param callBack ValueCallback<String>?
     */
    private fun load(trigger: String, callBack: ValueCallback<String>? = null) {
        Handler(mWebView.context.mainLooper).post {
            // Make sure every calls would be run on ui thread
            mWebView.evaluateJavascript(trigger, callBack)
        }
    }

    /**
     * A bridge between Jvm api and JS api
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @param mActionType Int type of calling action
     * @param options Array<out Any>
     */
    fun command(mActionType: EditorButton, vararg options: Any) {
        when (mActionType) {
            EditorButton.UNDO -> undo()
            EditorButton.REDO -> redo()
            EditorButton.BOLD -> bold()
            EditorButton.ITALIC -> italic()
            EditorButton.UNDERLINE -> underline()
            EditorButton.SUBSCRIPT -> script("sub")
            EditorButton.SUPERSCRIPT -> script("super")
            EditorButton.STRIKETHROUGH -> strikethrough()
            EditorButton.NORMAL -> header(0)
            EditorButton.H1 -> header(1)
            EditorButton.H2 -> header(2)
            EditorButton.H3 -> header(3)
            EditorButton.H4 -> header(4)
            EditorButton.H5 -> header(5)
            EditorButton.H6 -> header(6)
            EditorButton.JUSTIFY_LEFT -> align("")
            EditorButton.JUSTIFY_CENTER -> align("center")
            EditorButton.JUSTIFY_RIGHT -> align("right")
            EditorButton.JUSTIFY_FULL -> align("justify")
            EditorButton.ORDERED -> insertOrderedList()
            EditorButton.UNORDERED -> insertUnorderedList()
            EditorButton.CHECK -> insertCheckList()
            EditorButton.INDENT -> indent()
            EditorButton.OUTDENT -> outdent()
            EditorButton.LINE -> insertHorizontalRule()
            EditorButton.BLOCK_QUOTE -> formatBlockquote()
            EditorButton.BLOCK_CODE -> formatBlockCode()
            EditorButton.CODE_VIEW -> codeView()
            EditorButton.LINK -> try {
                createLink(options[0] as String)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    mWebView.context,
                    mWebView.context.getString(R.string.wrong_params),
                    Toast.LENGTH_SHORT
                ).show()
            }
            EditorButton.IMAGE -> getSelection {
                try {
                    // Check params
                    if (options.size < 2) {
                        Toast.makeText(
                            mWebView.context,
                            mWebView.context.getString(R.string.missing_params),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val selection = Gson().fromJson<Map<String, Int>>(it)
                        // BASE64 mode and URL mode
                        if (options[0] as Boolean) insertImageB64(
                            selection["index"]!!,
                            options[1] as String
                        )
                        else insertImage(selection["index"]!!, options[1] as String)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        mWebView.context,
                        mWebView.context.getString(R.string.wrong_params),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            EditorButton.SIZE -> fontSize(options[0] as String)
            EditorButton.FORE_COLOR -> foreColor(options[0] as String)
            EditorButton.BACK_COLOR -> backColor(options[0] as String)
        }
    }

    /**
     * Method to return if the current selection is a link
     *
     * @author ebolo (daothanhduy305@gmail.com)
     * @since 0.0.1
     *
     * @return Boolean
     */
    fun selectingLink() = !currentFormat.link.isNullOrBlank()
}
