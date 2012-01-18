package com.intellij.plugins.MT.higlighter;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Created by IntelliJ IDEA.
 * User: berkel
 * Date: 15.10.11
 * Time: 18:46
 */
class EditorInputHandler extends KeyAdapter implements EditorMouseMotionListener, EditorMouseListener {
    private Point lastPointLocation = null;
    private final Project project;
    private final Editor editor;
    private final PsiFile file;
    private final EditorIncludeLinkParser editorIncludeLinkParser;
    private boolean handCursor = false;
    private static final Logger LOG = Logger.getInstance(EditorInputHandler.class.getName());

    private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final Cursor TEXT_CURSOR = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

    public EditorInputHandler(@NotNull Project project, @NotNull Editor editor, PsiFile file, EditorIncludeLinkParser editorIncludeLinkParser) {
        this.project = project;
        this.editor = editor;
        this.file = file;
        this.editorIncludeLinkParser = editorIncludeLinkParser;
    }

    private void updateCursor() {
        IncludeLinkTextRange hoverRange = editorIncludeLinkParser.getIncludeLinkTextRange(editor, file, lastPointLocation);
        JComponent component = editor.getContentComponent();
        if (hoverRange != null && hoverRange.isActive()) {
            component.setToolTipText(hoverRange.getFilePath());
            component.setCursor(HAND_CURSOR);
            handCursor = true;
        } else if (handCursor) {
            component.setCursor(TEXT_CURSOR);
            component.setToolTipText(null);
            handCursor = false;
        }
    }

    public void mouseMoved(final EditorMouseEvent e) {
        MouseEvent mouseEvent = e.getMouseEvent();
        if (mouseEvent.isControlDown()) {
            lastPointLocation = mouseEvent.getPoint();
            updateCursor();
        }
    }

    public void mouseClicked(final EditorMouseEvent e) {
        MouseEvent mouseEvent = e.getMouseEvent();
        if (mouseEvent.isControlDown() && mouseEvent.getClickCount() == 1) {
            IncludeLinkTextRange hoverRange = editorIncludeLinkParser.getIncludeLinkTextRange(editor, file, mouseEvent.getPoint());
            if (hoverRange != null && hoverRange.isActive()) {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                VirtualFile virtualFile = hoverRange.getVirtualFile();
                if (virtualFile != null) {
                    fileEditorManager.openFile(virtualFile, true);
                }
            }
        }
    }

    public void keyPressed(final KeyEvent e) {
    }

    public void keyReleased(final KeyEvent e) {
    }

    public void mouseDragged(final EditorMouseEvent e) {
    }

    public void mousePressed(final EditorMouseEvent e) {
    }

    public void mouseReleased(final EditorMouseEvent e) {
    }

    public void mouseEntered(final EditorMouseEvent e) {
    }

    public void mouseExited(final EditorMouseEvent e) {
    }
}
