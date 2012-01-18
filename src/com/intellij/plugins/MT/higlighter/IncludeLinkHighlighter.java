package com.intellij.plugins.MT.higlighter;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: berkel
 * Date: 15.10.11
 * Time: 18:46
 */
public class IncludeLinkHighlighter {
	private final Project project;
	private final VirtualFile newFile;
	private final PsiFile psiFile;
	private final Editor editor;
	private final EditorIncludeLinkParser editorIncludeLinkParser;
	private final List<IncludeLinkTextRange> ranges = new ArrayList<IncludeLinkTextRange>();
	private EditorInputHandler inputEditorInputHandler = null;
	private DocumentAdapter docAdapter = null;
	private boolean isListening = false;
	private static final Logger LOG = Logger.getInstance(IncludeLinkHighlighter.class.getName());

	public IncludeLinkHighlighter(@NotNull final Project project, final VirtualFile newFile, final PsiFile psiFile, final Editor editor, EditorIncludeLinkParser editorIncludeLinkParser) {
		this.project = project;
		this.newFile = newFile;
		this.psiFile = psiFile;
		this.editor = editor;
		this.editorIncludeLinkParser = editorIncludeLinkParser;
	}

	public void stopListening() {
		if (isListening) {
			editor.removeEditorMouseListener(inputEditorInputHandler);
			editor.removeEditorMouseMotionListener(inputEditorInputHandler);
			editor.getContentComponent().removeKeyListener(inputEditorInputHandler);
			editor.getDocument().removeDocumentListener(docAdapter);
			isListening = false;
		}
	}

	public void removeAllRanges() {
		for (IncludeLinkTextRange range : ranges) {
			range.setActive(false);
		}
		highlightLink(ranges);
		ranges.clear();
	}

	public void startListeninig() {
		if (!isListening) {
			listenOnDocument();
			listenOnInput();
			isListening = true;
		}
	}

	public void reparseAll() {
		int length = editor.getDocument().getCharsSequence().length();
		parseDocumentRanges(0, length, length);
	}


	public void checkComments() {
		List<IncludeLinkTextRange> newRanges = new ArrayList<IncludeLinkTextRange>();
		for (IncludeLinkTextRange range : ranges) {
			boolean isComment = EditorIncludeLinkParser.isComment(psiFile, range.getStartOffset());
			if (isComment != range.isActive()) {
				range.setActive(isComment);
				newRanges.add(range);
			}
		}

		if (!newRanges.isEmpty()) {
			highlightLink(newRanges);
		}
	}

	private void listenOnInput() {
		inputEditorInputHandler = new EditorInputHandler(project, editor, psiFile, editorIncludeLinkParser);
		editor.getContentComponent().addKeyListener(inputEditorInputHandler);
		editor.addEditorMouseMotionListener(inputEditorInputHandler);
		editor.addEditorMouseListener(inputEditorInputHandler);
	}

	private void listenOnDocument() {
		final Document doc = editor.getDocument();
		docAdapter = new DocumentAdapter() {

			public void beforeDocumentChange(final DocumentEvent event) {
				if (event.getNewLength() < event.getOldLength()) { //deletion
					forgetDocumentRanges(event.getOffset(), event.getOffset() + event.getOldLength());
				}
				super.beforeDocumentChange(event);
			}

			public void documentChanged(final DocumentEvent event) {
				parseDocumentRanges(event.getOffset(), event.getOldLength(), event.getNewLength());
				super.documentChanged(event);
			}
		};

		doc.addDocumentListener(docAdapter);
		//@todo: implement unregistering listener

	}

	private void parseDocumentRanges(final int offset, final int oldLength, final int newLength) {
		List<IncludeLinkTextRange> newRanges = editorIncludeLinkParser.getIncludeLinkTextRange(psiFile, editor, getStartLineOffset(editor, offset), getEndLineOffset(editor, offset + newLength));
		List<IncludeLinkTextRange> rangesToForget = new ArrayList<IncludeLinkTextRange>();
		List<IncludeLinkTextRange> intersectingRanges = getIntersectingRanges(offset, oldLength);

		if (newLength != oldLength) {
			// shift ranges
			for (IncludeLinkTextRange range : ranges) {
				if (range.getStartOffset() >= offset) {
					range.shift(newLength - oldLength);
				}
			}
		}

		for (IncludeLinkTextRange irange : intersectingRanges) {
			boolean oldInNew = false;
			for (IncludeLinkTextRange newRange : newRanges) {
				if (irange.equals(newRange)) {
					oldInNew = true;
					break;
				}
			}

			if (!oldInNew) {
				rangesToForget.add(irange);
			}
		}

		if (!rangesToForget.isEmpty()) {
			for (IncludeLinkTextRange range : rangesToForget) {
				range.setActive(false);
				ranges.remove(range);
			}
			highlightLink(rangesToForget);
		}

		if (!newRanges.isEmpty()) {
			List<IncludeLinkTextRange> rangesToRemember = new ArrayList<IncludeLinkTextRange>();
			for (IncludeLinkTextRange range : newRanges) {
				if (!ranges.contains(range)) {
					rangesToRemember.add(range);
				}
			}

			if (!rangesToRemember.isEmpty()) {
				ranges.addAll(rangesToRemember);
				highlightLink(ranges);

			}
		}
	}

	private List<IncludeLinkTextRange> getIntersectingRanges(final int offset, final int oldLength) {
		List<IncludeLinkTextRange> intersecting = new ArrayList<IncludeLinkTextRange>();
		for (IncludeLinkTextRange range : ranges) {
			if (Math.max(range.getStartOffset(), offset) <= Math.min(range.getEndOffset(), offset + oldLength)) {
				intersecting.add(range);
			}
		}
		return intersecting;
	}

	public static int getEndLineOffset(final Editor editor, final int o) {
		final int textLength = editor.getDocument().getTextLength();
		int offset = Math.max(Math.min(o, Math.max(0, textLength)), Math.min(0, textLength));
		int lineCount = editor.getDocument().getLineCount();
		int lineNumber = 0;

		if (offset < 0) {
			lineNumber = 0;
		} else if (offset < textLength) {
			lineNumber = editor.getDocument().getLineNumber(offset);
		} else {
			lineNumber = lineCount - 1;
		}

		if (lineNumber >= 0 && lineNumber < lineCount) {
			return editor.getDocument().getLineEndOffset(lineNumber);
		} else {
			return 0;
		}
	}

	private void highlightLink(final List<IncludeLinkTextRange> rangesList) {
		MarkupModel markupModel = editor.getMarkupModel();
		for (RangeHighlighter h : markupModel.getAllHighlighters()) {
			IncludeLinkTextRange range = IncludeLinkTextRange.getFrom(h);
			if (range != null && rangesList.contains(range) && !range.isActive()) {
				markupModel.removeHighlighter(h);
			}
		}

		for (IncludeLinkTextRange urlTextRange : rangesList) {
			if (urlTextRange.isActive()) {
				urlTextRange.addLinkHighlighter(editor);
			}
		}
	}

	public static int getStartLineOffset(final Editor editor, final int o) {
		final int textLength = editor.getDocument().getTextLength();
		int offset = Math.max(Math.min(o, Math.max(0, textLength)), Math.min(0, textLength));
		int lineCount = editor.getDocument().getLineCount();
		int lineNumber = 0;

		if (offset < 0) {
			lineNumber = 0;
		} else if (offset < textLength) {
			lineNumber = editor.getDocument().getLineNumber(offset);
		} else {
			lineNumber = lineCount - 1;
		}

		if (lineNumber >= 0 && lineNumber < lineCount) {
			return editor.getDocument().getLineStartOffset(lineNumber);
		} else {
			return 0;
		}
	}

	private void forgetDocumentRanges(final int start, final int end) {
		List<IncludeLinkTextRange> forgetRangesList = new ArrayList<IncludeLinkTextRange>();
		for (IncludeLinkTextRange urlRange : ranges) {
			if (Math.max(start, urlRange.getStartOffset()) < Math.min(end, urlRange.getEndOffset())) {
				forgetRangesList.add(urlRange);
				urlRange.setActive(false);
			}

			if (!forgetRangesList.isEmpty()) {
				highlightLink(forgetRangesList);
			}

		}

		if (!forgetRangesList.isEmpty()) {
			ranges.removeAll(forgetRangesList);
		}
	}
}
