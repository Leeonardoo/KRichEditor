package com.ebolo.krichtexteditor.ui.enums

import com.ebolo.krichtexteditor.R

enum class EditorButton {
    NONE,

    // FONT
    FAMILY,
    SIZE,
    LINE_HEIGHT,
    FORE_COLOR,
    BACK_COLOR,

    // Format
    BOLD,
    ITALIC,
    UNDERLINE,
    SUBSCRIPT,
    SUPERSCRIPT,
    STRIKETHROUGH,

    // Style
    NORMAL,
    H1,
    H2,
    H3,
    H4,
    H5,
    H6,

    //Justify
    JUSTIFY_LEFT,
    JUSTIFY_CENTER,
    JUSTIFY_RIGHT,
    JUSTIFY_FULL,

    // List Style
    ORDERED,
    UNORDERED,
    CHECK,

    INDENT,
    OUTDENT,

    // Insert
    IMAGE,
    LINK,
    TABLE,
    LINE,

    BLOCK_QUOTE,
    BLOCK_CODE,

    CODE_VIEW,

    // Actions
    UNDO,
    REDO;

    companion object {
        val actionButtonDrawables by lazy {
            mapOf(
                UNDO to R.drawable.ic_undo,
                REDO to R.drawable.ic_redo,
                IMAGE to R.drawable.ic_insert_photo,
                LINK to R.drawable.ic_insert_link,
                BOLD to R.drawable.ic_format_bold,
                ITALIC to R.drawable.ic_format_italic,
                UNDERLINE to R.drawable.ic_format_underlined,
                STRIKETHROUGH to R.drawable.ic_format_strikethrough,
                SUBSCRIPT to R.drawable.ic_format_subscript,
                SUPERSCRIPT to R.drawable.ic_format_superscript,
                NORMAL to R.drawable.ic_format_para,
                H1 to R.drawable.ic_format_h1,
                H2 to R.drawable.ic_format_h2,
                H3 to R.drawable.ic_format_h3,
                H4 to R.drawable.ic_format_h4,
                H5 to R.drawable.ic_format_h5,
                H6 to R.drawable.ic_format_h6,
                INDENT to R.drawable.ic_format_indent_increase,
                OUTDENT to R.drawable.ic_format_indent_decrease,
                JUSTIFY_LEFT to R.drawable.ic_format_align_left,
                JUSTIFY_CENTER to R.drawable.ic_format_align_center,
                JUSTIFY_RIGHT to R.drawable.ic_format_align_right,
                JUSTIFY_FULL to R.drawable.ic_format_align_justify,
                ORDERED to R.drawable.ic_format_list_numbered,
                UNORDERED to R.drawable.ic_format_list_bulleted,
                CHECK to R.drawable.ic_format_list_check,
                LINE to R.drawable.ic_line,
                BLOCK_CODE to R.drawable.ic_code_block,
                BLOCK_QUOTE to R.drawable.ic_format_quote,
                CODE_VIEW to R.drawable.ic_code_review
            )
        }
    }
}