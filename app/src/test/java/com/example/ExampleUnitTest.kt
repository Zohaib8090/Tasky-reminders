package com.example

import com.example.data.model.AttachmentItem
import com.example.data.model.AttachmentType
import com.example.data.model.ChecklistItem
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAttachmentEncodingDecoding() {
    val items = listOf(
      AttachmentItem(
        type = AttachmentType.IMAGE,
        title = "IMG_2048.JPG",
        subtitle = "PNG • 2.4 MB • Added 10:32 AM",
        uriOrUrl = "sample://img_paint_palette"
      ),
      AttachmentItem(
        type = AttachmentType.VIDEO,
        title = "design_walkthrough.mp4",
        subtitle = "MP4 • 0:45 • Added 10:35 AM",
        uriOrUrl = "sample://img_interior_walkthrough",
        extraData = "0:45"
      ),
      AttachmentItem(
        type = AttachmentType.LINK,
        title = "Material 3 Expressive — Design Guidelines",
        subtitle = "https://developer.android.com/design/material/expressive",
        uriOrUrl = "https://developer.android.com/design/material/expressive",
        extraData = "Explore expressive shapes, dynamic color, and components for Android 16"
      ),
      AttachmentItem(
        type = AttachmentType.AUDIO,
        title = "voice_memo_1032.m4a",
        subtitle = "M4A • 1.2 MB • Added 10:38 AM",
        uriOrUrl = "sample://voice_memo_1032",
        extraData = "1:24 / 3:12"
      )
    )

    val encoded = AttachmentItem.encodeList(items)
    assertTrue(encoded.isNotBlank())

    val decoded = AttachmentItem.decodeList(encoded)
    assertEquals(4, decoded.size)
    assertEquals(AttachmentType.IMAGE, decoded[0].type)
    assertEquals("IMG_2048.JPG", decoded[0].title)
    assertEquals(AttachmentType.VIDEO, decoded[1].type)
    assertEquals("0:45", decoded[1].extraData)
    assertEquals(AttachmentType.LINK, decoded[2].type)
    assertEquals(AttachmentType.AUDIO, decoded[3].type)
  }

  @Test
  fun testChecklistEncodingDecoding() {
    val checklist = listOf(
      ChecklistItem(text = "Buy painting supplies", isDone = true),
      ChecklistItem(text = "Research color palette", isDone = false),
      ChecklistItem(text = "Sketch layout for living room", isDone = false)
    )

    val encoded = ChecklistItem.encodeList(checklist)
    val decoded = ChecklistItem.decodeList(encoded)

    assertEquals(3, decoded.size)
    assertTrue(decoded[0].isDone)
    assertEquals("Buy painting supplies", decoded[0].text)
    assertFalse(decoded[1].isDone)
    assertFalse(decoded[2].isDone)
  }
}

