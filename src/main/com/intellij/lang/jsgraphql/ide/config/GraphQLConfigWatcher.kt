package com.intellij.lang.jsgraphql.ide.config

import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.Alarm
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.ConcurrentHashMap


@Service(Service.Level.PROJECT)
class GraphQLConfigWatcher(private val project: Project) : Disposable {

    companion object {
        @JvmStatic
        fun getInstance(project: Project) = project.service<GraphQLConfigWatcher>()

        private const val SAVE_DOCUMENTS_TIMEOUT = 2000
    }

    private val configProvider = GraphQLConfigProvider.getInstance(project)

    private val documentsToSave = ConcurrentHashMap.newKeySet<WatchedFile>()
    private val documentsSaveAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    init {
        val connection: MessageBusConnection = project.messageBus.connect(this)

        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                ApplicationManager.getApplication().invokeLater {
                    configProvider.scheduleConfigurationReload()
                }
            }
        })

        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun before(events: MutableList<out VFileEvent>) {
                val fileIndex = ProjectFileIndex.getInstance(project)
                var configurationsChanged = false
                val watchedDirs = collectWatchedDirectories()

                for (event in events) {
                    if (configurationsChanged) break

                    if (event !is VFileDeleteEvent) {
                        continue
                    }

                    val file = event.file
                    if (!fileIndex.isInContent(file)) {
                        continue
                    }

                    if (file.isDirectory) {
                        if (file in watchedDirs || configProvider.findConfigFileInDirectory(file) != null) {
                            configurationsChanged = true
                        }
                    } else {
                        if (file.name in CONFIG_NAMES) {
                            configurationsChanged = true
                        }
                    }
                }

                if (configurationsChanged) {
                    configProvider.scheduleConfigurationReload()
                }
            }

            override fun after(events: List<VFileEvent>) {
                val fileIndex = ProjectFileIndex.getInstance(project)
                var configurationsChanged = false
                val watchedDirs = collectWatchedDirectories()

                for (event in events) {
                    if (configurationsChanged) break

                    if (event is VFileDeleteEvent) {
                        continue
                    }

                    val file = event.file ?: continue
                    if (!fileIndex.isInContent(file)) {
                        continue
                    }

                    if (file.isDirectory) {
                        if (file in watchedDirs ||
                            event is VFileCreateEvent && configProvider.findConfigFileInDirectory(file) != null
                        ) {
                            configurationsChanged = true
                        }
                    } else {
                        if (event is VFilePropertyChangeEvent) {
                            if (VirtualFile.PROP_NAME == event.propertyName) {
                                if (event.newValue is String && event.newValue in CONFIG_NAMES ||
                                    event.oldValue is String && event.oldValue in CONFIG_NAMES
                                ) {
                                    configurationsChanged = true
                                }
                            }
                        } else {
                            if (file.name in CONFIG_NAMES) {
                                configurationsChanged = true
                            }
                        }
                    }
                }

                if (configurationsChanged) {
                    configProvider.scheduleConfigurationReload()
                }
            }
        })

        val eventMulticaster = EditorFactory.getInstance().eventMulticaster
        eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val virtualFile = FileDocumentManager.getInstance().getFile(event.document)
                if (virtualFile != null && virtualFile !is LightVirtualFile && virtualFile.name in CONFIG_NAMES) {
                    documentsToSave.add(WatchedFile(virtualFile, event.document))
                    scheduleDocumentSave()
                }
            }
        }, this)
    }

    private fun collectWatchedDirectories() = configProvider.getAllConfigs()
        .asSequence()
        .map { it.dir }
        .filter { it.isDirectory }
        .toSet()

    private fun scheduleDocumentSave() {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            invokeLater { saveDocuments() }
        } else {
            documentsSaveAlarm.cancelAllRequests()
            documentsSaveAlarm.addRequest({
                BackgroundTaskUtil.runUnderDisposeAwareIndicator(this, ::saveDocuments)
            }, SAVE_DOCUMENTS_TIMEOUT)
        }
    }

    @RequiresEdt
    private fun saveDocuments() {
        if (documentsToSave.isEmpty()) {
            return
        }

        val documentManager = FileDocumentManager.getInstance()
        HashSet(documentsToSave)
            .also { documentsToSave.removeAll(it) }
            .filter { it.file.isValid }
            .forEach {
                ProgressManager.checkCanceled()
                documentManager.saveDocument(it.document)
            }
    }

    private data class WatchedFile(val file: VirtualFile, val document: Document)

    override fun dispose() {
    }
}
