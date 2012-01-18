package com.intellij.plugins.MT.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

/**
 * Created by IntelliJ IDEA.
 * User: berkel
 * Date: 17.10.11
 * Time: 12:03
 */
public final class FileUtil {

	public static VirtualFile findVirtualFileByPsiFile(PsiFile file, String fileName) {
		VirtualFile currentVirtualFile = file.getVirtualFile();
		if (currentVirtualFile != null && currentVirtualFile.isValid()) {
			VirtualFile dir = currentVirtualFile.getParent();
			if (dir != null && dir.isValid()) {
				VirtualFile virtualFile = dir.findFileByRelativePath(fileName);
				if (virtualFile != null && virtualFile.isValid()) {
					return virtualFile;
				}
			}
		}
		return null;
	}
}
