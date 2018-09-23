// Copyright Apr 2018-present boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.model

import com.paper.model.command.AddScrapCommand
import com.paper.model.command.UpdateScrapFrameCommand
import com.paper.model.command.WhiteboardCommand
import com.paper.model.command.WhiteboardCommandJSONTranslator.Companion.KEY_COMMAND_ID
import com.paper.model.command.WhiteboardCommandJSONTranslator.Companion.KEY_FROM_FRAME
import com.paper.model.command.WhiteboardCommandJSONTranslator.Companion.KEY_SCRAP
import com.paper.model.command.WhiteboardCommandJSONTranslator.Companion.KEY_SIGNATURE
import com.paper.model.command.WhiteboardCommandJSONTranslator.Companion.KEY_TO_FRAME
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner.Silent::class)
class WhiteboardCommandJSONTranslatorTest : BaseModelTest() {

    @Test
    fun `serialize "AddScrapCommand" with svg scrap`() {
        val candidate = jsonTranslator

        val scrap = createRandomSVGScrap()
        val command = AddScrapCommand(scrap = scrap)

        val jsonText = candidate.toJson(command, WhiteboardCommand::class.java)

        System.out.println(jsonText)

        Assert.assertTrue(jsonText.contains("\"$KEY_SIGNATURE\":\"AddScrapCommand\""))
        Assert.assertTrue(jsonText.contains("\"$KEY_COMMAND_ID\":\"${command.commandID}\""))
        Assert.assertTrue(jsonText.contains("\"$KEY_SCRAP\":{"))
    }

    @Test
    fun `deserialize "AddScrapCommand" with svg scrap`() {
        val jsonText = "{\"signature\":\"AddScrapCommand\",\"command_id\":\"5320dc29-7ceb-4b45-8b47-51bcf25caf2b\",\"scrap\":{\"uuid\":\"bfa87067-4546-478c-881c-24550390116c\",\"x\":0.24998796,\"y\":0.5076229,\"width\":180.0,\"height\":227.0,\"z\":496,\"scaleX\":2.0,\"scaleY\":2.0,\"rotationInDegrees\":67.0,\"type\":\"${ScrapType.SKETCH}\",\"svg\":{}}}"

        val candidate = jsonTranslator
        val command = candidate.fromJson<WhiteboardCommand>(jsonText, WhiteboardCommand::class.java)

        Assert.assertTrue(command is AddScrapCommand)
        Assert.assertEquals(UUID.fromString("5320dc29-7ceb-4b45-8b47-51bcf25caf2b"), command.commandID)
    }

    @Test
    fun `serialize "UpdateScrapFrameCommand"`() {
        val candidate = jsonTranslator

        val scrap = createRandomScrap()
        val command = UpdateScrapFrameCommand(scrapID = scrap.id,
                                              fromFrame = scrap.frame,
                                              toFrame = Frame(x = -100f,
                                                              y = 50f))

        val jsonText = candidate.toJson(command, WhiteboardCommand::class.java)

        System.out.println(jsonText)

        Assert.assertTrue(jsonText.contains("\"$KEY_SIGNATURE\":\"UpdateScrapFrameCommand\""))
        Assert.assertTrue(jsonText.contains("\"$KEY_COMMAND_ID\":\"${command.commandID}\""))
        Assert.assertTrue(jsonText.contains("\"$KEY_FROM_FRAME\":{"))
        Assert.assertTrue(jsonText.contains("\"$KEY_TO_FRAME\":{"))
    }
}
