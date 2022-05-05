package com.ebolo.krichtexteditor.ui.view

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class TextEditorWebView: WebView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onCheckIsTextEditor() = true

    fun initWebView(initCall: (TextEditorWebView) -> Unit) = initCall(this)
}