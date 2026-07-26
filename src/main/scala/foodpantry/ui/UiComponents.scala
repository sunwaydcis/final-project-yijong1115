package foodpantry.ui

import scalafx.scene.Node
import scalafx.scene.control.{
  Button,
  Label,
  TitledPane
}

// ai-assisted: #45
// why: AI helped centralise repeated UI styling and accessible text patterns.
object UiComponents:

  def pageTitle(textValue: String): Label =
    new Label(textValue):
      styleClass += "page-title"

  def pageDescription(textValue: String): Label =
    new Label(textValue):
      wrapText = true
      styleClass += "page-description"

  def sectionTitle(textValue: String): Label =
    new Label(textValue):
      styleClass += "section-title"

  def fieldLabel(textValue: String): Label =
    new Label(textValue):
      styleClass += "form-label"

  def statusLabel(textValue: String): Label =
    new Label(textValue):
      wrapText = true
      styleClass += "status-label"

  def primaryButton(textValue: String): Button =
    new Button(textValue):
      styleClass += "primary-button"

  def secondaryButton(textValue: String): Button =
    new Button(textValue):
      styleClass += "secondary-button"

  def dangerButton(textValue: String): Button =
    new Button(textValue):
      styleClass += "danger-button"

  def formSection(
      titleValue: String,
      contentNode: Node
  ): TitledPane =
    new TitledPane:
      text = titleValue
      content = contentNode
      expanded = true
