package com.ebolo.kricheditor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ebolo.krichtexteditor.ui.enums.EditorButton
import com.ebolo.krichtexteditor.views.KRichEditorView
import com.ebolo.krichtexteditor.views.Options
import com.esafirm.imagepicker.features.ImagePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import io.paperdb.Paper
import org.jetbrains.anko.alert

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val editorView: KRichEditorView by lazy {
        findViewById(R.id.editor_view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editorView.initView(
            Options(
                onClickImageButton = { ImagePicker.create(this@MainActivity).start() },
                placeHolder = "Write something cool..",
                allowedButtons = listOf(
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
                ),
                onInitialized = {
                    // Simulate loading saved contents action
                    editorView.editor.setContents(
                        Paper.book("demo").read("content", "")
                    )
                },
                onClickAddUrl = { hasTextSelected ->
                    if (!hasTextSelected) {
                        Snackbar.make(
                            editorView.rootView,
                            com.ebolo.krichtexteditor.R.string.link_empty_warning,
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        // Setup dialog view
                        val inflatedView = LayoutInflater.from(this).inflate(
                            com.ebolo.krichtexteditor.R.layout.address_input_dialog,
                            parent as ViewGroup?,
                            false
                        )

                        val addressInput: TextInputEditText? = inflatedView.findViewById(
                            com.ebolo.krichtexteditor.R.id.address_input
                        )

                        MaterialAlertDialogBuilder(this).also {
                            it.setView(inflatedView)
                            it.setPositiveButton(android.R.string.ok) { _, _ ->
                                val urlValue = addressInput?.text.toString()
                                if (urlValue.startsWith("http://", true)
                                    || urlValue.startsWith("https://", true)
                                ) {
                                    editorView.addUrl(urlValue)
                                } else {
                                    Toast.makeText(
                                        this,
                                        com.ebolo.krichtexteditor.R.string.link_missing_protocol,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            it.setNegativeButton(android.R.string.cancel) { _, _ -> }
                        }.show()
                    }
                }
            )
        )

        editorView.editor.onTextChanged = {
            Log.d("MainActivity", "Content changed to: $it")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_html -> {
                editorView.editor.getHtmlContent { html ->
                    runOnUiThread { alert(message = html, title = "HTML").show() }
                }
                true
            }
            R.id.action_text -> {
                editorView.editor.getText { text ->
                    runOnUiThread { alert(message = text, title = "Text").show() }
                }
                true
            }
            R.id.action_set_html -> {
                editorView.editor.setHtmlContent("<strong>This is a test HTML content</strong>")
                true
            }
            R.id.action_save_content -> {
                editorView.editor.getContents { contents -> // String
                    Paper.book("demo").write("content", contents)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            val image = ImagePicker.getFirstImageOrNull(data)
            if (image != null) {
                // The second param (true/false) would not reflect BASE64 mode or not
                // Normal URL mode would pass the URL
                /*editorFragment.editor.command(IMAGE, false, "https://" +
                        "beebom-redkapmedia.netdna-ssl.com/wp-content/uploads/2016/01/" +
                        "Reverse-Image-Search-Engines-Apps-And-Its-Uses-2016.jpg")*/

                // For BASE64, image file path would be passed instead
                editorView.editor.command(EditorButton.IMAGE, true, image.path)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        // Simulate saving action
        editorView.editor.getContents { content -> Paper.book("demo").write("content", content) }
        super.onPause()
    }
}
