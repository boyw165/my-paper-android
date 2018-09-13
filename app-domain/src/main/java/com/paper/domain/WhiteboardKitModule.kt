package com.paper.domain

import android.content.ContentResolver
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.domain.store.IWhiteboardStore
import com.paper.domain.store.WhiteboardStore
import com.paper.domain.ui.ScrapWidget
import com.paper.domain.ui.UndoWidget
import com.paper.domain.ui.manipulator.DragManipulator
import com.paper.model.Frame
import com.paper.model.Scrap
import com.paper.model.Whiteboard
import com.paper.model.command.WhiteboardCommand
import com.paper.model.command.WhiteboardCommandJSONTranslator
import com.paper.model.repository.CommandRepository
import com.paper.model.repository.ICommandRepository
import com.paper.model.repository.IWhiteboardRepository
import com.paper.model.repository.WhiteboardRepoSQLite
import com.paper.model.repository.json.FrameJSONTranslator
import com.paper.model.repository.json.ScrapJSONTranslator
import com.paper.model.repository.json.VectorGraphicsJSONTranslator
import com.paper.model.repository.json.WhiteboardJSONTranslator
import com.paper.model.sketch.VectorGraphics
import org.koin.dsl.module.module
import java.io.File

object WhiteboardKitModule {

    val module = module {

        single<Gson> {
            GsonBuilder()
                .registerTypeAdapter(Whiteboard::class.java,
                                     WhiteboardJSONTranslator())
                .registerTypeAdapter(Scrap::class.java,
                                     ScrapJSONTranslator())
                .registerTypeAdapter(Frame::class.java,
                                     FrameJSONTranslator())
                .registerTypeAdapter(VectorGraphics::class.java,
                                     VectorGraphicsJSONTranslator())
                .registerTypeAdapter(WhiteboardCommand::class.java,
                                     WhiteboardCommandJSONTranslator())
                .create()
        }

        factory<IWhiteboardRepository> { (authority: String,
                                             resolver: ContentResolver,
                                             bmpCacheDir: File) ->
            WhiteboardRepoSQLite(
                authority = authority,
                resolver = resolver,
                jsonTranslator = get(),
                bmpCacheDir = bmpCacheDir,
                prefs = get(),
                schedulers = get())
        }

        factory<IWhiteboardStore> { (documentID: Long) ->
            WhiteboardStore(whiteboardID = documentID,
                            whiteboardRepo = get(),
                            schedulers = get())
        }

        factory<ICommandRepository> { (logDir: File,
                                          logJournalFileName: String) ->
            CommandRepository(logDir = logDir,
                              logJournalFileName = logJournalFileName,
                              jsonTranslator = get(),
                              capacity = 60,
                              schedulers = get())
        }

        factory { (logDir: File) ->
            val undoRepo = CommandRepository(logDir = logDir,
                                             logJournalFileName = "_undoJournal",
                                             jsonTranslator = get(),
                                             capacity = 60,
                                             schedulers = get())
            val redoRepo = CommandRepository(logDir = logDir,
                                             logJournalFileName = "_redoJournal",
                                             jsonTranslator = get(),
                                             capacity = 60,
                                             schedulers = get())

            UndoWidget(undoRepo = undoRepo,
                       redoRepo = redoRepo,
                       schedulers = get())
        }

//        factory { (scrapWidget: ScrapWidget) ->
//            DragManipulator(scrapWidget = scrapWidget,
//                            whiteboardStore = get(),
//                            undoWidget = get(),
//                            schedulers = get())
//        }
    }
}
