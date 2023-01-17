package application;

import javafx.beans.property.SimpleDoubleProperty;

public class Bandwidth
{
  private double milliseconds = 0;
  private long[] timeStamps;
  private int[] numBytes;
  private int head = 0;
  private int tail = 0;
  private SimpleDoubleProperty bandwidth;
  private int totalBytes = 0;
  
  public Bandwidth(long milliseconds, int numSamples)
  {
    this.milliseconds = milliseconds;
    timeStamps = new long[numSamples];
    numBytes = new int[numSamples];
    bandwidth = new SimpleDoubleProperty();
  }
  
  public void reset()
  {
    head = tail;
    totalBytes = 0;
    bandwidth.set(0);
  }
  
  private long totalTime()
  {
    // check to see if the queue is empty
    if(head == tail)
      return 0;
    
    int length = timeStamps.length;
    int last = (tail + length - 1) % length;
    return timeStamps[last] - timeStamps[head];
  }
  
  public SimpleDoubleProperty bandwidthProperty()
  {
    return bandwidth;
  }
  
  private void removeOldPackets(long currentTime)
  {
    // keep removing data until we exit the loop
    while(head != tail)
    {
      // check to see if this data is old
      if(timeStamps[head] + milliseconds < currentTime)
      {
        // remove old data from the total
        totalBytes -= numBytes[head];
        head = (head+1)%timeStamps.length;        
      }
      else
      {
        // no more old data found
        return;
      }
    }
  }
  
  public void addPacket(int size) throws Exception
  {
    // get the current time
    long currentTime = System.currentTimeMillis();
    
    // remove old data
    removeOldPackets(currentTime);
    
    // check for overflow
    int nextTail = (tail+1)%timeStamps.length;
    if(nextTail == head)
    {
      throw new Exception("overflow capacity " + timeStamps.length);
    }
    
    // keep this data
    numBytes[tail] = size;
    timeStamps[tail] = currentTime;    
    tail = nextTail;
    totalBytes += size;
    
    // update the bandwidth
    long duration = totalTime();
    if(duration == 0)
    {
      bandwidth.set(0);
    }
    else
    {
      bandwidth.set(1000.0 * totalBytes / duration);
    }    
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    int curr = head;
    while(curr != tail)
    {
      sb.append(String.format("%d:%04d\n", timeStamps[curr], numBytes[curr]));
      curr = (curr+1)%timeStamps.length;
    }
    return sb.toString();
  }

  public static void main(String[] args) throws Exception
  {
    Bandwidth bw = new Bandwidth(10000, 20);
    bw.bandwidthProperty().addListener(e -> {
      System.out.println(e);
    });
    
    for(int i=0; i<20; i++)
    {  
      bw.addPacket(100);
      System.out.println("-------------------------------------------");
      System.out.print(bw.toString());
      Thread.sleep(1000);
    }

  }//public static void main(String[] args) throws Exception

}
