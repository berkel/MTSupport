package com.intellij.plugins.MT.higlighter;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.MT.util.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: berkel
 * Date: 15.10.11
 * Time: 18:46
 */
public final class EditorIncludeLinkParser {
    private static final Pattern INCLUDE_LINK_SEARCH_PATTERN = Pattern.compile("\\b(INCLUDE\\s+)([^\\s]+\\.(?:html|tpl))\\b");
    private final Project project;
    private static final Logger LOG = Logger.getInstance(EditorIncludeLinkParser.class.getName());

    EditorIncludeLinkParser(Project project) {
        this.project = project;
    }

    public List<IncludeLinkTextRange> getIncludeLinkTextRange(final PsiFile psiFile, final Editor editor, int startOffset, int endOffset) {
        if (endOffset <= startOffset) {
            return Collections.emptyList();
        }

        startOffset = inBounds(editor, startOffset);
        endOffset = inBounds(editor, endOffset);
        CharSequence text;

        try {
            text = editor.getDocument().getCharsSequence().subSequence(startOffset, endOffset);
        } catch (IndexOutOfBoundsException e) {
            return Collections.emptyList();
        }

        List<IncludeLinkTextRange> newRanges = getNewRanges(psiFile, text);
        for (IncludeLinkTextRange range : newRanges) {
            range.shift(startOffset);
        }

        return newRanges;
    }

    public static boolean isInstanceOfPsiDocToken(Object obj) {
        Class psiDocTokenClass = null;

        try {
            psiDocTokenClass = Class.forName("com.intellij.psi.javadoc.PsiDocToken");
        } catch (ClassNotFoundException e) {
            return false;
        }

        return psiDocTokenClass != null && psiDocTokenClass.isInstance(obj);
    }

    public static boolean isComment(final PsiFile psiFile, final int startOffset) {
        Boolean result = false;
        if (psiFile != null) {
            PsiElement element = psiFile.findElementAt(startOffset);
            if (element != null) {
                result = element instanceof XmlToken && ((XmlToken) element).getTokenType() == XmlTokenType.XML_COMMENT_CHARACTERS;
            }
        }
        return result;
    }

    public static boolean isFileValid(final PsiFile psiFile, final String filePath) {
        VirtualFile file = FileUtil.findVirtualFileByPsiFile(psiFile, filePath);
        return file != null && file.isValid();
    }

    private List<IncludeLinkTextRange> getNewRanges(final PsiFile psiFile, final CharSequence text) {
        List<IncludeLinkTextRange> ranges = new ArrayList<IncludeLinkTextRange>();
        Matcher matcher = INCLUDE_LINK_SEARCH_PATTERN.matcher(text);
        while (matcher.find()) {
            String filePath = matcher.group(2);
            if (isFileValid(psiFile, filePath)) {
                IncludeLinkTextRange range = new IncludeLinkTextRange(project, psiFile, matcher.start() + matcher.group(1).length(), matcher.end(), filePath, true);
                ranges.add(range);
            }
        }
        return ranges;
    }


    public IncludeLinkTextRange getIncludeLinkTextRange(final Editor editor, final PsiFile file, final Point point) {
        if (editor == null || point == null) {
            return null;
        }

        int offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(point));
        return getIncludeLinkTextRange(editor, file, offset);
    }

    private IncludeLinkTextRange getIncludeLinkTextRange(final Editor editor, final PsiFile file, final int offset) {
        if (file == null) {
            return null;
        }

        int length = editor.getDocument().getTextLength();
        if (length > 0 && offset < length) {
            int startLineOffset = IncludeLinkHighlighter.getStartLineOffset(editor, offset);
            int endLineOffset = IncludeLinkHighlighter.getEndLineOffset(editor, offset);

            CharSequence lineText = editor.getDocument().getCharsSequence().subSequence(startLineOffset, endLineOffset);
            List<IncludeLinkTextRange> newRanges = getNewRanges(file, lineText);
            if (!newRanges.isEmpty()) {
                for (IncludeLinkTextRange range : newRanges) {
                    if (startLineOffset + range.getStartOffset() <= offset && startLineOffset + range.getEndOffset() >= offset) {
                        range.setActive(isComment(file, offset));
                        return range;
                    }
                }
            }
        }

        return null;
    }

    private static int inBounds(@NotNull Editor editor, int offset) {
        int maxValue = editor.getDocument().getTextLength();
        return Math.max(Math.min(offset, Math.max(0, maxValue)), Math.min(0, maxValue));

    }

    public IncludeLinkTextRange getURLTextRange(DataContext context) {
        PsiFile file = LangDataKeys.PSI_FILE.getData(context);
        Editor editor = LangDataKeys.EDITOR.getData(context);
        if (editor == null || editor.isDisposed()) {
            return null;
        }
        return getIncludeLinkTextRange(editor, file, editor.getCaretModel().getOffset());
    }
}
