package com.intellij.plugins.MT;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.MT.higlighter.FileEditorListenerImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: berkel
 * Date: 15.10.11
 * Time: 12:03
 */
public class MTProjectComponent implements ProjectComponent {

	private final Project project;
	private FileEditorListenerImpl fileEditorListener;
	private static final Logger LOG = Logger.getInstance(MTProjectComponent.class.getName());

	public MTProjectComponent(Project project) {
		this.project = project;
	}

	public void initComponent() {
		this.fileEditorListener = new FileEditorListenerImpl(project);
	}

	public void disposeComponent() {
	}

	@NotNull
	public String getComponentName() {
		return "MTProjectComponent";
	}

	public void projectOpened() {
		fileEditorListener.projectOpened();
	}

	public void projectClosed() {
		fileEditorListener.projectClosed();
	}
}
