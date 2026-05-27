package com.github.youjiyun1123.youregexinterpreter.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RegexToolWindowFactory : ToolWindowFactory {

    companion object {
        @Volatile
        private var lastInstance: RegexToolWindow? = null

        fun getInstance(): RegexToolWindow? = lastInstance
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val regexToolWindow = RegexToolWindow(project, toolWindow)
        lastInstance = regexToolWindow

        val content = ContentFactory.getInstance().createContent(
            regexToolWindow.getContent(),
            null,
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
