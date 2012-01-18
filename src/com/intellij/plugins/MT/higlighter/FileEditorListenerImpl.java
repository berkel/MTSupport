package com.intellij.plugins.MT.higlighter;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.HashMap;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: berkel
 * Date: 15.10.11
 * Time: 18:46
 */
public class FileEditorListenerImpl implements FileEditorManagerListener {
    private final Map<VirtualFile, IncludeLinkHighlighter> linkHighlighters = new HashMap<VirtualFile, IncludeLinkHighlighter>();
    private Project project;
    private boolean isRegistered;
    private EditorIncludeLinkParser editorIncludeLinkParser;
    private static final Logger LOG = Logger.getInstance(FileEditorListenerImpl.class.getName());
    private MessageBusConnection messageBusConnection;

    public FileEditorListenerImpl(@NotNull Project project) {
        this.project = project;
        this.editorIncludeLinkParser = new EditorIncludeLinkParser(project);
    }

    public void selectionChanged(final FileEditorManagerEvent event) {
        final FileEditorManager editorManager = event.getManager();
        if (project != editorManager.getProject()) {
            assert false : this;
            return;
        }

        VirtualFile newFile = event.getNewFile();
        VirtualFile oldFile = event.getOldFile();

        if (oldFile != null && newFile == null) {
            removeHighlighter(oldFile);
        } else if (newFile != null && !linkHighlighters.containsKey(newFile)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(newFile);
            if (psiFile != null) {
                Editor editor = editorManager.getSelectedTextEditor();
                if (editor != null) {
                    addHighlighter(newFile, psiFile, editor);
                }
            }
        }
    }

    public void projectClosed() {
        deactivate();
    }

    public void projectOpened() {
        activate();
        Task.Backgroundable task = new ScanningIncludeLinksTask(project, this);
        ProgressManager.getInstance().run(task);
    }

    public void deactivate() {
        if (isRegistered) {
            if (!project.isDisposed()) {
                messageBusConnection.disconnect();
            }
            isRegistered = false;
        }
    }

    public void activate() {
        if (!isRegistered) {
            messageBusConnection = project.getMessageBus().connect();
            messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
            isRegistered = true;
        }
    }

    private void addHighlighter(final VirtualFile newFile, final PsiFile psiFile, final Editor editor) {
        IncludeLinkHighlighter highlighter = new IncludeLinkHighlighter(project, newFile, psiFile, editor, editorIncludeLinkParser);
        highlighter.startListeninig();
        linkHighlighters.put(newFile, highlighter);
        highlighter.reparseAll();
        highlighter.checkComments();
    }

    private void removeHighlighter(final VirtualFile oldFile) {
        IncludeLinkHighlighter hl = linkHighlighters.get(oldFile);
        if (hl != null) {
            hl.stopListening();
            hl.removeAllRanges();
            linkHighlighters.remove(oldFile);
        }
    }

    public void scanOpenEditors() {
        final FileEditorManager editorManager = FileEditorManager.getInstance(project);
        if (editorManager == null) {
            return;
        }
        for (VirtualFile openFile : editorManager.getSelectedFiles()) {
            if (!linkHighlighters.containsKey(openFile)) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(openFile);
                if (psiFile != null) {
                    Editor editor = editorManager.getSelectedTextEditor();
                    if (editor != null) {
                        addHighlighter(openFile, psiFile, editor);
                    }
                }
            } else {
                linkHighlighters.get(openFile).reparseAll();
                linkHighlighters.get(openFile).checkComments();
            }
        }
    }

    public void removeAllLinkHighlighers() {
        for (VirtualFile vf : linkHighlighters.keySet()) {
            removeHighlighter(vf);
        }
        linkHighlighters.clear();
    }

    private class ScanningIncludeLinksTask extends Task.Backgroundable {
        private final FileEditorListenerImpl fileEditor;
        public ScanningIncludeLinksTask(@org.jetbrains.annotations.Nullable Project project, final FileEditorListenerImpl fileEditor) {
            super(project, "Scanning files for include", false);
            this.fileEditor = fileEditor;
        }
        public void run(@NotNull final ProgressIndicator progressIndicator) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fileEditor.scanOpenEditors();
                }
            });
        }
    }

    public void fileOpened(final FileEditorManager fileEditorManager, final VirtualFile virtualFile) {
    }

    public void fileClosed(final FileEditorManager fileEditorManager, final VirtualFile virtualFile) {
        removeHighlighter(virtualFile);
    }
}
