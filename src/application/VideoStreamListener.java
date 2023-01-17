package application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

public class VideoStreamListener implements Runnable
{
  private static int DATA_PORT = 1235;

  // 32-bit packet number
  // 32-bit image number
  // 8-bit status
  // bit 7 : first packet of image flag
  // bit 6 : last packet of image flag
  // bit 5 : first packet since being commanded
  // bits 3 downto 0 : camera mode
  private static final int HEADER_SIZE = 9;
  private static final byte START_OF_IMAGE = (byte) 0x80;
  private static final byte END_OF_IMAGE = (byte) 0x40;
  private static final byte FIRST_PACKET = (byte) 0x20;
  private static final byte COMMAND_MODE = (byte) 0x0f;
  private static final long BANDWIDTH_WINDOW_MILLISECONDS = 4000;
  private static final int MAX_BANDWIDTH_SAMPLES = 1024;
  private static final long FRAMERATE_WINDOW_MILLISECONDS = 5000;
  private static final int MAX_FRAMERATE_SAMPLES = 128;

  private SimpleIntegerProperty packetNumber;
  private SimpleIntegerProperty imageNumber;
  private FrameRate frameRate;
  private Bandwidth bandwidth;

  private SimpleIntegerProperty droppedPackets;
  private SimpleIntegerProperty droppedFrames;

  private SimpleObjectProperty<Image> image;
  private SimpleIntegerProperty resolution;
  private long startTime = 0;
  private int numFrames = 0;
  private byte[] lastImage = null;

  private boolean running;

  public VideoStreamListener()
  {
    packetNumber = new SimpleIntegerProperty();
    imageNumber = new SimpleIntegerProperty();
    frameRate = new FrameRate(FRAMERATE_WINDOW_MILLISECONDS, MAX_FRAMERATE_SAMPLES);
    bandwidth = new Bandwidth(BANDWIDTH_WINDOW_MILLISECONDS, MAX_BANDWIDTH_SAMPLES);
    image = new SimpleObjectProperty<>();
    resolution = new SimpleIntegerProperty();
    droppedPackets = new SimpleIntegerProperty();
    droppedFrames = new SimpleIntegerProperty();
    running = false;
  }

  public SimpleIntegerProperty packetNumberProperty()
  {
    return packetNumber;
  }

  public SimpleIntegerProperty imageNumberProperty()
  {
    return imageNumber;
  }

  public SimpleDoubleProperty frameRateProperty()
  {
    return frameRate.frameRateProperty();
  }

  public SimpleDoubleProperty bandwidthProperty()
  {
    return bandwidth.bandwidthProperty();
  }

  public SimpleIntegerProperty droppedPacketsProperty()
  {
    return droppedPackets;
  }

  public SimpleIntegerProperty droppedFramesProperty()
  {
    return droppedFrames;
  }

  public SimpleObjectProperty<Image> imageProperty()
  {
    return image;
  }

  public SimpleIntegerProperty resolutionProperty()
  {
    return resolution;
  }
  
  public byte[] lastImageProperty() {
	  return lastImage;
  }
 
  public void stop()
  {
    running = false;
  }

  public void reset()
  {
    packetNumber.set(0);
    imageNumber.set(0);
    frameRate.reset();
    bandwidth.reset();
    droppedFrames.set(0);
    numFrames = 0;
    startTime = System.currentTimeMillis();
    lastImage = null;
  }

  @Override
  public void run()
  {
    running = true;
    ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();

    try (DatagramChannel d = DatagramChannel.open())
    {
      DatagramSocket s = d.socket();
      s.bind(new InetSocketAddress(DATA_PORT));

      // we know that the arduino can't send more than 2048 bytes in a UDP packet
      byte[] buffer = new byte[2048];

      while (running)
      {
        try
        {
          // get the packet
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
          s.receive(packet);

          // count the bytes received
          int length = packet.getLength();

          if (length < HEADER_SIZE)
          {
            // we received a bad packet
            System.out.println("received UDP packet of length " + length);
            droppedPackets.add(1);
            continue;
          }

          // parse the header
          ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
          header.order(ByteOrder.LITTLE_ENDIAN);
          header.put(buffer, 0, HEADER_SIZE);
          header.rewind();
          int packetNumber = header.getInt();
          int imageNumber = header.getInt();
          byte status = header.get();

          // System.out.println("packetNumber=" + packetNumber + ", imageNumber=" +
          // imageNumber + ", status=" + Integer.toHexString(0xff & status));

          boolean startOfImage = (status & START_OF_IMAGE) == START_OF_IMAGE;
          boolean endOfImage = (status & END_OF_IMAGE) == END_OF_IMAGE;
          boolean firstPacket = (status & FIRST_PACKET) == FIRST_PACKET;
          int mode = status & COMMAND_MODE;
          this.resolution.set(mode);

          // check to see if this is the first packet and reset the statistics
          if (firstPacket)
            reset();

          // check for dropped packets
          if (this.packetNumber.get() + 1 < packetNumber)
          {
            // update the number of dropped packets and images
            this.droppedPackets.add(packetNumber - this.packetNumber.get() - 1);
            this.droppedFrames.add(imageNumber - this.imageNumber.get() + 1);

            // if this isn't the start of an image, then we are missing data.
            // we will have to wait for the next image
            if (!startOfImage)
            {
              this.imageNumber.set(imageNumber + 1);
            }
          }

          this.packetNumber.set(packetNumber);

          if (startOfImage)
          {
            // System.out.println("start of image");
            imageBuffer = new ByteArrayOutputStream();
            this.imageNumber.set(imageNumber);
          }
          if (this.imageNumber.get() == imageNumber)
            imageBuffer.write(buffer, HEADER_SIZE, length - HEADER_SIZE);

          // get the current time
          long currentTime = System.currentTimeMillis();

          if (endOfImage)
          {
            // System.out.println("end of image");
            image.set(new Image(new ByteArrayInputStream(imageBuffer.toByteArray())));
            numFrames++;
            
            lastImage = imageBuffer.toByteArray();
            // FileOutputStream out = new FileOutputStream(String.format("image%04d.jpg",
            // numFrames));
            // out.write(imageBuffer.toByteArray());
            // out.close();

            // only compute the frame rate when we get a new frame
            frameRate.addFrame();
          }

          // compute the bandwidth on every packet
          bandwidth.addPacket(length);

        } catch (Exception e)
        {
          System.out.println("VideoStreamListener " + e.toString());
        }
      } // while(running)

      System.out.println("VideoStreamListener exit");

    } catch (IOException e1)
    {
      e1.printStackTrace();
    }

  }

}
