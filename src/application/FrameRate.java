package application;

import javafx.beans.property.SimpleDoubleProperty;

public class FrameRate
{
  private double milliseconds = 0;
  private long[] timeStamps;
  private int head = 0;
  private int tail = 0;
  private SimpleDoubleProperty frameRate;
  private int totalFrames = 0;
  
  public FrameRate(long milliseconds, int numSamples)
  {
    this.milliseconds = milliseconds;
    timeStamps = new long[numSamples];
    frameRate = new SimpleDoubleProperty();
  }
  
  public void reset()
  {
    head = tail;
    totalFrames = 0;
    frameRate.set(0);
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

  public SimpleDoubleProperty frameRateProperty()
  {
    return frameRate;
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
        totalFrames--;
        head = (head+1)%timeStamps.length;        
      }
      else
      {
        // no more old data found
        return;
      }
    }
  }
  
  public void addFrame() throws Exception
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
    timeStamps[tail] = currentTime;    
    tail = nextTail;
    totalFrames++;
    
    // update the bandwidth
    long duration = totalTime();
    if(duration == 0)
    {
      frameRate.set(0);
    }
    else
    {
      frameRate.set(1000.0 * totalFrames / duration);
    }    
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    int curr = head;
    while(curr != tail)
    {
      sb.append(String.format("%d\n", timeStamps[curr]));
      curr = (curr+1)%timeStamps.length;
    }
    return sb.toString();
  }
  
  public static void main(String[] args) throws Exception
  {
    FrameRate fr = new FrameRate(10000, 20);
    fr.frameRateProperty().addListener(e -> {
      System.out.println(e);
    });
    
    for(int i=0; i<20; i++)
    {  
      fr.addFrame();
      System.out.println("-------------------------------------------");
      System.out.print(fr.toString());
      Thread.sleep(1000);
    }

  }//public static void main(String[] args) throws Exception

}
