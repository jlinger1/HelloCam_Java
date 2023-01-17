package application;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class VideoPanel extends Pane
{
  private Canvas canvas;
  private static final Color BACKGROUND_GREY = new Color(47.0 / 255, 47.0 / 255, 47.0 / 255, 1.0);
  private SimpleObjectProperty<Image> image;
  public VideoPanel()
  {
    canvas = new Canvas();
    image = new SimpleObjectProperty<>();
    image.addListener(e -> update());
    this.getChildren().add(canvas);
    canvas.widthProperty().bind(this.widthProperty());
    canvas.heightProperty().bind(this.heightProperty());
    canvas.widthProperty().addListener(e -> update());
    canvas.heightProperty().addListener(e -> update());
  }
  
  public SimpleObjectProperty<Image> imageProperty()
  {
    return image;
  }
  
  private void update ()
  {
    GraphicsContext gc = canvas.getGraphicsContext2D();
    double width = this.getWidth();
    double height = this.getHeight();
    
    if(image.get() == null)
    {
      gc.clearRect(0, 0, width, height);
      gc.setFill(BACKGROUND_GREY);
      gc.fillRect(0, 0, width, height);
    }
    else
      gc.drawImage(image.get(), 0, 0, width, height);
  }  
}
