package application;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class VideoApp extends Application
{
  private static final int COMMAND_PORT = 1234;
  private static final int CONNECT_TIMEOUT = 3000;
  private static final int READ_TIMEOUT = 3000;

  private int bgImgCount = 0;
  private int fgImgCount = 0;

  private static void showCommand(PrintStream ps, String host, int port, byte[] command)
  {
    ps.print("Send (" + host + ":" + port + ") --> ");
    for (byte b : command)
      ps.printf("%02X ", 0xff & b);
    ps.println();
  }

  @Override
  public void start(Stage primaryStage) throws Exception
  {
    // load values from the configuration file
    PropertyManager.load();

    primaryStage.setTitle("VideoApp");
    BorderPane rootPane = new BorderPane();
    VideoPanel videoPanel = new VideoPanel();
    rootPane.setCenter(videoPanel);

    int height = Integer.parseInt(PropertyManager.thePropertyManager.get("height").toString());
    int width = Integer.parseInt(PropertyManager.thePropertyManager.get("width").toString());
    primaryStage.setScene(new Scene(rootPane, width, height));

    TextField hostIP = new TextField(PropertyManager.thePropertyManager.get("arduino").toString());
    String camera = PropertyManager.thePropertyManager.get("camera").toString();

    ComboBox comboBox;
    if (camera.equals("OV2640"))
    {
      comboBox = new ComboBox<OV2640>();
      comboBox.getItems().addAll(OV2640.values());
      comboBox.setValue(OV2640.OV2640_320x240);
    } else if (camera.equals("OV5642"))
    {
      comboBox = new ComboBox<OV5642>();
      comboBox.getItems().addAll(OV5642.values());
      comboBox.setValue(OV5642.OV5642_320x240);
    } else
      throw new Exception("configuration.properties camera not found or unsupported");

    Label hostLabel = new Label("Host IP");
    Label resolutionLabel = new Label("Resolution");
    Label packetSizeLabel = new Label("Packet Size");
    Spinner<Integer> packetSize = new Spinner<>(1024, 2048, 1);
    packetSize.setEditable(true);

    VideoStreamListener videoStreamListener = new VideoStreamListener();

    // reset button
    Button rstBtn = new Button();
    rstBtn.setText("Reset");
    rstBtn.setOnAction(new EventHandler<ActionEvent>()
    {

      @Override
      public void handle(ActionEvent event)
      {
        videoStreamListener.reset();
        fgImgCount = 0;
        bgImgCount = 0;
        /*
         * From desktop to arduino. 1-byte mode corresponding to OV5642 resolutions
         * 4-byte packet length. this is for the arduino-to-desktop UDP packet lengths.
         */
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        Object cameraMode = comboBox.getValue();
        if (cameraMode instanceof OV2640)
        {
          buffer.put((byte) ((OV2640) cameraMode).getMode());
        } else if (cameraMode instanceof OV5642)
        {
          buffer.put((byte) ((OV5642) cameraMode).getMode());
        }

        buffer.putInt(packetSize.getValue());
        byte[] command = buffer.array();
        showCommand(System.out, hostIP.getText(), COMMAND_PORT, command);

        try (Socket socket = new Socket())
        {
          socket.connect(new InetSocketAddress(hostIP.getText(), COMMAND_PORT), CONNECT_TIMEOUT);
          socket.setTcpNoDelay(false);
          socket.setSoTimeout(READ_TIMEOUT);
          // InputStream input = socket.getInputStream();
          // BufferedReader reader = new BufferedReader(new InputStreamReader(input));
          OutputStream output = socket.getOutputStream();
          output.write(command);
        } catch (Exception ex)
        {
          System.out.println("I/O error: " + ex.getMessage());
        }
      }
    });

    // bind the video panel image to the video stream listener image
    videoPanel.imageProperty().bind(videoStreamListener.imageProperty());

    Label packetLabel = new Label("Packet Number: ");
    Label packetNumber = new Label("");
    Label imageLabel = new Label("Image Number: ");
    Label imageNumber = new Label("");
    Label frameRateLabel = new Label("Frame Rate (Hz): ");
    Label frameRate = new Label("");
    Label bandwidthLabel = new Label("Band Width (KB/s): ");
    Label bandwidth = new Label("");
    Label modeLabel = new Label("Resolution: ");
    Label mode = new Label("");

    // background button
    Button bgBtn = new Button();
    bgBtn.setText("Background Image");
    bgBtn.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        if (videoStreamListener.lastImageProperty() == null)
        {
          return;
        }
        String fileName = "images/" + mode.getText() + "-bg-" + String.format("%04d", bgImgCount++) + ".jpg";
        try
        {
          FileOutputStream out = new FileOutputStream(fileName);
          out.write(videoStreamListener.lastImageProperty());
          out.close();
        } catch (IOException ex)
        {

          System.out.println("I/O error: " + ex.getMessage());
        }

      }

    });

    // foreground button
    Button fgBtn = new Button();
    fgBtn.setText("Foreground image");
    fgBtn.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        if (videoStreamListener.lastImageProperty() == null)
        {
          return;
        }
        String fileName = "images/" + mode.getText() + "-fg-" + String.format("%04d", fgImgCount++) + ".jpg";
        try
        {
          FileOutputStream out = new FileOutputStream(fileName);
          out.write(videoStreamListener.lastImageProperty());
          out.close();
        } catch (IOException ex)
        {

          System.out.println("I/O error: " + ex.getMessage());
        }
      }

    });

    videoStreamListener.packetNumberProperty().addListener(e ->
    {
      Platform.runLater(new Runnable()
      {

        @Override
        public void run()
        {
          packetNumber.setText("" + ((SimpleIntegerProperty) e).intValue());

        }
      });
    });

    videoStreamListener.imageNumberProperty().addListener(e ->
    {
      Platform.runLater(new Runnable()
      {

        @Override
        public void run()
        {
          imageNumber.setText("" + ((SimpleIntegerProperty) e).intValue());

        }
      });
    });

    videoStreamListener.frameRateProperty().addListener(e ->
    {
      Platform.runLater(new Runnable()
      {

        @Override
        public void run()
        {
          frameRate.setText(String.format("%3.3f", ((SimpleDoubleProperty) e).doubleValue()));
        }
      });
    });

    videoStreamListener.bandwidthProperty().addListener(e ->
    {
      Platform.runLater(new Runnable()
      {

        @Override
        public void run()
        {
          // convert from Bytes per second to KBytes per second
          bandwidth.setText(String.format("%3.2f", 0.001 * ((SimpleDoubleProperty) e).doubleValue()));
        }
      });
    });

    videoStreamListener.resolutionProperty().addListener(e ->
    {
      System.out.println(e);
      Platform.runLater(new Runnable()
      {
        @Override
        public void run()
        {
          Object cameraMode = comboBox.getValue();
          if (e == null)
          {
            mode.setText("null");
          } else if (cameraMode instanceof OV2640)
          {
            mode.setText(OV2640.valueOf(((SimpleIntegerProperty) e).get()).getName());
          } else if (cameraMode instanceof OV5642)
          {
            mode.setText(OV5642.valueOf(((SimpleIntegerProperty) e).get()).getName());
          }
        }
      });
    });

    GridPane topPane = new GridPane();
    topPane.hgapProperty().set(10);
    HBox hbox = new HBox();
    hbox.setAlignment(Pos.CENTER);
    hbox.getChildren().add(rstBtn);
    topPane.add(hbox, 0, 0);

    hbox = new HBox();
    hbox.setAlignment(Pos.CENTER);
    hbox.getChildren().add(hostLabel);
    hbox.getChildren().add(hostIP);
    topPane.add(hbox, 1, 0);

    hbox = new HBox();
    hbox.setAlignment(Pos.CENTER);
    hbox.getChildren().add(resolutionLabel);
    hbox.getChildren().add(comboBox);
    topPane.add(hbox, 2, 0);

    hbox = new HBox();
    hbox.setAlignment(Pos.CENTER);
    hbox.getChildren().add(packetSizeLabel);
    hbox.getChildren().add(packetSize);
    topPane.add(hbox, 3, 0);

    hbox = new HBox();
    hbox.setAlignment(Pos.CENTER);
    hbox.getChildren().add(bgBtn);
    topPane.add(hbox, 4, 0);

    hbox = new HBox();
    hbox.setAlignment(Pos.CENTER);
    hbox.getChildren().add(fgBtn);
    topPane.add(hbox, 5, 0);

    GridPane bottomPane = new GridPane();
    FlowPane pane = new FlowPane();
    pane.getChildren().add(packetLabel);
    pane.getChildren().add(packetNumber);
    bottomPane.add(pane, 0, 0);

    pane = new FlowPane();
    pane.getChildren().add(imageLabel);
    pane.getChildren().add(imageNumber);
    bottomPane.add(pane, 1, 0);

    pane = new FlowPane();
    pane.getChildren().add(frameRateLabel);
    pane.getChildren().add(frameRate);
    bottomPane.add(pane, 2, 0);

    pane = new FlowPane();
    pane.getChildren().add(bandwidthLabel);
    pane.getChildren().add(bandwidth);
    bottomPane.add(pane, 3, 0);

    pane = new FlowPane();
    pane.getChildren().add(modeLabel);
    pane.getChildren().add(mode);
    bottomPane.add(pane, 4, 0);

    rootPane.setTop(topPane);
    rootPane.setBottom(bottomPane);

    Thread thread = new Thread(videoStreamListener);
    thread.start();

    primaryStage.setOnCloseRequest(e ->
    {
      System.out.println(e);
      videoStreamListener.stop();
      thread.interrupt();
    });

    primaryStage.show();

  }

  public static void main(String[] args)
  {
    launch(args);
  }

}
