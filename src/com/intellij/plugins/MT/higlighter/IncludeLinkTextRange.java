package com.intellij.plugins.MT.higlighter;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.MT.util.FileUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: berkel
 * Date: 15.10.11
 * Time: 18:46
 */
public class IncludeLinkTextRange {

	public static final TextAttributes DEFAULT_COMMENT_ATTRIBUTES = SyntaxHighlighterColors.LINE_COMMENT.getDefaultAttributes();

	public static TextAttributes INCLUDE_ATTRIBUTES = TextAttributes.merge(DEFAULT_COMMENT_ATTRIBUTES, new TextAttributes(null, null, DEFAULT_COMMENT_ATTRIBUTES.getForegroundColor(), EffectType.LINE_UNDERSCORE, Font.BOLD));

	private static final Logger LOG = Logger.getInstance(IncludeLinkTextRange.class.getName());

	private final Project project;
	private final PsiFile file;
	private final VirtualFile virtualFile;
	private int startOffset;
	private int endOffset;
	private final String fileRelativePath;
	private boolean active;
	private int hash = 0;
	private RangeHighlighter rangeHighlighter = null;

	private static final Key<IncludeLinkTextRange> INCLUDE_LINK_HIGHLIGHTER_KEY = Key.create("IncludeLinkHighlighter");

	public IncludeLinkTextRange(@NotNull final Project project, final PsiFile file, final int startOffset, final int endOffset, final String fileRelativePath, final boolean isActive) {
		this.project = project;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.fileRelativePath = fileRelativePath;
		this.active = isActive;
		this.file = file;
		this.virtualFile = FileUtil.findVirtualFileByPsiFile(file, fileRelativePath);
	}

	public VirtualFile getVirtualFile() {
		return virtualFile;
	}

	public String getFilePath() {
		return virtualFile != null && virtualFile.isValid() ? virtualFile.getPath() : null;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public void setActive(final boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public void addLinkHighlighter(final Editor editor) {
		MarkupModel markupModel = editor.getMarkupModel();
		if (rangeHighlighter != null) {
			markupModel.removeHighlighter(rangeHighlighter);
		}
		rangeHighlighter = markupModel.addRangeHighlighter(this.getStartOffset(), this.getEndOffset(), HighlighterLayer.WARNING - 1, INCLUDE_ATTRIBUTES, HighlighterTargetArea.EXACT_RANGE);
		rangeHighlighter.putUserData(INCLUDE_LINK_HIGHLIGHTER_KEY, this);
	}

	public void removeLinkHighlighter(final Editor editor) {
		MarkupModel markupModel = editor.getMarkupModel();
		if (rangeHighlighter != null) {
			markupModel.removeHighlighter(rangeHighlighter);
			rangeHighlighter = null;
		}
	}

	public static IncludeLinkTextRange getFrom(final RangeHighlighter rangeHighlighter) {
		return rangeHighlighter.getUserData(INCLUDE_LINK_HIGHLIGHTER_KEY);
	}

	public void shift(final int shiftOffset) {
		startOffset += shiftOffset;
		endOffset += shiftOffset;
		hash += 2 * shiftOffset;
	}

	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof IncludeLinkTextRange)) {
			return false;
		}

		final IncludeLinkTextRange that = (IncludeLinkTextRange) o;

		if (endOffset != that.endOffset) {
			return false;
		}
		if (startOffset != that.startOffset) {
			return false;
		}
		if (!fileRelativePath.equals(that.fileRelativePath)) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		if (hash == 0) {
			result = startOffset;
			result = 31 * result + endOffset;
			result = 31 * result + fileRelativePath.hashCode();
			hash = result;
		}
		return hash;
	}
}
