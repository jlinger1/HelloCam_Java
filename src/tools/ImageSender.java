package tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageSender
{
  private static final byte START_OF_IMAGE = (byte) 0x80;
  private static final byte END_OF_IMAGE = (byte) 0x40;
  private static final byte FIRST_PACKET = (byte) 0x20;
  private static final byte COMMAND_MODE = (byte) 0x0f;

  public static void main(String[] args)
  {
    final int PORT_NUMBER = 1235;
    final int HEADER_SIZE = 9;
    final int PACKET_SIZE = 1472;
    String fileName = "E:\\working\\mlinger\\EclipseWorkspace\\arduino_video\\image.jpg";
    try(FileInputStream is = new FileInputStream(fileName))
    {
      byte[] buffer = new byte[PACKET_SIZE];
      DatagramSocket socket = new DatagramSocket();
      //InetAddress address = InetAddress.getByName("localhost");
      InetAddress address = InetAddress.getByName("127.0.0.1");
      int imageNumber = 1;
      int packetNumber = 1;
      byte mode = 0x05;
      mode |= START_OF_IMAGE;
      mode |= FIRST_PACKET;
      for(int num = is.read(buffer, HEADER_SIZE, PACKET_SIZE-HEADER_SIZE); num > 0; num = is.read(buffer, HEADER_SIZE, PACKET_SIZE-HEADER_SIZE))
      {        
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(packetNumber++);
        header.putInt(imageNumber);
        
        if(is.available() == 0)
          mode |= END_OF_IMAGE;
        
        header.put(mode);
        header.rewind();
        header.get(buffer, 0, HEADER_SIZE);
        DatagramPacket packet = new DatagramPacket(buffer, num + HEADER_SIZE, address, PORT_NUMBER);
        socket.send(packet);
        //System.out.println("sent " + (num + HEADER_SIZE) + " bytes");
        
        mode &= ~FIRST_PACKET;
        mode &= ~START_OF_IMAGE;                
      }
      
    } catch (FileNotFoundException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    System.out.println("exit");
  }

}
